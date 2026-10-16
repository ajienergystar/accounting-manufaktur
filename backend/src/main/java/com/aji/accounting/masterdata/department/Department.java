package com.aji.accounting.masterdata.department;

import com.aji.accounting.common.entity.MasterEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "departments")
public class Department extends MasterEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "dept_type", nullable = false, length = 20)
    private DepartmentType deptType;
}
