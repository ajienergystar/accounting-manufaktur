package com.aji.accounting.masterdata.warehouse;

import com.aji.accounting.audit.AuditService;
import com.aji.accounting.common.master.AbstractMasterService;
import com.aji.accounting.common.master.MasterRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;

@Service
public class WarehouseService extends AbstractMasterService<Warehouse, WarehouseService.WarehouseRequest,
        WarehouseService.WarehouseResponse> {

    public record WarehouseRequest(
            @NotBlank(message = "Kode wajib diisi") @Size(max = 20) String code,
            @NotBlank(message = "Nama wajib diisi") @Size(max = 100) String name,
            @Size(max = 255) String address,
            Boolean active) implements MasterRequest {
    }

    public record WarehouseResponse(Long id, String code, String name, String address, boolean active) {
    }

    public WarehouseService(WarehouseRepository repository, AuditService audit) {
        super(repository, audit);
    }

    @Override
    protected String entityName() {
        return "Gudang";
    }

    @Override
    protected Warehouse newEntity() {
        return new Warehouse();
    }

    @Override
    protected void apply(WarehouseRequest req, Warehouse e) {
        e.setAddress(req.address());
    }

    @Override
    protected WarehouseResponse toResponse(Warehouse e) {
        return new WarehouseResponse(e.getId(), e.getCode(), e.getName(), e.getAddress(), e.isActive());
    }
}
