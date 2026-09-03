package com.mdl.platform.alerts.dto;

import java.util.List;

public record OwnerAttentionReport(
        long totalOpenAlerts,
        long criticalCount,
        long warningCount,
        List<AttentionCategory> categories,
        List<AlertResponse> recentAlerts
) {
}
