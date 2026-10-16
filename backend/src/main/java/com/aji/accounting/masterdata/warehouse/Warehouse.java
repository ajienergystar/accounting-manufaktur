package com.aji.accounting.masterdata.warehouse;

import com.aji.accounting.common.entity.MasterEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "warehouses")
public class Warehouse extends MasterEntity {
    @Column(length = 255)
    private String address;
}
