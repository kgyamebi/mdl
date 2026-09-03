package com.mdl.platform.reports.dto;

public record CsvExportResult(
        byte[] content,
        String fileName,
        String contentType,
        int rowCount,
        Long exportLogId
) {
}
