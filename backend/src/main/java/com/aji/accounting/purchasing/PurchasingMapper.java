package com.aji.accounting.purchasing;

import com.aji.accounting.coa.Account;
import com.aji.accounting.common.entity.MasterEntity;
import com.aji.accounting.purchasing.PurchasingDtos.AllocationResponse;
import com.aji.accounting.purchasing.PurchasingDtos.ApPaymentResponse;
import com.aji.accounting.purchasing.PurchasingDtos.GrnLineResponse;
import com.aji.accounting.purchasing.PurchasingDtos.GrnResponse;
import com.aji.accounting.purchasing.PurchasingDtos.PaymentHistory;
import com.aji.accounting.purchasing.PurchasingDtos.PiLineResponse;
import com.aji.accounting.purchasing.PurchasingDtos.PiResponse;
import com.aji.accounting.purchasing.PurchasingDtos.PoLineResponse;
import com.aji.accounting.purchasing.PurchasingDtos.PoResponse;
import com.aji.accounting.purchasing.PurchasingDtos.PrLineResponse;
import com.aji.accounting.purchasing.PurchasingDtos.PrResponse;
import com.aji.accounting.purchasing.PurchasingDtos.PrtLineResponse;
import com.aji.accounting.purchasing.PurchasingDtos.PrtResponse;
import com.aji.accounting.purchasing.PurchasingDtos.Ref;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Konversi entity pembelian → DTO response. */
final class PurchasingMapper {
    private PurchasingMapper() {
    }

    static Ref ref(MasterEntity e) {
        return e == null ? null : new Ref(e.getId(), e.getCode(), e.getName());
    }

    static Ref ref(Account a) {
        return a == null ? null : new Ref(a.getId(), a.getCode(), a.getName());
    }

    static PrResponse pr(PurchaseRequest r, boolean lines) {
        return new PrResponse(r.getId(), r.getDocNo(), r.getDocDate(), ref(r.getDepartment()), r.getRequestedBy(),
                r.getNeededDate(), r.getNotes(), r.getStatus(), r.getApprovedBy(), r.getApprovedAt(),
                lines ? r.getLines().stream().map(l -> new PrLineResponse(l.getId(), l.getItem().getId(),
                        l.getItem().getCode(), l.getItem().getName(), l.getItem().getUom().getCode(), l.getQty(),
                        l.getNote())).toList() : List.of());
    }

    static PoResponse po(PurchaseOrder o, boolean lines) {
        return new PoResponse(o.getId(), o.getDocNo(), o.getDocDate(), ref(o.getSupplier()), ref(o.getWarehouse()),
                o.getRequest() == null ? null : o.getRequest().getId(),
                o.getRequest() == null ? null : o.getRequest().getDocNo(), o.getExpectedDate(),
                o.getTaxRateRef() == null ? null : o.getTaxRateRef().getId(), o.getTaxRate(), o.getSubtotal(),
                o.getDiscountAmount(), o.getTaxAmount(), o.getTotal(), o.getNotes(), o.getStatus(), o.getApprovedBy(),
                o.getApprovedAt(),
                lines ? o.getLines().stream().map(l -> new PoLineResponse(l.getId(), l.getLineNo(), l.getItem().getId(),
                        l.getItem().getCode(), l.getItem().getName(), l.getItem().getUom().getCode(), l.getDescription(),
                        l.getQty(), l.getUnitPrice(), l.getDiscountPct(), l.getLineTotal(), l.getQtyReceived(),
                        l.remainingQty(), l.netUnitPrice().multiply(o.headerDiscountFactor())
                        .setScale(4, java.math.RoundingMode.HALF_UP))).toList() : List.of());
    }

    static GrnResponse grn(GoodsReceipt g, boolean lines) {
        return new GrnResponse(g.getId(), g.getDocNo(), g.getDocDate(), ref(g.getSupplier()),
                g.getOrder() == null ? null : g.getOrder().getId(), g.getOrder() == null ? null : g.getOrder().getDocNo(),
                ref(g.getWarehouse()), g.getDeliveryNoteNo(), g.getFreightCost(), g.getTotalValue(), g.getNotes(),
                g.getStatus(), g.getJournal() == null ? null : g.getJournal().getId(),
                lines ? g.getLines().stream().map(l -> new GrnLineResponse(l.getId(), l.getLineNo(),
                        l.getOrderLine() == null ? null : l.getOrderLine().getId(), l.getItem().getId(),
                        l.getItem().getCode(), l.getItem().getName(), l.getItem().getUom().getCode(), l.getQty(),
                        l.getUnitPrice(), l.getFreightAlloc(), l.getUnitCost(), l.getTotalCost(), l.getQtyInvoiced(),
                        l.getQtyReturned())).toList() : List.of());
    }

