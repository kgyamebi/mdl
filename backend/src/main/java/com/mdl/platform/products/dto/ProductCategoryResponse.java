package com.mdl.platform.products.dto;

public record ProductCategoryResponse(
        Long id,
        Long parentId,
        String code,
        String name,
        String description,
        int sortOrder,
        String status
) {
}
