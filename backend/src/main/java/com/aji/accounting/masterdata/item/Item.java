package com.aji.accounting.masterdata.item;

import com.aji.accounting.common.entity.MasterEntity;
import com.aji.accounting.masterdata.uom.Uom;
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
@Table(name = "items")
public class Item extends MasterEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private ItemType itemType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ItemCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uom_id", nullable = false)
    private Uom uom;

    /** Biaya standar per unit (dipakai standard costing & analisis selisih). */
    @Column(name = "standard_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal standardCost = BigDecimal.ZERO;

    @Column(name = "selling_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    @Column(name = "min_stock", nullable = false, precision = 19, scale = 4)
    private BigDecimal minStock = BigDecimal.ZERO;

    @Column(name = "reorder_point", nullable = false, precision = 19, scale = 4)
    private BigDecimal reorderPoint = BigDecimal.ZERO;

    /** false untuk item jasa (tidak dicatat di persediaan). */
    @Column(name = "is_stocked", nullable = false)
    private boolean stocked = true;

    @Column(length = 500)
    private String description;
}
