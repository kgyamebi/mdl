package com.mdl.platform.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record StocktakeResponse(
        Long id,
        String stocktakeNumber,
        Long locationId,
        String locationCode,
        String locationName,
        String status,
        String notes,
        int lineCount,
        int varianceLineCount,
        BigDecimal totalVariance,
        Long startedBy,
        Long submittedBy,
        Instant submittedAt,
        Long approvedBy,
        Instant approvedAt,
        Long cancelledBy,
        Instant cancelledAt,
        String cancelReason,
        List<StocktakeLineResponse> lines,
        Instant createdAt,
        Instant updatedAt
) {
}
