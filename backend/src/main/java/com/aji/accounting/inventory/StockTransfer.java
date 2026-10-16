package com.aji.accounting.inventory;

import com.aji.accounting.common.entity.BaseEntity;
import com.aji.accounting.masterdata.item.Item;
import com.aji.accounting.masterdata.warehouse.Warehouse;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "stock_transfers")
public class StockTransfer extends BaseEntity {

    @Column(name = "doc_no", nullable = false, length = 30)
    private String docNo;

    @Column(name = "doc_date", nullable = false)
    private LocalDate docDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_warehouse_id", nullable = false)
    private Warehouse fromWarehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_warehouse_id", nullable = false)
    private Warehouse toWarehouse;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DocStatus status = DocStatus.POSTED;

    @Column(name = "total_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalValue = BigDecimal.ZERO;

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<Line> lines = new ArrayList<>();

    @Getter
    @Setter
    @Entity(name = "StockTransferLine")
    @Table(name = "stock_transfer_lines")
    public static class Line {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "transfer_id", nullable = false)
        private StockTransfer transfer;

        @Column(name = "line_no", nullable = false)
        private int lineNo;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "item_id", nullable = false)
        private Item item;

        @Column(nullable = false, precision = 19, scale = 4)
        private BigDecimal qty;

        @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
        private BigDecimal unitCost = BigDecimal.ZERO;

        @Column(name = "total_value", nullable = false, precision = 19, scale = 2)
        private BigDecimal totalValue = BigDecimal.ZERO;
    }
}
