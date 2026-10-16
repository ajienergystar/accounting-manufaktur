package com.aji.accounting.purchasing;

import com.aji.accounting.audit.AuditAction;
import com.aji.accounting.audit.AuditService;
import com.aji.accounting.coa.AccountMappingKey;
import com.aji.accounting.coa.AccountMappingService;
import com.aji.accounting.common.exception.BusinessException;
import com.aji.accounting.common.exception.NotFoundException;
import com.aji.accounting.common.sequence.DocType;
import com.aji.accounting.common.sequence.DocumentNumberService;
import com.aji.accounting.common.util.Money;
import com.aji.accounting.common.web.PageResponse;
import com.aji.accounting.inventory.DocStatus;
import com.aji.accounting.inventory.InventoryService;
import com.aji.accounting.inventory.StockMovement;
import com.aji.accounting.ledger.JournalDraft;
import com.aji.accounting.ledger.PostingService;
import com.aji.accounting.ledger.SourceType;
import com.aji.accounting.period.PeriodService;
import com.aji.accounting.purchasing.PurchasingDtos.PrtLineRequest;
import com.aji.accounting.purchasing.PurchasingDtos.PrtRequest;
import com.aji.accounting.purchasing.PurchasingDtos.PrtResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Retur pembelian.
 * <ul>
 *   <li>Belum difakturkan: Dr GRNI / Cr Persediaan (± selisih harga)</li>
 *   <li>Sudah difakturkan (nota debit): Dr Utang Usaha / Cr PPN Masukan / Cr Persediaan (± selisih harga)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PurchaseReturnService {

    private final PurchaseReturnRepository returns;
    private final GoodsReceiptService receiptService;
    private final PurchaseInvoiceService invoiceService;
    private final PurchaseInvoiceRepository invoices;
    private final InventoryService inventory;
    private final PostingService posting;
    private final PeriodService periods;
    private final AccountMappingService mappings;
    private final DocumentNumberService numbers;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<PrtResponse> search(LocalDate from, LocalDate to, Long supplierId, String q, int page, int size) {
        var p = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Order.desc("docDate"), Sort.Order.desc("id")));
        return PageResponse.of(returns.search(from, to, supplierId, PurchaseOrderService.blank(q), p),
                r -> PurchasingMapper.prt(r, false));
    }

    @Transactional(readOnly = true)
    public PrtResponse get(Long id) {
        return PurchasingMapper.prt(load(id), true);
    }

    @Transactional
    public PrtResponse create(PrtRequest req) {
        periods.assertOpen(req.docDate());
        GoodsReceipt grn = receiptService.load(req.receiptId());
        if (grn.getStatus() != DocStatus.POSTED) {
            throw new BusinessException("Penerimaan " + grn.getDocNo() + " sudah dibatalkan");
        }
        PurchaseInvoice inv = null;
        if (req.invoiceId() != null) {
            inv = invoiceService.load(req.invoiceId());
            if (inv.getStatus() == PurchaseInvoice.Status.VOID || !inv.getSupplier().getId().equals(grn.getSupplier().getId())) {
                throw new BusinessException("Faktur tidak valid untuk retur ini");
            }
        }
        Map<Long, GoodsReceipt.Line> grnLines = new HashMap<>();
        grn.getLines().forEach(l -> grnLines.put(l.getId(), l));

        PurchaseReturn r = new PurchaseReturn();
        r.setDocNo(numbers.next(DocType.PURCHASE_RETURN, req.docDate()));
        r.setDocDate(req.docDate());
        r.setSupplier(grn.getSupplier());
        r.setReceipt(grn);
        r.setInvoice(inv);
        r.setWarehouse(grn.getWarehouse());
        r.setReason(req.reason().trim());
        returns.save(r);

        JournalDraft jd = JournalDraft.of(req.docDate(), SourceType.PURCHASE_RETURN, r.getId(), r.getDocNo(),
                "Retur pembelian " + r.getDocNo() + " ke " + grn.getSupplier().getName());
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal grniTotal = BigDecimal.ZERO;
        BigDecimal actualTotal = BigDecimal.ZERO;
        BigDecimal discountFactor = inv == null || inv.getSubtotal().signum() == 0 ? BigDecimal.ONE
                : BigDecimal.ONE.subtract(inv.getDiscountAmount().divide(inv.getSubtotal(), 10, RoundingMode.HALF_UP));
        int no = 1;
        for (PrtLineRequest lr : req.lines()) {
            GoodsReceipt.Line gl = grnLines.get(lr.receiptLineId());
            if (gl == null) {
                throw new BusinessException("Baris penerimaan #" + lr.receiptLineId() + " bukan bagian dari " + grn.getDocNo());
            }
            BigDecimal value;
            if (inv == null) {
                if (lr.qty().compareTo(gl.uninvoicedQty()) > 0) {
                    throw new BusinessException("Qty retur " + gl.getItem().getCode() + " melebihi qty yang belum difakturkan ("
                            + gl.uninvoicedQty().stripTrailingZeros().toPlainString() + "). Pilih faktur untuk nota debit.");
                }
                value = Money.multiply(lr.qty(), gl.getUnitCost());
                gl.setQtyReturned(gl.getQtyReturned().add(lr.qty()));
                grniTotal = grniTotal.add(value);
            } else {
                PurchaseInvoice.Line il = invoices.findLineForReceipt(inv.getId(), gl.getId())
                        .orElseThrow(() -> new BusinessException("Item " + gl.getItem().getCode() + " tidak ada di faktur terpilih"));
                BigDecimal already = returns.sumReturnedAgainstInvoice(inv.getId(), gl.getId());
                if (lr.qty().add(already).compareTo(il.getQty()) > 0) {
                    throw new BusinessException("Qty retur " + gl.getItem().getCode() + " melebihi qty faktur yang belum diretur");
                }
                value = Money.amount(lr.qty().multiply(il.getUnitPrice()).multiply(discountFactor));
                subtotal = subtotal.add(value);
            }
            StockMovement m = inventory.issueAtCost(gl.getItem(), grn.getWarehouse(), req.docDate(), lr.qty(),
                    gl.getUnitCost(), SourceType.PURCHASE_RETURN, r.getId(), r.getDocNo(), "Retur ke " + grn.getSupplier().getName());
            jd.credit(inventory.inventoryAccount(gl.getItem().getItemType()), m.getTotalCost(), gl.getItem().getCode());
            actualTotal = actualTotal.add(m.getTotalCost());

            PurchaseReturn.Line l = new PurchaseReturn.Line();
            l.setPurchaseReturn(r);
            l.setLineNo(no++);
            l.setReceiptLine(gl);
            l.setItem(gl.getItem());
            l.setQty(lr.qty());
            l.setUnitCost(Money.divideCost(value, lr.qty()));
            l.setTotalCost(value);
            r.getLines().add(l);
        }

        if (inv == null) {
            r.setSubtotal(grniTotal);
            r.setTotal(grniTotal);
            jd.debit(mappings.get(AccountMappingKey.GRNI), grniTotal, "Retur sebelum faktur");
            jd.credit(mappings.get(AccountMappingKey.PURCHASE_PRICE_VARIANCE), grniTotal.subtract(actualTotal), "Selisih nilai retur");
        } else {
            BigDecimal tax = Money.percentOf(subtotal, inv.getTaxRate());
            BigDecimal total = subtotal.add(tax);
            if (total.compareTo(inv.outstanding()) > 0) {
                throw new BusinessException("Nilai nota debit (" + total.toPlainString() + ") melebihi sisa utang faktur ("
                        + inv.outstanding().toPlainString() + ")");
            }
            r.setSubtotal(subtotal);
            r.setTaxAmount(tax);
            r.setTotal(total);
            jd.debit(mappings.get(AccountMappingKey.ACCOUNTS_PAYABLE), total, "Nota debit faktur " + inv.getSupplierInvoiceNo());
            jd.credit(mappings.get(AccountMappingKey.PPN_INPUT), tax, "Koreksi PPN masukan");
            jd.credit(mappings.get(AccountMappingKey.PURCHASE_PRICE_VARIANCE), subtotal.subtract(actualTotal), "Selisih nilai retur");
            inv.setReturnedAmount(inv.getReturnedAmount().add(total));
            inv.refreshStatus();
        }
        r.setJournal(posting.post(jd.merged()));
        audit.log(AuditAction.POST, "PurchaseReturn", r.getId(), "Retur " + r.getDocNo() + " Rp " + r.getTotal().toPlainString());
        return PurchasingMapper.prt(r, true);
    }

    @Transactional
    public PrtResponse voidDoc(Long id, LocalDate date, String reason) {
        PurchaseReturn r = load(id);
        if (r.getStatus() == DocStatus.VOID) {
            throw new BusinessException("Retur sudah dibatalkan");
        }
        periods.assertOpen(date);
        inventory.reverseSource(SourceType.PURCHASE_RETURN, r.getId(), date, reason);
        posting.reverseBySource(SourceType.PURCHASE_RETURN, r.getId(), date, reason);
        if (r.getInvoice() == null) {
            r.getLines().forEach(l -> l.getReceiptLine().setQtyReturned(l.getReceiptLine().getQtyReturned().subtract(l.getQty())));
        } else {
            PurchaseInvoice inv = r.getInvoice();
            inv.setReturnedAmount(inv.getReturnedAmount().subtract(r.getTotal()));
            inv.refreshStatus();
        }
        r.setStatus(DocStatus.VOID);
        audit.log(AuditAction.VOID, "PurchaseReturn", id, "Batal retur " + r.getDocNo() + ": " + reason);
        return PurchasingMapper.prt(r, true);
    }

    private PurchaseReturn load(Long id) {
        return returns.findFull(id).orElseThrow(() -> NotFoundException.of("Retur pembelian", id));
    }
}
