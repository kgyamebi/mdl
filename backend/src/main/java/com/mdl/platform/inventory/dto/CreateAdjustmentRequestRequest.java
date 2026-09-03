package com.mdl.platform.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAdjustmentRequestRequest(
        @NotNull Long locationId,
        @NotNull Long productId,
        @NotNull BigDecimal requestedChange,
        @NotBlank @Size(max = 500) String reason
) {
}
