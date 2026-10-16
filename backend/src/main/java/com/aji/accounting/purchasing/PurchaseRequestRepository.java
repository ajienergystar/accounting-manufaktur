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

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {

    @Query("select distinct r from PurchaseRequest r left join fetch r.department left join fetch r.lines l left join fetch l.item i left join fetch i.uom where r.id = :id")
    Optional<PurchaseRequest> findFull(@Param("id") Long id);

    @EntityGraph(attributePaths = {"department"})
    @Query("""
            select r from PurchaseRequest r
            where (:from is null or r.docDate >= :from) and (:to is null or r.docDate <= :to)
              and (:status is null or r.status = :status)
              and (:q is null or lower(r.docNo) like lower(concat('%', :q, '%')) or lower(r.notes) like lower(concat('%', :q, '%')))
            """)
    Page<PurchaseRequest> search(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                 @Param("status") PurchaseRequest.Status status, @Param("q") String q, Pageable pageable);
}
