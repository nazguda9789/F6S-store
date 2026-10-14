package com.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.web.enums.InvoiceStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Pageable;

import java.sql.Date;

@Getter
@Setter
public class InvoiceRequest {

    private InvoiceStatus invoiceStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private Date from;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private Date to;

    private String search;


}
