package com.aji.accounting.common.sequence;

/** Jenis dokumen beserta prefix penomorannya. */
public enum DocType {
    JOURNAL("JU"),
    JOURNAL_ADJUSTMENT("JP"),
    JOURNAL_CLOSING("JT"),
    JOURNAL_OPENING("SA"),
    JOURNAL_AUTO("JA"),
    JOURNAL_REVERSAL("JB"),
    PURCHASE_REQUEST("PR"),
    PURCHASE_ORDER("PO"),
    GOODS_RECEIPT("GRN"),
    PURCHASE_INVOICE("PI"),
    PURCHASE_RETURN("RB"),
    AP_PAYMENT("PB"),
    SALES_QUOTATION("QT"),
    SALES_ORDER("SO"),
    DELIVERY_ORDER("DO"),
    SALES_INVOICE("SI"),
    SALES_RETURN("RJ"),
    AR_RECEIPT("PP"),
    STOCK_ADJUSTMENT("ADJ"),
    STOCK_TRANSFER("TRF"),
    STOCK_OPNAME("SOP"),
    WORK_ORDER("WO"),
    MATERIAL_ISSUE("MI"),
    LABOR_ENTRY("TK"),
    PRODUCTION_COMPLETION("FG"),
    CASH_IN("KM"),
    CASH_OUT("KK"),
    BANK_TRANSFER("TB"),
    FIXED_ASSET("FA"),
    DEPRECIATION("DEP"),
    ASSET_DISPOSAL("DSP"),
    PAYROLL("PAY"),
    OVERHEAD_ACTUAL("BOP");

    private final String prefix;

    DocType(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }
}
