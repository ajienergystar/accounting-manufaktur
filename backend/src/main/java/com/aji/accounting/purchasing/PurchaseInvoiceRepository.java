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

public interface PurchaseInvoiceRepository extends JpaRepository<PurchaseInvoice, Long> {

    @Query("""
            select distinct p from PurchaseInvoice p join fetch p.supplier left join fetch p.taxRateRef left join fetch p.pphRateRef
            left join fetch p.lines l left join fetch l.item left join fetch l.account left join fetch l.department
            left join fetch l.receiptLine rl left join fetch rl.receipt
            where p.id = :id
            """)
    Optional<PurchaseInvoice> findFull(@Param("id") Long id);

    @EntityGraph(attributePaths = {"supplier"})
    @Query("""
            select p from PurchaseInvoice p
            where (:from is null or p.docDate >= :from) and (:to is null or p.docDate <= :to)
              and (:supplierId is null or p.supplier.id = :supplierId)
              and (:status is null or p.status = :status)
              and (:q is null or lower(p.docNo) like lower(concat('%', :q, '%'))
                   or lower(p.supplierInvoiceNo) like lower(concat('%', :q, '%'))
                   or lower(p.taxInvoiceNo) like lower(concat('%', :q, '%')))
            """)
    Page<PurchaseInvoice> search(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                 @Param("supplierId") Long supplierId, @Param("status") PurchaseInvoice.Status status,
                                 @Param("q") String q, Pageable pageable);

    @EntityGraph(attributePaths = {"supplier"})
    @Query("""
            select p from PurchaseInvoice p
            where p.status in (com.aji.accounting.purchasing.PurchaseInvoice.Status.UNPAID, com.aji.accounting.purchasing.PurchaseInvoice.Status.PARTIAL)
              and (:supplierId is null or p.supplier.id = :supplierId)
            order by p.dueDate, p.id
            """)
    List<PurchaseInvoice> findOutstanding(@Param("supplierId") Long supplierId);

    boolean existsBySupplierIdAndSupplierInvoiceNoIgnoreCaseAndStatusNot(Long supplierId, String no,
                                                                         PurchaseInvoice.Status status);

    @Query("""
            select coalesce(sum(l.receiptCost), 0) from PurchaseInvoiceLine l
            where l.receiptLine.id = :receiptLineId and l.invoice.status <> com.aji.accounting.purchasing.PurchaseInvoice.Status.VOID
            """)
    BigDecimal sumReceiptCostCleared(@Param("receiptLineId") Long receiptLineId);

    @Query("""
            select l from PurchaseInvoiceLine l join fetch l.invoice p
            where l.receiptLine.id = :receiptLineId and p.id = :invoiceId
            """)
    Optional<PurchaseInvoice.Line> findLineForReceipt(@Param("invoiceId") Long invoiceId,
                                                      @Param("receiptLineId") Long receiptLineId);
}
