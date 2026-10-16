package com.aji.accounting.period;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PeriodRepository extends JpaRepository<Period, Long> {

    @Query("select p from Period p where :date between p.startDate and p.endDate")
    Optional<Period> findContaining(LocalDate date);

    @Query("select p from Period p join fetch p.fiscalYear where p.id = :id")
    Optional<Period> findWithYear(Long id);

    List<Period> findByFiscalYearIdOrderByPeriodNo(Long fiscalYearId);

    @Query("select count(p) from Period p where p.fiscalYear.id = :fyId and p.periodNo < :periodNo and p.status = com.aji.accounting.period.PeriodStatus.OPEN")
    long countOpenBefore(Long fyId, int periodNo);

    @Query("select count(p) from Period p where p.fiscalYear.id = :fyId and p.periodNo > :periodNo and p.status <> com.aji.accounting.period.PeriodStatus.OPEN")
    long countClosedAfter(Long fyId, int periodNo);
}
