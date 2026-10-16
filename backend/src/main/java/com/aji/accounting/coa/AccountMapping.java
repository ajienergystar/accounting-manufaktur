package com.aji.accounting.coa;

import com.aji.accounting.common.entity.BaseEntity;
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

@Getter
@Setter
@Entity
@Table(name = "account_mappings")
public class AccountMapping extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_key", nullable = false, length = 50, unique = true)
    private AccountMappingKey mappingKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "group_name", nullable = false, length = 50)
    private String groupName;

    @Column(nullable = false, length = 255)
    private String description;
}
