package com.aji.accounting.ledger;

import com.aji.accounting.coa.Account;
import com.aji.accounting.coa.AccountRepository;
import com.aji.accounting.coa.AccountType;
import com.aji.accounting.coa.NormalBalance;
import com.aji.accounting.common.exception.BusinessException;
import com.aji.accounting.common.exception.NotFoundException;
import com.aji.accounting.period.FiscalYear;
import com.aji.accounting.period.FiscalYearRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Query saldo & mutasi buku besar. Hanya jurnal berstatus POSTED yang dihitung.
 * Akun laba-rugi (nominal) dihitung sejak awal tahun fiskal; akun neraca (riil) sejak awal.
 */
@Service
@RequiredArgsConstructor
public class LedgerQueryService {

    private final NamedParameterJdbcTemplate jdbc;
    private final AccountRepository accounts;
    private final FiscalYearRepository fiscalYears;

    // --------------------------------------------------------------- DTO

    public record LedgerLine(Long journalId, String journalNo, LocalDate date, JournalType journalType,
                             SourceType sourceType, Long sourceId, String sourceNo, String description,
                             String departmentName, BigDecimal debit, BigDecimal credit, BigDecimal balance) {
    }

    public record GeneralLedger(Long accountId, String accountCode, String accountName, NormalBalance normalBalance,
                                LocalDate from, LocalDate to, BigDecimal openingBalance, BigDecimal totalDebit,
                                BigDecimal totalCredit, BigDecimal closingBalance, List<LedgerLine> lines) {
    }

    /** Saldo signed: positif = sesuai saldo normal akun. */
    public record AccountBalance(Long accountId, String code, String name, AccountType accountType,
                                 NormalBalance normalBalance, Long parentId, int level,
                                 BigDecimal opening, BigDecimal debit, BigDecimal credit, BigDecimal closing) {
        public BigDecimal closingDebitSide() {
            return normalBalance == NormalBalance.DEBIT ? closing : closing.negate();
        }
    }

    public record TrialBalanceRow(Long accountId, String code, String name, AccountType accountType,
                                  String accountTypeLabel, NormalBalance normalBalance,
                                  BigDecimal openingDebit, BigDecimal openingCredit,
                                  BigDecimal mutationDebit, BigDecimal mutationCredit,
                                  BigDecimal closingDebit, BigDecimal closingCredit) {
    }

    public record TrialBalance(LocalDate from, LocalDate to, List<TrialBalanceRow> rows,
                               BigDecimal totalOpeningDebit, BigDecimal totalOpeningCredit,
                               BigDecimal totalMutationDebit, BigDecimal totalMutationCredit,
                               BigDecimal totalClosingDebit, BigDecimal totalClosingCredit, boolean balanced) {
    }

    // --------------------------------------------------------------- Buku besar

