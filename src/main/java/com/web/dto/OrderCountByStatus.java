package com.web.dto;

import com.web.enums.InvoiceStatus;

public interface OrderCountByStatus {

    InvoiceStatus getStatus();
    Long getOrderCount();
}
