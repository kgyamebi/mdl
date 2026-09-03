package com.mdl.platform.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateStocktakeRequest(
        @NotNull Long locationId,
        @Size(max = 1000) String notes,
        Boolean preloadBalances
) {
}
