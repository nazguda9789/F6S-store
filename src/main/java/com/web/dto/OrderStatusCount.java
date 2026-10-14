package com.web.dto;

import com.web.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class OrderStatusCount {
    private InvoiceStatus invoiceStatus;
    private Long number;
}
