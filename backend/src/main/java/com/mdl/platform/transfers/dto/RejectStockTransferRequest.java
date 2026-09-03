package com.mdl.platform.transfers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectStockTransferRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
