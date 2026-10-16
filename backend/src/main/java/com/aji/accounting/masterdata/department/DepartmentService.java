package com.aji.accounting.masterdata.department;

import com.aji.accounting.audit.AuditService;
import com.aji.accounting.common.master.AbstractMasterService;
import com.aji.accounting.common.master.MasterRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;

@Service
public class DepartmentService extends AbstractMasterService<Department, DepartmentService.DepartmentRequest,
        DepartmentService.DepartmentResponse> {

    public record DepartmentRequest(
            @NotBlank(message = "Kode wajib diisi") @Size(max = 20) String code,
            @NotBlank(message = "Nama wajib diisi") @Size(max = 100) String name,
            @NotNull(message = "Tipe departemen wajib dipilih") DepartmentType deptType,
            Boolean active) implements MasterRequest {
    }

    public record DepartmentResponse(Long id, String code, String name, DepartmentType deptType, boolean active) {
    }

    public DepartmentService(DepartmentRepository repository, AuditService audit) {
        super(repository, audit);
    }

    @Override
    protected String entityName() {
        return "Departemen";
    }

    @Override
    protected Department newEntity() {
        return new Department();
    }

    @Override
    protected void apply(DepartmentRequest req, Department e) {
        e.setDeptType(req.deptType());
    }

    @Override
    protected DepartmentResponse toResponse(Department e) {
        return new DepartmentResponse(e.getId(), e.getCode(), e.getName(), e.getDeptType(), e.isActive());
    }
}
