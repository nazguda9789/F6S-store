package com.web.utils;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WebSocketSessionListener {

    private final AtomicInteger onlineUsers = new AtomicInteger(0);

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        int count = onlineUsers.incrementAndGet();
        System.out.println("New connection, online: " + count);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        int count = onlineUsers.decrementAndGet();
        System.out.println("Disconnected, online: " + count);
    }

    public int getOnlineUsers() {
        return onlineUsers.get();
    }
}
