package com.aji.accounting.common.exception;

/** Gagal login / token tidak valid (dipetakan ke HTTP 401). */
public class AuthFailedException extends RuntimeException {
    public AuthFailedException(String message) {
        super(message);
    }
}
