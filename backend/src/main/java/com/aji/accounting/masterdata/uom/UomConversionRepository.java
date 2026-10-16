package com.aji.accounting.masterdata.uom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UomConversionRepository extends JpaRepository<UomConversion, Long> {

    @Query("select c from UomConversion c join fetch c.fromUom join fetch c.toUom order by c.fromUom.code")
    List<UomConversion> findAllWithUoms();

    Optional<UomConversion> findByFromUomIdAndToUomId(Long fromUomId, Long toUomId);
}
