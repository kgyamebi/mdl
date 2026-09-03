package com.mdl.platform.transfers.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ReceiveStockTransferRequest(
        @NotEmpty @Valid List<ReceiveStockTransferItemRequest> items
) {
    public record ReceiveStockTransferItemRequest(
            @NotNull Long itemId,
            @NotNull @Positive BigDecimal quantityReceived,
            @Size(max = 500) String notes
    ) {
    }
}