    @Transactional(readOnly = true)
    public GeneralLedger generalLedger(Long accountId, LocalDate from, LocalDate to, Long departmentId) {
        validateRange(from, to);
        Account acc = accounts.findById(accountId).orElseThrow(() -> NotFoundException.of("Akun", accountId));
        if (acc.isHeader()) {
            throw new BusinessException("Pilih akun detail (bukan header) untuk buku besar");
        }
        LocalDate openingStart = acc.getAccountType().isBalanceSheet() ? LocalDate.of(1900, 1, 1) : fiscalYearStart(from);
        int sign = acc.getNormalBalance() == NormalBalance.DEBIT ? 1 : -1;

        var p = new MapSqlParameterSource()
                .addValue("acc", accountId).addValue("from", from).addValue("to", to)
                .addValue("openStart", openingStart).addValue("dept", departmentId);

        BigDecimal openingRaw = jdbc.queryForObject("""
                SELECT COALESCE(SUM(jl.debit - jl.credit), 0)
                FROM journal_lines jl JOIN journal_entries je ON je.id = jl.journal_entry_id
                WHERE jl.account_id = :acc AND je.status = 'POSTED'
                  AND je.journal_date >= :openStart AND je.journal_date < :from
                  AND (:dept IS NULL OR jl.department_id = :dept)
                """, p, BigDecimal.class);
        BigDecimal opening = openingRaw.multiply(BigDecimal.valueOf(sign));

        List<LedgerLine> lines = new ArrayList<>();
        BigDecimal[] running = {opening};
        BigDecimal[] totals = {BigDecimal.ZERO, BigDecimal.ZERO};
        jdbc.query("""
                SELECT je.id, je.journal_no, je.journal_date, je.journal_type, je.source_type, je.source_id,
                       je.source_no, COALESCE(jl.description, je.description) AS description,
                       d.name AS dept_name, jl.debit, jl.credit
                FROM journal_lines jl
                JOIN journal_entries je ON je.id = jl.journal_entry_id
                LEFT JOIN departments d ON d.id = jl.department_id
                WHERE jl.account_id = :acc AND je.status = 'POSTED'
                  AND je.journal_date BETWEEN :from AND :to
                  AND (:dept IS NULL OR jl.department_id = :dept)
                ORDER BY je.journal_date, je.id, jl.line_no
                """, p, rs -> {
            BigDecimal debit = rs.getBigDecimal("debit");
            BigDecimal credit = rs.getBigDecimal("credit");
            running[0] = running[0].add(debit.subtract(credit).multiply(BigDecimal.valueOf(sign)));
            totals[0] = totals[0].add(debit);
            totals[1] = totals[1].add(credit);
            lines.add(new LedgerLine(rs.getLong("id"), rs.getString("journal_no"),
                    rs.getObject("journal_date", LocalDate.class), JournalType.valueOf(rs.getString("journal_type")),
                    SourceType.valueOf(rs.getString("source_type")), (Long) rs.getObject("source_id", Long.class),
                    rs.getString("source_no"), rs.getString("description"), rs.getString("dept_name"),
                    debit, credit, running[0]));
        });

        return new GeneralLedger(acc.getId(), acc.getCode(), acc.getName(), acc.getNormalBalance(), from, to,
                opening, totals[0], totals[1], running[0], lines);
    }

    // --------------------------------------------------------------- Saldo per akun

    /**
     * Saldo semua akun detail untuk rentang tanggal.
     * @param excludeClosing true untuk laporan laba rugi (abaikan jurnal penutup tahunan)
     */
    @Transactional(readOnly = true)
    public List<AccountBalance> balances(LocalDate from, LocalDate to, boolean excludeClosing) {
        validateRange(from, to);
        LocalDate fyStart = fiscalYearStart(from);
        var p = new MapSqlParameterSource().addValue("from", from).addValue("to", to).addValue("fyStart", fyStart)
                .addValue("exclClosing", excludeClosing);
        Map<Long, AccountBalance> result = new LinkedHashMap<>();
        jdbc.query("""
                SELECT a.id, a.code, a.name, a.account_type, a.normal_balance, a.parent_id, a.level,
                  COALESCE(SUM(CASE WHEN je.journal_date < :from
                        AND (a.account_type IN ('ASSET','LIABILITY','EQUITY') OR je.journal_date >= :fyStart)
                        THEN jl.debit - jl.credit END), 0) AS opening_raw,
                  COALESCE(SUM(CASE WHEN je.journal_date BETWEEN :from AND :to THEN jl.debit END), 0) AS mut_debit,
                  COALESCE(SUM(CASE WHEN je.journal_date BETWEEN :from AND :to THEN jl.credit END), 0) AS mut_credit
                FROM accounts a
                LEFT JOIN journal_lines jl ON jl.account_id = a.id
                LEFT JOIN journal_entries je ON je.id = jl.journal_entry_id AND je.status = 'POSTED'
                     AND je.journal_date <= :to
                     AND (:exclClosing = FALSE OR je.journal_type <> 'CLOSING')
                WHERE a.is_header = FALSE
                GROUP BY a.id, a.code, a.name, a.account_type, a.normal_balance, a.parent_id, a.level
                ORDER BY a.code
                """, p, rs -> {
            NormalBalance nb = NormalBalance.valueOf(rs.getString("normal_balance"));
            BigDecimal sign = nb == NormalBalance.DEBIT ? BigDecimal.ONE : BigDecimal.ONE.negate();
            BigDecimal opening = rs.getBigDecimal("opening_raw").multiply(sign);
            BigDecimal debit = rs.getBigDecimal("mut_debit");
            BigDecimal credit = rs.getBigDecimal("mut_credit");
            BigDecimal closing = opening.add(debit.subtract(credit).multiply(sign));
            long id = rs.getLong("id");
            result.put(id, new AccountBalance(id, rs.getString("code"), rs.getString("name"),
                    AccountType.valueOf(rs.getString("account_type")), nb, (Long) rs.getObject("parent_id", Long.class),
                    rs.getInt("level"), opening, debit, credit, closing));
        });
        return new ArrayList<>(result.values());
    }

