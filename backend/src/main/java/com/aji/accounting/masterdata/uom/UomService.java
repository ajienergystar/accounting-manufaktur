package com.aji.accounting.masterdata.uom;

import com.aji.accounting.audit.AuditAction;
import com.aji.accounting.audit.AuditService;
import com.aji.accounting.common.exception.BusinessException;
import com.aji.accounting.common.exception.NotFoundException;
import com.aji.accounting.common.master.AbstractMasterService;
import com.aji.accounting.common.master.MasterRequest;
import com.aji.accounting.common.util.Money;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class UomService extends AbstractMasterService<Uom, UomService.UomRequest, UomService.UomResponse> {

    public record UomRequest(
            @NotBlank(message = "Kode wajib diisi") @Size(max = 20) String code,
            @NotBlank(message = "Nama wajib diisi") @Size(max = 100) String name,
            Boolean active) implements MasterRequest {
    }

    public record UomResponse(Long id, String code, String name, boolean active) {
    }

    public record ConversionRequest(@NotNull Long fromUomId, @NotNull Long toUomId,
                                    @NotNull @DecimalMin(value = "0.000001", message = "Faktor harus > 0") BigDecimal factor) {
    }

    public record ConversionResponse(Long id, Long fromUomId, String fromUomCode, Long toUomId, String toUomCode,
                                     BigDecimal factor) {
    }

    private final UomRepository uoms;
    private final UomConversionRepository conversions;

    public UomService(UomRepository repository, UomConversionRepository conversions, AuditService audit) {
        super(repository, audit);
        this.uoms = repository;
        this.conversions = conversions;
    }

    @Override
    protected String entityName() {
        return "Satuan";
    }

    @Override
    protected Uom newEntity() {
        return new Uom();
    }

    @Override
    protected void apply(UomRequest req, Uom entity) {
        // tidak ada field tambahan
    }

    @Override
    protected UomResponse toResponse(Uom e) {
        return new UomResponse(e.getId(), e.getCode(), e.getName(), e.isActive());
    }

    @Transactional(readOnly = true)
    public List<ConversionResponse> conversions() {
        return conversions.findAllWithUoms().stream().map(UomService::toResponse).toList();
    }

    @Transactional
    public ConversionResponse saveConversion(ConversionRequest req) {
        if (req.fromUomId().equals(req.toUomId())) {
            throw new BusinessException("Satuan asal dan tujuan tidak boleh sama");
        }
        Uom from = uoms.findById(req.fromUomId()).orElseThrow(() -> NotFoundException.of("Satuan", req.fromUomId()));
        Uom to = uoms.findById(req.toUomId()).orElseThrow(() -> NotFoundException.of("Satuan", req.toUomId()));
        UomConversion c = conversions.findByFromUomIdAndToUomId(from.getId(), to.getId()).orElseGet(UomConversion::new);
        c.setFromUom(from);
        c.setToUom(to);
        c.setFactor(req.factor().setScale(6, RoundingMode.HALF_UP));
        conversions.save(c);
        audit.log(AuditAction.UPDATE, "KonversiSatuan", c.getId(),
                "1 " + from.getCode() + " = " + req.factor().toPlainString() + " " + to.getCode());
        return toResponse(c);
    }

    @Transactional
    public void deleteConversion(Long id) {
        conversions.deleteById(id);
        audit.log(AuditAction.DELETE, "KonversiSatuan", id, "Hapus konversi satuan");
    }

    /**
     * Konversi kuantitas antar satuan. Mendukung konversi langsung dan kebalikannya.
     */
    @Transactional(readOnly = true)
    public BigDecimal convert(BigDecimal qty, Long fromUomId, Long toUomId) {
        if (fromUomId.equals(toUomId)) {
            return qty;
        }
        return conversions.findByFromUomIdAndToUomId(fromUomId, toUomId)
                .map(c -> qty.multiply(c.getFactor()).setScale(Money.COST_SCALE, Money.ROUNDING))
                .or(() -> conversions.findByFromUomIdAndToUomId(toUomId, fromUomId)
                        .map(c -> qty.divide(c.getFactor(), Money.COST_SCALE, Money.ROUNDING)))
                .orElseThrow(() -> new BusinessException("Konversi satuan belum didefinisikan"));
    }

    private static ConversionResponse toResponse(UomConversion c) {
        return new ConversionResponse(c.getId(), c.getFromUom().getId(), c.getFromUom().getCode(),
                c.getToUom().getId(), c.getToUom().getCode(), c.getFactor());
    }
}
