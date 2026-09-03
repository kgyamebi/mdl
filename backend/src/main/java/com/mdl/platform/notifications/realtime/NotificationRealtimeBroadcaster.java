package com.mdl.platform.notifications.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdl.platform.notifications.dto.NotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationRealtimeBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(NotificationRealtimeBroadcaster.class);

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessionsByUserId = new ConcurrentHashMap<>();

    public NotificationRealtimeBroadcaster(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void registerSession(Long userId, WebSocketSession session) {
        sessionsByUserId.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregisterSession(Long userId, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByUserId.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByUserId.remove(userId);
        }
    }

    public void broadcastToUser(Long userId, long unreadCount, NotificationResponse notification) {
        Set<WebSocketSession> sessions = sessionsByUserId.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        NotificationWebSocketMessage payload = NotificationWebSocketMessage.inboxUpdate(unreadCount, notification);
        try {
            String json = objectMapper.writeValueAsString(payload);
            TextMessage message = new TextMessage(json);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (IOException ex) {
            log.warn("Failed to broadcast notification to user {}: {}", userId, ex.getMessage());
        }
    }

    public int activeSessionCount() {
        return sessionsByUserId.values().stream().mapToInt(Set::size).sum();
    }
}
