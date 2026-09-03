package com.mdl.platform.notifications.realtime;

import com.mdl.platform.notifications.dto.NotificationResponse;

public record NotificationWebSocketMessage(
        String type,
        long unreadCount,
        NotificationResponse notification
) {
    public static NotificationWebSocketMessage inboxUpdate(long unreadCount, NotificationResponse notification) {
        return new NotificationWebSocketMessage("INBOX_UPDATE", unreadCount, notification);
    }
}
