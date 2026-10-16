package com.aji.accounting.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockBalanceRepository extends JpaRepository<StockBalance, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from StockBalance b where b.item.id = :itemId and b.warehouse.id = :whId")
    Optional<StockBalance> findForUpdate(@Param("itemId") Long itemId, @Param("whId") Long warehouseId);

    @Query("select b from StockBalance b where b.item.id = :itemId and b.warehouse.id = :whId")
    Optional<StockBalance> find(@Param("itemId") Long itemId, @Param("whId") Long warehouseId);

    @Query(value = """
            select b from StockBalance b join fetch b.item i join fetch i.uom join fetch b.warehouse w
            where (:whId is null or w.id = :whId)
              and (:type is null or i.itemType = :type)
              and (:q is null or lower(i.code) like lower(concat('%', :q, '%')) or lower(i.name) like lower(concat('%', :q, '%')))
              and (:nonZero = false or b.qty <> 0)
              and (:lowOnly = false or b.qty <= i.minStock)
            """,
            countQuery = """
            select count(b) from StockBalance b join b.item i join b.warehouse w
            where (:whId is null or w.id = :whId)
              and (:type is null or i.itemType = :type)
              and (:q is null or lower(i.code) like lower(concat('%', :q, '%')) or lower(i.name) like lower(concat('%', :q, '%')))
              and (:nonZero = false or b.qty <> 0)
              and (:lowOnly = false or b.qty <= i.minStock)
            """)
    Page<StockBalance> search(@Param("whId") Long warehouseId,
                              @Param("type") com.aji.accounting.masterdata.item.ItemType type,
                              @Param("q") String q, @Param("nonZero") boolean nonZero,
                              @Param("lowOnly") boolean lowOnly, Pageable pageable);

    List<StockBalance> findByWarehouseId(Long warehouseId);
}
