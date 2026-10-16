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

public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, Long> {

    @Query("""
            select distinct r from PurchaseReturn r join fetch r.supplier join fetch r.receipt join fetch r.warehouse
            left join fetch r.invoice left join fetch r.lines l left join fetch l.item i left join fetch i.uom
            left join fetch l.receiptLine
            where r.id = :id
            """)
    Optional<PurchaseReturn> findFull(@Param("id") Long id);

    @EntityGraph(attributePaths = {"supplier", "receipt", "invoice"})
    @Query("""
            select r from PurchaseReturn r
            where (:from is null or r.docDate >= :from) and (:to is null or r.docDate <= :to)
              and (:supplierId is null or r.supplier.id = :supplierId)
              and (:q is null or lower(r.docNo) like lower(concat('%', :q, '%')))
            """)
    Page<PurchaseReturn> search(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                @Param("supplierId") Long supplierId, @Param("q") String q, Pageable pageable);

    @Query("""
            select coalesce(sum(l.qty), 0) from PurchaseReturnLine l
            where l.receiptLine.id = :receiptLineId and l.purchaseReturn.invoice.id = :invoiceId
              and l.purchaseReturn.status = com.aji.accounting.inventory.DocStatus.POSTED
            """)
    BigDecimal sumReturnedAgainstInvoice(@Param("invoiceId") Long invoiceId, @Param("receiptLineId") Long receiptLineId);

    @Query("""
            select coalesce(sum(l.totalCost), 0) from PurchaseReturnLine l
            where l.receiptLine.id = :receiptLineId and l.purchaseReturn.invoice is null
              and l.purchaseReturn.status = com.aji.accounting.inventory.DocStatus.POSTED
            """)
    BigDecimal sumUninvoicedReturnCost(@Param("receiptLineId") Long receiptLineId);
}
