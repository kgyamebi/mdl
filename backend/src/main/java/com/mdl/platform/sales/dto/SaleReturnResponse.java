package com.mdl.platform.sales.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleReturnResponse(
        Long id,
        String returnNumber,
        Long saleId,
        String saleNumber,
        Long shopId,
        String currencyCode,
        String status,
        BigDecimal totalRefundAmount,
        String reason,
        String notes,
        Long processedBy,
        List<SaleReturnItemResponse> items,
        List<SaleReturnRefundResponse> refunds,
        Instant createdAt
) {
}
