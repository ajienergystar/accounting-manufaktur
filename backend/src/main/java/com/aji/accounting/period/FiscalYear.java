package com.aji.accounting.period;

import com.aji.accounting.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "fiscal_years")
public class FiscalYear extends BaseEntity {

    public enum Status { OPEN, CLOSED }

    @Column(nullable = false)
    private int year;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by", length = 100)
    private String closedBy;

    @OneToMany(mappedBy = "fiscalYear", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("periodNo ASC")
    private List<Period> periods = new ArrayList<>();
}
