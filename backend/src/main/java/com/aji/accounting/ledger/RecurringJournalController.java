package com.aji.accounting.ledger;

import com.aji.accounting.ledger.RecurringJournalService.GenerateResult;
import com.aji.accounting.ledger.RecurringJournalService.RecurringRequest;
import com.aji.accounting.ledger.RecurringJournalService.RecurringResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Jurnal Berulang")
@RestController
@RequestMapping("/api/v1/recurring-journals")
@RequiredArgsConstructor
public class RecurringJournalController {

    private final RecurringJournalService service;

    @GetMapping
    @PreAuthorize("hasAuthority('LEDGER:VIEW')")
    public List<RecurringResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LEDGER:CREATE')")
    public RecurringResponse create(@Valid @RequestBody RecurringRequest req) {
        return service.save(null, req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LEDGER:EDIT')")
    public RecurringResponse update(@PathVariable Long id, @Valid @RequestBody RecurringRequest req) {
        return service.save(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('LEDGER:DELETE')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('LEDGER:POST')")
    public GenerateResult generateNow() {
        return service.generateDue(LocalDate.now());
    }
}
