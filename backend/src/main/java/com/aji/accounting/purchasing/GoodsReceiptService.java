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
import com.aji.accounting.masterdata.item.Item;
import com.aji.accounting.masterdata.item.ItemRepository;
import com.aji.accounting.masterdata.supplier.SupplierRepository;
import com.aji.accounting.masterdata.warehouse.Warehouse;
import com.aji.accounting.masterdata.warehouse.WarehouseRepository;
import com.aji.accounting.period.PeriodService;
import com.aji.accounting.purchasing.PurchasingDtos.GrnLineRequest;
import com.aji.accounting.purchasing.PurchasingDtos.GrnRequest;
import com.aji.accounting.purchasing.PurchasingDtos.GrnResponse;
import com.aji.accounting.purchasing.PurchasingDtos.UninvoicedLine;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Penerimaan barang (GRN). Barang masuk persediaan pada biaya perolehan (harga bersih PO + alokasi ongkos angkut).
 * Jurnal: Dr Persediaan / Cr Barang Diterima Belum Ditagih (GRNI).
 */
@Service
@RequiredArgsConstructor
public class GoodsReceiptService {

    private final GoodsReceiptRepository receipts;
    private final PurchaseOrderService orderService;
    private final SupplierRepository suppliers;
    private final WarehouseRepository warehouses;
    private final ItemRepository items;
    private final InventoryService inventory;
    private final PostingService posting;
    private final PeriodService periods;
    private final AccountMappingService mappings;
    private final DocumentNumberService numbers;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<GrnResponse> search(LocalDate from, LocalDate to, Long supplierId, DocStatus status, String q,
                                            int page, int size) {
        var p = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Order.desc("docDate"), Sort.Order.desc("id")));
        return PageResponse.of(receipts.search(from, to, supplierId, status, PurchaseOrderService.blank(q), p),
                g -> PurchasingMapper.grn(g, false));
    }

    @Transactional(readOnly = true)
    public GrnResponse get(Long id) {
        return PurchasingMapper.grn(load(id), true);
    }

    @Transactional(readOnly = true)
    public List<UninvoicedLine> uninvoiced(Long supplierId) {
        return receipts.findUninvoicedLines(supplierId).stream().map(l -> new UninvoicedLine(l.getId(),
                l.getReceipt().getId(), l.getReceipt().getDocNo(), l.getReceipt().getDocDate(), l.getItem().getId(),
                l.getItem().getCode(), l.getItem().getName(), l.getItem().getUom().getCode(), l.uninvoicedQty(),
                l.getUnitPrice(), l.getUnitCost())).toList();
    }

    @Transactional
    public GrnResponse create(GrnRequest req) {
        periods.assertOpen(req.docDate());
        GoodsReceipt g = new GoodsReceipt();
        g.setSupplier(suppliers.findById(req.supplierId()).orElseThrow(() -> NotFoundException.of("Pemasok", req.supplierId())));
        Warehouse wh = warehouses.findById(req.warehouseId()).orElseThrow(() -> NotFoundException.of("Gudang", req.warehouseId()));
        g.setWarehouse(wh);
        g.setDocDate(req.docDate());
        g.setDeliveryNoteNo(req.deliveryNoteNo());
        g.setNotes(req.notes());
        g.setFreightCost(Money.amount(req.freightCost()));

        PurchaseOrder po = null;
        Map<Long, PurchaseOrder.Line> poLines = new HashMap<>();
        if (req.orderId() != null) {
            po = orderService.loadOrder(req.orderId());
            if (po.getStatus() != PurchaseOrder.Status.APPROVED && po.getStatus() != PurchaseOrder.Status.PARTIAL) {
                throw new BusinessException("PO " + po.getDocNo() + " berstatus " + po.getStatus() + " dan tidak dapat diterima");
            }
            if (!po.getSupplier().getId().equals(g.getSupplier().getId())) {
                throw new BusinessException("Pemasok penerimaan berbeda dengan pemasok PO");
            }
            po.getLines().forEach(l -> poLines.put(l.getId(), l));
            g.setOrder(po);
        }

        // 1. Susun baris & nilai barang
        record Draft(PurchaseOrder.Line poLine, Item item, BigDecimal qty, BigDecimal price, BigDecimal goods) {
        }
        List<Draft> drafts = new ArrayList<>();
        BigDecimal goodsTotal = BigDecimal.ZERO;
        for (GrnLineRequest lr : req.lines()) {
            PurchaseOrder.Line pol = null;
            Item item;
            BigDecimal price;
            if (po != null) {
                if (lr.orderLineId() == null || !poLines.containsKey(lr.orderLineId())) {
                    throw new BusinessException("Baris penerimaan harus merujuk baris PO " + po.getDocNo());
                }
                pol = poLines.get(lr.orderLineId());
                if (lr.qty().compareTo(pol.remainingQty()) > 0) {
                    throw new BusinessException("Qty diterima " + pol.getItem().getCode() + " melebihi sisa PO ("
                            + pol.remainingQty().stripTrailingZeros().toPlainString() + ")");
                }
                item = pol.getItem();
                price = pol.netUnitPrice().multiply(po.headerDiscountFactor()).setScale(Money.COST_SCALE, RoundingMode.HALF_UP);
            } else {
                if (lr.itemId() == null || lr.unitPrice() == null) {
                    throw new BusinessException("Penerimaan tanpa PO wajib mengisi item dan harga");
                }
                item = items.findById(lr.itemId()).orElseThrow(() -> NotFoundException.of("Item", lr.itemId()));
                price = Money.cost(lr.unitPrice());
            }
            BigDecimal goods = Money.multiply(lr.qty(), price);
            goodsTotal = goodsTotal.add(goods);
            drafts.add(new Draft(pol, item, lr.qty(), price, goods));
        }

        g.setDocNo(numbers.next(DocType.GOODS_RECEIPT, req.docDate()));
        receipts.save(g);

        // 2. Alokasi ongkos angkut proporsional nilai; sisa pembulatan ke baris terakhir
        BigDecimal freightLeft = g.getFreightCost();
        JournalDraft jd = JournalDraft.of(req.docDate(), SourceType.GOODS_RECEIPT, g.getId(), g.getDocNo(),
                "Penerimaan barang " + g.getDocNo() + " dari " + g.getSupplier().getName());
        BigDecimal total = BigDecimal.ZERO;
        int no = 1;
        for (int i = 0; i < drafts.size(); i++) {
            Draft d = drafts.get(i);
            BigDecimal freight = i == drafts.size() - 1 ? freightLeft
                    : goodsTotal.signum() == 0 ? BigDecimal.ZERO
                    : g.getFreightCost().multiply(d.goods()).divide(goodsTotal, 2, RoundingMode.HALF_UP);
            freightLeft = freightLeft.subtract(freight);
            BigDecimal lineCost = d.goods().add(freight);

            StockMovement m = inventory.receiveValue(d.item(), wh, req.docDate(), d.qty(), lineCost,
                    SourceType.GOODS_RECEIPT, g.getId(), g.getDocNo(), "Terima dari " + g.getSupplier().getName());

            GoodsReceipt.Line l = new GoodsReceipt.Line();
            l.setReceipt(g);
            l.setLineNo(no++);
            l.setOrderLine(d.poLine());
            l.setItem(d.item());
            l.setQty(d.qty());
            l.setUnitPrice(d.price());
            l.setFreightAlloc(freight);
            l.setUnitCost(m.getUnitCost());
            l.setTotalCost(lineCost);
            g.getLines().add(l);
            total = total.add(lineCost);

            if (d.poLine() != null) {
                d.poLine().setQtyReceived(d.poLine().getQtyReceived().add(d.qty()));
            }
            jd.debit(inventory.inventoryAccount(d.item().getItemType()), lineCost, d.item().getCode() + " " + d.item().getName());
        }
        jd.credit(mappings.get(AccountMappingKey.GRNI), total, "GRNI " + g.getDocNo());
        g.setTotalValue(total);
        g.setJournal(posting.post(jd.merged()));
        if (po != null) {
            orderService.refreshStatus(po);
        }
        audit.log(AuditAction.POST, "GoodsReceipt", g.getId(), "GRN " + g.getDocNo() + " Rp " + total.toPlainString());
        return PurchasingMapper.grn(g, true);
    }

    @Transactional
    public GrnResponse voidDoc(Long id, LocalDate date, String reason) {
        GoodsReceipt g = load(id);
        if (g.getStatus() == DocStatus.VOID) {
            throw new BusinessException("Penerimaan sudah dibatalkan");
        }
        boolean touched = g.getLines().stream().anyMatch(l -> l.getQtyInvoiced().signum() > 0 || l.getQtyReturned().signum() > 0);
        if (touched) {
            throw new BusinessException("Penerimaan sudah difakturkan/diretur sebagian; batalkan dokumen terkait terlebih dahulu");
        }
        periods.assertOpen(date);
        inventory.reverseSource(SourceType.GOODS_RECEIPT, g.getId(), date, reason);
        posting.reverseBySource(SourceType.GOODS_RECEIPT, g.getId(), date, reason);
        for (GoodsReceipt.Line l : g.getLines()) {
            if (l.getOrderLine() != null) {
                l.getOrderLine().setQtyReceived(l.getOrderLine().getQtyReceived().subtract(l.getQty()));
            }
        }
        g.setStatus(DocStatus.VOID);
        if (g.getOrder() != null) {
            orderService.refreshStatus(orderService.loadOrder(g.getOrder().getId()));
        }
        audit.log(AuditAction.VOID, "GoodsReceipt", id, "Batal GRN " + g.getDocNo() + ": " + reason);
        return PurchasingMapper.grn(g, true);
    }

    GoodsReceipt load(Long id) {
        return receipts.findFull(id).orElseThrow(() -> NotFoundException.of("Penerimaan barang", id));
    }
}
