package com.aji.accounting.coa;

import com.aji.accounting.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "accounts")
public class Account extends BaseEntity {

    @Column(nullable = false, length = 20, unique = true)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Account parent;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "normal_balance", nullable = false, length = 6)
    private NormalBalance normalBalance;

    /** Akun header hanya untuk pengelompokan; tidak boleh dipakai di jurnal. */
    @Column(name = "is_header", nullable = false)
    private boolean header;

    @Column(name = "is_cash_bank", nullable = false)
    private boolean cashBank;

    @Enumerated(EnumType.STRING)
    @Column(name = "cash_flow_category", length = 20)
    private CashFlowCategory cashFlowCategory;

    @Column(nullable = false)
    private int level = 1;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    public String label() {
        return code + " - " + name;
    }
}
