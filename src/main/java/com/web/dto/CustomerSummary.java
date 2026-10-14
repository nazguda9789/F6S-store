package com.web.dto;

import java.time.LocalDateTime;

public interface CustomerSummary {
    String getFullName();
    String getPhone();
    String getEmail();
    Long getTotalInvoice();
    Double getTotalAmount();
    String getLastInvoiceCode();
    String getLastInvoiceId();
    LocalDateTime getLastInvoiceDate();
}

