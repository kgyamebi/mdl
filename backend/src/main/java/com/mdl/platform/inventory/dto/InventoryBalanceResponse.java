package com.mdl.platform.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record InventoryBalanceResponse(
        Long id,
        Long locationId,
        String locationCode,
        String locationName,
        String locationType,
        Long productId,
        String productSku,
        String productName,
        String unitOfMeasure,
        BigDecimal quantityOnHand,
        BigDecimal quantityReserved,
        BigDecimal quantityAvailable,
        Integer reorderLevel,
        boolean belowReorderLevel,
        Instant updatedAt
) {
}
