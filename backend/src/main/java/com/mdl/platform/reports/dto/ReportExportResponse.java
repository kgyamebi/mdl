package com.mdl.platform.reports.dto;

import java.time.Instant;

public record ReportExportResponse(
        Long id,
        String reportType,
        String exportFormat,
        String fileName,
        int rowCount,
        String parameters,
        String status,
        Long exportedBy,
        Instant createdAt
) {
}
