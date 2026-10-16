package com.aji.accounting.company;

import com.aji.accounting.audit.AuditAction;
import com.aji.accounting.audit.AuditService;
import com.aji.accounting.common.exception.BusinessException;
import com.aji.accounting.common.exception.NotFoundException;
import com.aji.accounting.common.util.TableUsage;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyService {

    public record CompanyRequest(
            @NotBlank(message = "Nama perusahaan wajib diisi") @Size(max = 150) String name,
            @Size(max = 30) String npwp,
            @Size(max = 500) String address,
            @Size(max = 100) String city,
            @Size(max = 30) String phone,
            @Email @Size(max = 150) String email,
            @Size(max = 500) String logoUrl,
            @NotNull @Min(1) @Max(12) Integer fiscalYearStartMonth,
            @NotNull InventoryCostingMethod inventoryCostingMethod,
            boolean allowNegativeStock) {
    }

    public record CompanyResponse(Long id, String code, String name, String npwp, String address, String city,
                                  String phone, String email, String logoUrl, String baseCurrency,
                                  int fiscalYearStartMonth, InventoryCostingMethod inventoryCostingMethod,
                                  boolean allowNegativeStock) {
        static CompanyResponse from(Company c) {
            return new CompanyResponse(c.getId(), c.getCode(), c.getName(), c.getNpwp(), c.getAddress(), c.getCity(),
                    c.getPhone(), c.getEmail(), c.getLogoUrl(), c.getBaseCurrency(), c.getFiscalYearStartMonth(),
                    c.getInventoryCostingMethod(), c.isAllowNegativeStock());
        }
    }

    private final CompanyRepository companies;
    private final TableUsage usage;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public Company current() {
        return companies.findFirstByOrderByIdAsc().orElseThrow(() -> new NotFoundException("Data perusahaan belum ada"));
    }

    @Transactional(readOnly = true)
    public CompanyResponse get() {
        return CompanyResponse.from(current());
    }

    @Transactional
    public CompanyResponse update(CompanyRequest r) {
        Company c = current();
        CompanyResponse before = CompanyResponse.from(c);
        boolean hasPeriods = usage.hasRows("fiscal_years");
        if (hasPeriods && r.fiscalYearStartMonth() != c.getFiscalYearStartMonth()) {
            throw new BusinessException("Bulan awal tahun fiskal tidak dapat diubah setelah tahun fiskal dibuat");
        }
        if (r.inventoryCostingMethod() != c.getInventoryCostingMethod() && hasStockMovements()) {
            throw new BusinessException("Metode penilaian persediaan tidak dapat diubah setelah ada mutasi stok");
        }
        c.setName(r.name().trim());
        c.setNpwp(r.npwp());
        c.setAddress(r.address());
        c.setCity(r.city());
        c.setPhone(r.phone());
        c.setEmail(r.email());
        c.setLogoUrl(r.logoUrl());
        c.setFiscalYearStartMonth(r.fiscalYearStartMonth());
        c.setInventoryCostingMethod(r.inventoryCostingMethod());
        c.setAllowNegativeStock(r.allowNegativeStock());
        companies.flush();
        CompanyResponse after = CompanyResponse.from(c);
        audit.log(AuditAction.UPDATE, "Perusahaan", c.getId(), "Ubah profil perusahaan", before, after);
        return after;
    }

    private boolean hasStockMovements() {
        return usage.hasRows("stock_movements");
    }
}
