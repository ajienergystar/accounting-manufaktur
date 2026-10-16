package com.aji.accounting.audit;

import com.aji.accounting.config.CurrentUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * Mencatat audit trail. Ikut transaksi pemanggil, sehingga bila transaksi bisnis gagal
 * (rollback), catatan audit-nya pun ikut batal dan tidak ada log palsu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void log(String action, String entityType, Object entityId, String description, Object before, Object after) {
        save(CurrentUser.usernameOrSystem(), action, entityType, entityId, description, before, after);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void log(String action, String entityType, Object entityId, String description) {
        log(action, entityType, entityId, description, null, null);
    }

    /** Untuk event yang dicatat sebelum user terautentikasi (mis. login). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void logAs(String username, String action, String entityType, Object entityId, String description) {
        save(username, action, entityType, entityId, description, null, null);
    }

    private void save(String username, String action, String entityType, Object entityId, String description,
                      Object before, Object after) {
        AuditLog a = new AuditLog();
        a.setOccurredAt(LocalDateTime.now());
        a.setUsername(username);
        a.setAction(action);
        a.setEntityType(entityType);
        a.setEntityId(entityId == null ? null : String.valueOf(entityId));
        a.setDescription(description == null ? null : truncate(description, 500));
        a.setBeforeData(toJson(before));
        a.setAfterData(toJson(after));
        a.setIpAddress(clientIp());
        repository.save(a);
    }

    private String toJson(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            log.warn("Gagal serialisasi data audit: {}", e.getMessage());
            return null;
        }
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    public static String clientIp() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        HttpServletRequest req = attrs.getRequest();
        String fwd = req.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            return fwd.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
