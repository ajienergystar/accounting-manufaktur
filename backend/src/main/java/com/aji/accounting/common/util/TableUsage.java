package com.aji.accounting.common.util;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Mengecek apakah sebuah id sudah dipakai di tabel lain (mis. akun di journal_lines)
 * tanpa membuat dependensi antar-modul. Aman dipanggil walau tabel belum ada.
 */
@Component
public class TableUsage {

    private final JdbcTemplate jdbc;

    public TableUsage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** true bila tabel ada dan berisi minimal satu baris. */
    public boolean hasRows(String table) {
        if (!table.matches("[a-z_]+")) {
            throw new IllegalArgumentException("Nama tabel tidak valid");
        }
        if (!tableExists(table)) {
            return false;
        }
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM (SELECT 1 FROM " + table + " LIMIT 1) t", Integer.class);
        return n != null && n > 0;
    }

    private boolean tableExists(String table) {
        Integer exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, table);
        return exists != null && exists > 0;
    }

    public boolean isUsed(String table, String column, Long id) {
        if (!table.matches("[a-z_]+") || !column.matches("[a-z_]+")) {
            throw new IllegalArgumentException("Nama tabel/kolom tidak valid");
        }
        if (!tableExists(table)) {
            return false;
        }
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM (SELECT 1 FROM " + table + " WHERE " + column + " = ? LIMIT 1) t",
                Integer.class, id);
        return n != null && n > 0;
    }
}
