package com.aji.accounting.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringJournalRepository extends JpaRepository<RecurringJournal, Long> {

    @Query("select distinct r from RecurringJournal r left join fetch r.lines l left join fetch l.account left join fetch l.department where r.id = :id")
    Optional<RecurringJournal> findWithLines(@Param("id") Long id);

    @Query("select r from RecurringJournal r order by r.active desc, r.name")
    List<RecurringJournal> findAllOrdered();

    @Query("""
            select r.id from RecurringJournal r
            where r.active = true and r.nextRunDate <= :today
              and (r.endDate is null or r.nextRunDate <= r.endDate)
            """)
    List<Long> findDueIds(@Param("today") LocalDate today);
}
