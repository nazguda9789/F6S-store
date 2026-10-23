package com.web.service;

import org.springframework.stereotype.Service;
import java.util.Locale;

/** Stable internal identifiers, independent of editable names/color labels. */
@Service
public class CatalogCodeService {
    public String productCode(Long id) { return format("F6S-SP", id); }
    public String variantCode(Long id) { return format("F6S-SKU", id); }
    private String format(String prefix, Long id) {
        if (id == null || id < 1) throw new IllegalArgumentException("Chưa có ID hợp lệ để sinh mã.");
        return prefix + String.format(Locale.ROOT, "%06d", id);
    }
}
