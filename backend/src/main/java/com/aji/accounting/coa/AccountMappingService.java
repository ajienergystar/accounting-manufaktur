package com.aji.accounting.coa;

import com.aji.accounting.audit.AuditAction;
import com.aji.accounting.audit.AuditService;
import com.aji.accounting.common.exception.BusinessException;
import com.aji.accounting.common.exception.NotFoundException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Resolusi akun default untuk jurnal otomatis. */
@Service
@RequiredArgsConstructor
public class AccountMappingService {

    public record MappingResponse(AccountMappingKey key, String groupName, String description, Long accountId,
                                  String accountCode, String accountName) {
        static MappingResponse from(AccountMapping m) {
            return new MappingResponse(m.getMappingKey(), m.getGroupName(), m.getDescription(),
                    m.getAccount().getId(), m.getAccount().getCode(), m.getAccount().getName());
        }
    }

    public record UpdateMappingRequest(@NotNull(message = "Akun wajib dipilih") Long accountId) {
    }

    private final AccountMappingRepository mappings;
    private final AccountRepository accounts;
    private final AuditService audit;

    /** @throws BusinessException bila mapping belum dikonfigurasi. */
    @Transactional(readOnly = true)
    public Account get(AccountMappingKey key) {
        return mappings.findByKey(key).map(AccountMapping::getAccount)
                .orElseThrow(() -> new BusinessException("ACCOUNT_MAPPING_MISSING",
                        "Account mapping " + key + " belum diatur. Atur di Pengaturan → Account Mapping."));
    }

    @Transactional(readOnly = true)
    public List<MappingResponse> list() {
        return mappings.findAllWithAccount().stream().map(MappingResponse::from).toList();
    }

    @Transactional
    public MappingResponse update(AccountMappingKey key, Long accountId) {
        AccountMapping m = mappings.findByKey(key)
                .orElseThrow(() -> new NotFoundException("Mapping " + key + " tidak ditemukan"));
        Account acc = accounts.findById(accountId).orElseThrow(() -> NotFoundException.of("Akun", accountId));
        boolean headerAllowed = key == AccountMappingKey.FOH_ACTUAL_CONTROL;
        if (acc.isHeader() != headerAllowed) {
            throw new BusinessException(headerAllowed
                    ? "Mapping " + key + " harus berupa akun header (kelompok BOP aktual)"
                    : "Akun header tidak dapat dipakai untuk mapping " + key);
        }
        if (!acc.isActive()) {
            throw new BusinessException("Akun " + acc.label() + " tidak aktif");
        }
        String before = m.getAccount().label();
        m.setAccount(acc);
        audit.log(AuditAction.UPDATE, "AccountMapping", key.name(), "Mapping " + key + ": " + before + " → " + acc.label());
        return MappingResponse.from(m);
    }
}
