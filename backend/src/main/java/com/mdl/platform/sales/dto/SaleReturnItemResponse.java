package com.mdl.platform.sales.dto;

import java.math.BigDecimal;

public record SaleReturnItemResponse(
        Long id,
        Long saleItemId,
        Long productId,
        String productSku,
        String productName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineRefund
) {
}
