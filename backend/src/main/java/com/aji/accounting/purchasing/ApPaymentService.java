package com.aji.accounting.purchasing;

import com.aji.accounting.audit.AuditAction;
import com.aji.accounting.audit.AuditService;
import com.aji.accounting.coa.Account;
import com.aji.accounting.coa.AccountMappingKey;
import com.aji.accounting.coa.AccountMappingService;
import com.aji.accounting.coa.AccountRepository;
import com.aji.accounting.common.exception.BusinessException;
import com.aji.accounting.common.exception.FieldValidationException;
import com.aji.accounting.common.exception.NotFoundException;
import com.aji.accounting.common.sequence.DocType;
import com.aji.accounting.common.sequence.DocumentNumberService;
import com.aji.accounting.common.util.Money;
import com.aji.accounting.common.web.PageResponse;
import com.aji.accounting.inventory.DocStatus;
import com.aji.accounting.ledger.JournalDraft;
import com.aji.accounting.ledger.PostingService;
import com.aji.accounting.ledger.SourceType;
import com.aji.accounting.masterdata.supplier.Supplier;
import com.aji.accounting.masterdata.supplier.SupplierRepository;
import com.aji.accounting.period.PeriodService;
import com.aji.accounting.purchasing.PurchasingDtos.AllocationRequest;
import com.aji.accounting.purchasing.PurchasingDtos.ApPaymentRequest;
import com.aji.accounting.purchasing.PurchasingDtos.ApPaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pembayaran utang usaha, uang muka pembelian, dan kompensasi uang muka ke faktur.
 */
@Service
@RequiredArgsConstructor
public class ApPaymentService {

