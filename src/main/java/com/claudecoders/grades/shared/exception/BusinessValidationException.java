package com.claudecoders.grades.shared.exception;

import java.util.Map;

public class BusinessValidationException extends RuntimeException {
    private final Map<String, String> errors;

    public BusinessValidationException(String field, String message) {
        super("La solicitud contiene datos inválidos");
        this.errors = Map.of(field, message);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
