package com.aji.accounting.inventory;

import com.aji.accounting.masterdata.item.Item;
import com.aji.accounting.masterdata.warehouse.Warehouse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Saldo stok terkini per item per gudang (kuantitas, nilai, biaya rata-rata). */
@Getter
@Setter
@Entity
@Table(name = "stock_balances")
public class StockBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal qty = BigDecimal.ZERO;

    @Column(name = "total_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalValue = BigDecimal.ZERO;

    @Column(name = "avg_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal avgCost = BigDecimal.ZERO;

    @Column(name = "last_movement_date")
    private LocalDate lastMovementDate;

    @Version
    @Column(nullable = false)
    private Long version;
}
