package com.aji.accounting.coa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AccountDtos {
    private AccountDtos() {
    }

    public record AccountRequest(
            @NotBlank(message = "Kode akun wajib diisi")
            @Pattern(regexp = "^[0-9]-[0-9]{4}$", message = "Format kode akun: X-XXXX (contoh 1-1210)") String code,
            @NotBlank(message = "Nama akun wajib diisi") @Size(max = 150) String name,
            @NotNull(message = "Akun induk wajib dipilih") Long parentId,
            AccountType accountType,
            NormalBalance normalBalance,
            boolean header,
            boolean cashBank,
            CashFlowCategory cashFlowCategory,
            @Size(max = 255) String description,
            Boolean active) {
    }

    public record AccountResponse(Long id, String code, String name, Long parentId, String parentCode,
                                  AccountType accountType, String accountTypeLabel, NormalBalance normalBalance,
                                  boolean header, boolean cashBank, CashFlowCategory cashFlowCategory, int level,
                                  String description, boolean active) {
        public static AccountResponse from(Account a) {
            Account p = a.getParent();
            return new AccountResponse(a.getId(), a.getCode(), a.getName(), p == null ? null : p.getId(),
                    p == null ? null : p.getCode(), a.getAccountType(), a.getAccountType().label(),
                    a.getNormalBalance(), a.isHeader(), a.isCashBank(), a.getCashFlowCategory(), a.getLevel(),
                    a.getDescription(), a.isActive());
        }
    }

    /** Ringkas untuk AccountPicker. */
    public record AccountOption(Long id, String code, String name, AccountType accountType,
                                NormalBalance normalBalance, boolean cashBank) {
        public static AccountOption from(Account a) {
            return new AccountOption(a.getId(), a.getCode(), a.getName(), a.getAccountType(), a.getNormalBalance(),
                    a.isCashBank());
        }
    }
}
