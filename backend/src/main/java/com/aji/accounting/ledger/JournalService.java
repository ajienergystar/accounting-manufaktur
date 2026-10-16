package com.aji.accounting.ledger;

import com.aji.accounting.audit.AuditAction;
import com.aji.accounting.audit.AuditService;
import com.aji.accounting.coa.Account;
import com.aji.accounting.coa.AccountMappingKey;
import com.aji.accounting.coa.AccountMappingService;
import com.aji.accounting.coa.AccountRepository;
import com.aji.accounting.common.exception.BusinessException;
import com.aji.accounting.common.exception.FieldValidationException;
import com.aji.accounting.common.exception.NotFoundException;
import com.aji.accounting.common.util.Money;
import com.aji.accounting.common.web.PageResponse;
import com.aji.accounting.ledger.JournalDtos.JournalDetail;
import com.aji.accounting.ledger.JournalDtos.JournalLineRequest;
import com.aji.accounting.ledger.JournalDtos.JournalRequest;
import com.aji.accounting.ledger.JournalDtos.JournalSummary;
import com.aji.accounting.masterdata.department.Department;
import com.aji.accounting.masterdata.department.DepartmentRepository;
import com.aji.accounting.period.PeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/** Jurnal manual (umum & penyesuaian): draft → posting → (opsional) pembalik. */
@Service
@RequiredArgsConstructor
public class JournalService {

    private static final EnumSet<JournalType> MANUAL_TYPES = EnumSet.of(JournalType.GENERAL, JournalType.ADJUSTMENT);

    private final JournalEntryRepository journals;
    private final AccountRepository accounts;
    private final DepartmentRepository departments;
    private final PeriodService periods;
    private final PostingService posting;
    private final AuditService audit;
    private final AccountMappingService mappings;

