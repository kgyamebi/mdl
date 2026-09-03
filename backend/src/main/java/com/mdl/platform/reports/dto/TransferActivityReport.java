package com.mdl.platform.reports.dto;

import java.time.Instant;
import java.util.List;

public record TransferActivityReport(
        Instant from,
        Instant to,
        long totalTransfers,
        List<TransferStatusCount> statusCounts
) {
    public record TransferStatusCount(
            String status,
            long count
    ) {
    }
}
