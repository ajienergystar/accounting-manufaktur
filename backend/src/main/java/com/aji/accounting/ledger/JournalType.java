package com.aji.accounting.ledger;

import com.aji.accounting.common.sequence.DocType;

public enum JournalType {
    GENERAL("Jurnal Umum", DocType.JOURNAL),
    ADJUSTMENT("Jurnal Penyesuaian", DocType.JOURNAL_ADJUSTMENT),
    AUTO("Jurnal Otomatis", DocType.JOURNAL_AUTO),
    REVERSAL("Jurnal Pembalik", DocType.JOURNAL_REVERSAL),
    OPENING("Saldo Awal", DocType.JOURNAL_OPENING),
    CLOSING("Jurnal Penutup", DocType.JOURNAL_CLOSING);

    private final String label;
    private final DocType docType;

    JournalType(String label, DocType docType) {
        this.label = label;
        this.docType = docType;
    }

    public String label() {
        return label;
    }

    public DocType docType() {
        return docType;
    }
}
