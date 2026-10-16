package com.aji.accounting.common.exception;

/**
 * Pelanggaran aturan bisnis/akuntansi. Dipetakan ke HTTP 422.
 */
public class BusinessException extends RuntimeException {
    private final String code;

    public BusinessException(String message) {
        this("BUSINESS_RULE", message);
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
