package com.mdl.platform.reports.dto;

public record ReportExportResult(
        byte[] content,
        String fileName,
        String contentType,
        int rowCount,
        Long exportLogId
) {
}
