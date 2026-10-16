package com.aji.accounting.coa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AccountMappingRepository extends JpaRepository<AccountMapping, Long> {

    @Query("select m from AccountMapping m join fetch m.account where m.mappingKey = :key")
    Optional<AccountMapping> findByKey(AccountMappingKey key);

    @Query("select m from AccountMapping m join fetch m.account order by m.groupName, m.mappingKey")
    List<AccountMapping> findAllWithAccount();

    boolean existsByAccountId(Long accountId);
}
