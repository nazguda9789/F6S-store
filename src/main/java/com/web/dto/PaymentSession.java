package com.web.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.RequestParam;

@Getter
@Setter
public class PaymentSession {

    private ResponsePayment responsePayment;
    String fullname;
    String phone;
    String address;
    String note;

    public PaymentSession() {
    }

    public PaymentSession(ResponsePayment responsePayment, String fullname, String phone, String address, String note) {
        this.responsePayment = responsePayment;
        this.fullname = fullname;
        this.phone = phone;
        this.address = address;
        this.note = note;
    }
}
