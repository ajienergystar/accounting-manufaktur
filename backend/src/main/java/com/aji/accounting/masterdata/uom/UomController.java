package com.aji.accounting.masterdata.uom;

import com.aji.accounting.common.master.AbstractMasterController;
import com.aji.accounting.masterdata.uom.UomService.ConversionRequest;
import com.aji.accounting.masterdata.uom.UomService.ConversionResponse;
import com.aji.accounting.masterdata.uom.UomService.UomRequest;
import com.aji.accounting.masterdata.uom.UomService.UomResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Master - Satuan")
@RestController
@RequestMapping("/api/v1/uoms")
public class UomController extends AbstractMasterController<UomRequest, UomResponse> {

    private final UomService uomService;

    public UomController(UomService service) {
        super(service);
        this.uomService = service;
    }

    @GetMapping("/conversions")
    @PreAuthorize("hasAuthority('MASTER:VIEW')")
    public List<ConversionResponse> conversions() {
        return uomService.conversions();
    }

    @PostMapping("/conversions")
    @PreAuthorize("hasAuthority('MASTER:EDIT')")
    public ConversionResponse saveConversion(@Valid @RequestBody ConversionRequest req) {
        return uomService.saveConversion(req);
    }

    @DeleteMapping("/conversions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('MASTER:DELETE')")
    public void deleteConversion(@PathVariable Long id) {
        uomService.deleteConversion(id);
    }
}
