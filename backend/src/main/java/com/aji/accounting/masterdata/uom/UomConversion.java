package com.aji.accounting.masterdata.uom;

import com.aji.accounting.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 1 fromUom = factor x toUom. */
@Getter
@Setter
@Entity
@Table(name = "uom_conversions")
public class UomConversion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_uom_id", nullable = false)
    private Uom fromUom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_uom_id", nullable = false)
    private Uom toUom;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal factor;
}
