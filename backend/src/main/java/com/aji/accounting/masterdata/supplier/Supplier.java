package com.aji.accounting.masterdata.supplier;

import com.aji.accounting.common.entity.MasterEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name = "suppliers")
public class Supplier extends MasterEntity {

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

}
