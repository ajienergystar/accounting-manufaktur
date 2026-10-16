package com.aji.accounting.purchasing;

import com.aji.accounting.inventory.DocStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class PurchasingDtos {
    private PurchasingDtos() {
    }

    public record Ref(Long id, String code, String name) {
    }

    // ================================================================ Purchase Request
    public record PrLineRequest(@NotNull Long itemId, @NotNull @DecimalMin("0.0001") BigDecimal qty,
                                @Size(max = 255) String note) {
    }

    public record PrRequest(@NotNull LocalDate docDate, Long departmentId, LocalDate neededDate,
                            @Size(max = 500) String notes, @NotEmpty List<@Valid PrLineRequest> lines) {
    }

    public record PrLineResponse(Long id, Long itemId, String itemCode, String itemName, String uomCode,
                                 BigDecimal qty, String note) {
    }

    public record PrResponse(Long id, String docNo, LocalDate docDate, Ref department, String requestedBy,
                             LocalDate neededDate, String notes, PurchaseRequest.Status status, String approvedBy,
                             LocalDateTime approvedAt, List<PrLineResponse> lines) {
    }

    // ================================================================ Purchase Order
    public record PoLineRequest(@NotNull Long itemId, @Size(max = 255) String description,
                                @NotNull @DecimalMin("0.0001") BigDecimal qty,
                                @NotNull @DecimalMin("0") BigDecimal unitPrice,
                                @DecimalMin("0") @DecimalMax("100") BigDecimal discountPct) {
    }

    public record PoRequest(@NotNull LocalDate docDate, @NotNull Long supplierId, @NotNull Long warehouseId,
                            Long requestId, LocalDate expectedDate, Long taxRateId,
                            @DecimalMin("0") BigDecimal discountAmount, @Size(max = 500) String notes,
                            @NotEmpty List<@Valid PoLineRequest> lines) {
    }

    public record PoLineResponse(Long id, int lineNo, Long itemId, String itemCode, String itemName, String uomCode,
                                 String description, BigDecimal qty, BigDecimal unitPrice, BigDecimal discountPct,
                                 BigDecimal lineTotal, BigDecimal qtyReceived, BigDecimal remainingQty,
                                 BigDecimal netUnitPrice) {
    }

    public record PoResponse(Long id, String docNo, LocalDate docDate, Ref supplier, Ref warehouse, Long requestId,
                             String requestNo, LocalDate expectedDate, Long taxRateId, BigDecimal taxRate,
                             BigDecimal subtotal, BigDecimal discountAmount, BigDecimal taxAmount, BigDecimal total,
                             String notes, PurchaseOrder.Status status, String approvedBy, LocalDateTime approvedAt,
                             List<PoLineResponse> lines) {
    }

    // ================================================================ Goods Receipt
    public record GrnLineRequest(Long orderLineId, Long itemId, @NotNull @DecimalMin("0.0001") BigDecimal qty,
                                 @DecimalMin("0") BigDecimal unitPrice) {
    }

    public record GrnRequest(@NotNull LocalDate docDate, @NotNull Long supplierId, Long orderId,
                             @NotNull Long warehouseId, @Size(max = 50) String deliveryNoteNo,
                             @DecimalMin("0") BigDecimal freightCost, @Size(max = 500) String notes,
                             @NotEmpty List<@Valid GrnLineRequest> lines) {
    }

    public record GrnLineResponse(Long id, int lineNo, Long orderLineId, Long itemId, String itemCode,
                                  String itemName, String uomCode, BigDecimal qty, BigDecimal unitPrice,
                                  BigDecimal freightAlloc, BigDecimal unitCost, BigDecimal totalCost,
                                  BigDecimal qtyInvoiced, BigDecimal qtyReturned) {
    }

    public record GrnResponse(Long id, String docNo, LocalDate docDate, Ref supplier, Long orderId, String orderNo,
                              Ref warehouse, String deliveryNoteNo, BigDecimal freightCost, BigDecimal totalValue,
                              String notes, DocStatus status, Long journalId, List<GrnLineResponse> lines) {
    }

    /** Baris penerimaan yang siap difakturkan. */
    public record UninvoicedLine(Long receiptLineId, Long receiptId, String receiptNo, LocalDate receiptDate,
                                 Long itemId, String itemCode, String itemName, String uomCode,
                                 BigDecimal qtyOpen, BigDecimal unitPrice, BigDecimal unitCost) {
    }

    // ================================================================ Purchase Invoice
    public record PiLineRequest(Long receiptLineId, Long accountId, Long departmentId,
                                @Size(max = 255) String description,
                                @NotNull @DecimalMin("0.0001") BigDecimal qty,
                                @NotNull @DecimalMin("0") BigDecimal unitPrice) {
    }

    public record PiRequest(@NotNull LocalDate docDate, LocalDate dueDate, @NotNull Long supplierId,
                            @NotBlank(message = "No. faktur pemasok wajib diisi") @Size(max = 50) String supplierInvoiceNo,
                            @Size(max = 50) String taxInvoiceNo, Long taxRateId, Long pphRateId,
                            @DecimalMin("0") BigDecimal discountAmount, @DecimalMin("0") BigDecimal freightAmount,
                            @Size(max = 500) String notes, boolean overridePriceCheck,
                            @NotEmpty List<@Valid PiLineRequest> lines) {
    }

    public record PiLineResponse(Long id, int lineNo, Long receiptLineId, String receiptNo, Long itemId,
                                 String itemCode, Long accountId, String accountCode, String accountName,
                                 Long departmentId, String departmentName, String description, BigDecimal qty,
                                 BigDecimal unitPrice, BigDecimal lineTotal, BigDecimal receiptCost) {
    }

    public record PaymentHistory(Long paymentId, String paymentNo, LocalDate date, ApPayment.Type type,
                                 BigDecimal amount) {
    }

    public record PiResponse(Long id, String docNo, LocalDate docDate, LocalDate dueDate, Ref supplier,
                             String supplierInvoiceNo, String taxInvoiceNo, Long taxRateId, BigDecimal taxRate,
                             Long pphRateId, BigDecimal pphRate, BigDecimal subtotal, BigDecimal discountAmount,
                             BigDecimal freightAmount, BigDecimal dpp, BigDecimal taxAmount, BigDecimal pphAmount,
                             BigDecimal total, BigDecimal payableAmount, BigDecimal paidAmount,
                             BigDecimal returnedAmount, BigDecimal outstanding, String notes,
                             PurchaseInvoice.Status status, Long journalId, long daysOverdue,
                             List<PiLineResponse> lines, List<PaymentHistory> payments) {
    }

    // ================================================================ Purchase Return
    public record PrtLineRequest(@NotNull Long receiptLineId, @NotNull @DecimalMin("0.0001") BigDecimal qty) {
    }

    public record PrtRequest(@NotNull LocalDate docDate, @NotNull Long receiptId, Long invoiceId,
                             @NotBlank(message = "Alasan retur wajib diisi") @Size(max = 500) String reason,
                             @NotEmpty List<@Valid PrtLineRequest> lines) {
    }

    public record PrtLineResponse(Long id, Long receiptLineId, Long itemId, String itemCode, String itemName,
                                  String uomCode, BigDecimal qty, BigDecimal unitCost, BigDecimal totalCost) {
    }

    public record PrtResponse(Long id, String docNo, LocalDate docDate, Ref supplier, Long receiptId,
                              String receiptNo, Long invoiceId, String invoiceNo, Ref warehouse, String reason,
                              BigDecimal subtotal, BigDecimal taxAmount, BigDecimal total, DocStatus status,
                              Long journalId, List<PrtLineResponse> lines) {
    }

    // ================================================================ AP Payment
    public record AllocationRequest(@NotNull Long invoiceId, @NotNull @DecimalMin("0.01") BigDecimal amount) {
    }

    public record ApPaymentRequest(@NotNull LocalDate docDate, @NotNull Long supplierId,
                                   @NotNull ApPayment.Type paymentType, Long cashAccountId,
                                   @DecimalMin("0") BigDecimal amount, @DecimalMin("0") BigDecimal bankCharges,
                                   @Size(max = 50) String referenceNo, @Size(max = 500) String notes,
                                   List<@Valid AllocationRequest> allocations) {
    }

    public record AllocationResponse(Long invoiceId, String invoiceNo, String supplierInvoiceNo, BigDecimal amount) {
    }

    public record ApPaymentResponse(Long id, String docNo, LocalDate docDate, Ref supplier,
                                    ApPayment.Type paymentType, Ref cashAccount, BigDecimal amount,
                                    BigDecimal bankCharges, String referenceNo, String notes, DocStatus status,
                                    Long journalId, List<AllocationResponse> allocations) {
    }

    // ================================================================ Laporan
    public record AgingRow(Long supplierId, String supplierCode, String supplierName, BigDecimal notDue,
                           BigDecimal d1to30, BigDecimal d31to60, BigDecimal d61to90, BigDecimal over90,
                           BigDecimal total) {
    }

    public record AgingReport(LocalDate asOf, List<AgingRow> rows, AgingRow totals) {
    }

    public record SupplierSummary(Long supplierId, BigDecimal outstanding, BigDecimal advanceBalance,
                                  int openInvoices) {
    }
}
