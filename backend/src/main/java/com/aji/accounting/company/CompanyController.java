package com.aji.accounting.company;

import com.aji.accounting.company.CompanyService.CompanyRequest;
import com.aji.accounting.company.CompanyService.CompanyResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Perusahaan")
@RestController
@RequestMapping("/api/v1/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public CompanyResponse get() {
        return service.get();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('SETTINGS:EDIT')")
    public CompanyResponse update(@Valid @RequestBody CompanyRequest req) {
        return service.update(req);
    }
}
