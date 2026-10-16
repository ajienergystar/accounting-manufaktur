package com.aji.accounting.inventory;

import com.aji.accounting.ledger.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Query("select m from StockMovement m join fetch m.item join fetch m.warehouse where m.sourceType = :type and m.sourceId = :id order by m.id")
    List<StockMovement> findBySource(@Param("type") SourceType type, @Param("id") Long id);
}
