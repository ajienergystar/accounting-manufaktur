package com.aji.accounting.inventory;

import com.aji.accounting.audit.AuditAction;
import com.aji.accounting.audit.AuditService;
import com.aji.accounting.common.exception.BusinessException;
import com.aji.accounting.common.exception.NotFoundException;
import com.aji.accounting.common.sequence.DocType;
import com.aji.accounting.common.sequence.DocumentNumberService;
import com.aji.accounting.common.util.Money;
import com.aji.accounting.common.web.PageResponse;
import com.aji.accounting.inventory.InventoryDtos.AdjustmentLineRequest;
import com.aji.accounting.inventory.InventoryDtos.OpnameCountLine;
import com.aji.accounting.inventory.InventoryDtos.OpnameCountRequest;
import com.aji.accounting.inventory.InventoryDtos.OpnameCreateRequest;
import com.aji.accounting.inventory.InventoryDtos.OpnameLineResponse;
import com.aji.accounting.inventory.InventoryDtos.OpnameResponse;
import com.aji.accounting.ledger.PostingService;
import com.aji.accounting.ledger.SourceType;
import com.aji.accounting.masterdata.item.Item;
import com.aji.accounting.masterdata.item.ItemRepository;
import com.aji.accounting.masterdata.item.ItemType;
import com.aji.accounting.masterdata.warehouse.Warehouse;
import com.aji.accounting.masterdata.warehouse.WarehouseRepository;
import com.aji.accounting.period.PeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stock opname: (1) buat lembar hitung dari saldo sistem, (2) isi hasil hitung fisik,
 * (3) posting → selisih dibukukan sebagai penyesuaian tipe OPNAME.
 */
@Service
@RequiredArgsConstructor
public class StockOpnameService {

    private final StockOpnameRepository repo;
    private final StockBalanceRepository balances;
    private final ItemRepository items;
    private final WarehouseRepository warehouses;
    private final StockAdjustmentService adjustments;
    private final InventoryService inventory;
    private final PostingService posting;
    private final PeriodService periods;
    private final DocumentNumberService numbers;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<OpnameResponse> list(int page, int size) {
        return PageResponse.of(repo.findAllWithWarehouse(PageRequest.of(page, Math.min(size, 100))), o -> toResponse(o, false));
    }

    @Transactional(readOnly = true)
    public OpnameResponse get(Long id) {
        return toResponse(load(id), true);
    }

    @Transactional
    public OpnameResponse create(OpnameCreateRequest req) {
        Warehouse wh = warehouses.findById(req.warehouseId()).orElseThrow(() -> NotFoundException.of("Gudang", req.warehouseId()));
        StockOpname o = new StockOpname();
        o.setDocNo(numbers.next(DocType.STOCK_OPNAME, req.docDate()));
        o.setDocDate(req.docDate());
        o.setWarehouse(wh);
        o.setNotes(req.notes());

        Map<Long, StockBalance> byItem = new HashMap<>();
        balances.findByWarehouseId(wh.getId()).forEach(b -> byItem.put(b.getItem().getId(), b));
        List<Item> candidates = req.includeZeroStock()
                ? items.findByActiveTrueAndItemTypeInOrderByCodeAsc(EnumSet.complementOf(EnumSet.of(ItemType.SERVICE, ItemType.WORK_IN_PROCESS)))
                : byItem.values().stream().filter(b -> b.getQty().signum() != 0).map(StockBalance::getItem)
                .sorted(Comparator.comparing(Item::getCode)).toList();
        for (Item item : candidates) {
            StockBalance b = byItem.get(item.getId());
            StockOpname.Line l = new StockOpname.Line();
            l.setOpname(o);
            l.setItem(item);
            l.setSystemQty(b == null ? BigDecimal.ZERO : b.getQty());
            l.setUnitCost(b != null && b.getQty().signum() > 0 ? b.getAvgCost() : item.getStandardCost());
            o.getLines().add(l);
        }
        if (o.getLines().isEmpty()) {
            throw new BusinessException("Tidak ada item untuk dihitung di gudang " + wh.getName());
        }
        repo.save(o);
        audit.log(AuditAction.CREATE, "StockOpname", o.getId(), "Lembar opname " + o.getDocNo());
        return toResponse(o, true);
    }

    @Transactional
    public OpnameResponse saveCounts(Long id, OpnameCountRequest req) {
        StockOpname o = load(id);
        assertDraft(o);
        Map<Long, StockOpname.Line> byItem = new HashMap<>();
        o.getLines().forEach(l -> byItem.put(l.getItem().getId(), l));
        for (OpnameCountLine c : req.lines()) {
            StockOpname.Line l = byItem.get(c.itemId());
            if (l == null) {
                throw new BusinessException("Item id " + c.itemId() + " tidak ada di lembar opname");
            }
            if (c.countedQty() != null && c.countedQty().signum() < 0) {
                throw new BusinessException("Hasil hitung tidak boleh negatif");
            }
            l.setCountedQty(c.countedQty());
            l.setNote(c.note());
        }
        return toResponse(o, true);
    }

