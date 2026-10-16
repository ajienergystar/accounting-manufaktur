package com.aji.accounting.coa;

public enum AccountType {
    ASSET("Aset", NormalBalance.DEBIT, true),
    LIABILITY("Liabilitas", NormalBalance.CREDIT, true),
    EQUITY("Ekuitas", NormalBalance.CREDIT, true),
    REVENUE("Pendapatan", NormalBalance.CREDIT, false),
    COST_OF_GOODS("Harga Pokok", NormalBalance.DEBIT, false),
    EXPENSE("Beban Operasional", NormalBalance.DEBIT, false),
    OTHER_INCOME("Pendapatan Lain-lain", NormalBalance.CREDIT, false),
    OTHER_EXPENSE("Beban Lain-lain", NormalBalance.DEBIT, false),
    INCOME_TAX("Beban Pajak Penghasilan", NormalBalance.DEBIT, false);

    private final String label;
    private final NormalBalance defaultNormalBalance;
    private final boolean balanceSheet;

    AccountType(String label, NormalBalance defaultNormalBalance, boolean balanceSheet) {
        this.label = label;
        this.defaultNormalBalance = defaultNormalBalance;
        this.balanceSheet = balanceSheet;
    }

    public String label() {
        return label;
    }

    public NormalBalance defaultNormalBalance() {
        return defaultNormalBalance;
    }

    /** true = akun neraca (riil), false = akun laba rugi (nominal, ditutup akhir tahun). */
    public boolean isBalanceSheet() {
        return balanceSheet;
    }

    /** Kelompok laba-rugi (pendapatan & beban) yang dapat bertukar tipe di grup 7. */
    public boolean isProfitAndLoss() {
        return !balanceSheet;
    }
}
