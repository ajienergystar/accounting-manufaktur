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

public interface ApPaymentRepository extends JpaRepository<ApPayment, Long> {

    @Query("""
            select distinct p from ApPayment p join fetch p.supplier left join fetch p.cashAccount
            left join fetch p.allocations a left join fetch a.invoice
            where p.id = :id
            """)
    Optional<ApPayment> findFull(@Param("id") Long id);

    @EntityGraph(attributePaths = {"supplier", "cashAccount"})
    @Query("""
            select p from ApPayment p
            where (:from is null or p.docDate >= :from) and (:to is null or p.docDate <= :to)
              and (:supplierId is null or p.supplier.id = :supplierId)
              and (:type is null or p.paymentType = :type)
              and (:q is null or lower(p.docNo) like lower(concat('%', :q, '%')) or lower(p.referenceNo) like lower(concat('%', :q, '%')))
            """)
    Page<ApPayment> search(@Param("from") LocalDate from, @Param("to") LocalDate to,
                           @Param("supplierId") Long supplierId, @Param("type") ApPayment.Type type,
                           @Param("q") String q, Pageable pageable);

    /** Saldo uang muka pemasok = uang muka dibayar - uang muka dikompensasikan. */
    @Query("""
            select coalesce(sum(case when p.paymentType = com.aji.accounting.purchasing.ApPayment.Type.ADVANCE then p.amount
                                     when p.paymentType = com.aji.accounting.purchasing.ApPayment.Type.ADVANCE_OFFSET then -p.amount
                                     else 0 end), 0)
            from ApPayment p
            where p.supplier.id = :supplierId and p.status = com.aji.accounting.inventory.DocStatus.POSTED
            """)
    BigDecimal advanceBalance(@Param("supplierId") Long supplierId);

    @Query("select a from ApPaymentAllocation a join fetch a.payment p where a.invoice.id = :invoiceId and p.status = com.aji.accounting.inventory.DocStatus.POSTED order by p.docDate")
    List<ApPayment.Allocation> findAllocationsForInvoice(@Param("invoiceId") Long invoiceId);
}
