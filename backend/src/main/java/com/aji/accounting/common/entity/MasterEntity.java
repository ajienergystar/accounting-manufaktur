package com.aji.accounting.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/** Superclass master data: kode unik, nama, dan flag aktif (soft delete). */
@Getter
@Setter
@MappedSuperclass
public abstract class MasterEntity extends BaseEntity {

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private boolean active = true;
}
