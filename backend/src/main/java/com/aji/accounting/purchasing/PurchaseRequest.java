package com.aji.accounting.purchasing;

import com.aji.accounting.common.entity.BaseEntity;
import com.aji.accounting.masterdata.department.Department;
import com.aji.accounting.masterdata.item.Item;
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
@Table(name = "purchase_requests")
public class PurchaseRequest extends BaseEntity {

    public enum Status { DRAFT, SUBMITTED, APPROVED, REJECTED, ORDERED, CANCELLED }

    @Column(name = "doc_no", nullable = false, length = 30)
    private String docNo;

    @Column(name = "doc_date", nullable = false)
    private LocalDate docDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "requested_by", nullable = false, length = 100)
    private String requestedBy;

    @Column(name = "needed_date")
    private LocalDate neededDate;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Status status = Status.DRAFT;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<Line> lines = new ArrayList<>();

    @Getter
    @Setter
    @Entity(name = "PurchaseRequestLine")
    @Table(name = "purchase_request_lines")
    public static class Line {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "request_id", nullable = false)
        private PurchaseRequest request;

        @Column(name = "line_no", nullable = false)
        private int lineNo;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "item_id", nullable = false)
        private Item item;

        @Column(nullable = false, precision = 19, scale = 4)
        private BigDecimal qty;

        @Column(length = 255)
        private String note;
    }
}
