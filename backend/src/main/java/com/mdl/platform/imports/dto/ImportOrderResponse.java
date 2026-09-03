package com.mdl.platform.imports.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ImportOrderResponse(
        Long id,
        String importNumber,
        String supplierName,
        String supplierReference,
        Long destinationLocationId,
        String destinationLocationCode,
        String destinationLocationName,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        String status,
        LocalDate expectedArrivalDate,
        String notes,
        Long assignedReceiverUserId,
        Long createdBy,
        Long approvedBy,
        Instant approvedAt,
        Long verifiedBy,
        Instant verifiedAt,
        List<ImportItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
}
