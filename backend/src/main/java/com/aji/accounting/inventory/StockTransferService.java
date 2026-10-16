package com.aji.accounting.inventory;

import com.aji.accounting.audit.AuditAction;
import com.aji.accounting.audit.AuditService;
import com.aji.accounting.common.exception.BusinessException;
import com.aji.accounting.common.exception.NotFoundException;
import com.aji.accounting.common.sequence.DocType;
import com.aji.accounting.common.sequence.DocumentNumberService;
import com.aji.accounting.common.web.PageResponse;
import com.aji.accounting.inventory.InventoryDtos.TransferLineRequest;
import com.aji.accounting.inventory.InventoryDtos.TransferLineResponse;
import com.aji.accounting.inventory.InventoryDtos.TransferRequest;
import com.aji.accounting.inventory.InventoryDtos.TransferResponse;
import com.aji.accounting.ledger.SourceType;
import com.aji.accounting.masterdata.item.Item;
import com.aji.accounting.masterdata.item.ItemRepository;
import com.aji.accounting.masterdata.warehouse.Warehouse;
import com.aji.accounting.masterdata.warehouse.WarehouseRepository;
import com.aji.accounting.period.PeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Transfer antar gudang. Nilai keluar dari gudang asal = nilai masuk ke gudang tujuan,
 * dan akun persediaan ditentukan oleh tipe item (bukan gudang), sehingga tidak ada jurnal.
 */
@Service
@RequiredArgsConstructor
public class StockTransferService {

    private final StockTransferRepository repo;
    private final ItemRepository items;
    private final WarehouseRepository warehouses;
    private final InventoryService inventory;
    private final PeriodService periods;
    private final DocumentNumberService numbers;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<TransferResponse> search(LocalDate from, LocalDate to, String q, int page, int size) {
        var p = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Order.desc("docDate"), Sort.Order.desc("id")));
        return PageResponse.of(repo.search(from, to, q == null || q.isBlank() ? null : q, p), t -> toResponse(t, false));
    }

    @Transactional(readOnly = true)
    public TransferResponse get(Long id) {
        return toResponse(load(id), true);
    }

    @Transactional
    public TransferResponse create(TransferRequest req) {
        if (req.fromWarehouseId().equals(req.toWarehouseId())) {
            throw new BusinessException("Gudang asal dan tujuan tidak boleh sama");
        }
        periods.assertOpen(req.docDate());
        Warehouse from = warehouses.findById(req.fromWarehouseId()).orElseThrow(() -> NotFoundException.of("Gudang", req.fromWarehouseId()));
        Warehouse to = warehouses.findById(req.toWarehouseId()).orElseThrow(() -> NotFoundException.of("Gudang", req.toWarehouseId()));

        StockTransfer t = new StockTransfer();
        t.setDocNo(numbers.next(DocType.STOCK_TRANSFER, req.docDate()));
        t.setDocDate(req.docDate());
        t.setFromWarehouse(from);
        t.setToWarehouse(to);
        t.setNotes(req.notes());
        repo.save(t);

        BigDecimal total = BigDecimal.ZERO;
        int no = 1;
        for (TransferLineRequest lr : req.lines()) {
            Item item = items.findById(lr.itemId()).orElseThrow(() -> NotFoundException.of("Item", lr.itemId()));
            String desc = "Transfer " + from.getCode() + " → " + to.getCode();
            StockMovement out = inventory.issue(item, from, req.docDate(), lr.qty(), SourceType.STOCK_TRANSFER, t.getId(),
                    t.getDocNo(), desc);
            inventory.receiveValue(item, to, req.docDate(), lr.qty(), out.getTotalCost(), SourceType.STOCK_TRANSFER,
                    t.getId(), t.getDocNo(), desc);
            StockTransfer.Line l = new StockTransfer.Line();
            l.setTransfer(t);
            l.setLineNo(no++);
            l.setItem(item);
            l.setQty(lr.qty());
            l.setUnitCost(out.getUnitCost());
            l.setTotalValue(out.getTotalCost());
            t.getLines().add(l);
            total = total.add(out.getTotalCost());
        }
        t.setTotalValue(total);
        audit.log(AuditAction.POST, "TransferStok", t.getId(), "Transfer " + t.getDocNo());
        return toResponse(t, true);
    }

    @Transactional
    public TransferResponse voidDoc(Long id, LocalDate date, String reason) {
        StockTransfer t = load(id);
        if (t.getStatus() == DocStatus.VOID) {
            throw new BusinessException("Dokumen sudah dibatalkan");
        }
        periods.assertOpen(date);
        inventory.reverseSource(SourceType.STOCK_TRANSFER, t.getId(), date, reason);
        t.setStatus(DocStatus.VOID);
        audit.log(AuditAction.VOID, "TransferStok", id, "Batal " + t.getDocNo() + ": " + reason);
        return toResponse(t, true);
    }

    private StockTransfer load(Long id) {
        return repo.findFull(id).orElseThrow(() -> NotFoundException.of("Transfer stok", id));
    }

    private TransferResponse toResponse(StockTransfer t, boolean withLines) {
        List<TransferLineResponse> lines = withLines ? t.getLines().stream().map(l -> new TransferLineResponse(
                l.getItem().getId(), l.getItem().getCode(), l.getItem().getName(), l.getItem().getUom().getCode(),
                l.getQty(), l.getUnitCost(), l.getTotalValue())).toList() : List.of();
        return new TransferResponse(t.getId(), t.getDocNo(), t.getDocDate(), t.getFromWarehouse().getId(),
                t.getFromWarehouse().getName(), t.getToWarehouse().getId(), t.getToWarehouse().getName(), t.getNotes(),
                t.getStatus(), t.getTotalValue(), lines);
    }
}
