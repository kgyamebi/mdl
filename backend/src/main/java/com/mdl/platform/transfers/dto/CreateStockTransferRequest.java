package com.mdl.platform.transfers.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateStockTransferRequest(
        @NotNull Long fromWarehouseId,
        @NotNull Long toWarehouseId,
        @Size(max = 1000) String notes,
        @NotEmpty @Valid List<CreateStockTransferItemRequest> items
) {
    public record CreateStockTransferItemRequest(
            @NotNull Long productId,
            @NotNull @Positive BigDecimal quantity,
            @Size(max = 500) String notes
    ) {
    }
}
