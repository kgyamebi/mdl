package com.mdl.platform.products.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductSearchEventRequest(
        @NotBlank @Size(max = 255) String queryText,
        Integer resultCount,
        Integer latencyMs,
        Long selectedProductId,
        Integer selectionLatencyMs,
        Long shopId,
        Boolean failed
) {
}
