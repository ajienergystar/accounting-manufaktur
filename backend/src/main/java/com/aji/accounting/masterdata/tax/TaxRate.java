package com.aji.accounting.masterdata.tax;

import com.aji.accounting.coa.Account;
import com.aji.accounting.common.entity.MasterEntity;
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

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "tax_rates")
public class TaxRate extends MasterEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false, length = 20)
    private TaxType taxType;

    /** Persentase, contoh 11.0000 untuk PPN 11%. */
    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal rate;

    /** Opsional: override akun default dari account mapping. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;
}
