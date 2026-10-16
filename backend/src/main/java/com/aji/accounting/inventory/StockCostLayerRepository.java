package com.aji.accounting.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockCostLayerRepository extends JpaRepository<StockCostLayer, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select l from StockCostLayer l
            where l.itemId = :itemId and l.warehouseId = :whId and l.qtyRemaining > 0
            order by l.layerDate, l.id
            """)
    List<StockCostLayer> findOpenLayersForUpdate(@Param("itemId") Long itemId, @Param("whId") Long warehouseId);

    Optional<StockCostLayer> findByMovementId(Long movementId);
}
