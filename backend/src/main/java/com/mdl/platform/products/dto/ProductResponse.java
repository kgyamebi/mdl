package com.mdl.platform.products.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        String brand,
        Long categoryId,
        String categoryName,
        String unitOfMeasure,
        BigDecimal costPrice,
        BigDecimal sellingPrice,
        String currencyCode,
        boolean taxInclusive,
        boolean trackInventory,
        Integer reorderLevel,
        String status,
        List<BarcodeResponse> barcodes,
        Instant createdAt,
        Instant updatedAt
) {
}
