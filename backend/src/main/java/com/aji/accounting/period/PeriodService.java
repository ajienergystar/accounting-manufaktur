package com.aji.accounting.period;

import com.aji.accounting.audit.AuditAction;
import com.aji.accounting.audit.AuditService;
import com.aji.accounting.common.exception.BusinessException;
import com.aji.accounting.common.exception.ConflictException;
import com.aji.accounting.common.exception.NotFoundException;
import com.aji.accounting.company.CompanyRepository;
import com.aji.accounting.config.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PeriodService {

    private static final Locale ID = Locale.forLanguageTag("id-ID");

    public record PeriodResponse(Long id, int periodNo, String name, LocalDate startDate, LocalDate endDate,
                                 PeriodStatus status, LocalDateTime closedAt, String closedBy) {
        static PeriodResponse from(Period p) {
            return new PeriodResponse(p.getId(), p.getPeriodNo(), p.getName(), p.getStartDate(), p.getEndDate(),
                    p.getStatus(), p.getClosedAt(), p.getClosedBy());
        }
    }

    public record FiscalYearResponse(Long id, int year, LocalDate startDate, LocalDate endDate,
                                     FiscalYear.Status status, LocalDateTime closedAt, List<PeriodResponse> periods) {
        static FiscalYearResponse from(FiscalYear f) {
            return new FiscalYearResponse(f.getId(), f.getYear(), f.getStartDate(), f.getEndDate(), f.getStatus(),
                    f.getClosedAt(), f.getPeriods().stream().map(PeriodResponse::from).toList());
        }
    }

    /** Hook validasi sebelum periode ditutup (diisi modul lain, mis. cek selisih BOP). */
    public interface PeriodCloseCheck {
        void beforeClose(Period period);
    }

    private final FiscalYearRepository fiscalYears;
    private final PeriodRepository periods;
    private final CompanyRepository companies;
    private final AuditService audit;
    private final ObjectProvider<PeriodCloseCheck> closeChecks;

    @Transactional(readOnly = true)
    public List<FiscalYearResponse> list() {
        return fiscalYears.findAllWithPeriods().stream().map(FiscalYearResponse::from).toList();
    }

    /** Membuat tahun fiskal beserta 12 periode bulanan sesuai bulan awal tahun fiskal perusahaan. */
    @Transactional
    public FiscalYearResponse createFiscalYear(int year) {
        if (year < 2000 || year > 2100) {
            throw new BusinessException("Tahun tidak valid");
        }
        if (fiscalYears.existsByYear(year)) {
            throw new ConflictException("Tahun fiskal " + year + " sudah ada");
        }
        int startMonth = companies.findFirstByOrderByIdAsc().map(c -> c.getFiscalYearStartMonth()).orElse(1);
        LocalDate start = LocalDate.of(year, startMonth, 1);
        LocalDate end = start.plusYears(1).minusDays(1);
        if (fiscalYears.findContaining(start).isPresent() || fiscalYears.findContaining(end).isPresent()) {
            throw new ConflictException("Rentang tanggal tahun fiskal " + year + " bertumpuk dengan tahun fiskal lain");
        }

        FiscalYear fy = new FiscalYear();
        fy.setYear(year);
        fy.setStartDate(start);
        fy.setEndDate(end);
        for (int i = 0; i < 12; i++) {
            LocalDate ps = start.plusMonths(i);
            Period p = new Period();
            p.setFiscalYear(fy);
            p.setPeriodNo(i + 1);
            p.setStartDate(ps);
            p.setEndDate(ps.withDayOfMonth(ps.lengthOfMonth()));
            p.setName(ps.getMonth().getDisplayName(TextStyle.SHORT, ID) + " " + ps.getYear());
            fy.getPeriods().add(p);
        }
        fiscalYears.save(fy);
        audit.log(AuditAction.CREATE, "TahunFiskal", fy.getId(), "Buat tahun fiskal " + year + " (12 periode)");
        return FiscalYearResponse.from(fy);
    }

    /** Periode yang memuat tanggal tertentu. */
    @Transactional(readOnly = true)
    public Period findFor(LocalDate date) {
        return periods.findContaining(date).orElseThrow(() -> new BusinessException("PERIOD_NOT_FOUND",
                "Periode akuntansi untuk tanggal " + date + " belum dibuat. Buat tahun fiskal di menu Periode Akuntansi."));
    }

    /** Memastikan tanggal transaksi berada di periode OPEN. */
    @Transactional(readOnly = true)
    public Period assertOpen(LocalDate date) {
        Period p = findFor(date);
        if (p.getStatus() != PeriodStatus.OPEN) {
            throw new BusinessException("PERIOD_CLOSED", "Periode " + p.getName() + " sudah "
                    + (p.getStatus() == PeriodStatus.LOCKED ? "dikunci" : "ditutup") + ". Transaksi tidak dapat diposting.");
        }
        return p;
    }

    @Transactional
    public PeriodResponse close(Long periodId) {
        Period p = load(periodId);
        if (p.getStatus() != PeriodStatus.OPEN) {
            throw new BusinessException("Hanya periode OPEN yang dapat ditutup");
        }
        if (periods.countOpenBefore(p.getFiscalYear().getId(), p.getPeriodNo()) > 0) {
            throw new BusinessException("Tutup periode sebelumnya terlebih dahulu (penutupan harus berurutan)");
        }
        closeChecks.orderedStream().forEach(c -> c.beforeClose(p));
        p.setStatus(PeriodStatus.CLOSED);
        p.setClosedAt(LocalDateTime.now());
        p.setClosedBy(CurrentUser.usernameOrSystem());
        audit.log(AuditAction.CLOSE, "Periode", p.getId(), "Tutup periode " + p.getName());
        return PeriodResponse.from(p);
    }

    @Transactional
    public PeriodResponse reopen(Long periodId) {
        Period p = load(periodId);
        if (p.getStatus() != PeriodStatus.CLOSED) {
            throw new BusinessException(p.getStatus() == PeriodStatus.LOCKED
                    ? "Periode yang sudah dikunci tidak dapat dibuka kembali"
                    : "Periode sudah dalam status OPEN");
        }
        if (p.getFiscalYear().getStatus() == FiscalYear.Status.CLOSED) {
            throw new BusinessException("Tahun fiskal sudah ditutup; periode tidak dapat dibuka kembali");
        }
        if (periods.countClosedAfter(p.getFiscalYear().getId(), p.getPeriodNo()) > 0) {
            throw new BusinessException("Buka kembali periode setelahnya terlebih dahulu");
        }
        p.setStatus(PeriodStatus.OPEN);
        p.setClosedAt(null);
        p.setClosedBy(null);
        audit.log(AuditAction.REOPEN, "Periode", p.getId(), "Buka kembali periode " + p.getName());
        return PeriodResponse.from(p);
    }

    @Transactional
    public PeriodResponse lock(Long periodId) {
        Period p = load(periodId);
        if (p.getStatus() != PeriodStatus.CLOSED) {
            throw new BusinessException("Periode harus ditutup (CLOSED) sebelum dikunci");
        }
        p.setStatus(PeriodStatus.LOCKED);
        audit.log(AuditAction.CLOSE, "Periode", p.getId(), "Kunci permanen periode " + p.getName());
        return PeriodResponse.from(p);
    }

    private Period load(Long id) {
        return periods.findWithYear(id).orElseThrow(() -> NotFoundException.of("Periode", id));
    }
}
