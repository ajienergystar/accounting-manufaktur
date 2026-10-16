package com.aji.accounting.ledger;

import com.aji.accounting.ledger.LedgerQueryService.GeneralLedger;
import com.aji.accounting.ledger.LedgerQueryService.TrialBalance;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Buku Besar")
@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerQueryService service;

    @GetMapping("/general-ledger")
    @PreAuthorize("hasAuthority('LEDGER:VIEW')")
    public GeneralLedger generalLedger(@RequestParam Long accountId,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                       @RequestParam(required = false) Long departmentId) {
        return service.generalLedger(accountId, from, to, departmentId);
    }

    @GetMapping("/trial-balance")
    @PreAuthorize("hasAuthority('LEDGER:VIEW')")
    public TrialBalance trialBalance(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                     @RequestParam(defaultValue = "false") boolean includeZero) {
        return service.trialBalance(from, to, includeZero);
    }
}
