package com.web.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public enum InvoiceType {

    ONLINE("Đặt qua web"), TAI_QUAY("Đặt tại quầy"), FACEBOOK("Đơn facebook");

    private final String label;

    InvoiceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
