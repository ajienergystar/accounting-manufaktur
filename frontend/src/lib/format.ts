import { format, parseISO } from "date-fns";
import { id } from "date-fns/locale";

const idrFormatter = new Intl.NumberFormat("id-ID", {
  style: "currency",
  currency: "IDR",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const numberFormatter = new Intl.NumberFormat("id-ID", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const qtyFormatter = new Intl.NumberFormat("id-ID", {
  minimumFractionDigits: 0,
  maximumFractionDigits: 4,
});

type Numeric = number | string | null | undefined;

function toNumber(v: Numeric): number {
  if (v === null || v === undefined || v === "") return 0;
  return typeof v === "number" ? v : Number(v);
}

/** Rp 1.250.000,00 */
export function formatRupiah(v: Numeric): string {
  return idrFormatter.format(toNumber(v));
}

/** 1.250.000,00 — angka akuntansi; negatif ditampilkan dalam kurung. */
export function formatAmount(v: Numeric, opts?: { parentheses?: boolean }): string {
  const n = toNumber(v);
  if (opts?.parentheses && n < 0) return `(${numberFormatter.format(Math.abs(n))})`;
  return numberFormatter.format(n);
}

export function formatQty(v: Numeric): string {
  return qtyFormatter.format(toNumber(v));
}

/** Parse "1.250.000,50" → "1250000.50" (string desimal untuk dikirim ke backend). */
export function parseIndonesianNumber(input: string): string {
  const cleaned = input.replace(/[^\d,-]/g, "").replace(",", ".");
  if (cleaned === "" || cleaned === "-") return "0";
  const n = Number(cleaned);
  return Number.isFinite(n) ? cleaned : "0";
}

export function formatDate(v: string | Date | null | undefined, pattern = "dd MMM yyyy"): string {
  if (!v) return "-";
  const d = typeof v === "string" ? parseISO(v) : v;
  return format(d, pattern, { locale: id });
}

export function formatDateTime(v: string | Date | null | undefined): string {
  return formatDate(v, "dd MMM yyyy HH:mm");
}

export function todayIso(): string {
  return format(new Date(), "yyyy-MM-dd");
}
