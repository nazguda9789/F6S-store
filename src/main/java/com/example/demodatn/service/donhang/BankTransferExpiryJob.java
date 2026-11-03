package com.example.demodatn.service.donhang;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.LocalDateTime;

@Component @RequiredArgsConstructor @Slf4j
public class BankTransferExpiryJob {
    private final JdbcTemplate jdbc;
    private final OrderStatusService statuses;
    private final Clock clock;
    @Value("${shop.order.bank-transfer-timeout-hours:24}") private long timeoutHours;
    @Scheduled(fixedDelayString="${shop.order.bank-transfer-cleanup-interval:PT5M}",initialDelayString="${shop.order.bank-transfer-cleanup-interval:PT5M}")
    public void cancelExpiredOrders() {
        var ids=jdbc.queryForList("SELECT TOP(100) id FROM orders WHERE order_type='ONLINE' AND status='PENDING' AND payment_status='UNPAID' AND payment_method IN ('TRANSFER','BANK_TRANSFER') AND created_at<=? ORDER BY created_at,id",Integer.class,LocalDateTime.now(clock).minusHours(Math.max(1,timeoutHours)));
        for(Integer id:ids) {
            try { if(statuses.cancelExpiredTransfer(id))log.info("Đã hủy đơn chuyển khoản quá hạn {}.",id); }
            catch(RuntimeException ex) { log.warn("Không thể hủy đơn chuyển khoản quá hạn {}: {}",id,ex.getMessage()); }
        }
    }
}
