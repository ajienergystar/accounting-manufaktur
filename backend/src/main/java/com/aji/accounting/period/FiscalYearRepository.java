package com.aji.accounting.period;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FiscalYearRepository extends JpaRepository<FiscalYear, Long> {

    boolean existsByYear(int year);

    @Query("select distinct f from FiscalYear f left join fetch f.periods order by f.year desc")
    List<FiscalYear> findAllWithPeriods();

    @Query("select f from FiscalYear f where :date between f.startDate and f.endDate")
    Optional<FiscalYear> findContaining(LocalDate date);

    Optional<FiscalYear> findByYear(int year);
}
