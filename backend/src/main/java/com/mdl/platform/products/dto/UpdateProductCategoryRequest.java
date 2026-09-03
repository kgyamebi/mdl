package com.mdl.platform.products.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProductCategoryRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 500) String description,
        Integer sortOrder,
        @NotBlank @Size(max = 32) String status
) {
}
