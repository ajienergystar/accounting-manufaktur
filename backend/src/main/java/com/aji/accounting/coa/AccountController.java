package com.aji.accounting.coa;

import com.aji.accounting.coa.AccountDtos.AccountOption;
import com.aji.accounting.coa.AccountDtos.AccountRequest;
import com.aji.accounting.coa.AccountDtos.AccountResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Bagan Akun")
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;

    @GetMapping
    @PreAuthorize("hasAuthority('COA:VIEW')")
    @Operation(summary = "Seluruh bagan akun terurut kode (untuk tampilan pohon)")
    public List<AccountResponse> tree() {
        return service.tree();
    }

    @GetMapping("/postable")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Akun detail aktif yang boleh dipakai di jurnal (untuk AccountPicker)")
    public List<AccountOption> postable(@RequestParam(required = false) String q,
                                        @RequestParam(required = false) AccountType type,
                                        @RequestParam(required = false) Boolean cashBank) {
        return service.postable(q, type, cashBank);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COA:VIEW')")
    public AccountResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('COA:CREATE')")
    public AccountResponse create(@Valid @RequestBody AccountRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COA:EDIT')")
    public AccountResponse update(@PathVariable Long id, @Valid @RequestBody AccountRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('COA:DELETE')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