    static PiResponse pi(PurchaseInvoice p, boolean lines, List<ApPayment.Allocation> payments) {
        long overdue = p.getStatus() == PurchaseInvoice.Status.PAID || p.getStatus() == PurchaseInvoice.Status.VOID ? 0
                : Math.max(0, ChronoUnit.DAYS.between(p.getDueDate(), LocalDate.now()));
        return new PiResponse(p.getId(), p.getDocNo(), p.getDocDate(), p.getDueDate(), ref(p.getSupplier()),
                p.getSupplierInvoiceNo(), p.getTaxInvoiceNo(),
                p.getTaxRateRef() == null ? null : p.getTaxRateRef().getId(), p.getTaxRate(),
                p.getPphRateRef() == null ? null : p.getPphRateRef().getId(), p.getPphRate(), p.getSubtotal(),
                p.getDiscountAmount(), p.getFreightAmount(), p.getDpp(), p.getTaxAmount(), p.getPphAmount(),
                p.getTotal(), p.getPayableAmount(), p.getPaidAmount(), p.getReturnedAmount(), p.outstanding(),
                p.getNotes(), p.getStatus(), p.getJournal() == null ? null : p.getJournal().getId(), overdue,
                lines ? p.getLines().stream().map(l -> new PiLineResponse(l.getId(), l.getLineNo(),
                        l.getReceiptLine() == null ? null : l.getReceiptLine().getId(),
                        l.getReceiptLine() == null ? null : l.getReceiptLine().getReceipt().getDocNo(),
                        l.getItem() == null ? null : l.getItem().getId(),
                        l.getItem() == null ? null : l.getItem().getCode(),
                        l.getAccount() == null ? null : l.getAccount().getId(),
                        l.getAccount() == null ? null : l.getAccount().getCode(),
                        l.getAccount() == null ? null : l.getAccount().getName(),
                        l.getDepartment() == null ? null : l.getDepartment().getId(),
                        l.getDepartment() == null ? null : l.getDepartment().getName(), l.getDescription(), l.getQty(),
                        l.getUnitPrice(), l.getLineTotal(), l.getReceiptCost())).toList() : List.of(),
                payments == null ? List.of() : payments.stream().map(a -> new PaymentHistory(a.getPayment().getId(),
                        a.getPayment().getDocNo(), a.getPayment().getDocDate(), a.getPayment().getPaymentType(),
                        a.getAmount())).toList());
    }

    static PrtResponse prt(PurchaseReturn r, boolean lines) {
        return new PrtResponse(r.getId(), r.getDocNo(), r.getDocDate(), ref(r.getSupplier()), r.getReceipt().getId(),
                r.getReceipt().getDocNo(), r.getInvoice() == null ? null : r.getInvoice().getId(),
                r.getInvoice() == null ? null : r.getInvoice().getDocNo(), ref(r.getWarehouse()), r.getReason(),
                r.getSubtotal(), r.getTaxAmount(), r.getTotal(), r.getStatus(),
                r.getJournal() == null ? null : r.getJournal().getId(),
                lines ? r.getLines().stream().map(l -> new PrtLineResponse(l.getId(), l.getReceiptLine().getId(),
                        l.getItem().getId(), l.getItem().getCode(), l.getItem().getName(),
                        l.getItem().getUom().getCode(), l.getQty(), l.getUnitCost(), l.getTotalCost())).toList()
                        : List.of());
    }

    static ApPaymentResponse payment(ApPayment p, boolean lines) {
        return new ApPaymentResponse(p.getId(), p.getDocNo(), p.getDocDate(), ref(p.getSupplier()), p.getPaymentType(),
                ref(p.getCashAccount()), p.getAmount(), p.getBankCharges(), p.getReferenceNo(), p.getNotes(),
                p.getStatus(), p.getJournal() == null ? null : p.getJournal().getId(),
                lines ? p.getAllocations().stream().map(a -> new AllocationResponse(a.getInvoice().getId(),
                        a.getInvoice().getDocNo(), a.getInvoice().getSupplierInvoiceNo(), a.getAmount())).toList()
                        : List.of());
    }
}
