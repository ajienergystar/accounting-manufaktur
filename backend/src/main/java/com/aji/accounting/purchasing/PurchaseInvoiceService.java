package com.aji.accounting.purchasing;

import com.aji.accounting.audit.AuditAction;
import com.aji.accounting.audit.AuditService;
import com.aji.accounting.coa.Account;
import com.aji.accounting.coa.AccountMappingKey;
import com.aji.accounting.coa.AccountMappingService;
import com.aji.accounting.coa.AccountRepository;
import com.aji.accounting.coa.AccountService;
import com.aji.accounting.common.exception.BusinessException;
import com.aji.accounting.common.exception.FieldValidationException;
import com.aji.accounting.common.exception.NotFoundException;
import com.aji.accounting.common.sequence.DocType;
import com.aji.accounting.common.sequence.DocumentNumberService;
import com.aji.accounting.common.util.Money;
import com.aji.accounting.common.web.PageResponse;
import com.aji.accounting.config.CurrentUser;
import com.aji.accounting.inventory.DocStatus;
import com.aji.accounting.ledger.JournalDraft;
import com.aji.accounting.ledger.PostingService;
import com.aji.accounting.ledger.SourceType;
import com.aji.accounting.masterdata.department.DepartmentRepository;
import com.aji.accounting.masterdata.supplier.Supplier;
import com.aji.accounting.masterdata.supplier.SupplierRepository;
import com.aji.accounting.masterdata.tax.TaxRate;
import com.aji.accounting.masterdata.tax.TaxRateRepository;
import com.aji.accounting.masterdata.tax.TaxType;
import com.aji.accounting.period.PeriodService;
import com.aji.accounting.purchasing.PurchasingDtos.PiLineRequest;
import com.aji.accounting.purchasing.PurchasingDtos.PiRequest;
import com.aji.accounting.purchasing.PurchasingDtos.PiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Faktur pembelian dengan three-way matching (PO ↔ penerimaan ↔ faktur).
 * <pre>
 * Dr GRNI (biaya perolehan saat penerimaan)        Dr Beban/BOP (baris jasa)
 * Dr/Cr Selisih harga pembelian                    Dr PPN Masukan
 *      Cr Utang Usaha (total - PPh)                     Cr Utang PPh (PPh dipotong)
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class PurchaseInvoiceService {

    /** Toleransi selisih harga faktur vs harga penerimaan sebelum butuh persetujuan. */
    private static final BigDecimal PRICE_TOLERANCE_PCT = new BigDecimal("5");

    private final PurchaseInvoiceRepository invoices;
    private final GoodsReceiptRepository receipts;
    private final PurchaseReturnRepository returns;
    private final ApPaymentRepository payments;
    private final SupplierRepository suppliers;
    private final AccountRepository accounts;
    private final AccountService accountService;
    private final DepartmentRepository departments;
    private final TaxRateRepository taxRates;
    private final PostingService posting;
    private final PeriodService periods;
    private final AccountMappingService mappings;
    private final DocumentNumberService numbers;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<PiResponse> search(LocalDate from, LocalDate to, Long supplierId, PurchaseInvoice.Status status,
                                           String q, int page, int size) {
        var p = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Order.desc("docDate"), Sort.Order.desc("id")));
        return PageResponse.of(invoices.search(from, to, supplierId, status, PurchaseOrderService.blank(q), p),
                i -> PurchasingMapper.pi(i, false, null));
    }

    @Transactional(readOnly = true)
    public PiResponse get(Long id) {
        PurchaseInvoice inv = load(id);
        return PurchasingMapper.pi(inv, true, payments.findAllocationsForInvoice(id));
    }

    @Transactional(readOnly = true)
    public List<PiResponse> outstanding(Long supplierId) {
        return invoices.findOutstanding(supplierId).stream().map(i -> PurchasingMapper.pi(i, false, null)).toList();
    }

    @Transactional
    public PiResponse create(PiRequest req) {
        periods.assertOpen(req.docDate());
        Supplier supplier = suppliers.findById(req.supplierId()).orElseThrow(() -> NotFoundException.of("Pemasok", req.supplierId()));
        String supInvNo = req.supplierInvoiceNo().trim();
        if (invoices.existsBySupplierIdAndSupplierInvoiceNoIgnoreCaseAndStatusNot(supplier.getId(), supInvNo,
                PurchaseInvoice.Status.VOID)) {
            throw new FieldValidationException("supplierInvoiceNo", "Faktur " + supInvNo + " dari pemasok ini sudah dicatat");
        }

        PurchaseInvoice inv = new PurchaseInvoice();
        inv.setDocDate(req.docDate());
        inv.setSupplier(supplier);
        inv.setSupplierInvoiceNo(supInvNo);
        inv.setTaxInvoiceNo(req.taxInvoiceNo());
        inv.setDueDate(req.dueDate() != null ? req.dueDate() : req.docDate().plusDays(supplier.getPaymentTermDays()));
        if (inv.getDueDate().isBefore(inv.getDocDate())) {
            throw new FieldValidationException("dueDate", "Jatuh tempo tidak boleh sebelum tanggal faktur");
        }
        inv.setNotes(req.notes());
        TaxRate ppn = tax(req.taxRateId(), TaxType.PPN);
        TaxRate pph = tax(req.pphRateId(), null);
        if (pph != null && pph.getTaxType() == TaxType.PPN) {
            throw new BusinessException("Pajak potongan harus berjenis PPh");
        }
        inv.setTaxRateRef(ppn);
        inv.setTaxRate(ppn == null ? BigDecimal.ZERO : ppn.getRate());
        inv.setPphRateRef(pph);
        inv.setPphRate(pph == null ? BigDecimal.ZERO : pph.getRate());

        // ---------------------------------------------------------------- baris
        Map<Long, GoodsReceipt.Line> grnLines = new HashMap<>();
        List<Long> ids = req.lines().stream().map(PiLineRequest::receiptLineId).filter(java.util.Objects::nonNull).toList();
        if (!ids.isEmpty()) {
            receipts.findLinesByIds(ids).forEach(l -> grnLines.put(l.getId(), l));
        }
        BigDecimal stockTotal = BigDecimal.ZERO;
        BigDecimal expenseTotal = BigDecimal.ZERO;
        BigDecimal receiptCostTotal = BigDecimal.ZERO;
        if (ids.size() != new java.util.HashSet<>(ids).size()) {
            throw new BusinessException("Baris penerimaan yang sama tidak boleh muncul dua kali dalam satu faktur");
        }
        int no = 1;
        for (PiLineRequest lr : req.lines()) {
            PurchaseInvoice.Line l = new PurchaseInvoice.Line();
            l.setInvoice(inv);
            l.setLineNo(no++);
            l.setQty(lr.qty());
            l.setUnitPrice(Money.cost(lr.unitPrice()));
            l.setLineTotal(Money.multiply(lr.qty(), lr.unitPrice()));
            if (lr.receiptLineId() != null) {
                GoodsReceipt.Line gl = grnLines.get(lr.receiptLineId());
                if (gl == null || !gl.getReceipt().getSupplier().getId().equals(supplier.getId())
                        || gl.getReceipt().getStatus() != DocStatus.POSTED) {
                    throw new BusinessException("Baris penerimaan #" + lr.receiptLineId() + " tidak valid untuk pemasok ini");
                }
                if (lr.qty().compareTo(gl.uninvoicedQty()) > 0) {
                    throw new BusinessException("Qty faktur " + gl.getItem().getCode() + " (" + plain(lr.qty())
                            + ") melebihi qty yang belum difakturkan (" + plain(gl.uninvoicedQty()) + ")");
                }
                checkPrice(gl, lr.unitPrice(), req.overridePriceCheck());
                l.setReceiptLine(gl);
                l.setItem(gl.getItem());
                l.setDescription(lr.description() == null || lr.description().isBlank()
                        ? gl.getItem().getCode() + " " + gl.getItem().getName() : lr.description());
                boolean completes = gl.getQtyInvoiced().add(gl.getQtyReturned()).add(lr.qty()).compareTo(gl.getQty()) == 0;
                BigDecimal cost = completes
                        ? gl.getTotalCost().subtract(invoices.sumReceiptCostCleared(gl.getId()))
                        .subtract(returns.sumUninvoicedReturnCost(gl.getId()))
                        : Money.multiply(lr.qty(), gl.getUnitCost());
                l.setReceiptCost(cost);
                gl.setQtyInvoiced(gl.getQtyInvoiced().add(lr.qty()));
                stockTotal = stockTotal.add(l.getLineTotal());
                receiptCostTotal = receiptCostTotal.add(cost);
            } else {
                if (lr.accountId() == null) {
                    throw new BusinessException("Baris " + l.getLineNo() + ": pilih baris penerimaan atau akun biaya");
                }
                Account acc = accounts.findById(lr.accountId()).orElseThrow(() -> NotFoundException.of("Akun", lr.accountId()));
                accountService.assertPostable(acc);
                l.setAccount(acc);
                l.setDepartment(lr.departmentId() == null ? null : departments.findById(lr.departmentId()).orElseThrow());
                if (lr.description() == null || lr.description().isBlank()) {
                    throw new BusinessException("Baris " + l.getLineNo() + ": keterangan wajib diisi untuk baris biaya");
                }
                l.setDescription(lr.description());
                expenseTotal = expenseTotal.add(l.getLineTotal());
            }
            inv.getLines().add(l);
        }

        // ---------------------------------------------------------------- total & pajak
        BigDecimal subtotal = stockTotal.add(expenseTotal);
        BigDecimal discount = Money.amount(req.discountAmount());
        BigDecimal freight = Money.amount(req.freightAmount());
        if (discount.compareTo(subtotal) > 0) {
            throw new FieldValidationException("discountAmount", "Diskon melebihi subtotal");
        }
        inv.setSubtotal(subtotal);
        inv.setDiscountAmount(discount);
        inv.setFreightAmount(freight);
        inv.setDpp(subtotal.subtract(discount).add(freight));
        inv.setTaxAmount(Money.percentOf(inv.getDpp(), inv.getTaxRate()));
        BigDecimal expenseNetForPph = expenseTotal.subtract(share(discount, expenseTotal, subtotal));
        inv.setPphAmount(Money.percentOf(expenseNetForPph, inv.getPphRate()));
        inv.setTotal(inv.getDpp().add(inv.getTaxAmount()));
        inv.setPayableAmount(inv.getTotal().subtract(inv.getPphAmount()));
        inv.setDocNo(numbers.next(DocType.PURCHASE_INVOICE, req.docDate()));
        invoices.save(inv);

        // ---------------------------------------------------------------- jurnal
        JournalDraft jd = JournalDraft.of(req.docDate(), SourceType.PURCHASE_INVOICE, inv.getId(), inv.getDocNo(),
                "Faktur pembelian " + supInvNo + " — " + supplier.getName());
        jd.debit(mappings.get(AccountMappingKey.GRNI), receiptCostTotal, "Clear GRNI");
        BigDecimal stockDiscount = share(discount, stockTotal, subtotal);
        BigDecimal variance = stockTotal.subtract(stockDiscount).add(freight).subtract(receiptCostTotal);
        jd.debit(mappings.get(AccountMappingKey.PURCHASE_PRICE_VARIANCE), variance, "Selisih harga faktur vs penerimaan");

        BigDecimal expenseDiscountLeft = discount.subtract(stockDiscount);
        List<PurchaseInvoice.Line> expenseLines = inv.getLines().stream().filter(x -> x.getAccount() != null).toList();
        for (int i = 0; i < expenseLines.size(); i++) {
            PurchaseInvoice.Line l = expenseLines.get(i);
            BigDecimal d = i == expenseLines.size() - 1 ? expenseDiscountLeft : share(discount, l.getLineTotal(), subtotal);
            expenseDiscountLeft = expenseDiscountLeft.subtract(d);
            jd.debit(l.getAccount(), l.getDepartment(), l.getLineTotal().subtract(d), l.getDescription());
        }
        jd.debit(mappings.get(AccountMappingKey.PPN_INPUT), inv.getTaxAmount(), "PPN masukan " + nz(inv.getTaxInvoiceNo()));
        jd.credit(mappings.get(AccountMappingKey.ACCOUNTS_PAYABLE), inv.getPayableAmount(), "Utang " + supplier.getName());
        jd.credit(mappings.get(AccountMappingKey.PPH_PAYABLE), inv.getPphAmount(), "PPh dipotong");
        inv.setJournal(posting.post(jd));

        audit.log(AuditAction.POST, "PurchaseInvoice", inv.getId(), "Faktur " + inv.getDocNo() + " (" + supInvNo
                + ") Rp " + inv.getTotal().toPlainString());
        return PurchasingMapper.pi(inv, true, List.of());
    }

    @Transactional
    public PiResponse voidDoc(Long id, LocalDate date, String reason) {
        PurchaseInvoice inv = load(id);
        if (inv.getStatus() == PurchaseInvoice.Status.VOID) {
            throw new BusinessException("Faktur sudah dibatalkan");
        }
        if (inv.getPaidAmount().signum() > 0 || inv.getReturnedAmount().signum() > 0) {
            throw new BusinessException("Faktur sudah dibayar/diretur; batalkan pembayaran/retur terlebih dahulu");
        }
        periods.assertOpen(date);
        posting.reverseBySource(SourceType.PURCHASE_INVOICE, inv.getId(), date, reason);
        for (PurchaseInvoice.Line l : inv.getLines()) {
            if (l.getReceiptLine() != null) {
                l.getReceiptLine().setQtyInvoiced(l.getReceiptLine().getQtyInvoiced().subtract(l.getQty()));
            }
        }
        inv.setStatus(PurchaseInvoice.Status.VOID);
        audit.log(AuditAction.VOID, "PurchaseInvoice", id, "Batal faktur " + inv.getDocNo() + ": " + reason);
        return PurchasingMapper.pi(inv, true, List.of());
    }

    private void checkPrice(GoodsReceipt.Line gl, BigDecimal invoicePrice, boolean override) {
        if (gl.getUnitPrice().signum() == 0) {
            return;
        }
        BigDecimal diffPct = invoicePrice.subtract(gl.getUnitPrice()).abs()
                .multiply(BigDecimal.valueOf(100)).divide(gl.getUnitPrice(), 4, RoundingMode.HALF_UP);
        if (diffPct.compareTo(PRICE_TOLERANCE_PCT) > 0) {
            boolean canOverride = CurrentUser.principal().map(p -> p.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).anyMatch("PURCHASE:APPROVE"::equals)).orElse(false);
            if (!override || !canOverride) {
                throw new BusinessException("PRICE_MISMATCH", "Harga faktur " + gl.getItem().getCode() + " berbeda "
                        + diffPct.stripTrailingZeros().toPlainString() + "% dari harga penerimaan (toleransi "
                        + PRICE_TOLERANCE_PCT + "%). Butuh persetujuan (PURCHASE:APPROVE) dengan opsi override.");
            }
        }
    }

    private TaxRate tax(Long id, TaxType expected) {
        if (id == null) {
            return null;
        }
        TaxRate t = taxRates.findById(id).orElseThrow(() -> NotFoundException.of("Pajak", id));
        if (expected != null && t.getTaxType() != expected) {
            throw new BusinessException("Jenis pajak harus " + expected);
        }
        return t;
    }

    /** Porsi proporsional: total × bagian / keseluruhan. */
    static BigDecimal share(BigDecimal total, BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0 || total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return total.multiply(part).divide(whole, 2, RoundingMode.HALF_UP);
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    PurchaseInvoice load(Long id) {
        return invoices.findFull(id).orElseThrow(() -> NotFoundException.of("Faktur pembelian", id));
    }
}
