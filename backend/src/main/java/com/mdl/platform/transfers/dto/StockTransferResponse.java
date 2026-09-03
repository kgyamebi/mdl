package com.mdl.platform.transfers.dto;

import java.time.Instant;
import java.util.List;

public record StockTransferResponse(
        Long id,
        String transferNumber,
        Long fromWarehouseId,
        String fromWarehouseCode,
        String fromWarehouseName,
        Long toWarehouseId,
        String toWarehouseCode,
        String toWarehouseName,
        Long fromLocationId,
        String fromLocationCode,
        Long toLocationId,
        String toLocationCode,
        String status,
        String notes,
        Long requestedBy,
        Long approvedBy,
        Instant approvedAt,
        Long dispatchedBy,
        Instant dispatchedAt,
        Long rejectedBy,
        Instant rejectedAt,
        String rejectReason,
        List<StockTransferItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
}
