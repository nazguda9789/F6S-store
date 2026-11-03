package com.example.demodatn.repository.donhang;

/** Điều kiện dùng chung cho doanh thu và bán chạy; alias đơn hàng luôn là o. */
public final class RevenueCondition {
    public static final String SQL = "o.payment_status='PAID' AND o.status NOT IN ('CANCELLED','DELIVERY_FAILED')";

    private RevenueCondition() {
    }
}
