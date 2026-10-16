package com.aji.accounting.period;

import com.aji.accounting.period.PeriodService.FiscalYearResponse;
import com.aji.accounting.period.PeriodService.PeriodResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Periode Akuntansi")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PeriodController {

    public record CreateFiscalYearRequest(@Min(2000) @Max(2100) int year) {
    }

    private final PeriodService service;

    @GetMapping("/fiscal-years")
    @PreAuthorize("hasAuthority('PERIOD:VIEW')")
    public List<FiscalYearResponse> list() {
        return service.list();
    }

    @PostMapping("/fiscal-years")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PERIOD:CREATE')")
    public FiscalYearResponse create(@Valid @RequestBody CreateFiscalYearRequest req) {
        return service.createFiscalYear(req.year());
    }

    @PostMapping("/periods/{id}/close")
    @PreAuthorize("hasAuthority('PERIOD:POST')")
    public PeriodResponse close(@PathVariable Long id) {
        return service.close(id);
    }

    @PostMapping("/periods/{id}/reopen")
    @PreAuthorize("hasRole('ADMIN')")
    public PeriodResponse reopen(@PathVariable Long id) {
        return service.reopen(id);
    }

    @PostMapping("/periods/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public PeriodResponse lock(@PathVariable Long id) {
        return service.lock(id);
    }
}
