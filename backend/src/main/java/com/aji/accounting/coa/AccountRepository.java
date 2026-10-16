package com.aji.accounting.coa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByParentId(Long parentId);

    @Query("select a from Account a left join fetch a.parent order by a.code")
    List<Account> findAllOrdered();

    @Query("""
            select a from Account a
            where a.header = false and a.active = true
              and (:type is null or a.accountType = :type)
              and (:cashBank is null or a.cashBank = :cashBank)
              and (:q is null or lower(a.code) like lower(concat('%', :q, '%'))
                   or lower(a.name) like lower(concat('%', :q, '%')))
            order by a.code
            """)
    List<Account> findPostable(@Param("q") String q, @Param("type") AccountType type,
                               @Param("cashBank") Boolean cashBank);
}
