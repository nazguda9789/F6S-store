package com.example.demodatn.service.donhang;

import com.example.demodatn.entity.donhang.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Component("bankTransferDeadline")
public class BankTransferDeadline {
    private final long timeoutHours;

    public BankTransferDeadline(@Value("${shop.order.bank-transfer-timeout-hours:24}") long timeoutHours) {
        this.timeoutHours = Math.max(1, timeoutHours);
    }

    public LocalDateTime paymentDeadline(Order order) {
        if (order == null || order.getCreatedAt() == null || !"ONLINE".equals(order.getOrderType())
                || !OrderStatusService.bankTransfer(order) || !"UNPAID".equals(order.getPaymentStatus())
                || order.getStatus() == null || Set.of("CANCELLED", "DELIVERY_FAILED", "DELIVERED").contains(order.getStatus())) {
            return null;
        }
        return order.getCreatedAt().plusHours(timeoutHours);
    }

    public LocalDateTime autoCancelAt(Order order) {
        return order != null && "PENDING".equals(order.getStatus()) ? paymentDeadline(order) : null;
    }
}
