package com.aji.accounting.masterdata.tax;

import com.aji.accounting.audit.AuditService;
import com.aji.accounting.coa.AccountRepository;
import com.aji.accounting.common.exception.BusinessException;
import com.aji.accounting.common.exception.NotFoundException;
import com.aji.accounting.common.master.AbstractMasterService;
import com.aji.accounting.common.master.MasterRequest;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TaxRateService extends AbstractMasterService<TaxRate, TaxRateService.TaxRateRequest,
        TaxRateService.TaxRateResponse> {

    public record TaxRateRequest(
            @NotBlank(message = "Kode wajib diisi") @Size(max = 20) String code,
            @NotBlank(message = "Nama wajib diisi") @Size(max = 100) String name,
            @NotNull(message = "Jenis pajak wajib dipilih") TaxType taxType,
            @NotNull(message = "Tarif wajib diisi") @DecimalMin("0") @DecimalMax("100") BigDecimal rate,
            Long accountId,
            Boolean active) implements MasterRequest {
    }

    public record TaxRateResponse(Long id, String code, String name, TaxType taxType, BigDecimal rate,
                                  Long accountId, String accountCode, String accountName, boolean active) {
    }

    private final AccountRepository accounts;

    public TaxRateService(TaxRateRepository repository, AccountRepository accounts, AuditService audit) {
        super(repository, audit);
        this.accounts = accounts;
    }

    @Override
    protected String entityName() {
        return "Pajak";
    }

    @Override
    protected TaxRate newEntity() {
        return new TaxRate();
    }

    @Override
    protected void apply(TaxRateRequest r, TaxRate e) {
        e.setTaxType(r.taxType());
        e.setRate(r.rate().setScale(4, RoundingMode.HALF_UP));
        if (r.accountId() == null) {
            e.setAccount(null);
        } else {
            var acc = accounts.findById(r.accountId()).orElseThrow(() -> NotFoundException.of("Akun", r.accountId()));
            if (acc.isHeader()) {
                throw new BusinessException("Akun header tidak dapat dipakai sebagai akun pajak");
            }
            e.setAccount(acc);
        }
    }

    @Override
    protected TaxRateResponse toResponse(TaxRate e) {
        var a = e.getAccount();
        return new TaxRateResponse(e.getId(), e.getCode(), e.getName(), e.getTaxType(), e.getRate(),
                a == null ? null : a.getId(), a == null ? null : a.getCode(), a == null ? null : a.getName(),
                e.isActive());
    }
}
