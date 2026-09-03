package com.mdl.platform.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record InventoryTransactionResponse(
        Long id,
        Long locationId,
        String locationCode,
        String locationName,
        Long productId,
        String productSku,
        String productName,
        String transactionType,
        BigDecimal quantityChange,
        BigDecimal quantityAfter,
        String referenceType,
        Long referenceId,
        String notes,
        Long performedBy,
        Instant transactionAt
) {
}
