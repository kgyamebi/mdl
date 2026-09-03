package com.mdl.platform.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelStocktakeRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
