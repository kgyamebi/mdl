package com.mdl.platform.reports.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SalesByProductReport(
        String currencyCode,
        Instant from,
        Instant to,
        Long shopId,
        List<SalesByProductRow> items
) {
    public record SalesByProductRow(
            Long productId,
            String productSku,
            String productName,
            BigDecimal quantitySold,
            BigDecimal revenue
    ) {
    }
}
