package com.aji.accounting.ledger;

import com.aji.accounting.audit.AuditAction;
import com.aji.accounting.audit.AuditService;
import com.aji.accounting.coa.AccountMappingKey;
import com.aji.accounting.coa.AccountMappingService;
import com.aji.accounting.coa.AccountService;
import com.aji.accounting.common.exception.BusinessException;
import com.aji.accounting.common.sequence.DocumentNumberService;
import com.aji.accounting.common.util.Money;
import com.aji.accounting.config.CurrentUser;
import com.aji.accounting.period.Period;
import com.aji.accounting.period.PeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Posting engine terpusat. SEMUA jurnal (manual maupun otomatis) masuk ke buku besar lewat kelas ini,
 * sehingga aturan akuntansi ditegakkan di satu tempat:
 * <ul>
 *   <li>minimal dua baris, setiap baris hanya debit ATAU kredit</li>
 *   <li>total debit = total kredit</li>
 *   <li>hanya akun detail & aktif</li>
 *   <li>tanggal berada di periode OPEN</li>
 *   <li>jurnal POSTED tidak dapat diubah; koreksi lewat jurnal pembalik</li>
 * </ul>
 * Dipanggil di dalam transaksi dokumen sumber (MANDATORY) agar dokumen & jurnalnya atomik.
 */
@Service
@RequiredArgsConstructor
public class PostingService {

    /** Toleransi selisih pembulatan otomatis untuk jurnal sistem (bukan manual). */
    private static final BigDecimal AUTO_ROUNDING_TOLERANCE = new BigDecimal("1.00");

    private final JournalEntryRepository journals;
    private final PeriodService periods;
    private final AccountService accounts;
    private final AccountMappingService mappings;
    private final DocumentNumberService numbers;
    private final AuditService audit;

    /** Validasi + simpan + posting jurnal dari draft. */
    @Transactional(propagation = Propagation.MANDATORY)
    public JournalEntry post(JournalDraft draft) {
        JournalDraft d = absorbRounding(draft);
        JournalEntry je = new JournalEntry();
        je.setJournalDate(d.date());
        je.setJournalType(d.type());
        je.setSourceType(d.sourceType());
        je.setSourceId(d.sourceId());
        je.setSourceNo(d.sourceNo());
        je.setDescription(truncate(d.description(), 500));
        je.replaceLines(toLines(d.lines()));
        return postEntry(je);
    }

    /** Posting jurnal yang sudah tersimpan sebagai DRAFT (jurnal manual). */
    @Transactional(propagation = Propagation.MANDATORY)
    public JournalEntry postEntry(JournalEntry je) {
        if (je.getStatus() == JournalStatus.POSTED) {
            throw new BusinessException("Jurnal " + je.getJournalNo() + " sudah diposting");
        }
        if (je.getStatus() == JournalStatus.VOID) {
            throw new BusinessException("Jurnal yang sudah dibatalkan tidak dapat diposting");
        }
        Period period = periods.assertOpen(je.getJournalDate());
        validateLines(je);

        je.setPeriod(period);
        je.setJournalNo(numbers.next(je.getJournalType().docType(), je.getJournalDate()));
        je.setStatus(JournalStatus.POSTED);
        je.setPostedAt(LocalDateTime.now());
        je.setPostedBy(CurrentUser.usernameOrSystem());
        journals.save(je);
        audit.log(AuditAction.POST, "Jurnal", je.getId(), "Posting " + je.getJournalNo() + " (" + je.getDescription()
                + ") Rp " + je.getTotalDebit().toPlainString());
        return je;
    }

