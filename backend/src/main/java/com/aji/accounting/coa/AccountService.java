package com.aji.accounting.coa;

import com.aji.accounting.audit.AuditAction;
import com.aji.accounting.audit.AuditService;
import com.aji.accounting.coa.AccountDtos.AccountOption;
import com.aji.accounting.coa.AccountDtos.AccountRequest;
import com.aji.accounting.coa.AccountDtos.AccountResponse;
import com.aji.accounting.common.exception.BusinessException;
import com.aji.accounting.common.exception.ConflictException;
import com.aji.accounting.common.exception.FieldValidationException;
import com.aji.accounting.common.exception.NotFoundException;
import com.aji.accounting.common.util.TableUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accounts;
    private final AccountMappingRepository mappings;
    private final TableUsage usage;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<AccountResponse> tree() {
        return accounts.findAllOrdered().stream().map(AccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AccountOption> postable(String q, AccountType type, Boolean cashBank) {
        return accounts.findPostable(q == null || q.isBlank() ? null : q.trim(), type, cashBank).stream()
                .map(AccountOption::from).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse get(Long id) {
        return AccountResponse.from(load(id));
    }

    @Transactional
    public AccountResponse create(AccountRequest req) {
        if (accounts.existsByCode(req.code())) {
            throw new FieldValidationException("code", "Kode akun " + req.code() + " sudah digunakan");
        }
        Account parent = loadParent(req.parentId(), null);
        Account a = new Account();
        a.setCode(req.code());
        applyCommon(req, a, parent);
        accounts.save(a);
        audit.log(AuditAction.CREATE, "Akun", a.getId(), "Tambah akun " + a.label());
        return AccountResponse.from(a);
    }

    @Transactional
    public AccountResponse update(Long id, AccountRequest req) {
        Account a = load(id);
        AccountResponse before = AccountResponse.from(a);
        boolean used = isUsedInJournal(id);

        if (!a.getCode().equals(req.code())) {
            if (used) {
                throw new BusinessException("Kode akun tidak dapat diubah karena sudah dipakai di jurnal");
            }
            if (accounts.existsByCode(req.code())) {
                throw new FieldValidationException("code", "Kode akun " + req.code() + " sudah digunakan");
            }
            a.setCode(req.code());
        }
        if (a.isHeader() && !req.header() && accounts.existsByParentId(id)) {
            throw new BusinessException("Akun header yang masih memiliki sub-akun tidak dapat diubah menjadi akun detail");
        }
        if (!a.isHeader() && req.header() && used) {
            throw new BusinessException("Akun yang sudah dipakai di jurnal tidak dapat dijadikan akun header");
        }
        if (used && req.accountType() != null && req.accountType() != a.getAccountType()) {
            throw new BusinessException("Tipe akun tidak dapat diubah karena sudah dipakai di jurnal");
        }
        Account parent = loadParent(req.parentId(), a);
        applyCommon(req, a, parent);
        accounts.flush();
        AccountResponse after = AccountResponse.from(a);
        audit.log(AuditAction.UPDATE, "Akun", id, "Ubah akun " + a.label(), before, after);
        return after;
    }

    @Transactional
    public void delete(Long id) {
        Account a = load(id);
        if (accounts.existsByParentId(id)) {
            throw new ConflictException("Akun " + a.label() + " masih memiliki sub-akun");
        }
        if (isUsedInJournal(id)) {
            throw new ConflictException("Akun " + a.label() + " sudah dipakai di jurnal. Nonaktifkan saja.");
        }
        if (mappings.existsByAccountId(id)) {
            throw new ConflictException("Akun " + a.label() + " dipakai di Account Mapping. Ganti mapping terlebih dahulu.");
        }
        accounts.delete(a);
        accounts.flush();
        audit.log(AuditAction.DELETE, "Akun", id, "Hapus akun " + a.label());
    }

    /** Validasi akun boleh dipakai di baris jurnal. */
    public void assertPostable(Account a) {
        if (a.isHeader()) {
            throw new BusinessException("Akun " + a.label() + " adalah akun header dan tidak dapat dijurnal");
        }
        if (!a.isActive()) {
            throw new BusinessException("Akun " + a.label() + " tidak aktif");
        }
    }

    private void applyCommon(AccountRequest req, Account a, Account parent) {
        if (req.code().charAt(0) != parent.getCode().charAt(0)) {
            throw new FieldValidationException("code", "Digit pertama kode harus sama dengan akun induk ("
                    + parent.getCode().charAt(0) + ")");
        }
        AccountType type = req.accountType() == null ? parent.getAccountType() : req.accountType();
        if (!compatible(parent.getAccountType(), type)) {
            throw new FieldValidationException("accountType", "Tipe akun harus sama dengan akun induk ("
                    + parent.getAccountType().label() + ")");
        }
        a.setName(req.name().trim());
        a.setParent(parent);
        a.setLevel(parent.getLevel() + 1);
        a.setAccountType(type);
        a.setNormalBalance(req.normalBalance() == null ? type.defaultNormalBalance() : req.normalBalance());
        a.setHeader(req.header());
        a.setCashBank(!req.header() && req.cashBank());
        if (a.isCashBank() && type != AccountType.ASSET) {
            throw new FieldValidationException("cashBank", "Hanya akun aset yang dapat ditandai sebagai kas/bank");
        }
        a.setCashFlowCategory(a.isCashBank() ? CashFlowCategory.CASH
                : type.isBalanceSheet() ? req.cashFlowCategory() : null);
        a.setDescription(req.description());
        if (req.active() != null) {
            a.setActive(req.active());
        }
    }

    private static boolean compatible(AccountType parent, AccountType child) {
        if (parent == child) {
            return true;
        }
        boolean parentOther = parent == AccountType.OTHER_INCOME || parent == AccountType.OTHER_EXPENSE;
        boolean childOther = child == AccountType.OTHER_INCOME || child == AccountType.OTHER_EXPENSE;
        return parentOther && childOther;
    }

    private Account loadParent(Long parentId, Account self) {
        Account parent = accounts.findById(parentId).orElseThrow(() -> NotFoundException.of("Akun induk", parentId));
        if (!parent.isHeader()) {
            throw new FieldValidationException("parentId", "Akun induk harus berupa akun header");
        }
        if (self != null) {
            for (Account p = parent; p != null; p = p.getParent()) {
                if (p.getId().equals(self.getId())) {
                    throw new FieldValidationException("parentId", "Akun induk tidak boleh sub-akun dari akun ini sendiri");
                }
            }
        }
        return parent;
    }

    private boolean isUsedInJournal(Long accountId) {
        return usage.isUsed("journal_lines", "account_id", accountId);
    }

    private Account load(Long id) {
        return accounts.findById(id).orElseThrow(() -> NotFoundException.of("Akun", id));
    }
}
