package com.aji.accounting.inventory;

import com.aji.accounting.audit.AuditAction;
import com.aji.accounting.audit.AuditService;
import com.aji.accounting.coa.Account;
import com.aji.accounting.coa.AccountMappingKey;
import com.aji.accounting.coa.AccountMappingService;
import com.aji.accounting.common.exception.BusinessException;
import com.aji.accounting.common.exception.NotFoundException;
import com.aji.accounting.common.sequence.DocType;
import com.aji.accounting.common.sequence.DocumentNumberService;
import com.aji.accounting.common.util.Money;
import com.aji.accounting.common.web.PageResponse;
import com.aji.accounting.inventory.InventoryDtos.AdjustmentLineRequest;
import com.aji.accounting.inventory.InventoryDtos.AdjustmentLineResponse;
import com.aji.accounting.inventory.InventoryDtos.AdjustmentRequest;
import com.aji.accounting.inventory.InventoryDtos.AdjustmentResponse;
import com.aji.accounting.ledger.JournalDraft;
import com.aji.accounting.ledger.PostingService;
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
import java.util.HashSet;
import java.util.Set;

/**
 * Penyesuaian stok (tambah/kurang), saldo awal persediaan, dan hasil stock opname.
 * Jurnal: Persediaan ↔ Selisih Penyesuaian Persediaan (atau Ekuitas Saldo Awal untuk OPENING).
 */
@Service
@RequiredArgsConstructor
public class StockAdjustmentService {

