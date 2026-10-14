package com.web.dto;

import com.web.entity.Invoice;
import com.web.entity.InvoiceDetail;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class InvoiceUserResponse {

    private Invoice invoice;

    private List<InvoiceDetail> invoiceDetails = new ArrayList<>();
}
