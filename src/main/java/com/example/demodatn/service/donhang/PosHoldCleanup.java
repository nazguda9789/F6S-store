package com.example.demodatn.service.donhang;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;

@Component @RequiredArgsConstructor
public class PosHoldCleanup {
    private final PosHoldService holds;
    @Scheduled(fixedDelayString="${shop.pos.hold.cleanup-interval:PT5M}")
    public void run(){holds.cleanup();}
}
