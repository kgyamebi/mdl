package com.mdl.platform.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpsertStocktakeLinesRequest(
        @NotEmpty @Valid List<UpsertStocktakeLineRequest> items
) {
}
