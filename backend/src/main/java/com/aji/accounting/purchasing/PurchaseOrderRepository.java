package com.aji.accounting.purchasing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Query("""
            select distinct o from PurchaseOrder o join fetch o.supplier join fetch o.warehouse left join fetch o.taxRateRef
            left join fetch o.request left join fetch o.lines l left join fetch l.item i left join fetch i.uom
            where o.id = :id
            """)
    Optional<PurchaseOrder> findFull(@Param("id") Long id);

    @EntityGraph(attributePaths = {"supplier", "warehouse"})
    @Query("""
            select o from PurchaseOrder o
            where (:from is null or o.docDate >= :from) and (:to is null or o.docDate <= :to)
              and (:supplierId is null or o.supplier.id = :supplierId)
              and (:status is null or o.status = :status)
              and (:q is null or lower(o.docNo) like lower(concat('%', :q, '%')) or lower(o.notes) like lower(concat('%', :q, '%')))
            """)
    Page<PurchaseOrder> search(@Param("from") LocalDate from, @Param("to") LocalDate to,
                               @Param("supplierId") Long supplierId, @Param("status") PurchaseOrder.Status status,
                               @Param("q") String q, Pageable pageable);

    @EntityGraph(attributePaths = {"supplier"})
    @Query("select o from PurchaseOrder o where o.supplier.id = :supplierId and o.status in (com.aji.accounting.purchasing.PurchaseOrder.Status.APPROVED, com.aji.accounting.purchasing.PurchaseOrder.Status.PARTIAL) order by o.docDate")
    List<PurchaseOrder> findOpenBySupplier(@Param("supplierId") Long supplierId);
}
