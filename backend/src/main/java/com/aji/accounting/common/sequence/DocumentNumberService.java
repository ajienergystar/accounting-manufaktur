package com.aji.accounting.common.sequence;

import com.aji.accounting.config.CurrentUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Penomoran dokumen otomatis per tipe & periode: PREFIX/YYYY/MM/NNNN (contoh JU/2026/10/0001).
 * Memakai INSERT ... ON DUPLICATE KEY UPDATE: baris sequence terkunci (row lock) sampai transaksi selesai,
 * sehingga dua transaksi tidak pernah mendapat nomor yang sama.
 * Ikut transaksi pemanggil: bila transaksi gagal, nomor tidak terpakai (tidak ada lompatan nomor).
 */
@Service
public class DocumentNumberService {

    private final JdbcTemplate jdbc;

    public DocumentNumberService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String next(DocType type, LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();
        jdbc.update("""
                        INSERT INTO document_sequences
                            (doc_type, prefix, period_year, period_month, last_number, padding, created_at, created_by, version)
                        VALUES (?, ?, ?, ?, 1, 4, NOW(6), ?, 0)
                        ON DUPLICATE KEY UPDATE last_number = last_number + 1,
                                                updated_at = NOW(6), updated_by = VALUES(created_by)
                        """,
                type.name(), type.prefix(), year, month, CurrentUser.usernameOrSystem());
        Long number = jdbc.queryForObject(
                "SELECT last_number FROM document_sequences WHERE doc_type = ? AND period_year = ? AND period_month = ?",
                Long.class, type.name(), year, month);
        return String.format("%s/%04d/%02d/%04d", type.prefix(), year, month, number);
    }
}
