package com.aji.accounting.ledger;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    @Query("""
            select distinct j from JournalEntry j
            left join fetch j.lines l
            left join fetch l.account
            left join fetch l.department
            left join fetch j.period
            where j.id = :id
            """)
    Optional<JournalEntry> findWithLines(@Param("id") Long id);

    @Query("""
            select j from JournalEntry j
            where (:from is null or j.journalDate >= :from)
              and (:to is null or j.journalDate <= :to)
              and (:status is null or j.status = :status)
              and (:type is null or j.journalType = :type)
              and (:sourceType is null or j.sourceType = :sourceType)
              and (:q is null or lower(j.journalNo) like lower(concat('%', :q, '%'))
                   or lower(j.description) like lower(concat('%', :q, '%'))
                   or lower(j.sourceNo) like lower(concat('%', :q, '%')))
            """)
    Page<JournalEntry> search(@Param("from") LocalDate from, @Param("to") LocalDate to,
                              @Param("status") JournalStatus status, @Param("type") JournalType type,
                              @Param("sourceType") SourceType sourceType, @Param("q") String q, Pageable pageable);

    @Query("select j from JournalEntry j where j.sourceType = :type and j.sourceId = :id and j.status = com.aji.accounting.ledger.JournalStatus.POSTED and j.reversedBy is null")
    List<JournalEntry> findActiveBySource(@Param("type") SourceType type, @Param("id") Long id);

    @Query("select count(j) from JournalEntry j where j.period.id = :periodId and j.status = com.aji.accounting.ledger.JournalStatus.DRAFT")
    long countDraftsInPeriod(@Param("periodId") Long periodId);
}
