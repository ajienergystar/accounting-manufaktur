export const ACCOUNT_TYPES = {
  ASSET: "Aset",
  LIABILITY: "Liabilitas",
  EQUITY: "Ekuitas",
  REVENUE: "Pendapatan",
  COST_OF_GOODS: "Harga Pokok",
  EXPENSE: "Beban Operasional",
  OTHER_INCOME: "Pendapatan Lain-lain",
  OTHER_EXPENSE: "Beban Lain-lain",
  INCOME_TAX: "Beban Pajak Penghasilan",
} as const;
export type AccountType = keyof typeof ACCOUNT_TYPES;

export const CASH_FLOW = {
  OPERATING: "Operasi",
  INVESTING: "Investasi",
  FINANCING: "Pendanaan",
} as const;

export interface Account {
  id: number;
  code: string;
  name: string;
  parentId: number | null;
  parentCode: string | null;
  accountType: AccountType;
  accountTypeLabel: string;
  normalBalance: "DEBIT" | "CREDIT";
  header: boolean;
  cashBank: boolean;
  cashFlowCategory: "CASH" | "OPERATING" | "INVESTING" | "FINANCING" | null;
  level: number;
  description: string | null;
  active: boolean;
}
