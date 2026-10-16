package com.aji.accounting.inventory;

import com.aji.accounting.coa.Account;
import com.aji.accounting.common.exception.NotFoundException;
import com.aji.accounting.common.web.PageResponse;
import com.aji.accounting.inventory.InventoryDtos.StockCard;
import com.aji.accounting.inventory.InventoryDtos.StockCardLine;
import com.aji.accounting.inventory.InventoryDtos.StockRow;
import com.aji.accounting.inventory.InventoryDtos.ValuationRow;
import com.aji.accounting.ledger.LedgerQueryService;
import com.aji.accounting.ledger.SourceType;
import com.aji.accounting.masterdata.item.Item;
import com.aji.accounting.masterdata.item.ItemRepository;
import com.aji.accounting.masterdata.item.ItemType;
import com.aji.accounting.masterdata.warehouse.Warehouse;
import com.aji.accounting.masterdata.warehouse.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockQueryService {

    private final StockBalanceRepository balances;
    private final ItemRepository items;
    private final WarehouseRepository warehouses;
    private final NamedParameterJdbcTemplate jdbc;
    private final InventoryService inventory;
    private final LedgerQueryService ledger;

    @Transactional(readOnly = true)
    public PageResponse<StockRow> stock(Long warehouseId, ItemType type, String q, boolean nonZero, boolean lowOnly,
                                        int page, int size) {
        var pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 500), Sort.by("item.code"));
        return PageResponse.of(balances.search(warehouseId, type, q == null || q.isBlank() ? null : q.trim(), nonZero,
                lowOnly, pageable), b -> new StockRow(b.getItem().getId(), b.getItem().getCode(), b.getItem().getName(),
                b.getItem().getItemType(), b.getItem().getUom().getCode(), b.getWarehouse().getId(),
                b.getWarehouse().getName(), b.getQty(), b.getAvgCost(), b.getTotalValue(), b.getItem().getMinStock(),
                b.getItem().getMinStock().signum() > 0 && b.getQty().compareTo(b.getItem().getMinStock()) <= 0));
    }

    @Transactional(readOnly = true)
    public StockCard stockCard(Long itemId, Long warehouseId, LocalDate from, LocalDate to) {
        Item item = items.findById(itemId).orElseThrow(() -> NotFoundException.of("Item", itemId));
        Warehouse wh = warehouses.findById(warehouseId).orElseThrow(() -> NotFoundException.of("Gudang", warehouseId));
        var p = new MapSqlParameterSource().addValue("item", itemId).addValue("wh", warehouseId)
                .addValue("from", from).addValue("to", to);

        BigDecimal[] opening = jdbc.queryForObject("""
                SELECT COALESCE(SUM(CASE WHEN direction = 'IN' THEN qty ELSE -qty END), 0) AS q,
                       COALESCE(SUM(CASE WHEN direction = 'IN' THEN total_cost ELSE -total_cost END), 0) AS v
                FROM stock_movements WHERE item_id = :item AND warehouse_id = :wh AND movement_date < :from
                """, p, (rs, i) -> new BigDecimal[]{rs.getBigDecimal("q"), rs.getBigDecimal("v")});

        List<StockCardLine> lines = new ArrayList<>();
        BigDecimal[] run = {opening[0], opening[1]};
        BigDecimal[] tot = {BigDecimal.ZERO, BigDecimal.ZERO};
        jdbc.query("""
                SELECT id, movement_date, direction, qty, unit_cost, total_cost, source_type, source_id, source_no, description
                FROM stock_movements
                WHERE item_id = :item AND warehouse_id = :wh AND movement_date BETWEEN :from AND :to
                ORDER BY movement_date, id
                """, p, rs -> {
            boolean in = "IN".equals(rs.getString("direction"));
            BigDecimal qty = rs.getBigDecimal("qty");
            BigDecimal value = rs.getBigDecimal("total_cost");
            run[0] = in ? run[0].add(qty) : run[0].subtract(qty);
            run[1] = in ? run[1].add(value) : run[1].subtract(value);
            if (in) {
                tot[0] = tot[0].add(qty);
            } else {
                tot[1] = tot[1].add(qty);
            }
            SourceType st = SourceType.valueOf(rs.getString("source_type"));
            lines.add(new StockCardLine(rs.getLong("id"), rs.getObject("movement_date", LocalDate.class), st.name(),
                    st.label(), (Long) rs.getObject("source_id", Long.class), rs.getString("source_no"),
                    rs.getString("description"), in ? qty : BigDecimal.ZERO, in ? BigDecimal.ZERO : qty,
                    rs.getBigDecimal("unit_cost"), in ? value : BigDecimal.ZERO, in ? BigDecimal.ZERO : value,
                    run[0], run[1]));
        });
        return new StockCard(item.getId(), item.getCode(), item.getName(), item.getUom().getCode(), wh.getId(),
                wh.getName(), from, to, opening[0], opening[1], tot[0], tot[1], run[0], run[1], lines);
    }

    /** Rekonsiliasi nilai persediaan (sub-ledger) vs saldo akun persediaan di buku besar per tanggal. */
    @Transactional(readOnly = true)
    public List<ValuationRow> valuation(LocalDate asOf) {
        Map<ItemType, BigDecimal> values = new EnumMap<>(ItemType.class);
        jdbc.query("""
                SELECT i.item_type, COALESCE(SUM(CASE WHEN m.direction = 'IN' THEN m.total_cost ELSE -m.total_cost END), 0) AS v
                FROM stock_movements m JOIN items i ON i.id = m.item_id
                WHERE m.movement_date <= :asOf
                GROUP BY i.item_type
                """, new MapSqlParameterSource("asOf", asOf),
                rs -> {
                    values.put(ItemType.valueOf(rs.getString("item_type")), rs.getBigDecimal("v"));
                });
        List<ValuationRow> rows = new ArrayList<>();
        for (ItemType t : ItemType.values()) {
            if (t == ItemType.SERVICE) {
                continue;
            }
            Account acc = inventory.inventoryAccount(t);
            BigDecimal stock = values.getOrDefault(t, BigDecimal.ZERO);
            BigDecimal gl = ledger.balanceAsOf(acc.getId(), asOf);
            rows.add(new ValuationRow(t, t.label(), acc.getCode(), acc.getName(), stock, gl, gl.subtract(stock)));
        }
        return rows;
    }
}
