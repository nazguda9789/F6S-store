package com.example.demodatn.util;

import org.springframework.stereotype.Component;
import java.text.NumberFormat;
import java.util.Locale;

@Component("money")
public class MoneyFormat {
    public String format(Number amount) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        format.setMaximumFractionDigits(2);
        return format.format(amount == null ? 0 : amount);
    }
}