    @Transactional
    public OpnameResponse post(Long id) {
        StockOpname o = load(id);
        assertDraft(o);
        long uncounted = o.getLines().stream().filter(l -> l.getCountedQty() == null).count();
        if (uncounted > 0) {
            throw new BusinessException(uncounted + " item belum diisi hasil hitungnya");
        }
        // Saldo sistem bisa berubah sejak lembar dibuat → hitung ulang selisih terhadap saldo terkini.
        List<AdjustmentLineRequest> diffs = new ArrayList<>();
        for (StockOpname.Line l : o.getLines()) {
            StockBalance b = balances.find(l.getItem().getId(), o.getWarehouse().getId()).orElse(null);
            BigDecimal current = b == null ? BigDecimal.ZERO : b.getQty();
            BigDecimal diff = l.getCountedQty().subtract(current);
            if (diff.signum() != 0) {
                diffs.add(new AdjustmentLineRequest(l.getItem().getId(), diff, l.getUnitCost(), l.getNote()));
            }
        }
        if (!diffs.isEmpty()) {
            StockAdjustment adj = adjustments.post(o.getDocDate(), o.getWarehouse(), StockAdjustment.Type.OPNAME,
                    "Hasil stock opname " + o.getDocNo(), diffs);
            o.setAdjustment(adj);
        } else {
            periods.assertOpen(o.getDocDate());
        }
        o.setStatus(StockOpname.Status.POSTED);
        audit.log(AuditAction.POST, "StockOpname", id, "Posting opname " + o.getDocNo() + " (" + diffs.size() + " selisih)");
        return toResponse(o, true);
    }

    @Transactional
    public void deleteDraft(Long id) {
        StockOpname o = load(id);
        assertDraft(o);
        repo.delete(o);
        audit.log(AuditAction.DELETE, "StockOpname", id, "Hapus draft opname " + o.getDocNo());
    }

    @Transactional
    public OpnameResponse voidDoc(Long id, LocalDate date, String reason) {
        StockOpname o = load(id);
        if (o.getStatus() != StockOpname.Status.POSTED) {
            throw new BusinessException("Hanya opname POSTED yang dapat dibatalkan");
        }
        periods.assertOpen(date);
        if (o.getAdjustment() != null) {
            StockAdjustment adj = o.getAdjustment();
            inventory.reverseSource(SourceType.STOCK_OPNAME, adj.getId(), date, reason);
            posting.reverseBySource(SourceType.STOCK_OPNAME, adj.getId(), date, reason);
            adj.setStatus(DocStatus.VOID);
        }
        o.setStatus(StockOpname.Status.VOID);
        audit.log(AuditAction.VOID, "StockOpname", id, "Batal opname " + o.getDocNo() + ": " + reason);
        return toResponse(o, true);
    }

    private void assertDraft(StockOpname o) {
        if (o.getStatus() != StockOpname.Status.DRAFT) {
            throw new BusinessException("Stock opname " + o.getDocNo() + " sudah " + o.getStatus());
        }
    }

    private StockOpname load(Long id) {
        return repo.findFull(id).orElseThrow(() -> NotFoundException.of("Stock opname", id));
    }

    private OpnameResponse toResponse(StockOpname o, boolean withLines) {
        List<OpnameLineResponse> lines = new ArrayList<>();
        BigDecimal totalDiff = BigDecimal.ZERO;
        int counted = 0;
        if (withLines) {
            List<StockOpname.Line> sorted = new ArrayList<>(o.getLines());
            sorted.sort(Comparator.comparing(l -> l.getItem().getCode()));
            for (StockOpname.Line l : sorted) {
                BigDecimal diffValue = Money.multiply(l.diff(), l.getUnitCost());
                if (l.getCountedQty() != null) {
                    counted++;
                }
                totalDiff = totalDiff.add(diffValue);
                lines.add(new OpnameLineResponse(l.getItem().getId(), l.getItem().getCode(), l.getItem().getName(),
                        l.getItem().getUom().getCode(), l.getSystemQty(), l.getCountedQty(), l.diff(), l.getUnitCost(),
                        diffValue, l.getNote()));
            }
        }
        StockAdjustment adj = o.getAdjustment();
        return new OpnameResponse(o.getId(), o.getDocNo(), o.getDocDate(), o.getWarehouse().getId(),
                o.getWarehouse().getName(), o.getNotes(), o.getStatus(), adj == null ? null : adj.getId(),
                adj == null ? null : adj.getDocNo(), counted, withLines ? lines.size() : 0, totalDiff, lines);
    }
}
