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

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {

    @Query("""
            select distinct g from GoodsReceipt g join fetch g.supplier join fetch g.warehouse left join fetch g.order
            left join fetch g.lines l left join fetch l.item i left join fetch i.uom left join fetch l.orderLine
            where g.id = :id
            """)
    Optional<GoodsReceipt> findFull(@Param("id") Long id);

    @EntityGraph(attributePaths = {"supplier", "warehouse", "order"})
    @Query("""
            select g from GoodsReceipt g
            where (:from is null or g.docDate >= :from) and (:to is null or g.docDate <= :to)
              and (:supplierId is null or g.supplier.id = :supplierId)
              and (:status is null or g.status = :status)
              and (:q is null or lower(g.docNo) like lower(concat('%', :q, '%')) or lower(g.deliveryNoteNo) like lower(concat('%', :q, '%')))
            """)
    Page<GoodsReceipt> search(@Param("from") LocalDate from, @Param("to") LocalDate to,
                              @Param("supplierId") Long supplierId,
                              @Param("status") com.aji.accounting.inventory.DocStatus status,
                              @Param("q") String q, Pageable pageable);

    /** Baris penerimaan yang belum (sepenuhnya) difakturkan untuk pemasok tertentu. */
    @Query("""
            select l from GoodsReceiptLine l join fetch l.receipt r join fetch l.item i join fetch i.uom
            where r.supplier.id = :supplierId and r.status = com.aji.accounting.inventory.DocStatus.POSTED
              and l.qty - l.qtyInvoiced - l.qtyReturned > 0
            order by r.docDate, r.id, l.lineNo
            """)
    List<GoodsReceipt.Line> findUninvoicedLines(@Param("supplierId") Long supplierId);

    @Query("select l from GoodsReceiptLine l join fetch l.receipt r join fetch l.item where l.id in :ids")
    List<GoodsReceipt.Line> findLinesByIds(@Param("ids") List<Long> ids);
}
