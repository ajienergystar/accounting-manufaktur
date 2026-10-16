package com.aji.accounting.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {
    @Query("select distinct a from StockAdjustment a join fetch a.warehouse left join fetch a.lines l left join fetch l.item i left join fetch i.uom where a.id = :id")
    Optional<StockAdjustment> findFull(@Param("id") Long id);

    @EntityGraph(attributePaths = {"warehouse"})
    @Query("""
            select a from StockAdjustment a
            where (:from is null or a.docDate >= :from) and (:to is null or a.docDate <= :to)
              and (:q is null or lower(a.docNo) like lower(concat('%', :q, '%')) or lower(a.reason) like lower(concat('%', :q, '%')))
            """)
    Page<StockAdjustment> search(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("q") String q,
                                 Pageable pageable);
}
