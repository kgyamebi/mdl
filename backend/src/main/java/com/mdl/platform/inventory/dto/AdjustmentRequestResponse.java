package com.mdl.platform.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdjustmentRequestResponse(
        Long id,
        Long locationId,
        String locationCode,
        String locationName,
        Long productId,
        String productSku,
        String productName,
        BigDecimal requestedChange,
        String reason,
        String status,
        Long requestedBy,
        Long reviewedBy,
        Instant reviewedAt,
        String reviewNotes,
        Long resultTransactionId,
        Instant createdAt
) {
}
