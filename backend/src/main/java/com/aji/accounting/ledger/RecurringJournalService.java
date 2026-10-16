package com.aji.accounting.ledger;

import com.aji.accounting.audit.AuditAction;
import com.aji.accounting.audit.AuditService;
import com.aji.accounting.coa.AccountRepository;
import com.aji.accounting.common.exception.BusinessException;
import com.aji.accounting.common.exception.NotFoundException;
import com.aji.accounting.common.util.Money;
import com.aji.accounting.ledger.JournalDtos.JournalLineRequest;
import com.aji.accounting.masterdata.department.DepartmentRepository;
import com.aji.accounting.period.PeriodService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Jurnal berulang bulanan: template → jurnal (draft atau langsung posting) setiap tanggal tertentu. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringJournalService {

    public record RecurringRequest(
            @NotBlank(message = "Nama wajib diisi") @Size(max = 150) String name,
            @NotBlank(message = "Keterangan wajib diisi") @Size(max = 500) String description,
            @Min(1) @Max(28) int dayOfMonth,
            @NotNull(message = "Tanggal mulai wajib diisi") LocalDate startDate,
            LocalDate endDate,
            boolean autoPost,
            boolean active,
            @Size(min = 2, message = "Minimal 2 baris") List<@Valid JournalLineRequest> lines) {
    }

    public record RecurringResponse(Long id, String name, String description, int dayOfMonth, LocalDate nextRunDate,
                                    LocalDate endDate, boolean autoPost, boolean active, LocalDateTime lastRunAt,
                                    BigDecimal amount, List<JournalDtos.JournalLineResponse> lines) {
    }

    public record GenerateResult(int generated, List<String> journalNos, List<String> errors) {
    }

    private final RecurringJournalRepository repo;
    private final AccountRepository accounts;
    private final DepartmentRepository departments;
    private final JournalEntryRepository journals;
    private final PeriodService periods;
    private final PostingService posting;
    private final AuditService audit;
    private final TransactionTemplate tx;

    @Transactional(readOnly = true)
    public List<RecurringResponse> list() {
        return repo.findAllOrdered().stream().map(r -> toResponse(repo.findWithLines(r.getId()).orElseThrow())).toList();
    }

    @Transactional
    public RecurringResponse save(Long id, RecurringRequest req) {
        RecurringJournal r = id == null ? new RecurringJournal()
                : repo.findWithLines(id).orElseThrow(() -> NotFoundException.of("Jurnal berulang", id));
        r.setName(req.name().trim());
        r.setDescription(req.description().trim());
        r.setDayOfMonth(req.dayOfMonth());
        r.setEndDate(req.endDate());
        r.setAutoPost(req.autoPost());
        r.setActive(req.active());
        if (id == null || r.getLastRunAt() == null) {
            r.setNextRunDate(firstRunDate(req.startDate(), req.dayOfMonth()));
        }
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        r.getLines().clear();
        int no = 1;
        for (JournalLineRequest lr : req.lines()) {
            RecurringJournal.Line l = new RecurringJournal.Line();
            l.setRecurringJournal(r);
            l.setLineNo(no++);
            l.setAccount(accounts.findById(lr.accountId()).orElseThrow(() -> NotFoundException.of("Akun", lr.accountId())));
            l.setDepartment(lr.departmentId() == null ? null : departments.findById(lr.departmentId()).orElseThrow());
            l.setDescription(lr.description());
            l.setDebit(Money.amount(lr.debit()));
            l.setCredit(Money.amount(lr.credit()));
            if ((l.getDebit().signum() > 0) == (l.getCredit().signum() > 0)) {
                throw new BusinessException("Baris " + l.getLineNo() + ": isi salah satu, debit atau kredit");
            }
            if (l.getAccount().isHeader()) {
                throw new BusinessException("Baris " + l.getLineNo() + ": akun header tidak dapat dijurnal");
            }
            debit = debit.add(l.getDebit());
            credit = credit.add(l.getCredit());
            r.getLines().add(l);
        }
        if (debit.compareTo(credit) != 0) {
            throw new BusinessException("Template tidak balance: debit " + debit.toPlainString() + " ≠ kredit " + credit.toPlainString());
        }
        repo.save(r);
        audit.log(id == null ? AuditAction.CREATE : AuditAction.UPDATE, "JurnalBerulang", r.getId(), "Template " + r.getName());
        return toResponse(r);
    }

    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
        audit.log(AuditAction.DELETE, "JurnalBerulang", id, "Hapus template jurnal berulang");
    }

    /** Buat jurnal untuk semua template yang jatuh tempo s.d. hari ini. Tiap jurnal di transaksi terpisah. */
    public GenerateResult generateDue(LocalDate today) {
        List<String> nos = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (Long id : repo.findDueIds(today)) {
            // satu template bisa tertinggal beberapa bulan → loop sampai mengejar
            for (int guard = 0; guard < 24; guard++) {
                try {
                    String no = tx.execute(s -> generateOne(id, today));
                    if (no == null) {
                        break;
                    }
                    nos.add(no);
                } catch (RuntimeException e) {
                    errors.add("Template #" + id + ": " + e.getMessage());
                    break;
                }
            }
        }
        return new GenerateResult(nos.size(), nos, errors);
    }

    private String generateOne(Long id, LocalDate today) {
        RecurringJournal r = repo.findWithLines(id).orElseThrow();
        LocalDate runDate = r.getNextRunDate();
        if (!r.isActive() || runDate.isAfter(today) || (r.getEndDate() != null && runDate.isAfter(r.getEndDate()))) {
            return null;
        }
        JournalEntry je = new JournalEntry();
        je.setJournalNo("DRAFT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        je.setJournalDate(runDate);
        je.setPeriod(periods.findFor(runDate));
        je.setJournalType(JournalType.GENERAL);
        je.setSourceType(SourceType.RECURRING);
        je.setSourceId(r.getId());
        je.setSourceNo("RJ-" + r.getId());
        je.setDescription(r.getDescription() + " (" + runDate.getMonthValue() + "/" + runDate.getYear() + ")");
        List<JournalLine> lines = new ArrayList<>();
        for (RecurringJournal.Line rl : r.getLines()) {
            JournalLine l = new JournalLine();
            l.setAccount(rl.getAccount());
            l.setDepartment(rl.getDepartment());
            l.setDescription(rl.getDescription());
            l.setDebit(rl.getDebit());
            l.setCredit(rl.getCredit());
            lines.add(l);
        }
        je.replaceLines(lines);
        journals.save(je);
        if (r.isAutoPost()) {
            posting.postEntry(je);
        }
        r.setNextRunDate(nextMonth(runDate, r.getDayOfMonth()));
        r.setLastRunAt(LocalDateTime.now());
        audit.log(AuditAction.CREATE, "Jurnal", je.getId(), "Jurnal berulang dibuat dari template " + r.getName());
        return je.getJournalNo();
    }

    /** Dijalankan otomatis setiap hari pukul 01:05 WIB. */
    @Scheduled(cron = "0 5 1 * * *", zone = "Asia/Jakarta")
    public void scheduledRun() {
        GenerateResult res = generateDue(LocalDate.now());
        if (res.generated() > 0 || !res.errors().isEmpty()) {
            log.info("Jurnal berulang: {} dibuat, error: {}", res.generated(), res.errors());
        }
    }

    private static LocalDate firstRunDate(LocalDate start, int day) {
        LocalDate candidate = start.withDayOfMonth(day);
        return candidate.isBefore(start) ? nextMonth(candidate, day) : candidate;
    }

    private static LocalDate nextMonth(LocalDate d, int day) {
        return d.plusMonths(1).withDayOfMonth(day);
    }

    private RecurringResponse toResponse(RecurringJournal r) {
        BigDecimal amount = r.getLines().stream().map(RecurringJournal.Line::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<JournalDtos.JournalLineResponse> lines = r.getLines().stream().map(l -> new JournalDtos.JournalLineResponse(
                l.getId(), l.getLineNo(), l.getAccount().getId(), l.getAccount().getCode(), l.getAccount().getName(),
                l.getDepartment() == null ? null : l.getDepartment().getId(),
                l.getDepartment() == null ? null : l.getDepartment().getName(),
                l.getDescription(), l.getDebit(), l.getCredit())).toList();
        return new RecurringResponse(r.getId(), r.getName(), r.getDescription(), r.getDayOfMonth(), r.getNextRunDate(),
                r.getEndDate(), r.isAutoPost(), r.isActive(), r.getLastRunAt(), amount, lines);
    }
}
