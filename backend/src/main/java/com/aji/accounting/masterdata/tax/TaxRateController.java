package com.aji.accounting.masterdata.tax;

import com.aji.accounting.common.master.AbstractMasterController;
import com.aji.accounting.masterdata.tax.TaxRateService.TaxRateRequest;
import com.aji.accounting.masterdata.tax.TaxRateService.TaxRateResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Master - Pajak")
@RestController
@RequestMapping("/api/v1/tax-rates")
public class TaxRateController extends AbstractMasterController<TaxRateRequest, TaxRateResponse> {
    public TaxRateController(TaxRateService service) {
        super(service);
    }
}
