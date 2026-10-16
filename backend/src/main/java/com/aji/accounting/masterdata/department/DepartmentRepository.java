package com.aji.accounting.masterdata.department;

import com.aji.accounting.common.master.MasterRepository;

import java.util.List;

public interface DepartmentRepository extends MasterRepository<Department> {
    List<Department> findByDeptTypeAndActiveTrueOrderByCodeAsc(DepartmentType type);
}
