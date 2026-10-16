package com.aji.accounting.period;

public enum PeriodStatus {
    /** Boleh posting. */
    OPEN,
    /** Ditutup; dapat dibuka kembali oleh Admin. */
    CLOSED,
    /** Dikunci permanen; tidak dapat dibuka kembali. */
    LOCKED
}
