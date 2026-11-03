package com.example.demodatn.exception;

public class PeopleValidationException extends RuntimeException {
    private final String field;
    public PeopleValidationException(String field,String message) { super(message); this.field=field; }
    public String getField() { return field; }
}
