package com.aji.accounting.audit;

import com.aji.accounting.common.web.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Tag(name = "Audit Trail")
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository repository;

    public record AuditLogResponse(Long id, LocalDateTime occurredAt, String username, String action,
                                   String entityType, String entityId, String description,
                                   String beforeData, String afterData, String ipAddress) {
        static AuditLogResponse from(AuditLog a) {
            return new AuditLogResponse(a.getId(), a.getOccurredAt(), a.getUsername(), a.getAction(),
                    a.getEntityType(), a.getEntityId(), a.getDescription(), a.getBeforeData(), a.getAfterData(),
                    a.getIpAddress());
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT:VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 200), Sort.by(Sort.Direction.DESC, "occurredAt"));
        return PageResponse.of(repository.search(blankToNull(username), blankToNull(entityType),
                blankToNull(entityId), blankToNull(action),
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.plusDays(1).atStartOfDay(), pageable), AuditLogResponse::from);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
