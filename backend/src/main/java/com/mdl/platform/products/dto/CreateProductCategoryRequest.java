package com.mdl.platform.products.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProductCategoryRequest(
        Long parentId,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 50) String code,
        @Size(max = 500) String description,
        Integer sortOrder
) {
}
