package com.mdl.platform.transfers.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record StockTransferItemResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        String unitOfMeasure,
        BigDecimal requestedQuantity,
        BigDecimal dispatchedQuantity,
        BigDecimal receivedQuantity,
        BigDecimal remainingToReceive,
        String notes
) {
}
