package com.aji.accounting.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Layer biaya FIFO: setiap penerimaan membentuk layer; pengeluaran mengonsumsi layer tertua. */
@Getter
@Setter
@Entity
@Table(name = "stock_cost_layers")
public class StockCostLayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "movement_id", nullable = false)
    private Long movementId;

    @Column(name = "layer_date", nullable = false)
    private LocalDate layerDate;

    @Column(name = "qty_in", nullable = false, precision = 19, scale = 4)
    private BigDecimal qtyIn;

    @Column(name = "qty_remaining", nullable = false, precision = 19, scale = 4)
    private BigDecimal qtyRemaining;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitCost;
}
