package com.mdl.platform.imports.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ReceiveImportRequest(
        @NotEmpty @Valid List<ReceiveImportItemRequest> items
) {
    public record ReceiveImportItemRequest(
            @NotNull Long itemId,
            @NotNull @DecimalMin("0.0001") BigDecimal quantityReceived,
            String notes
    ) {
    }
}
