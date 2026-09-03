package com.mdl.platform.alerts.dto;

public record AttentionCategory(
        String code,
        String title,
        long count,
        String severity,
        String summary
) {
}
