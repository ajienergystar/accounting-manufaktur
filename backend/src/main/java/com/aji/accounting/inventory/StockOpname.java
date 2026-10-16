package com.aji.accounting.inventory;

import com.aji.accounting.common.entity.BaseEntity;
import com.aji.accounting.masterdata.item.Item;
import com.aji.accounting.masterdata.warehouse.Warehouse;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
@Table(name = "stock_opnames")
public class StockOpname extends BaseEntity {

    public enum Status { DRAFT, POSTED, VOID }

    @Column(name = "doc_no", nullable = false, length = 30)
    private String docNo;

    @Column(name = "doc_date", nullable = false)
    private LocalDate docDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(length = 500)
    private String notes;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status = Status.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjustment_id")
    private StockAdjustment adjustment;

    @OneToMany(mappedBy = "opname", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Line> lines = new ArrayList<>();

    @Getter
    @Setter
    @Entity(name = "StockOpnameLine")
    @Table(name = "stock_opname_lines")
    public static class Line {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "opname_id", nullable = false)
        private StockOpname opname;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "item_id", nullable = false)
        private Item item;

        @Column(name = "system_qty", nullable = false, precision = 19, scale = 4)
        private BigDecimal systemQty;

        /** null = belum dihitung. */
        @Column(name = "counted_qty", precision = 19, scale = 4)
        private BigDecimal countedQty;

        @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
        private BigDecimal unitCost = BigDecimal.ZERO;

        @Column(length = 255)
        private String note;

        public BigDecimal diff() {
            return countedQty == null ? BigDecimal.ZERO : countedQty.subtract(systemQty);
        }
    }
}