    private final ApPaymentRepository payments;
    private final PurchaseInvoiceService invoiceService;
    private final SupplierRepository suppliers;
    private final AccountRepository accounts;
    private final PostingService posting;
    private final PeriodService periods;
    private final AccountMappingService mappings;
    private final DocumentNumberService numbers;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<ApPaymentResponse> search(LocalDate from, LocalDate to, Long supplierId, ApPayment.Type type,
                                                  String q, int page, int size) {
        var p = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Order.desc("docDate"), Sort.Order.desc("id")));
        return PageResponse.of(payments.search(from, to, supplierId, type, PurchaseOrderService.blank(q), p),
                x -> PurchasingMapper.payment(x, false));
    }

    @Transactional(readOnly = true)
    public ApPaymentResponse get(Long id) {
        return PurchasingMapper.payment(load(id), true);
    }

    @Transactional(readOnly = true)
    public BigDecimal advanceBalance(Long supplierId) {
        return payments.advanceBalance(supplierId);
    }

    @Transactional
    public ApPaymentResponse create(ApPaymentRequest req) {
        periods.assertOpen(req.docDate());
        Supplier supplier = suppliers.findById(req.supplierId()).orElseThrow(() -> NotFoundException.of("Pemasok", req.supplierId()));
        ApPayment p = new ApPayment();
        p.setDocDate(req.docDate());
        p.setSupplier(supplier);
        p.setPaymentType(req.paymentType());
        p.setReferenceNo(req.referenceNo());
        p.setNotes(req.notes());
        p.setBankCharges(Money.amount(req.bankCharges()));

        Account cash = null;
        if (req.paymentType() != ApPayment.Type.ADVANCE_OFFSET) {
            if (req.cashAccountId() == null) {
                throw new FieldValidationException("cashAccountId", "Pilih akun kas/bank");
            }
            cash = accounts.findById(req.cashAccountId()).orElseThrow(() -> NotFoundException.of("Akun", req.cashAccountId()));
            if (!cash.isCashBank()) {
                throw new FieldValidationException("cashAccountId", "Akun " + cash.label() + " bukan akun kas/bank");
            }
        } else if (p.getBankCharges().signum() > 0) {
            throw new BusinessException("Kompensasi uang muka tidak memiliki biaya bank");
        }
        p.setCashAccount(cash);

        BigDecimal amount;
        if (req.paymentType() == ApPayment.Type.ADVANCE) {
            amount = Money.amount(req.amount());
            if (amount.signum() <= 0) {
                throw new FieldValidationException("amount", "Nominal uang muka wajib diisi");
            }
        } else {
            List<AllocationRequest> allocs = req.allocations() == null ? List.of() : req.allocations();
            if (allocs.isEmpty()) {
                throw new BusinessException("Pilih minimal satu faktur yang dibayar");
            }
            Set<Long> seen = new HashSet<>();
            amount = BigDecimal.ZERO;
            for (AllocationRequest a : allocs) {
                if (!seen.add(a.invoiceId())) {
                    throw new BusinessException("Faktur yang sama dialokasikan lebih dari sekali");
                }
                PurchaseInvoice inv = invoiceService.load(a.invoiceId());
                if (!inv.getSupplier().getId().equals(supplier.getId())) {
                    throw new BusinessException("Faktur " + inv.getDocNo() + " bukan milik pemasok ini");
                }
                if (inv.getStatus() == PurchaseInvoice.Status.VOID || inv.getStatus() == PurchaseInvoice.Status.PAID) {
                    throw new BusinessException("Faktur " + inv.getDocNo() + " berstatus " + inv.getStatus());
                }
                BigDecimal amt = Money.amount(a.amount());
                if (amt.compareTo(inv.outstanding()) > 0) {
                    throw new BusinessException("Pembayaran faktur " + inv.getDocNo() + " (" + amt.toPlainString()
                            + ") melebihi sisa utang (" + inv.outstanding().toPlainString() + ")");
                }
                ApPayment.Allocation al = new ApPayment.Allocation();
                al.setPayment(p);
                al.setInvoice(inv);
                al.setAmount(amt);
                p.getAllocations().add(al);
                inv.setPaidAmount(inv.getPaidAmount().add(amt));
                inv.refreshStatus();
                amount = amount.add(amt);
            }
            if (req.paymentType() == ApPayment.Type.ADVANCE_OFFSET) {
                BigDecimal balance = payments.advanceBalance(supplier.getId());
                if (amount.compareTo(balance) > 0) {
                    throw new BusinessException("Saldo uang muka pemasok hanya Rp " + balance.toPlainString());
                }
            }
        }
        p.setAmount(amount);
        p.setDocNo(numbers.next(DocType.AP_PAYMENT, req.docDate()));
        payments.save(p);

        String desc = switch (p.getPaymentType()) {
            case INVOICE -> "Pembayaran utang " + supplier.getName();
            case ADVANCE -> "Uang muka pembelian ke " + supplier.getName();
            case ADVANCE_OFFSET -> "Kompensasi uang muka " + supplier.getName();
        };
        JournalDraft jd = JournalDraft.of(req.docDate(),
                p.getPaymentType() == ApPayment.Type.ADVANCE ? SourceType.PURCHASE_ADVANCE : SourceType.AP_PAYMENT,
                p.getId(), p.getDocNo(), desc + (p.getReferenceNo() == null ? "" : " (" + p.getReferenceNo() + ")"));
        Account ap = mappings.get(AccountMappingKey.ACCOUNTS_PAYABLE);
        Account advance = mappings.get(AccountMappingKey.PURCHASE_ADVANCE);
        switch (p.getPaymentType()) {
            case INVOICE -> jd.debit(ap, amount, desc).credit(cash, amount, desc);
            case ADVANCE -> jd.debit(advance, amount, desc).credit(cash, amount, desc);
            case ADVANCE_OFFSET -> jd.debit(ap, amount, desc).credit(advance, amount, desc);
        }
        if (p.getBankCharges().signum() > 0) {
            jd.debit(mappings.get(AccountMappingKey.BANK_CHARGES), p.getBankCharges(), "Biaya transfer")
                    .credit(cash, p.getBankCharges(), "Biaya transfer");
        }
        p.setJournal(posting.post(jd));
        audit.log(AuditAction.POST, "ApPayment", p.getId(), desc + " " + p.getDocNo() + " Rp " + amount.toPlainString());
        return PurchasingMapper.payment(p, true);
    }

    @Transactional
    public ApPaymentResponse voidDoc(Long id, LocalDate date, String reason) {
        ApPayment p = load(id);
        if (p.getStatus() == DocStatus.VOID) {
            throw new BusinessException("Pembayaran sudah dibatalkan");
        }
        if (p.getPaymentType() == ApPayment.Type.ADVANCE
                && payments.advanceBalance(p.getSupplier().getId()).compareTo(p.getAmount()) < 0) {
            throw new BusinessException("Uang muka ini sudah dikompensasikan ke faktur; batalkan kompensasinya dulu");
        }
        periods.assertOpen(date);
        posting.reverseBySource(p.getPaymentType() == ApPayment.Type.ADVANCE ? SourceType.PURCHASE_ADVANCE
                : SourceType.AP_PAYMENT, p.getId(), date, reason);
        for (ApPayment.Allocation a : p.getAllocations()) {
            PurchaseInvoice inv = a.getInvoice();
            inv.setPaidAmount(inv.getPaidAmount().subtract(a.getAmount()));
            inv.refreshStatus();
        }
        p.setStatus(DocStatus.VOID);
        audit.log(AuditAction.VOID, "ApPayment", id, "Batal pembayaran " + p.getDocNo() + ": " + reason);
        return PurchasingMapper.payment(p, true);
    }

    private ApPayment load(Long id) {
        return payments.findFull(id).orElseThrow(() -> NotFoundException.of("Pembayaran", id));
    }
}
