package com.web.exception;


import org.springframework.security.core.AuthenticationException;

public class UserNotActivatedException extends AuthenticationException {
    private final String email;

    public UserNotActivatedException(String msg, String email) {
        super(msg);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}