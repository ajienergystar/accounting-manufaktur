package com.aji.accounting.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select a from AuditLog a
            where (:username is null or a.username = :username)
              and (:entityType is null or a.entityType = :entityType)
              and (:entityId is null or a.entityId = :entityId)
              and (:action is null or a.action = :action)
              and (:from is null or a.occurredAt >= :from)
              and (:to is null or a.occurredAt < :to)
            """)
    Page<AuditLog> search(@Param("username") String username,
                          @Param("entityType") String entityType,
                          @Param("entityId") String entityId,
                          @Param("action") String action,
                          @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to,
                          Pageable pageable);
}
