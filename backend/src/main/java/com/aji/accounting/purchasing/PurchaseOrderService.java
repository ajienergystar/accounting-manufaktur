package com.aji.accounting.purchasing;

import com.aji.accounting.audit.AuditAction;
import com.aji.accounting.audit.AuditService;
import com.aji.accounting.common.exception.BusinessException;
import com.aji.accounting.common.exception.NotFoundException;
import com.aji.accounting.common.sequence.DocType;
import com.aji.accounting.common.sequence.DocumentNumberService;
import com.aji.accounting.common.util.Money;
import com.aji.accounting.common.web.PageResponse;
import com.aji.accounting.config.CurrentUser;
import com.aji.accounting.masterdata.department.DepartmentRepository;
import com.aji.accounting.masterdata.item.Item;
import com.aji.accounting.masterdata.item.ItemRepository;
import com.aji.accounting.masterdata.item.ItemType;
import com.aji.accounting.masterdata.supplier.SupplierRepository;
import com.aji.accounting.masterdata.tax.TaxRate;
import com.aji.accounting.masterdata.tax.TaxRateRepository;
import com.aji.accounting.masterdata.tax.TaxType;
import com.aji.accounting.masterdata.warehouse.WarehouseRepository;
import com.aji.accounting.purchasing.PurchasingDtos.PoLineRequest;
import com.aji.accounting.purchasing.PurchasingDtos.PoRequest;
import com.aji.accounting.purchasing.PurchasingDtos.PoResponse;
import com.aji.accounting.purchasing.PurchasingDtos.PrLineRequest;
import com.aji.accounting.purchasing.PurchasingDtos.PrRequest;
import com.aji.accounting.purchasing.PurchasingDtos.PrResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Permintaan pembelian (PR) dan Purchase Order (PO) beserta approval-nya. */
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseRequestRepository requests;
    private final PurchaseOrderRepository orders;
    private final SupplierRepository suppliers;
    private final WarehouseRepository warehouses;
    private final DepartmentRepository departments;
    private final ItemRepository items;
    private final TaxRateRepository taxRates;
    private final DocumentNumberService numbers;
    private final AuditService audit;

    // ================================================================= PR
    @Transactional(readOnly = true)
    public PageResponse<PrResponse> searchRequests(LocalDate from, LocalDate to, PurchaseRequest.Status status, String q,
                                                   int page, int size) {
        var p = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Order.desc("docDate"), Sort.Order.desc("id")));
        return PageResponse.of(requests.search(from, to, status, blank(q), p), r -> PurchasingMapper.pr(r, false));
    }

    @Transactional(readOnly = true)
    public PrResponse getRequest(Long id) {
        return PurchasingMapper.pr(loadRequest(id), true);
    }

    @Transactional
    public PrResponse saveRequest(Long id, PrRequest req) {
        PurchaseRequest r;
        if (id == null) {
            r = new PurchaseRequest();
            r.setDocNo(numbers.next(DocType.PURCHASE_REQUEST, req.docDate()));
            r.setRequestedBy(CurrentUser.usernameOrSystem());
        } else {
            r = loadRequest(id);
            if (r.getStatus() != PurchaseRequest.Status.DRAFT) {
                throw new BusinessException("Hanya PR berstatus DRAFT yang dapat diubah");
            }
        }
        r.setDocDate(req.docDate());
        r.setDepartment(req.departmentId() == null ? null : departments.findById(req.departmentId()).orElseThrow());
        r.setNeededDate(req.neededDate());
        r.setNotes(req.notes());
        r.getLines().clear();
        int no = 1;
        for (PrLineRequest lr : req.lines()) {
            PurchaseRequest.Line l = new PurchaseRequest.Line();
            l.setRequest(r);
            l.setLineNo(no++);
            l.setItem(item(lr.itemId()));
            l.setQty(lr.qty());
            l.setNote(lr.note());
            r.getLines().add(l);
        }
        requests.save(r);
        audit.log(id == null ? AuditAction.CREATE : AuditAction.UPDATE, "PurchaseRequest", r.getId(), "PR " + r.getDocNo());
        return PurchasingMapper.pr(r, true);
    }

    @Transactional
    public PrResponse changeRequestStatus(Long id, PurchaseRequest.Status target) {
        PurchaseRequest r = loadRequest(id);
        PurchaseRequest.Status s = r.getStatus();
        boolean ok = switch (target) {
            case SUBMITTED -> s == PurchaseRequest.Status.DRAFT;
            case APPROVED, REJECTED -> s == PurchaseRequest.Status.SUBMITTED;
            case CANCELLED -> s == PurchaseRequest.Status.DRAFT || s == PurchaseRequest.Status.SUBMITTED
                    || s == PurchaseRequest.Status.APPROVED;
            default -> false;
        };
        if (!ok) {
            throw new BusinessException("Status PR tidak dapat diubah dari " + s + " ke " + target);
        }
        r.setStatus(target);
        if (target == PurchaseRequest.Status.APPROVED || target == PurchaseRequest.Status.REJECTED) {
            r.setApprovedBy(CurrentUser.usernameOrSystem());
            r.setApprovedAt(LocalDateTime.now());
        }
        audit.log(target == PurchaseRequest.Status.APPROVED ? AuditAction.APPROVE : AuditAction.UPDATE,
                "PurchaseRequest", id, "PR " + r.getDocNo() + " → " + target);
        return PurchasingMapper.pr(r, true);
    }

    // ================================================================= PO
    @Transactional(readOnly = true)
    public PageResponse<PoResponse> searchOrders(LocalDate from, LocalDate to, Long supplierId,
                                                 PurchaseOrder.Status status, String q, int page, int size) {
        var p = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Order.desc("docDate"), Sort.Order.desc("id")));
        return PageResponse.of(orders.search(from, to, supplierId, status, blank(q), p), o -> PurchasingMapper.po(o, false));
    }

    @Transactional(readOnly = true)
    public PoResponse getOrder(Long id) {
        return PurchasingMapper.po(loadOrder(id), true);
    }

    @Transactional(readOnly = true)
    public List<PoResponse> openOrders(Long supplierId) {
        return orders.findOpenBySupplier(supplierId).stream()
                .map(o -> PurchasingMapper.po(loadOrder(o.getId()), true)).toList();
    }

    @Transactional
    public PoResponse saveOrder(Long id, PoRequest req) {
        PurchaseOrder o;
        if (id == null) {
            o = new PurchaseOrder();
            o.setDocNo(numbers.next(DocType.PURCHASE_ORDER, req.docDate()));
        } else {
            o = loadOrder(id);
            if (o.getStatus() != PurchaseOrder.Status.DRAFT) {
                throw new BusinessException("Hanya PO berstatus DRAFT yang dapat diubah");
            }
        }
        o.setDocDate(req.docDate());
        o.setSupplier(suppliers.findById(req.supplierId()).orElseThrow(() -> NotFoundException.of("Pemasok", req.supplierId())));
        if (!o.getSupplier().isActive()) {
            throw new BusinessException("Pemasok " + o.getSupplier().getName() + " tidak aktif");
        }
        o.setWarehouse(warehouses.findById(req.warehouseId()).orElseThrow(() -> NotFoundException.of("Gudang", req.warehouseId())));
        o.setExpectedDate(req.expectedDate());
        o.setNotes(req.notes());
        if (req.requestId() != null) {
            PurchaseRequest pr = loadRequest(req.requestId());
            if (pr.getStatus() != PurchaseRequest.Status.APPROVED && pr.getStatus() != PurchaseRequest.Status.ORDERED) {
                throw new BusinessException("PR " + pr.getDocNo() + " belum disetujui");
            }
            o.setRequest(pr);
        }
        TaxRate tax = req.taxRateId() == null ? null : taxRates.findById(req.taxRateId()).orElseThrow();
        if (tax != null && tax.getTaxType() != TaxType.PPN) {
            throw new BusinessException("Pajak pada PO harus berjenis PPN");
        }
        o.setTaxRateRef(tax);
        o.setTaxRate(tax == null ? BigDecimal.ZERO : tax.getRate());

        o.getLines().clear();
        BigDecimal subtotal = BigDecimal.ZERO;
        int no = 1;
        for (PoLineRequest lr : req.lines()) {
            Item item = item(lr.itemId());
            if (item.getItemType() == ItemType.WORK_IN_PROCESS || item.getItemType() == ItemType.FINISHED_GOOD) {
                throw new BusinessException("Item " + item.getCode() + " (" + item.getItemType().label() + ") tidak dibeli lewat PO");
            }
            PurchaseOrder.Line l = new PurchaseOrder.Line();
            l.setOrder(o);
            l.setLineNo(no++);
            l.setItem(item);
            l.setDescription(lr.description());
            l.setQty(lr.qty());
            l.setUnitPrice(Money.cost(lr.unitPrice()));
            l.setDiscountPct(lr.discountPct() == null ? BigDecimal.ZERO : lr.discountPct());
            l.setLineTotal(Money.amount(lr.qty().multiply(l.netUnitPrice())));
            subtotal = subtotal.add(l.getLineTotal());
            o.getLines().add(l);
        }
        BigDecimal discount = Money.amount(req.discountAmount());
        if (discount.compareTo(subtotal) > 0) {
            throw new BusinessException("Diskon melebihi subtotal");
        }
        o.setSubtotal(subtotal);
        o.setDiscountAmount(discount);
        o.setTaxAmount(Money.percentOf(subtotal.subtract(discount), o.getTaxRate()));
        o.setTotal(subtotal.subtract(discount).add(o.getTaxAmount()));
        orders.save(o);
        audit.log(id == null ? AuditAction.CREATE : AuditAction.UPDATE, "PurchaseOrder", o.getId(),
                "PO " + o.getDocNo() + " Rp " + o.getTotal().toPlainString());
        return PurchasingMapper.po(o, true);
    }

    @Transactional
    public PoResponse approve(Long id) {
        PurchaseOrder o = loadOrder(id);
        if (o.getStatus() != PurchaseOrder.Status.DRAFT) {
            throw new BusinessException("Hanya PO DRAFT yang dapat disetujui");
        }
        o.setStatus(PurchaseOrder.Status.APPROVED);
        o.setApprovedBy(CurrentUser.usernameOrSystem());
        o.setApprovedAt(LocalDateTime.now());
        if (o.getRequest() != null) {
            o.getRequest().setStatus(PurchaseRequest.Status.ORDERED);
        }
        audit.log(AuditAction.APPROVE, "PurchaseOrder", id, "Setujui PO " + o.getDocNo());
        return PurchasingMapper.po(o, true);
    }

    @Transactional
    public PoResponse cancel(Long id) {
        PurchaseOrder o = loadOrder(id);
        if (o.getStatus() != PurchaseOrder.Status.DRAFT && o.getStatus() != PurchaseOrder.Status.APPROVED) {
            throw new BusinessException("PO yang sudah ada penerimaan tidak dapat dibatalkan; gunakan Tutup PO");
        }
        o.setStatus(PurchaseOrder.Status.CANCELLED);
        audit.log(AuditAction.VOID, "PurchaseOrder", id, "Batalkan PO " + o.getDocNo());
        return PurchasingMapper.po(o, true);
    }

    /** Tutup PO yang diterima sebagian (sisa tidak akan dikirim). */
    @Transactional
    public PoResponse close(Long id) {
        PurchaseOrder o = loadOrder(id);
        if (o.getStatus() != PurchaseOrder.Status.PARTIAL && o.getStatus() != PurchaseOrder.Status.APPROVED) {
            throw new BusinessException("Hanya PO APPROVED/PARTIAL yang dapat ditutup");
        }
        o.setStatus(PurchaseOrder.Status.CLOSED);
        audit.log(AuditAction.CLOSE, "PurchaseOrder", id, "Tutup PO " + o.getDocNo());
        return PurchasingMapper.po(o, true);
    }

    /** Dipanggil setelah penerimaan/void penerimaan untuk memperbarui status PO. */
    void refreshStatus(PurchaseOrder o) {
        if (o.getStatus() == PurchaseOrder.Status.CLOSED || o.getStatus() == PurchaseOrder.Status.CANCELLED) {
            return;
        }
        boolean anyReceived = o.getLines().stream().anyMatch(l -> l.getQtyReceived().signum() > 0);
        boolean allReceived = o.getLines().stream().allMatch(l -> l.remainingQty().signum() == 0);
        o.setStatus(allReceived ? PurchaseOrder.Status.RECEIVED
                : anyReceived ? PurchaseOrder.Status.PARTIAL : PurchaseOrder.Status.APPROVED);
    }

    PurchaseOrder loadOrder(Long id) {
        return orders.findFull(id).orElseThrow(() -> NotFoundException.of("Purchase order", id));
    }

    private PurchaseRequest loadRequest(Long id) {
        return requests.findFull(id).orElseThrow(() -> NotFoundException.of("Permintaan pembelian", id));
    }

    private Item item(Long id) {
        Item i = items.findById(id).orElseThrow(() -> NotFoundException.of("Item", id));
        if (!i.isActive()) {
            throw new BusinessException("Item " + i.getCode() + " tidak aktif");
        }
        return i;
    }

    static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
