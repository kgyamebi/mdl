package com.mdl.platform.products.service;

import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.common.exception.ConflictException;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.products.dto.CreateProductCategoryRequest;
import com.mdl.platform.products.dto.ProductCategoryResponse;
import com.mdl.platform.products.dto.UpdateProductCategoryRequest;
import com.mdl.platform.products.entity.ProductCategory;
import com.mdl.platform.products.repository.ProductCategoryRepository;
import com.mdl.platform.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class ProductCategoryService {

    private final AuthorizationService authorizationService;
    private final ProductCategoryRepository categoryRepository;

    public ProductCategoryService(
            AuthorizationService authorizationService,
            ProductCategoryRepository categoryRepository) {
        this.authorizationService = authorizationService;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductCategoryResponse> listCategories(boolean activeOnly) {
        authorizationService.requirePermission("product:view");
        UserContext context = authorizationService.requireAuthenticated();

        List<ProductCategory> categories = activeOnly
                ? categoryRepository.findByBusinessIdAndStatusOrderBySortOrderAscNameAsc(context.businessId(), "ACTIVE")
                : categoryRepository.findByBusinessIdOrderBySortOrderAscNameAsc(context.businessId());

        return categories.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductCategoryResponse getCategory(Long categoryId) {
        authorizationService.requirePermission("product:view");
        UserContext context = authorizationService.requireAuthenticated();
        return toResponse(requireCategory(context.businessId(), categoryId));
    }

    @Transactional
    public ProductCategoryResponse createCategory(CreateProductCategoryRequest request) {
        authorizationService.requirePermission("product:manage");
        UserContext context = authorizationService.requireAuthenticated();

        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (categoryRepository.existsByBusinessIdAndCode(context.businessId(), code)) {
            throw new ConflictException("Category code already exists: " + code);
        }

        if (request.parentId() != null) {
            requireCategory(context.businessId(), request.parentId());
        }

        ProductCategory category = new ProductCategory();
        category.setBusinessId(context.businessId());
        category.setParentId(request.parentId());
        category.setName(request.name().trim());
        category.setCode(code);
        category.setDescription(trimToNull(request.description()));
        category.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        category.setStatus("ACTIVE");

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public ProductCategoryResponse updateCategory(Long categoryId, UpdateProductCategoryRequest request) {
        authorizationService.requirePermission("product:manage");
        UserContext context = authorizationService.requireAuthenticated();

        ProductCategory category = requireCategory(context.businessId(), categoryId);
        category.setName(request.name().trim());
        category.setDescription(trimToNull(request.description()));
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
        category.setStatus(request.status().trim().toUpperCase(Locale.ROOT));

        return toResponse(categoryRepository.save(category));
    }

    ProductCategory requireCategory(Long businessId, Long categoryId) {
        return categoryRepository.findByIdAndBusinessId(categoryId, businessId)
                .orElseThrow(() -> new NotFoundException("Product category not found"));
    }

    private ProductCategoryResponse toResponse(ProductCategory category) {
        return new ProductCategoryResponse(
                category.getId(),
                category.getParentId(),
                category.getCode(),
                category.getName(),
                category.getDescription(),
                category.getSortOrder(),
                category.getStatus());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
