package com.aji.accounting.company;

import com.aji.accounting.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "companies")
public class Company extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

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

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency = "IDR";

    @Column(name = "fiscal_year_start_month", nullable = false)
    private Integer fiscalYearStartMonth = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_costing_method", nullable = false, length = 20)
    private InventoryCostingMethod inventoryCostingMethod = InventoryCostingMethod.MOVING_AVERAGE;

    @Column(name = "allow_negative_stock", nullable = false)
    private boolean allowNegativeStock = false;
}