    private final StockAdjustmentRepository repo;
    private final ItemRepository items;
    private final WarehouseRepository warehouses;
    private final InventoryService inventory;
    private final PostingService posting;
    private final PeriodService periods;
    private final AccountMappingService mappings;
    private final DocumentNumberService numbers;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<AdjustmentResponse> search(LocalDate from, LocalDate to, String q, int page, int size) {
        var p = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Order.desc("docDate"), Sort.Order.desc("id")));
        return PageResponse.of(repo.search(from, to, q == null || q.isBlank() ? null : q, p), a -> toResponse(a, false));
    }

    @Transactional(readOnly = true)
    public AdjustmentResponse get(Long id) {
        return toResponse(load(id), true);
    }

    @Transactional
    public AdjustmentResponse create(AdjustmentRequest req) {
        StockAdjustment adj = post(req.docDate(), loadWarehouse(req.warehouseId()), req.adjustmentType(), req.reason(),
                req.lines());
        return toResponse(adj, true);
    }

    /** Dipakai juga oleh stock opname. */
    @Transactional
    public StockAdjustment post(LocalDate date, Warehouse wh, StockAdjustment.Type type, String reason,
                                Iterable<AdjustmentLineRequest> lines) {
        periods.assertOpen(date);
        StockAdjustment adj = new StockAdjustment();
        adj.setDocNo(numbers.next(DocType.STOCK_ADJUSTMENT, date));
        adj.setDocDate(date);
        adj.setWarehouse(wh);
        adj.setAdjustmentType(type);
        adj.setReason(reason.trim());
        repo.save(adj);

        Account counter = mappings.get(type == StockAdjustment.Type.OPENING
                ? AccountMappingKey.OPENING_BALANCE_EQUITY : AccountMappingKey.INVENTORY_ADJUSTMENT);
        JournalDraft draft = JournalDraft.of(date, type == StockAdjustment.Type.OPENING ? SourceType.OPENING_BALANCE
                : type == StockAdjustment.Type.OPNAME ? SourceType.STOCK_OPNAME : SourceType.STOCK_ADJUSTMENT,
                adj.getId(), adj.getDocNo(), describe(type) + " " + adj.getDocNo() + ": " + adj.getReason());

        Set<Long> seen = new HashSet<>();
        int no = 1;
        BigDecimal netValue = BigDecimal.ZERO;
        for (AdjustmentLineRequest lr : lines) {
            if (!seen.add(lr.itemId())) {
                throw new BusinessException("Item yang sama muncul lebih dari sekali dalam satu dokumen");
            }
            if (lr.qtyChange().signum() == 0) {
                continue;
            }
            Item item = items.findById(lr.itemId()).orElseThrow(() -> NotFoundException.of("Item", lr.itemId()));
            if (type == StockAdjustment.Type.OPENING && lr.qtyChange().signum() < 0) {
                throw new BusinessException("Saldo awal persediaan tidak boleh bernilai negatif");
            }
            StockMovement m;
            if (lr.qtyChange().signum() > 0) {
                BigDecimal cost = lr.unitCost() != null && lr.unitCost().signum() > 0 ? lr.unitCost()
                        : inventory.currentUnitCost(item, wh.getId());
                m = inventory.receive(item, wh, date, lr.qtyChange(), cost, sourceType(type), adj.getId(),
                        adj.getDocNo(), adj.getReason());
                draft.debit(inventory.inventoryAccount(item.getItemType()), m.getTotalCost(), item.getCode() + " +" + plain(lr.qtyChange()))
                        .credit(counter, m.getTotalCost(), item.getCode());
                netValue = netValue.add(m.getTotalCost());
            } else {
                m = inventory.issue(item, wh, date, lr.qtyChange().negate(), sourceType(type), adj.getId(),
                        adj.getDocNo(), adj.getReason());
                draft.debit(counter, m.getTotalCost(), item.getCode())
                        .credit(inventory.inventoryAccount(item.getItemType()), m.getTotalCost(), item.getCode() + " " + plain(lr.qtyChange()));
                netValue = netValue.subtract(m.getTotalCost());
            }
            StockAdjustment.Line line = new StockAdjustment.Line();
            line.setAdjustment(adj);
            line.setLineNo(no++);
            line.setItem(item);
            line.setQtyChange(lr.qtyChange());
            line.setUnitCost(m.getUnitCost());
            line.setTotalValue(lr.qtyChange().signum() > 0 ? m.getTotalCost() : m.getTotalCost().negate());
            line.setNote(lr.note());
            adj.getLines().add(line);
        }
        if (adj.getLines().isEmpty()) {
            throw new BusinessException("Tidak ada baris dengan perubahan kuantitas");
        }
        adj.setTotalValue(Money.amount(netValue));
        if (!draft.isEmpty()) {
            adj.setJournal(posting.post(draft.merged()));
        }
        audit.log(AuditAction.POST, "PenyesuaianStok", adj.getId(), describe(type) + " " + adj.getDocNo());
        return adj;
    }

    @Transactional
    public AdjustmentResponse voidDoc(Long id, LocalDate date, String reason) {
        StockAdjustment adj = load(id);
        if (adj.getStatus() == DocStatus.VOID) {
            throw new BusinessException("Dokumen sudah dibatalkan");
        }
        if (adj.getAdjustmentType() == StockAdjustment.Type.OPNAME) {
            throw new BusinessException("Penyesuaian hasil opname dibatalkan lewat dokumen stock opname");
        }
        periods.assertOpen(date);
        inventory.reverseSource(sourceType(adj.getAdjustmentType()), adj.getId(), date, reason);
        posting.reverseBySource(sourceType(adj.getAdjustmentType()), adj.getId(), date, reason);
        adj.setStatus(DocStatus.VOID);
        audit.log(AuditAction.VOID, "PenyesuaianStok", id, "Batal " + adj.getDocNo() + ": " + reason);
        return toResponse(adj, true);
    }

    private static SourceType sourceType(StockAdjustment.Type t) {
        return switch (t) {
            case OPENING -> SourceType.OPENING_BALANCE;
            case OPNAME -> SourceType.STOCK_OPNAME;
            case ADJUSTMENT -> SourceType.STOCK_ADJUSTMENT;
        };
    }

    private static String describe(StockAdjustment.Type t) {
        return switch (t) {
            case OPENING -> "Saldo awal persediaan";
            case OPNAME -> "Selisih stock opname";
            case ADJUSTMENT -> "Penyesuaian stok";
        };
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }

    private StockAdjustment load(Long id) {
        return repo.findFull(id).orElseThrow(() -> NotFoundException.of("Penyesuaian stok", id));
    }

    private Warehouse loadWarehouse(Long id) {
        return warehouses.findById(id).orElseThrow(() -> NotFoundException.of("Gudang", id));
    }

    AdjustmentResponse toResponse(StockAdjustment a, boolean withLines) {
        return new AdjustmentResponse(a.getId(), a.getDocNo(), a.getDocDate(), a.getWarehouse().getId(),
                a.getWarehouse().getName(), a.getAdjustmentType(), a.getReason(), a.getStatus(), a.getTotalValue(),
                a.getJournal() == null ? null : a.getJournal().getId(),
                withLines ? a.getLines().stream().map(l -> new AdjustmentLineResponse(l.getItem().getId(),
                        l.getItem().getCode(), l.getItem().getName(), l.getItem().getUom().getCode(), l.getQtyChange(),
                        l.getUnitCost(), l.getTotalValue(), l.getNote())).toList() : java.util.List.of());
    }
}
