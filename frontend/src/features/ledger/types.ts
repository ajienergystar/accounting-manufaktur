export type JournalType = "GENERAL" | "ADJUSTMENT" | "AUTO" | "REVERSAL" | "OPENING" | "CLOSING";
export type JournalStatus = "DRAFT" | "POSTED" | "VOID";

export interface JournalSummary {
  id: number;
  journalNo: string;
  journalDate: string;
  journalType: JournalType;
  journalTypeLabel: string;
  sourceType: string;
  sourceTypeLabel: string;
  sourceId: number | null;
  sourceNo: string | null;
  description: string;
  status: JournalStatus;
  totalDebit: string;
  totalCredit: string;
  reversed: boolean;
}

export interface JournalLine {
  id: number;
  lineNo: number;
  accountId: number;
  accountCode: string;
  accountName: string;
  departmentId: number | null;
  departmentName: string | null;
  description: string | null;
  debit: string;
  credit: string;
}

export interface JournalDetail extends Omit<JournalSummary, "reversed"> {
  periodName: string;
  postedAt: string | null;
  postedBy: string | null;
  createdBy: string | null;
  createdAt: string;
  reversalOfId: number | null;
  reversalOfNo: string | null;
  reversedById: number | null;
  reversedByNo: string | null;
  lines: JournalLine[];
}

export const STATUS_META: Record<JournalStatus, { label: string; tone: "warning" | "success" | "neutral" }> = {
  DRAFT: { label: "Draft", tone: "warning" },
  POSTED: { label: "Posted", tone: "success" },
  VOID: { label: "Batal", tone: "neutral" },
};

export const JOURNAL_TYPE_LABEL: Record<JournalType, string> = {
  GENERAL: "Jurnal Umum",
  ADJUSTMENT: "Jurnal Penyesuaian",
  AUTO: "Jurnal Otomatis",
  REVERSAL: "Jurnal Pembalik",
  OPENING: "Saldo Awal",
  CLOSING: "Jurnal Penutup",
};

/** Penjumlahan desimal aman (dalam sen) untuk total di UI. */
export function sumDecimal(values: (string | undefined | null)[]): number {
  const cents = values.reduce((acc, v) => acc + Math.round(Number(v || 0) * 100), 0);
  return cents / 100;
}
