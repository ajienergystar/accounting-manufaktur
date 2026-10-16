package com.aji.accounting.masterdata.customer;

import com.aji.accounting.audit.AuditService;
import com.aji.accounting.common.master.AbstractMasterService;
import com.aji.accounting.common.master.MasterRequest;
import com.aji.accounting.common.util.Money;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CustomerService extends AbstractMasterService<Customer, CustomerService.CustomerRequest,
        CustomerService.CustomerResponse> {

    public record CustomerRequest(
            @NotBlank(message = "Kode wajib diisi") @Size(max = 20) String code,
            @NotBlank(message = "Nama wajib diisi") @Size(max = 150) String name,
            @Size(max = 30) String npwp,
            @Size(max = 500) String address,
            @Size(max = 100) String city,
            @Size(max = 30) String phone,
            @Email(message = "Format email tidak valid") @Size(max = 150) String email,
            @Size(max = 100) String contactPerson,
            @Min(0) @Max(365) Integer paymentTermDays,
            @DecimalMin(value = "0", message = "Limit kredit tidak boleh negatif") BigDecimal creditLimit,
            Boolean active) implements MasterRequest {
    }

    public record CustomerResponse(Long id, String code, String name, String npwp, String address, String city,
                                   String phone, String email, String contactPerson, int paymentTermDays,
                                   BigDecimal creditLimit, boolean active) {
    }

    public CustomerService(CustomerRepository repository, AuditService audit) {
        super(repository, audit);
    }

    @Override
    protected String entityName() {
        return "Pelanggan";
    }

    @Override
    protected Customer newEntity() {
        return new Customer();
    }

    @Override
    protected void apply(CustomerRequest r, Customer e) {
        e.setNpwp(r.npwp());
        e.setAddress(r.address());
        e.setCity(r.city());
        e.setPhone(r.phone());
        e.setEmail(r.email());
        e.setContactPerson(r.contactPerson());
        e.setPaymentTermDays(r.paymentTermDays() == null ? 30 : r.paymentTermDays());
        e.setCreditLimit(Money.amount(r.creditLimit()));
    }

    @Override
    protected CustomerResponse toResponse(Customer e) {
        return new CustomerResponse(e.getId(), e.getCode(), e.getName(), e.getNpwp(), e.getAddress(), e.getCity(),
                e.getPhone(), e.getEmail(), e.getContactPerson(), e.getPaymentTermDays(), e.getCreditLimit(),
                e.isActive());
    }
}
