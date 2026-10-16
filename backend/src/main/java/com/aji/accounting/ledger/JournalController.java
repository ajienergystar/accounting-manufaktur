package com.aji.accounting.ledger;

import com.aji.accounting.common.web.PageResponse;
import com.aji.accounting.ledger.JournalDtos.JournalDetail;
import com.aji.accounting.ledger.JournalDtos.JournalRequest;
import com.aji.accounting.ledger.JournalDtos.JournalSummary;
import com.aji.accounting.ledger.JournalDtos.ReverseRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Jurnal")
@RestController
@RequestMapping("/api/v1/journals")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService service;

    @GetMapping
    @PreAuthorize("hasAuthority('LEDGER:VIEW')")
    public PageResponse<JournalSummary> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) JournalStatus status,
            @RequestParam(required = false) JournalType type,
            @RequestParam(required = false) SourceType sourceType,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.search(from, to, status, type, sourceType, q, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LEDGER:VIEW')")
    public JournalDetail get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LEDGER:CREATE') and (!#req.post() or hasAuthority('LEDGER:POST'))")
    public JournalDetail create(@Valid @RequestBody JournalRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LEDGER:EDIT') and (!#req.post() or hasAuthority('LEDGER:POST'))")
    public JournalDetail update(@PathVariable Long id, @Valid @RequestBody JournalRequest req) {
        return service.update(id, req);
    }

    @PostMapping("/opening-balance")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LEDGER:POST')")
    public JournalDetail openingBalance(@Valid @RequestBody JournalDtos.OpeningBalanceRequest req) {
        return service.openingBalance(req);
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasAuthority('LEDGER:POST')")
    public JournalDetail post(@PathVariable Long id) {
        return service.post(id);
    }

    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasAuthority('LEDGER:POST')")
    public JournalDetail reverse(@PathVariable Long id, @Valid @RequestBody ReverseRequest req) {
        return service.reverse(id, req.date(), req.reason());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('LEDGER:DELETE')")
    public void deleteDraft(@PathVariable Long id) {
        service.deleteDraft(id);
    }
}
