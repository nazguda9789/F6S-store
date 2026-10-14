package com.web.utils;

import java.util.regex.Pattern;

public class PhoneValidator {
    private static final String PHONE_REGEX = "^0\\d{9}$";
    // ^0: bắt đầu bằng số 0
    // \\d{9}: theo sau là 9 chữ số (tổng cộng 10 số)

    private static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);

    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        return PHONE_PATTERN.matcher(phone).matches();
    }

}
