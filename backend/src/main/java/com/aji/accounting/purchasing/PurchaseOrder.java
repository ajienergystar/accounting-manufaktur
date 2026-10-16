package com.aji.accounting.purchasing;

import com.aji.accounting.common.entity.BaseEntity;
import com.aji.accounting.masterdata.item.Item;
import com.aji.accounting.masterdata.supplier.Supplier;
import com.aji.accounting.masterdata.tax.TaxRate;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder extends BaseEntity {

    public enum Status { DRAFT, APPROVED, PARTIAL, RECEIVED, CLOSED, CANCELLED }

    @Column(name = "doc_no", nullable = false, length = 30)
    private String docNo;

    @Column(name = "doc_date", nullable = false)
    private LocalDate docDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private PurchaseRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_rate_id")
    private TaxRate taxRateRef;

    @Column(name = "tax_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Status status = Status.DRAFT;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<Line> lines = new ArrayList<>();

    /** Faktor diskon header (1 - diskon/subtotal) untuk menghitung harga bersih per unit. */
    public BigDecimal headerDiscountFactor() {
        if (subtotal.signum() == 0 || discountAmount.signum() == 0) {
            return BigDecimal.ONE;
        }
        return BigDecimal.ONE.subtract(discountAmount.divide(subtotal, 10, java.math.RoundingMode.HALF_UP));
    }

    @Getter
    @Setter
    @Entity(name = "PurchaseOrderLine")
    @Table(name = "purchase_order_lines")
    public static class Line {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "order_id", nullable = false)
        private PurchaseOrder order;

        @Column(name = "line_no", nullable = false)
        private int lineNo;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "item_id", nullable = false)
        private Item item;

        @Column(length = 255)
        private String description;

        @Column(nullable = false, precision = 19, scale = 4)
        private BigDecimal qty;

        @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
        private BigDecimal unitPrice;

        @Column(name = "discount_pct", nullable = false, precision = 7, scale = 4)
        private BigDecimal discountPct = BigDecimal.ZERO;

        @Column(name = "line_total", nullable = false, precision = 19, scale = 2)
        private BigDecimal lineTotal;

        @Column(name = "qty_received", nullable = false, precision = 19, scale = 4)
        private BigDecimal qtyReceived = BigDecimal.ZERO;

        public BigDecimal remainingQty() {
            return qty.subtract(qtyReceived).max(BigDecimal.ZERO);
        }

        /** Harga bersih per unit setelah diskon baris. */
        public BigDecimal netUnitPrice() {
            return unitPrice.multiply(BigDecimal.ONE.subtract(discountPct.movePointLeft(2)));
        }
    }
}