    // --------------------------------------------------------------- Neraca saldo

    @Transactional(readOnly = true)
    public TrialBalance trialBalance(LocalDate from, LocalDate to, boolean includeZero) {
        List<TrialBalanceRow> rows = new ArrayList<>();
        BigDecimal[] t = new BigDecimal[6];
        java.util.Arrays.fill(t, BigDecimal.ZERO);
        for (AccountBalance b : balances(from, to, false)) {
            boolean zero = b.opening().signum() == 0 && b.debit().signum() == 0 && b.credit().signum() == 0;
            if (zero && !includeZero) {
                continue;
            }
            BigDecimal openDebitSide = b.normalBalance() == NormalBalance.DEBIT ? b.opening() : b.opening().negate();
            BigDecimal closeDebitSide = b.closingDebitSide();
            BigDecimal od = openDebitSide.signum() > 0 ? openDebitSide : BigDecimal.ZERO;
            BigDecimal oc = openDebitSide.signum() < 0 ? openDebitSide.negate() : BigDecimal.ZERO;
            BigDecimal cd = closeDebitSide.signum() > 0 ? closeDebitSide : BigDecimal.ZERO;
            BigDecimal cc = closeDebitSide.signum() < 0 ? closeDebitSide.negate() : BigDecimal.ZERO;
            rows.add(new TrialBalanceRow(b.accountId(), b.code(), b.name(), b.accountType(), b.accountType().label(),
                    b.normalBalance(), od, oc, b.debit(), b.credit(), cd, cc));
            t[0] = t[0].add(od);
            t[1] = t[1].add(oc);
            t[2] = t[2].add(b.debit());
            t[3] = t[3].add(b.credit());
            t[4] = t[4].add(cd);
            t[5] = t[5].add(cc);
        }
        boolean balanced = t[2].compareTo(t[3]) == 0 && t[4].compareTo(t[5]) == 0;
        return new TrialBalance(from, to, rows, t[0], t[1], t[2], t[3], t[4], t[5], balanced);
    }

    /** Saldo signed satu akun per tanggal (sesuai saldo normal). */
    @Transactional(readOnly = true)
    public BigDecimal balanceAsOf(Long accountId, LocalDate asOf) {
        Account acc = accounts.findById(accountId).orElseThrow(() -> NotFoundException.of("Akun", accountId));
        LocalDate start = acc.getAccountType().isBalanceSheet() ? LocalDate.of(1900, 1, 1) : fiscalYearStart(asOf);
        BigDecimal raw = jdbc.queryForObject("""
                SELECT COALESCE(SUM(jl.debit - jl.credit), 0)
                FROM journal_lines jl JOIN journal_entries je ON je.id = jl.journal_entry_id
                WHERE jl.account_id = :acc AND je.status = 'POSTED' AND je.journal_date BETWEEN :start AND :asOf
                """, new MapSqlParameterSource().addValue("acc", accountId).addValue("start", start).addValue("asOf", asOf),
                BigDecimal.class);
        return acc.getNormalBalance() == NormalBalance.DEBIT ? raw : raw.negate();
    }

    public LocalDate fiscalYearStart(LocalDate date) {
        return fiscalYears.findContaining(date).map(FiscalYear::getStartDate).orElse(LocalDate.of(date.getYear(), 1, 1));
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessException("Tanggal awal dan akhir wajib diisi");
        }
        if (from.isAfter(to)) {
            throw new BusinessException("Tanggal awal tidak boleh setelah tanggal akhir");
        }
    }
}
