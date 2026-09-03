package com.mdl.platform.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ReservationResponse(
        Long id,
        Long locationId,
        String locationCode,
        String locationName,
        Long productId,
        String productSku,
        String productName,
        BigDecimal quantity,
        String referenceType,
        Long referenceId,
        String status,
        String notes,
        Long reservedBy,
        Long releasedBy,
        Instant releasedAt,
        Instant createdAt
) {
}
