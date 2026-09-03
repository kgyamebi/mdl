package com.mdl.platform.reports.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SalesSummaryReport(
        String currencyCode,
        Instant from,
        Instant to,
        Long shopId,
        long completedSalesCount,
        long cancelledSalesCount,
        long refundedSalesCount,
        BigDecimal grossSalesAmount,
        BigDecimal cancelledSalesAmount,
        BigDecimal refundedSalesAmount,
        BigDecimal netSalesAmount,
        BigDecimal itemsSold
) {
}
