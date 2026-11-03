package com.example.demodatn.exception;
public class DiscountValidationException extends IllegalArgumentException {
    private final String field;
    public DiscountValidationException(String field,String message) { super(message);this.field=field; }
    public String getField() { return field; }
}
