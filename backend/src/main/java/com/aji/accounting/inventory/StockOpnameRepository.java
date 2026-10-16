package com.aji.accounting.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface StockOpnameRepository extends JpaRepository<StockOpname, Long> {
    @Query("select distinct o from StockOpname o join fetch o.warehouse left join fetch o.lines l left join fetch l.item i left join fetch i.uom where o.id = :id")
    Optional<StockOpname> findFull(@Param("id") Long id);

    @EntityGraph(attributePaths = {"warehouse"})
    @Query("select o from StockOpname o order by o.docDate desc, o.id desc")
    Page<StockOpname> findAllWithWarehouse(Pageable pageable);
}
