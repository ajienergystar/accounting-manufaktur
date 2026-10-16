package com.aji.accounting.masterdata.department;

public enum DepartmentType {
    /** Departemen produksi (menerima pembebanan BOP). */
    PRODUCTION,
    /** Departemen pendukung/jasa pabrik (biayanya dialokasikan ke departemen produksi). */
    SERVICE,
    /** Departemen non-produksi (penjualan, administrasi). */
    NON_PRODUCTION
}
