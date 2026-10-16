package com.aji.accounting.masterdata.tax;

import com.aji.accounting.common.master.MasterRepository;

import java.util.List;

public interface TaxRateRepository extends MasterRepository<TaxRate> {
    List<TaxRate> findByTaxTypeAndActiveTrueOrderByCodeAsc(TaxType type);
}
