package com.mdl.platform.notifications.realtime;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final NotificationRealtimeBroadcaster broadcaster;

    public NotificationWebSocketHandler(NotificationRealtimeBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get(JwtWebSocketHandshakeInterceptor.USER_ID_ATTR);
        if (userId != null) {
            broadcaster.registerSession(userId, session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get(JwtWebSocketHandshakeInterceptor.USER_ID_ATTR);
        if (userId != null) {
            broadcaster.unregisterSession(userId, session);
        }
    }
}
