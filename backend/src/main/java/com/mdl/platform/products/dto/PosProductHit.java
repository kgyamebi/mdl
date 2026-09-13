package com.mdl.platform.products.dto;

import java.math.BigDecimal;

public record PosProductHit(
        Long id,
        String sku,
        String name,
        String brand,
        Long categoryId,
        String categoryName,
        String unitOfMeasure,
        BigDecimal sellingPrice,
        String currencyCode,
        Integer reorderLevel,
        BigDecimal stockAvailable,
        String stockState,
        boolean favorite,
        long saleCount90d
) {
}
