package com.example.demodatn.util;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class PasswordPolicy {
    public static final String REGEX = "(?s:(?=.{8,}$)(?=.*\\p{L})(?=.*\\p{N}).+)";
    public static final String MESSAGE = "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ và số; tối đa 72 byte UTF-8.";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private PasswordPolicy() { }

    public static boolean isValid(String password) {
        return password != null && password.getBytes(StandardCharsets.UTF_8).length <= 72
                && PATTERN.matcher(password).matches();
    }

    public static void requireValid(String password) {
        if (!isValid(password)) throw new IllegalArgumentException(MESSAGE);
    }
}
