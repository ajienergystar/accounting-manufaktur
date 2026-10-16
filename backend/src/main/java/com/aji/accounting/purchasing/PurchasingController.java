package com.aji.accounting.purchasing;

import com.aji.accounting.common.web.PageResponse;
import com.aji.accounting.inventory.DocStatus;
import com.aji.accounting.ledger.JournalDtos.ReverseRequest;
import com.aji.accounting.purchasing.PurchasingDtos.AgingReport;
import com.aji.accounting.purchasing.PurchasingDtos.ApPaymentRequest;
import com.aji.accounting.purchasing.PurchasingDtos.ApPaymentResponse;
import com.aji.accounting.purchasing.PurchasingDtos.GrnRequest;
import com.aji.accounting.purchasing.PurchasingDtos.GrnResponse;
import com.aji.accounting.purchasing.PurchasingDtos.PiRequest;
import com.aji.accounting.purchasing.PurchasingDtos.PiResponse;
import com.aji.accounting.purchasing.PurchasingDtos.PoRequest;
import com.aji.accounting.purchasing.PurchasingDtos.PoResponse;
import com.aji.accounting.purchasing.PurchasingDtos.PrRequest;
import com.aji.accounting.purchasing.PurchasingDtos.PrResponse;
import com.aji.accounting.purchasing.PurchasingDtos.PrtRequest;
import com.aji.accounting.purchasing.PurchasingDtos.PrtResponse;
import com.aji.accounting.purchasing.PurchasingDtos.SupplierSummary;
import com.aji.accounting.purchasing.PurchasingDtos.UninvoicedLine;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Pembelian & Utang")
@RestController
@RequestMapping("/api/v1/purchasing")
@RequiredArgsConstructor
public class PurchasingController {

    private final PurchaseOrderService orders;
    private final GoodsReceiptService receipts;
    private final PurchaseInvoiceService invoices;
    private final PurchaseReturnService returns;
    private final ApPaymentService payments;
    private final ApReportService reports;

    // ------------------------------------------------------------ Purchase Request
    @GetMapping("/requests")
    @PreAuthorize("hasAuthority('PURCHASE:VIEW')")
    public PageResponse<PrResponse> requests(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                             @RequestParam(required = false) PurchaseRequest.Status status,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return orders.searchRequests(from, to, status, q, page, size);
    }

