package com.aji.accounting.ledger;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class JournalDtos {
    private JournalDtos() {
    }

    public record JournalLineRequest(
            @NotNull(message = "Akun wajib dipilih") Long accountId,
            Long departmentId,
            @Size(max = 255) String description,
            @DecimalMin(value = "0", message = "Debit tidak boleh negatif") BigDecimal debit,
            @DecimalMin(value = "0", message = "Kredit tidak boleh negatif") BigDecimal credit) {
    }

    public record JournalRequest(
            @NotNull(message = "Tanggal wajib diisi") LocalDate journalDate,
            JournalType journalType,
            @NotBlank(message = "Keterangan wajib diisi") @Size(max = 500) String description,
            @NotEmpty(message = "Baris jurnal wajib diisi") @Size(min = 2, message = "Minimal 2 baris") List<@Valid JournalLineRequest> lines,
            boolean post) {
    }

    public record OpeningBalanceLine(@NotNull Long accountId,
                                     @DecimalMin("0") BigDecimal debit,
                                     @DecimalMin("0") BigDecimal credit) {
    }

    /** Saldo awal; selisih debit-kredit otomatis ke akun Ekuitas Saldo Awal. */
    public record OpeningBalanceRequest(@NotNull(message = "Tanggal wajib diisi") LocalDate date,
                                        @NotEmpty(message = "Isi minimal satu saldo akun") List<@Valid OpeningBalanceLine> lines) {
    }

    public record ReverseRequest(@NotNull(message = "Tanggal wajib diisi") LocalDate date,
                                 @NotBlank(message = "Alasan wajib diisi") @Size(max = 300) String reason) {
    }

    public record JournalLineResponse(Long id, int lineNo, Long accountId, String accountCode, String accountName,
                                      Long departmentId, String departmentName, String description,
                                      BigDecimal debit, BigDecimal credit) {
        static JournalLineResponse from(JournalLine l) {
            var d = l.getDepartment();
            return new JournalLineResponse(l.getId(), l.getLineNo(), l.getAccount().getId(), l.getAccount().getCode(),
                    l.getAccount().getName(), d == null ? null : d.getId(), d == null ? null : d.getName(),
                    l.getDescription(), l.getDebit(), l.getCredit());
        }
    }

    public record JournalSummary(Long id, String journalNo, LocalDate journalDate, JournalType journalType,
                                 String journalTypeLabel, SourceType sourceType, String sourceTypeLabel, Long sourceId,
                                 String sourceNo, String description, JournalStatus status, BigDecimal totalDebit,
                                 BigDecimal totalCredit, boolean reversed) {
        static JournalSummary from(JournalEntry j) {
            return new JournalSummary(j.getId(), j.getJournalNo(), j.getJournalDate(), j.getJournalType(),
                    j.getJournalType().label(), j.getSourceType(), j.getSourceType().label(), j.getSourceId(),
                    j.getSourceNo(), j.getDescription(), j.getStatus(), j.getTotalDebit(), j.getTotalCredit(),
                    j.getReversedBy() != null);
        }
    }

    public record JournalDetail(Long id, String journalNo, LocalDate journalDate, String periodName,
                                JournalType journalType, String journalTypeLabel, SourceType sourceType,
                                String sourceTypeLabel, Long sourceId, String sourceNo, String description,
                                JournalStatus status, BigDecimal totalDebit, BigDecimal totalCredit,
                                LocalDateTime postedAt, String postedBy, String createdBy, LocalDateTime createdAt,
                                Long reversalOfId, String reversalOfNo, Long reversedById, String reversedByNo,
                                List<JournalLineResponse> lines) {
        static JournalDetail from(JournalEntry j) {
            var ro = j.getReversalOf();
            var rb = j.getReversedBy();
            return new JournalDetail(j.getId(), j.getJournalNo(), j.getJournalDate(), j.getPeriod().getName(),
                    j.getJournalType(), j.getJournalType().label(), j.getSourceType(), j.getSourceType().label(),
                    j.getSourceId(), j.getSourceNo(), j.getDescription(), j.getStatus(), j.getTotalDebit(),
                    j.getTotalCredit(), j.getPostedAt(), j.getPostedBy(), j.getCreatedBy(), j.getCreatedAt(),
                    ro == null ? null : ro.getId(), ro == null ? null : ro.getJournalNo(),
                    rb == null ? null : rb.getId(), rb == null ? null : rb.getJournalNo(),
                    j.getLines().stream().map(JournalLineResponse::from).toList());
        }
    }
}
