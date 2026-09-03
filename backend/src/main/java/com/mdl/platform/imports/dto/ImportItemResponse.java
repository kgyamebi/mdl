package com.mdl.platform.imports.dto;

import java.math.BigDecimal;

public record ImportItemResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        String unitOfMeasure,
        BigDecimal expectedQuantity,
        BigDecimal receivedQuantity,
        BigDecimal remainingQuantity,
        BigDecimal unitCost,
        String notes
) {
}
