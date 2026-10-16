package com.aji.accounting.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {
    @Query("select distinct t from StockTransfer t join fetch t.fromWarehouse join fetch t.toWarehouse left join fetch t.lines l left join fetch l.item i left join fetch i.uom where t.id = :id")
    Optional<StockTransfer> findFull(@Param("id") Long id);

    @EntityGraph(attributePaths = {"fromWarehouse", "toWarehouse"})
    @Query("""
            select t from StockTransfer t
            where (:from is null or t.docDate >= :from) and (:to is null or t.docDate <= :to)
              and (:q is null or lower(t.docNo) like lower(concat('%', :q, '%')))
            """)
    Page<StockTransfer> search(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("q") String q,
                               Pageable pageable);
}