    @GetMapping("/requests/{id}")
    @PreAuthorize("hasAuthority('PURCHASE:VIEW')")
    public PrResponse request(@PathVariable Long id) {
        return orders.getRequest(id);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PURCHASE:CREATE')")
    public PrResponse createRequest(@Valid @RequestBody PrRequest req) {
        return orders.saveRequest(null, req);
    }

    @PutMapping("/requests/{id}")
    @PreAuthorize("hasAuthority('PURCHASE:EDIT')")
    public PrResponse updateRequest(@PathVariable Long id, @Valid @RequestBody PrRequest req) {
        return orders.saveRequest(id, req);
    }

    @PostMapping("/requests/{id}/submit")
    @PreAuthorize("hasAuthority('PURCHASE:CREATE')")
    public PrResponse submitRequest(@PathVariable Long id) {
        return orders.changeRequestStatus(id, PurchaseRequest.Status.SUBMITTED);
    }

    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("hasAuthority('PURCHASE:APPROVE')")
    public PrResponse approveRequest(@PathVariable Long id) {
        return orders.changeRequestStatus(id, PurchaseRequest.Status.APPROVED);
    }

    @PostMapping("/requests/{id}/reject")
    @PreAuthorize("hasAuthority('PURCHASE:APPROVE')")
    public PrResponse rejectRequest(@PathVariable Long id) {
        return orders.changeRequestStatus(id, PurchaseRequest.Status.REJECTED);
    }

    @PostMapping("/requests/{id}/cancel")
    @PreAuthorize("hasAuthority('PURCHASE:EDIT')")
    public PrResponse cancelRequest(@PathVariable Long id) {
        return orders.changeRequestStatus(id, PurchaseRequest.Status.CANCELLED);
    }

    // ------------------------------------------------------------ Purchase Order
    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('PURCHASE:VIEW')")
    public PageResponse<PoResponse> orders(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                           @RequestParam(required = false) Long supplierId,
                                           @RequestParam(required = false) PurchaseOrder.Status status,
                                           @RequestParam(required = false) String q,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return orders.searchOrders(from, to, supplierId, status, q, page, size);
    }

    @GetMapping("/orders/open")
    @PreAuthorize("hasAuthority('PURCHASE:VIEW')")
    public List<PoResponse> openOrders(@RequestParam Long supplierId) {
        return orders.openOrders(supplierId);
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasAuthority('PURCHASE:VIEW')")
    public PoResponse order(@PathVariable Long id) {
        return orders.getOrder(id);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PURCHASE:CREATE')")
    public PoResponse createOrder(@Valid @RequestBody PoRequest req) {
        return orders.saveOrder(null, req);
    }

    @PutMapping("/orders/{id}")
    @PreAuthorize("hasAuthority('PURCHASE:EDIT')")
    public PoResponse updateOrder(@PathVariable Long id, @Valid @RequestBody PoRequest req) {
        return orders.saveOrder(id, req);
    }

    @PostMapping("/orders/{id}/approve")
    @PreAuthorize("hasAuthority('PURCHASE:APPROVE')")
    public PoResponse approveOrder(@PathVariable Long id) {
        return orders.approve(id);
    }

    @PostMapping("/orders/{id}/cancel")
    @PreAuthorize("hasAuthority('PURCHASE:EDIT')")
    public PoResponse cancelOrder(@PathVariable Long id) {
        return orders.cancel(id);
    }

    @PostMapping("/orders/{id}/close")
    @PreAuthorize("hasAuthority('PURCHASE:APPROVE')")
    public PoResponse closeOrder(@PathVariable Long id) {
        return orders.close(id);
    }

    // ------------------------------------------------------------ Goods Receipt
    @GetMapping("/receipts")
    @PreAuthorize("hasAuthority('PURCHASE:VIEW')")
    public PageResponse<GrnResponse> receipts(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                              @RequestParam(required = false) Long supplierId,
                                              @RequestParam(required = false) DocStatus status,
                                              @RequestParam(required = false) String q,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return receipts.search(from, to, supplierId, status, q, page, size);
    }

    @GetMapping("/receipts/uninvoiced")
    @PreAuthorize("hasAuthority('PURCHASE:VIEW')")
    public List<UninvoicedLine> uninvoiced(@RequestParam Long supplierId) {
        return receipts.uninvoiced(supplierId);
    }

    @GetMapping("/receipts/{id}")
    @PreAuthorize("hasAuthority('PURCHASE:VIEW')")
    public GrnResponse receipt(@PathVariable Long id) {
        return receipts.get(id);
    }

    @PostMapping("/receipts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PURCHASE:CREATE') or hasAuthority('INVENTORY:CREATE')")
    public GrnResponse createReceipt(@Valid @RequestBody GrnRequest req) {
        return receipts.create(req);
    }

    @PostMapping("/receipts/{id}/void")
    @PreAuthorize("hasAuthority('PURCHASE:APPROVE')")
    public GrnResponse voidReceipt(@PathVariable Long id, @Valid @RequestBody ReverseRequest req) {
        return receipts.voidDoc(id, req.date(), req.reason());
    }

    // ------------------------------------------------------------ Purchase Invoice
    @GetMapping("/invoices")
    @PreAuthorize("hasAuthority('PURCHASE:VIEW')")
    public PageResponse<PiResponse> invoices(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                             @RequestParam(required = false) Long supplierId,
                                             @RequestParam(required = false) PurchaseInvoice.Status status,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return invoices.search(from, to, supplierId, status, q, page, size);
    }

    @GetMapping("/invoices/outstanding")
    @PreAuthorize("hasAuthority('PURCHASE:VIEW')")
    public List<PiResponse> outstanding(@RequestParam(required = false) Long supplierId) {
        return invoices.outstanding(supplierId);
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasAuthority('PURCHASE:VIEW')")
    public PiResponse invoice(@PathVariable Long id) {
        return invoices.get(id);
    }

    @PostMapping("/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PURCHASE:POST') or hasAuthority('PURCHASE:CREATE')")
    public PiResponse createInvoice(@Valid @RequestBody PiRequest req) {
        return invoices.create(req);
    }

    @PostMapping("/invoices/{id}/void")
    @PreAuthorize("hasAuthority('PURCHASE:APPROVE')")
    public PiResponse voidInvoice(@PathVariable Long id, @Valid @RequestBody ReverseRequest req) {
        return invoices.voidDoc(id, req.date(), req.reason());
    }

    // ------------------------------------------------------------ Purchase Return
    @GetMapping("/returns")
    @PreAuthorize("hasAuthority('PURCHASE:VIEW')")
    public PageResponse<PrtResponse> returns(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                             @RequestParam(required = false) Long supplierId,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return returns.search(from, to, supplierId, q, page, size);
    }

    @GetMapping("/returns/{id}")
    @PreAuthorize("hasAuthority('PURCHASE:VIEW')")
    public PrtResponse purchaseReturn(@PathVariable Long id) {
        return returns.get(id);
    }

    @PostMapping("/returns")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PURCHASE:CREATE')")
    public PrtResponse createReturn(@Valid @RequestBody PrtRequest req) {
        return returns.create(req);
    }

    @PostMapping("/returns/{id}/void")
    @PreAuthorize("hasAuthority('PURCHASE:APPROVE')")
    public PrtResponse voidReturn(@PathVariable Long id, @Valid @RequestBody ReverseRequest req) {
        return returns.voidDoc(id, req.date(), req.reason());
    }

    // ------------------------------------------------------------ Payments
    @GetMapping("/payments")
    @PreAuthorize("hasAuthority('PURCHASE:VIEW')")
    public PageResponse<ApPaymentResponse> payments(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                    @RequestParam(required = false) Long supplierId,
                                                    @RequestParam(required = false) ApPayment.Type type,
                                                    @RequestParam(required = false) String q,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return payments.search(from, to, supplierId, type, q, page, size);
    }

    @GetMapping("/payments/{id}")
    @PreAuthorize("hasAuthority('PURCHASE:VIEW')")
    public ApPaymentResponse payment(@PathVariable Long id) {
        return payments.get(id);
    }

    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PURCHASE:POST') or hasAuthority('CASHBANK:CREATE')")
    public ApPaymentResponse createPayment(@Valid @RequestBody ApPaymentRequest req) {
        return payments.create(req);
    }

    @PostMapping("/payments/{id}/void")
    @PreAuthorize("hasAuthority('PURCHASE:APPROVE')")
    public ApPaymentResponse voidPayment(@PathVariable Long id, @Valid @RequestBody ReverseRequest req) {
        return payments.voidDoc(id, req.date(), req.reason());
    }

    // ------------------------------------------------------------ Reports
    @GetMapping("/aging")
    @PreAuthorize("hasAuthority('PURCHASE:VIEW') or hasAuthority('REPORT:VIEW')")
    public AgingReport aging(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return reports.aging(asOf == null ? LocalDate.now() : asOf);
    }

    @GetMapping("/suppliers/{id}/summary")
    @PreAuthorize("hasAuthority('PURCHASE:VIEW')")
    public SupplierSummary supplierSummary(@PathVariable Long id) {
        return reports.supplierSummary(id);
    }

    @GetMapping("/suppliers/{id}/advance-balance")
    @PreAuthorize("hasAuthority('PURCHASE:VIEW')")
    public BigDecimal advanceBalance(@PathVariable Long id) {
        return payments.advanceBalance(id);
    }
}
