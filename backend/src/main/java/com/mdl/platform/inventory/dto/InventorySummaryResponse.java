package com.mdl.platform.inventory.dto;

public record InventorySummaryResponse(
        long balanceRowCount,
        long lowStockCount,
        long pendingAdjustmentRequests,
        long activeReservations
) {
}
