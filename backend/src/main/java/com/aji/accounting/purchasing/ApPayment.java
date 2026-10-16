package com.aji.accounting.purchasing;

import com.aji.accounting.coa.Account;
import com.aji.accounting.common.entity.BaseEntity;
import com.aji.accounting.inventory.DocStatus;
import com.aji.accounting.ledger.JournalEntry;
import com.aji.accounting.masterdata.supplier.Supplier;
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
@Table(name = "ap_payments")
public class ApPayment extends BaseEntity {

    public enum Type {
        /** Pelunasan faktur dari kas/bank. */
        INVOICE,
        /** Uang muka ke pemasok (belum ada faktur). */
        ADVANCE,
        /** Kompensasi uang muka ke faktur (tanpa kas). */
        ADVANCE_OFFSET
    }

    @Column(name = "doc_no", nullable = false, length = 30)
    private String docNo;

    @Column(name = "doc_date", nullable = false)
    private LocalDate docDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private Type paymentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_account_id")
    private Account cashAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "bank_charges", nullable = false, precision = 19, scale = 2)
    private BigDecimal bankCharges = BigDecimal.ZERO;

    @Column(name = "reference_no", length = 50)
    private String referenceNo;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DocStatus status = DocStatus.POSTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntry journal;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Allocation> allocations = new ArrayList<>();

    @Getter
    @Setter
    @Entity(name = "ApPaymentAllocation")
    @Table(name = "ap_payment_allocations")
    public static class Allocation {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "payment_id", nullable = false)
        private ApPayment payment;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "invoice_id", nullable = false)
        private PurchaseInvoice invoice;

        @Column(nullable = false, precision = 19, scale = 2)
        private BigDecimal amount;
    }
}
