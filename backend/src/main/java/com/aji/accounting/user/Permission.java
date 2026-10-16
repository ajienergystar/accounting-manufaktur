package com.aji.accounting.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Permission granular, kode berformat MODULE:ACTION, contoh LEDGER:POST. */
@Getter
@Setter
@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60, unique = true)
    private String code;

    @Column(nullable = false, length = 30)
    private String module;

    @Column(nullable = false, length = 20)
    private String action;

    @Column(length = 255)
    private String description;
}
