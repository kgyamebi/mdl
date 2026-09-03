package com.mdl.platform.alerts.dto;

import java.time.Instant;

public record AlertResponse(
        Long id,
        String alertType,
        String severity,
        String module,
        String title,
        String summary,
        String entityType,
        Long entityId,
        String entityRef,
        String details,
        String status,
        Long acknowledgedBy,
        Instant acknowledgedAt,
        Instant resolvedAt,
        Instant createdAt
) {
}
