package com.example.demodatn.service.donhang;

import com.example.demodatn.entity.donhang.Order;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BankTransferDeadlineTest {
    @Test
    void deadlineUsesConfiguredHoursAndOnlyAdvertisesActualAutoCancellation() {
        var display = new BankTransferDeadline(2);
        var order = new Order();
        order.setOrderType("ONLINE");
        order.setPaymentMethod("BANK_TRANSFER");
        order.setPaymentStatus("UNPAID");
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.of(2026, 10, 23, 23, 50));
        var expected = LocalDateTime.of(2026, 10, 24, 1, 50);
        assertEquals(expected, display.paymentDeadline(order));
        assertEquals(expected, display.autoCancelAt(order));
        order.setPaymentMethod("TRANSFER");
        assertEquals(expected, display.paymentDeadline(order));
        order.setStatus("CONFIRMED");
        assertEquals(expected, display.paymentDeadline(order));
        assertNull(display.autoCancelAt(order));
        order.setPaymentStatus("PAID");
        assertNull(display.paymentDeadline(order));
        order.setPaymentStatus("UNPAID");
        order.setStatus("DELIVERY_FAILED");
        assertNull(display.paymentDeadline(order));
        order.setStatus("PENDING");
        order.setPaymentMethod("COD");
        assertNull(display.paymentDeadline(order));
        assertNull(display.paymentDeadline(null));
    }
}
