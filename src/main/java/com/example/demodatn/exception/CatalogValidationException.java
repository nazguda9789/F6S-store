package com.example.demodatn.exception;
public class CatalogValidationException extends RuntimeException {
    private final String field;
    public CatalogValidationException(String field, String message) { super(message); this.field = field; }
    public String getField() { return field; }
}

