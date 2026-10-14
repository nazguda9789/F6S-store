package com.web.utils;

public class FormatUtils {
    public static String formatPhone(String phone) {
        if (phone == null) return "";
        // tách 3-3-4
        return phone.replaceFirst("(\\d{3})(\\d{3})(\\d+)", "$1 $2 $3");
    }
}