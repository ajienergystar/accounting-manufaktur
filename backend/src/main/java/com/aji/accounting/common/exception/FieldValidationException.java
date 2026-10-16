package com.aji.accounting.common.exception;

import java.util.Map;

/** Error validasi pada field tertentu yang dicek di service (dipetakan ke HTTP 400). */
public class FieldValidationException extends RuntimeException {
    private final Map<String, String> errors;

    public FieldValidationException(String field, String message) {
        super(message);
        this.errors = Map.of(field, message);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
