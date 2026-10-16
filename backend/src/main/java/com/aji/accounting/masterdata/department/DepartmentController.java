package com.aji.accounting.masterdata.department;

import com.aji.accounting.common.master.AbstractMasterController;
import com.aji.accounting.masterdata.department.DepartmentService.DepartmentRequest;
import com.aji.accounting.masterdata.department.DepartmentService.DepartmentResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Master - Departemen")
@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController extends AbstractMasterController<DepartmentRequest, DepartmentResponse> {
    public DepartmentController(DepartmentService service) {
        super(service);
    }
}
