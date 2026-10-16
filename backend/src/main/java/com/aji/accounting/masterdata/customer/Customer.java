package com.aji.accounting.masterdata.customer;

import com.aji.accounting.common.entity.MasterEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "customers")
public class Customer extends MasterEntity {

    @Column(length = 30)
    private String npwp;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 30)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "payment_term_days", nullable = false)
    private int paymentTermDays = 30;

    /** Batas kredit; 0 = tanpa batas. */
    @Column(name = "credit_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;
}
