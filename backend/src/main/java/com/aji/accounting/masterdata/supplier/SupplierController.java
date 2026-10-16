package com.aji.accounting.masterdata.supplier;

import com.aji.accounting.common.master.AbstractMasterController;
import com.aji.accounting.masterdata.supplier.SupplierService.SupplierRequest;
import com.aji.accounting.masterdata.supplier.SupplierService.SupplierResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Master - Pemasok")
@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController extends AbstractMasterController<SupplierRequest, SupplierResponse> {
    public SupplierController(SupplierService service) {
        super(service);
    }
}
