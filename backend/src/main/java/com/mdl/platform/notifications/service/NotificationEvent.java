package com.mdl.platform.notifications.service;

public record NotificationEvent(
        String notificationType,
        String category,
        String title,
        String message,
        String entityType,
        Long entityId,
        String entityRef,
        String sourceType,
        Long sourceId,
        String dedupeKey
) {
}