    /**
     * Membuat jurnal pembalik untuk jurnal POSTED. Jurnal asli tetap POSTED dan ditandai reversedBy.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public JournalEntry reverse(JournalEntry original, LocalDate date, String reason) {
        if (original.getStatus() != JournalStatus.POSTED) {
            throw new BusinessException("Hanya jurnal POSTED yang dapat dibalik");
        }
        if (original.getReversedBy() != null) {
            throw new BusinessException("Jurnal " + original.getJournalNo() + " sudah pernah dibalik oleh "
                    + original.getReversedBy().getJournalNo());
        }
        if (original.getJournalType() == JournalType.REVERSAL) {
            throw new BusinessException("Jurnal pembalik tidak dapat dibalik lagi");
        }
        if (date.isBefore(original.getJournalDate())) {
            throw new BusinessException("Tanggal jurnal pembalik tidak boleh sebelum tanggal jurnal asli");
        }
        JournalEntry rev = new JournalEntry();
        rev.setJournalDate(date);
        rev.setJournalType(JournalType.REVERSAL);
        rev.setSourceType(original.getSourceType());
        rev.setSourceId(original.getSourceId());
        rev.setSourceNo(original.getSourceNo());
        rev.setDescription(truncate("Pembalik " + original.getJournalNo() + ": " + reason, 500));
        rev.setReversalOf(original);
        List<JournalLine> lines = new ArrayList<>();
        for (JournalLine l : original.getLines()) {
            JournalLine n = new JournalLine();
            n.setAccount(l.getAccount());
            n.setDepartment(l.getDepartment());
            n.setDescription(l.getDescription());
            n.setDebit(l.getCredit());
            n.setCredit(l.getDebit());
            lines.add(n);
        }
        rev.replaceLines(lines);
        postEntry(rev);
        original.setReversedBy(rev);
        audit.log(AuditAction.REVERSE, "Jurnal", original.getId(), "Dibalik oleh " + rev.getJournalNo() + ": " + reason);
        return rev;
    }

    /** Membalik semua jurnal aktif milik dokumen sumber (dipakai saat dokumen di-void). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reverseBySource(SourceType type, Long sourceId, LocalDate date, String reason) {
        for (JournalEntry je : journals.findActiveBySource(type, sourceId)) {
            JournalEntry withLines = journals.findWithLines(je.getId()).orElseThrow();
            reverse(withLines, date.isBefore(withLines.getJournalDate()) ? withLines.getJournalDate() : date, reason);
        }
    }

    void validateLines(JournalEntry je) {
        if (je.getLines().size() < 2) {
            throw new BusinessException("JOURNAL_LINES", "Jurnal minimal terdiri dari 2 baris");
        }
        for (JournalLine l : je.getLines()) {
            accounts.assertPostable(l.getAccount());
            boolean d = l.getDebit().signum() > 0;
            boolean c = l.getCredit().signum() > 0;
            if (l.getDebit().signum() < 0 || l.getCredit().signum() < 0) {
                throw new BusinessException("JOURNAL_LINES", "Baris " + l.getLineNo() + ": nilai tidak boleh negatif");
            }
            if (d == c) {
                throw new BusinessException("JOURNAL_LINES", "Baris " + l.getLineNo()
                        + ": isi salah satu, debit ATAU kredit (tidak boleh keduanya atau kosong)");
            }
            if (l.getDepartment() != null && !l.getDepartment().isActive()) {
                throw new BusinessException("JOURNAL_LINES", "Baris " + l.getLineNo() + ": departemen tidak aktif");
            }
        }
        je.recalcTotals();
        if (!je.isBalanced()) {
            throw new BusinessException("JOURNAL_UNBALANCED", "Jurnal tidak balance: total debit Rp "
                    + je.getTotalDebit().toPlainString() + " ≠ total kredit Rp " + je.getTotalCredit().toPlainString());
        }
    }

    /**
     * Jurnal otomatis dari perhitungan biaya bisa selisih beberapa rupiah akibat pembulatan per baris.
     * Selisih ≤ Rp1 dibukukan ke akun Selisih Pembulatan agar tetap balance; selisih lebih besar = bug → tolak.
     */
    private JournalDraft absorbRounding(JournalDraft d) {
        if (d.type() != JournalType.AUTO) {
            return d;
        }
        BigDecimal diff = d.totalDebit().subtract(d.totalCredit());
        if (diff.signum() == 0) {
            return d;
        }
        if (diff.abs().compareTo(AUTO_ROUNDING_TOLERANCE) > 0) {
            throw new BusinessException("JOURNAL_UNBALANCED", "Jurnal otomatis " + d.sourceType().label()
                    + " tidak balance (selisih Rp " + diff.toPlainString() + ")");
        }
        var rounding = mappings.get(AccountMappingKey.ROUNDING);
        return diff.signum() > 0
                ? d.credit(rounding, diff, "Selisih pembulatan")
                : d.debit(rounding, diff.negate(), "Selisih pembulatan");
    }

    private static List<JournalLine> toLines(List<JournalDraft.Line> draftLines) {
        List<JournalLine> lines = new ArrayList<>();
        for (JournalDraft.Line dl : draftLines) {
            JournalLine l = new JournalLine();
            l.setAccount(dl.account());
            l.setDepartment(dl.department());
            l.setDescription(dl.description() == null ? null : truncate(dl.description(), 255));
            l.setDebit(Money.amount(dl.debit()));
            l.setCredit(Money.amount(dl.credit()));
            lines.add(l);
        }
        return lines;
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
