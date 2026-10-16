package com.aji.accounting.masterdata.uom;

import com.aji.accounting.common.entity.MasterEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "uoms")
public class Uom extends MasterEntity {
}
