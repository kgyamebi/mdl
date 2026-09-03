package com.mdl.platform.sales.dto;

import java.math.BigDecimal;

public record SaleItemResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        String unitOfMeasure,
        BigDecimal quantity,
        BigDecimal quantityReturned,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
