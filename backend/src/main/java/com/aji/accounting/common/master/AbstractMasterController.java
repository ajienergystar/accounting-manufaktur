package com.aji.accounting.common.master;

import com.aji.accounting.common.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

/** Endpoint REST standar master data (permission modul MASTER). */
public abstract class AbstractMasterController<Q extends MasterRequest, R> {

    protected final AbstractMasterService<?, Q, R> service;

    protected AbstractMasterController(AbstractMasterService<?, Q, R> service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MASTER:VIEW')")
    public PageResponse<R> search(@RequestParam(required = false) String q,
                                  @RequestParam(required = false) Boolean active,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(required = false) String sort) {
        return service.search(q, active, page, size, sort);
    }

    @GetMapping("/options")
    @PreAuthorize("isAuthenticated()")
    public List<OptionResponse> options() {
        return service.options();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MASTER:VIEW')")
    public R get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('MASTER:CREATE')")
    public R create(@Valid @RequestBody Q req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MASTER:EDIT')")
    public R update(@PathVariable Long id, @Valid @RequestBody Q req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('MASTER:DELETE')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
