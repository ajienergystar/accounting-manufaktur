package com.aji.accounting.ledger;

import com.aji.accounting.coa.Account;
import com.aji.accounting.common.util.Money;
import com.aji.accounting.masterdata.department.Department;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Rancangan jurnal yang dibangun modul lain sebelum diposting oleh {@link PostingService}.
 * Contoh:
 * <pre>
 * JournalDraft.of(date, SourceType.MATERIAL_ISSUE, issue.getId(), issue.getNo(), "Pemakaian bahan WO-001")
 *     .debit(wipAccount, 1_500_000, "BBB WO-001")
 *     .credit(rawMaterialAccount, 1_500_000, "Bahan baku keluar");
 * </pre>
 * Nilai nol diabaikan; nilai negatif otomatis dipindah ke sisi sebaliknya.
 */
public final class JournalDraft {

    public record Line(Account account, Department department, String description, BigDecimal debit, BigDecimal credit) {
    }

    private final LocalDate date;
    private final JournalType type;
    private final SourceType sourceType;
    private final Long sourceId;
    private final String sourceNo;
    private final String description;
    private final List<Line> lines = new ArrayList<>();

    private JournalDraft(LocalDate date, JournalType type, SourceType sourceType, Long sourceId, String sourceNo,
                         String description) {
        this.date = Objects.requireNonNull(date);
        this.type = type;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.sourceNo = sourceNo;
        this.description = description;
    }

    /** Jurnal otomatis dari dokumen modul. */
    public static JournalDraft of(LocalDate date, SourceType sourceType, Long sourceId, String sourceNo, String description) {
        return new JournalDraft(date, JournalType.AUTO, sourceType, sourceId, sourceNo, description);
    }

    public static JournalDraft of(LocalDate date, JournalType type, SourceType sourceType, Long sourceId, String sourceNo,
                                  String description) {
        return new JournalDraft(date, type, sourceType, sourceId, sourceNo, description);
    }

    public JournalDraft debit(Account account, BigDecimal amount, String desc) {
        return add(account, null, amount, desc, true);
    }

    public JournalDraft credit(Account account, BigDecimal amount, String desc) {
        return add(account, null, amount, desc, false);
    }

    public JournalDraft debit(Account account, Department dept, BigDecimal amount, String desc) {
        return add(account, dept, amount, desc, true);
    }

    public JournalDraft credit(Account account, Department dept, BigDecimal amount, String desc) {
        return add(account, dept, amount, desc, false);
    }

    private JournalDraft add(Account account, Department dept, BigDecimal amount, String desc, boolean debit) {
        BigDecimal amt = Money.amount(amount);
        if (amt.signum() == 0) {
            return this;
        }
        boolean isDebit = amt.signum() > 0 == debit;
        BigDecimal abs = amt.abs();
        lines.add(new Line(account, dept, desc, isDebit ? abs : BigDecimal.ZERO, isDebit ? BigDecimal.ZERO : abs));
        return this;
    }

    /**
     * Gabungkan baris dengan akun, departemen, dan sisi yang sama agar jurnal ringkas.
     * Keterangan baris pertama dipakai.
     */
    public JournalDraft merged() {
        Map<String, Line> map = new LinkedHashMap<>();
        for (Line l : lines) {
            boolean isDebit = l.debit().signum() > 0;
            String key = l.account().getId() + "|" + (l.department() == null ? "" : l.department().getId()) + "|" + isDebit;
            map.merge(key, l, (a, b) -> new Line(a.account(), a.department(), a.description(),
                    a.debit().add(b.debit()), a.credit().add(b.credit())));
        }
        JournalDraft d = new JournalDraft(date, type, sourceType, sourceId, sourceNo, description);
        d.lines.addAll(map.values());
        return d;
    }

    public BigDecimal totalDebit() {
        return lines.stream().map(Line::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalCredit() {
        return lines.stream().map(Line::credit).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public LocalDate date() {
        return date;
    }

    public JournalType type() {
        return type;
    }

    public SourceType sourceType() {
        return sourceType;
    }

    public Long sourceId() {
        return sourceId;
    }

    public String sourceNo() {
        return sourceNo;
    }

    public String description() {
        return description;
    }

    public List<Line> lines() {
        return List.copyOf(lines);
    }
}
