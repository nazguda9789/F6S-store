package com.web.enums;

import lombok.Getter;

@Getter
public enum InvoiceStatus {
    DANG_CHO_VAN_CHUYEN("Đang chờ vận chuyển", "#6C757D", "fas fa-hourglass-half"), // đồng hồ cát
    DA_VAN_CHUYEN("Đã vận chuyển", "#0D6EFD", "fas fa-shipping-fast"),              // xe tải nhanh
    HOAN_THANH("Hoàn thành", "#198754", "fas fa-check-circle"),                     // check xanh
    DA_HUY("Đã hủy", "#DC3545", "fas fa-times-circle"),                             // dấu X
    TRA_VE("Trả về", "#FFC107", "fas fa-undo-alt"),                                 // mũi tên quay lại
    KHONG_NHAN_DUOC_HANG("Không nhận được hàng", "#FD7E14", "fas fa-exclamation-circle"), // chấm than
    YEU_CAU_DOI_HANG("Yêu cầu đổi hàng", "#6610F2", "fas fa-exchange-alt"),         // mũi tên hai chiều
    DA_DOI_HANG("Đã đổi hàng", "#20C997", "fas fa-sync");                           // sync

    private final String label;
    private final String color;
    private final String icon;

    InvoiceStatus(String label, String color, String icon) {
        this.label = label;
        this.color = color;
        this.icon = icon;
    }
}

