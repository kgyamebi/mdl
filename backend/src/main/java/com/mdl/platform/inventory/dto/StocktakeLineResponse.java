package com.mdl.platform.inventory.dto;

import java.math.BigDecimal;

public record StocktakeLineResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        String unitOfMeasure,
        BigDecimal expectedQuantity,
        BigDecimal countedQuantity,
        BigDecimal variance,
        String notes,
        Long resultTransactionId
) {
}
