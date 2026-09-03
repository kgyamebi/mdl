package com.mdl.platform.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpsertStocktakeLineRequest(
        @NotNull Long productId,
        @NotNull BigDecimal countedQuantity,
        @Size(max = 500) String notes
) {
}
