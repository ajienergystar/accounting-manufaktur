package com.aji.accounting.system;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "System")
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbc;

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("time", OffsetDateTime.now());
        String db;
        try {
            db = jdbc.queryForObject("SELECT VERSION()", String.class);
        } catch (Exception e) {
            db = "DOWN: " + e.getMessage();
            body.put("status", "DEGRADED");
        }
        body.put("database", db);
        return body;
    }
}
