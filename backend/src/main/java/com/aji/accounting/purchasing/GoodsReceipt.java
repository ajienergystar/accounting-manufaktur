package com.aji.accounting.purchasing;

import com.aji.accounting.common.entity.BaseEntity;
import com.aji.accounting.inventory.DocStatus;
import com.aji.accounting.ledger.JournalEntry;
import com.aji.accounting.masterdata.item.Item;
import com.aji.accounting.masterdata.supplier.Supplier;
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
@Table(name = "goods_receipts")
public class GoodsReceipt extends BaseEntity {

    @Column(name = "doc_no", nullable = false, length = 30)
    private String docNo;

    @Column(name = "doc_date", nullable = false)
    private LocalDate docDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private PurchaseOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "delivery_note_no", length = 50)
    private String deliveryNoteNo;

    @Column(name = "freight_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal freightCost = BigDecimal.ZERO;

    @Column(name = "total_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalValue = BigDecimal.ZERO;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DocStatus status = DocStatus.POSTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntry journal;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<Line> lines = new ArrayList<>();

    @Getter
    @Setter
    @Entity(name = "GoodsReceiptLine")
    @Table(name = "goods_receipt_lines")
    public static class Line {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "receipt_id", nullable = false)
        private GoodsReceipt receipt;

        @Column(name = "line_no", nullable = false)
        private int lineNo;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "order_line_id")
        private PurchaseOrder.Line orderLine;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "item_id", nullable = false)
        private Item item;

        @Column(nullable = false, precision = 19, scale = 4)
        private BigDecimal qty;

        /** Harga bersih per unit (setelah diskon PO). */
        @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
        private BigDecimal unitPrice;

        @Column(name = "freight_alloc", nullable = false, precision = 19, scale = 2)
        private BigDecimal freightAlloc = BigDecimal.ZERO;

        /** Biaya perolehan per unit (harga + alokasi ongkos angkut). */
        @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
        private BigDecimal unitCost;

        @Column(name = "total_cost", nullable = false, precision = 19, scale = 2)
        private BigDecimal totalCost;

        @Column(name = "qty_invoiced", nullable = false, precision = 19, scale = 4)
        private BigDecimal qtyInvoiced = BigDecimal.ZERO;

        @Column(name = "qty_returned", nullable = false, precision = 19, scale = 4)
        private BigDecimal qtyReturned = BigDecimal.ZERO;

        public BigDecimal uninvoicedQty() {
            return qty.subtract(qtyInvoiced).subtract(qtyReturned).max(BigDecimal.ZERO);
        }
    }
}
