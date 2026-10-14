package com.web.utils;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class OnlineCounterBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private int online = 0;

    public OnlineCounterBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void onConnect(SessionConnectEvent event) {
        online++;
        messagingTemplate.convertAndSend("/topic/online", online);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        online--;
        messagingTemplate.convertAndSend("/topic/online", online);
    }
}

