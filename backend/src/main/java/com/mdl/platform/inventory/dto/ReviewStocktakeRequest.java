package com.mdl.platform.inventory.dto;

import jakarta.validation.constraints.Size;

public record ReviewStocktakeRequest(
        @Size(max = 500) String reviewNotes
) {
}
