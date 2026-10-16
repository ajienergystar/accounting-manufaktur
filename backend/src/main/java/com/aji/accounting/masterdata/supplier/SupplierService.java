package com.aji.accounting.masterdata.supplier;

import com.aji.accounting.audit.AuditService;
import com.aji.accounting.common.master.AbstractMasterService;
import com.aji.accounting.common.master.MasterRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;

@Service
public class SupplierService extends AbstractMasterService<Supplier, SupplierService.SupplierRequest,
        SupplierService.SupplierResponse> {

    public record SupplierRequest(
            @NotBlank(message = "Kode wajib diisi") @Size(max = 20) String code,
            @NotBlank(message = "Nama wajib diisi") @Size(max = 150) String name,
            @Size(max = 30) String npwp,
            @Size(max = 500) String address,
            @Size(max = 100) String city,
            @Size(max = 30) String phone,
            @Email(message = "Format email tidak valid") @Size(max = 150) String email,
            @Size(max = 100) String contactPerson,
            @Min(0) @Max(365) Integer paymentTermDays,
            Boolean active) implements MasterRequest {
    }

    public record SupplierResponse(Long id, String code, String name, String npwp, String address, String city,
                                   String phone, String email, String contactPerson, int paymentTermDays,
                                   boolean active) {
    }

    public SupplierService(SupplierRepository repository, AuditService audit) {
        super(repository, audit);
    }

    @Override
    protected String entityName() {
        return "Pemasok";
    }

    @Override
    protected Supplier newEntity() {
        return new Supplier();
    }

    @Override
    protected void apply(SupplierRequest r, Supplier e) {
        e.setNpwp(r.npwp());
        e.setAddress(r.address());
        e.setCity(r.city());
        e.setPhone(r.phone());
        e.setEmail(r.email());
        e.setContactPerson(r.contactPerson());
        e.setPaymentTermDays(r.paymentTermDays() == null ? 30 : r.paymentTermDays());
    }

    @Override
    protected SupplierResponse toResponse(Supplier e) {
        return new SupplierResponse(e.getId(), e.getCode(), e.getName(), e.getNpwp(), e.getAddress(), e.getCity(),
                e.getPhone(), e.getEmail(), e.getContactPerson(), e.getPaymentTermDays(), e.isActive());
    }
}
