package com.aji.accounting.purchasing;

import com.aji.accounting.coa.Account;
import com.aji.accounting.common.entity.BaseEntity;
import com.aji.accounting.ledger.JournalEntry;
import com.aji.accounting.masterdata.department.Department;
import com.aji.accounting.masterdata.item.Item;
import com.aji.accounting.masterdata.supplier.Supplier;
import com.aji.accounting.masterdata.tax.TaxRate;
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
@Table(name = "purchase_invoices")
public class PurchaseInvoice extends BaseEntity {

    public enum Status { UNPAID, PARTIAL, PAID, VOID }

    @Column(name = "doc_no", nullable = false, length = 30)
    private String docNo;

    @Column(name = "doc_date", nullable = false)
    private LocalDate docDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "supplier_invoice_no", nullable = false, length = 50)
    private String supplierInvoiceNo;

    @Column(name = "tax_invoice_no", length = 50)
    private String taxInvoiceNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_rate_id")
    private TaxRate taxRateRef;

    @Column(name = "tax_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pph_rate_id")
    private TaxRate pphRateRef;

    @Column(name = "pph_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal pphRate = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "freight_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal freightAmount = BigDecimal.ZERO;

    /** Dasar pengenaan pajak = subtotal - diskon + ongkos angkut. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal dpp = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "pph_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal pphAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    /** Utang ke pemasok = total - PPh dipotong. */
    @Column(name = "payable_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal payableAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "returned_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal returnedAmount = BigDecimal.ZERO;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Status status = Status.UNPAID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntry journal;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<Line> lines = new ArrayList<>();

    public BigDecimal outstanding() {
        return payableAmount.subtract(paidAmount).subtract(returnedAmount);
    }

    public void refreshStatus() {
        if (status == Status.VOID) {
            return;
        }
        BigDecimal out = outstanding();
        status = out.signum() <= 0 ? Status.PAID
                : paidAmount.signum() > 0 || returnedAmount.signum() > 0 ? Status.PARTIAL : Status.UNPAID;
    }

    @Getter
    @Setter
    @Entity(name = "PurchaseInvoiceLine")
    @Table(name = "purchase_invoice_lines")
    public static class Line {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "invoice_id", nullable = false)
        private PurchaseInvoice invoice;

        @Column(name = "line_no", nullable = false)
        private int lineNo;

        /** Baris barang: menunjuk baris penerimaan (three-way match). */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "receipt_line_id")
        private GoodsReceipt.Line receiptLine;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "item_id")
        private Item item;

        /** Baris jasa/biaya: langsung ke akun beban/BOP. */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "account_id")
        private Account account;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "department_id")
        private Department department;

        @Column(nullable = false, length = 255)
        private String description;

        @Column(nullable = false, precision = 19, scale = 4)
        private BigDecimal qty;

        @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
        private BigDecimal unitPrice;

        @Column(name = "line_total", nullable = false, precision = 19, scale = 2)
        private BigDecimal lineTotal;

        /** Nilai GRNI yang di-clear oleh baris ini (biaya perolehan saat penerimaan). */
        @Column(name = "receipt_cost", nullable = false, precision = 19, scale = 2)
        private BigDecimal receiptCost = BigDecimal.ZERO;
    }
}
