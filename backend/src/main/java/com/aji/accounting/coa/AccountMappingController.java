package com.aji.accounting.coa;

import com.aji.accounting.coa.AccountMappingService.MappingResponse;
import com.aji.accounting.coa.AccountMappingService.UpdateMappingRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Account Mapping")
@RestController
@RequestMapping("/api/v1/account-mappings")
@RequiredArgsConstructor
public class AccountMappingController {

    private final AccountMappingService service;

    @GetMapping
    @PreAuthorize("hasAuthority('SETTINGS:VIEW')")
    public List<MappingResponse> list() {
        return service.list();
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasAuthority('SETTINGS:EDIT')")
    public MappingResponse update(@PathVariable AccountMappingKey key, @Valid @RequestBody UpdateMappingRequest req) {
        return service.update(key, req.accountId());
    }
}
