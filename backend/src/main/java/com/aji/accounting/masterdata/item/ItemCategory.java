package com.aji.accounting.masterdata.item;

import com.aji.accounting.common.entity.MasterEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "item_categories")
public class ItemCategory extends MasterEntity {
}
