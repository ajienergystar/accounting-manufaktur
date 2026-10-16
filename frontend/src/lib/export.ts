/** Ekspor tabel ke CSV (UTF-8 BOM agar terbaca benar di Excel). */
export function exportCsv(filename: string, headers: string[], rows: (string | number | null | undefined)[][]) {
  const esc = (v: string | number | null | undefined) => {
    const s = v === null || v === undefined ? "" : String(v);
    return /[";\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
  };
  // Pemisah ";" sesuai pengaturan regional Indonesia di Excel
  const csv = [headers, ...rows].map((r) => r.map(esc).join(";")).join("\r\n");
  const blob = new Blob(["﻿" + csv], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename.endsWith(".csv") ? filename : `${filename}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

/** Angka untuk CSV: format desimal Indonesia tanpa pemisah ribuan. */
export function csvNumber(v: string | number | null | undefined): string {
  const n = Number(v ?? 0);
  return n.toFixed(2).replace(".", ",");
}
