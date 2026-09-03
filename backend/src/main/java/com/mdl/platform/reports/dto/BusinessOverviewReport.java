package com.mdl.platform.reports.dto;

import java.math.BigDecimal;

public record BusinessOverviewReport(
        String currencyCode,
        long completedSalesToday,
        BigDecimal salesAmountToday,
        long lowStockBalanceCount,
        long pendingTransferRequests,
        long activeTemporaryPermissions
) {
}
