package com.mdl.platform.notifications.dto;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String notificationType,
        String category,
        String title,
        String message,
        String entityType,
        Long entityId,
        String entityRef,
        String sourceType,
        Long sourceId,
        String status,
        Instant readAt,
        Instant dismissedAt,
        Instant createdAt
) {
}