    @Transactional(readOnly = true)
    public PageResponse<JournalSummary> search(LocalDate from, LocalDate to, JournalStatus status, JournalType type,
                                               SourceType sourceType, String q, int page, int size) {
        var pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Order.desc("journalDate"), Sort.Order.desc("id")));
        return PageResponse.of(journals.search(from, to, status, type, sourceType,
                q == null || q.isBlank() ? null : q.trim(), pageable), JournalSummary::from);
    }

    @Transactional(readOnly = true)
    public JournalDetail get(Long id) {
        return JournalDetail.from(load(id));
    }

    @Transactional
    public JournalDetail create(JournalRequest req) {
        JournalEntry je = new JournalEntry();
        je.setJournalNo("DRAFT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        je.setStatus(JournalStatus.DRAFT);
        je.setSourceType(SourceType.MANUAL);
        apply(req, je);
        journals.save(je);
        audit.log(AuditAction.CREATE, "Jurnal", je.getId(), "Draft jurnal: " + je.getDescription());
        if (req.post()) {
            posting.postEntry(je);
        }
        return JournalDetail.from(je);
    }

    @Transactional
    public JournalDetail update(Long id, JournalRequest req) {
        JournalEntry je = load(id);
        assertDraft(je);
        apply(req, je);
        journals.flush();
        audit.log(AuditAction.UPDATE, "Jurnal", id, "Ubah draft jurnal " + je.getJournalNo());
        if (req.post()) {
            posting.postEntry(je);
        }
        return JournalDetail.from(je);
    }

    @Transactional
    public JournalDetail post(Long id) {
        JournalEntry je = load(id);
        assertDraft(je);
        posting.postEntry(je);
        return JournalDetail.from(je);
    }

    @Transactional
    public void deleteDraft(Long id) {
        JournalEntry je = load(id);
        assertDraft(je);
        journals.delete(je);
        audit.log(AuditAction.DELETE, "Jurnal", id, "Hapus draft jurnal " + je.getJournalNo());
    }

    @Transactional
    public JournalDetail reverse(Long id, LocalDate date, String reason) {
        JournalEntry je = load(id);
        if (je.getSourceType() != SourceType.MANUAL && je.getSourceType() != SourceType.RECURRING
                && je.getSourceType() != SourceType.OPENING_BALANCE) {
            throw new BusinessException("Jurnal otomatis dari " + je.getSourceType().label()
                    + " tidak dapat dibalik langsung. Batalkan (void) dokumen sumbernya: " + je.getSourceNo());
        }
        JournalEntry rev = posting.reverse(je, date, reason);
        return JournalDetail.from(rev);
    }

    /**
     * Input saldo awal saat mulai memakai aplikasi. Selisih otomatis dibukukan ke akun
     * Ekuitas Saldo Awal (OPENING_BALANCE_EQUITY).
     */
    @Transactional
    public JournalDetail openingBalance(JournalDtos.OpeningBalanceRequest req) {
        Account equity = mappings.get(AccountMappingKey.OPENING_BALANCE_EQUITY);
        JournalDraft draft = JournalDraft.of(req.date(), JournalType.OPENING, SourceType.OPENING_BALANCE, null,
                null, "Saldo awal per " + req.date());
        for (JournalDtos.OpeningBalanceLine l : req.lines()) {
            Account acc = accounts.findById(l.accountId()).orElseThrow(() -> NotFoundException.of("Akun", l.accountId()));
            if (!acc.getAccountType().isBalanceSheet()) {
                throw new BusinessException("Saldo awal hanya untuk akun neraca: " + acc.label());
            }
            draft.debit(acc, Money.nz(l.debit()), "Saldo awal").credit(acc, Money.nz(l.credit()), "Saldo awal");
        }
        var diff = draft.totalDebit().subtract(draft.totalCredit());
        draft.credit(equity, diff, "Penyeimbang saldo awal");
        if (draft.isEmpty()) {
            throw new BusinessException("Tidak ada saldo yang diinput");
        }
        JournalEntry je = posting.post(draft);
        return JournalDetail.from(journals.findWithLines(je.getId()).orElseThrow());
    }

    private void apply(JournalRequest req, JournalEntry je) {
        JournalType type = req.journalType() == null ? JournalType.GENERAL : req.journalType();
        if (!MANUAL_TYPES.contains(type)) {
            throw new FieldValidationException("journalType", "Jurnal manual hanya boleh tipe Umum atau Penyesuaian");
        }
        je.setJournalDate(req.journalDate());
        je.setPeriod(periods.findFor(req.journalDate()));
        je.setJournalType(type);
        je.setDescription(req.description().trim());

        List<JournalLine> lines = new ArrayList<>();
        int no = 1;
        for (JournalLineRequest lr : req.lines()) {
            Account acc = accounts.findById(lr.accountId())
                    .orElseThrow(() -> new FieldValidationException("lines", "Akun id " + lr.accountId() + " tidak ditemukan"));
            Department dept = lr.departmentId() == null ? null : departments.findById(lr.departmentId())
                    .orElseThrow(() -> NotFoundException.of("Departemen", lr.departmentId()));
            JournalLine l = new JournalLine();
            l.setAccount(acc);
            l.setDepartment(dept);
            l.setDescription(lr.description());
            l.setDebit(Money.amount(lr.debit()));
            l.setCredit(Money.amount(lr.credit()));
            if (l.getDebit().signum() == 0 && l.getCredit().signum() == 0) {
                throw new FieldValidationException("lines", "Baris " + no + ": isi nilai debit atau kredit");
            }
            lines.add(l);
            no++;
        }
        je.replaceLines(lines);
    }

    private void assertDraft(JournalEntry je) {
        if (je.getStatus() != JournalStatus.DRAFT) {
            throw new BusinessException("Jurnal " + je.getJournalNo() + " sudah " + je.getStatus()
                    + " dan tidak dapat diubah. Gunakan jurnal pembalik untuk koreksi.");
        }
    }

    private JournalEntry load(Long id) {
        return journals.findWithLines(id).orElseThrow(() -> NotFoundException.of("Jurnal", id));
    }
}
