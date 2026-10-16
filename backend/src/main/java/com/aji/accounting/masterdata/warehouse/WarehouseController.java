package com.aji.accounting.masterdata.warehouse;

import com.aji.accounting.common.master.AbstractMasterController;
import com.aji.accounting.masterdata.warehouse.WarehouseService.WarehouseRequest;
import com.aji.accounting.masterdata.warehouse.WarehouseService.WarehouseResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Master - Gudang")
@RestController
@RequestMapping("/api/v1/warehouses")
public class WarehouseController extends AbstractMasterController<WarehouseRequest, WarehouseResponse> {
    public WarehouseController(WarehouseService service) {
        super(service);
    }
}
