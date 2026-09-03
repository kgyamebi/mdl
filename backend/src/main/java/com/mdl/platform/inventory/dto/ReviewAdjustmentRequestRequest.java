package com.mdl.platform.inventory.dto;

import jakarta.validation.constraints.Size;

public record ReviewAdjustmentRequestRequest(
        @Size(max = 500) String reviewNotes
) {
}
