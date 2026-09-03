package com.mdl.platform.sales.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefundSaleRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
