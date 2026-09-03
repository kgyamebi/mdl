package com.mdl.platform.products.service;

import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.businesses.repository.BusinessRepository;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.common.exception.ConflictException;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.products.dto.AddBarcodeRequest;
import com.mdl.platform.products.dto.BarcodeResponse;
import com.mdl.platform.products.dto.CreateProductRequest;
import com.mdl.platform.products.dto.ProductResponse;
import com.mdl.platform.products.dto.UpdateProductRequest;
import com.mdl.platform.products.entity.Product;
import com.mdl.platform.products.entity.ProductBarcode;
import com.mdl.platform.products.entity.ProductCategory;
import com.mdl.platform.products.repository.ProductBarcodeRepository;
import com.mdl.platform.products.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mdl.platform.security.UserContext;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final AuthorizationService authorizationService;
    private final ProductRepository productRepository;
    private final ProductBarcodeRepository barcodeRepository;
    private final ProductCategoryService categoryService;
    private final BusinessRepository businessRepository;

    public ProductService(
            AuthorizationService authorizationService,
            ProductRepository productRepository,
            ProductBarcodeRepository barcodeRepository,
            ProductCategoryService categoryService,
            BusinessRepository businessRepository) {
        this.authorizationService = authorizationService;
        this.productRepository = productRepository;
        this.barcodeRepository = barcodeRepository;
        this.categoryService = categoryService;
        this.businessRepository = businessRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listProducts(String search, Long categoryId, String status, int page, int size) {
        authorizationService.requirePermission("product:view");
        UserContext context = authorizationService.requireAuthenticated();

        String normalizedStatus = normalizeStatusFilter(status);
        Page<Product> result = productRepository.search(
                context.businessId(),
                normalizedStatus,
                categoryId,
                normalizeSearch(search),
                PageRequest.of(Math.max(page, 0), Math.max(size, 1)));

        String currencyCode = requireCurrencyCode(context.businessId());
        Map<Long, ProductCategory> categories = loadCategories(context.businessId(), result.getContent());
        Map<Long, List<ProductBarcode>> barcodes = loadBarcodes(result.getContent());

        List<ProductResponse> items = result.getContent().stream()
                .map(product -> toResponse(product, currencyCode, categories, barcodes))
                .toList();

        return new PageResponse<>(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {
        authorizationService.requirePermission("product:view");
        UserContext context = authorizationService.requireAuthenticated();
        Product product = requireProduct(context.businessId(), productId);
        return toDetailedResponse(product, context.businessId());
    }

    @Transactional(readOnly = true)
    public ProductResponse lookupByBarcode(String barcode) {
        authorizationService.requirePermission("product:view");
        UserContext context = authorizationService.requireAuthenticated();

        ProductBarcode productBarcode = barcodeRepository
                .findByBusinessIdAndBarcode(context.businessId(), barcode.trim())
                .orElseThrow(() -> new NotFoundException("No product found for barcode: " + barcode));

        Product product = requireProduct(context.businessId(), productBarcode.getProductId());
        return toDetailedResponse(product, context.businessId());
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        authorizationService.requirePermission("product:manage");
        UserContext context = authorizationService.requireAuthenticated();

        String sku = request.sku().trim().toUpperCase(Locale.ROOT);
        if (productRepository.existsByBusinessIdAndSku(context.businessId(), sku)) {
            throw new ConflictException("Product SKU already exists: " + sku);
        }

        ProductCategory category = null;
        if (request.categoryId() != null) {
            category = categoryService.requireCategory(context.businessId(), request.categoryId());
        }

        Product product = new Product();
        product.setBusinessId(context.businessId());
        product.setCategoryId(category != null ? category.getId() : null);
        product.setSku(sku);
        applyProductFields(product, request.name(), request.description(), request.brand(),
                request.unitOfMeasure(), request.costPrice(), request.sellingPrice(),
                request.taxInclusive(), request.trackInventory(), request.reorderLevel(),
                request.status() != null ? request.status() : "ACTIVE");

        product = productRepository.save(product);
        return toDetailedResponse(product, context.businessId());
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, UpdateProductRequest request) {
        authorizationService.requirePermission("product:manage");
        UserContext context = authorizationService.requireAuthenticated();

        Product product = requireProduct(context.businessId(), productId);

        if (request.categoryId() != null) {
            categoryService.requireCategory(context.businessId(), request.categoryId());
            product.setCategoryId(request.categoryId());
        } else {
            product.setCategoryId(null);
        }

        applyProductFields(product, request.name(), request.description(), request.brand(),
                request.unitOfMeasure(), request.costPrice(), request.sellingPrice(),
                request.taxInclusive(), request.trackInventory(), request.reorderLevel(),
                request.status());

        product = productRepository.save(product);
        return toDetailedResponse(product, context.businessId());
    }

    @Transactional
    public BarcodeResponse addBarcode(Long productId, AddBarcodeRequest request) {
        authorizationService.requirePermission("product:manage");
        UserContext context = authorizationService.requireAuthenticated();

        Product product = requireProduct(context.businessId(), productId);
        String barcodeValue = request.barcode().trim();

        if (barcodeRepository.existsByBusinessIdAndBarcode(context.businessId(), barcodeValue)) {
            throw new ConflictException("Barcode already in use: " + barcodeValue);
        }

        boolean makePrimary = Boolean.TRUE.equals(request.primary());
        if (makePrimary) {
            clearPrimaryBarcodes(product.getId());
        }

        ProductBarcode barcode = new ProductBarcode();
        barcode.setBusinessId(context.businessId());
        barcode.setProductId(product.getId());
        barcode.setBarcode(barcodeValue);
        barcode.setBarcodeType(request.barcodeType().trim().toUpperCase(Locale.ROOT));
        barcode.setPrimaryBarcode(makePrimary || barcodeRepository.findByProductIdOrderByPrimaryBarcodeDescBarcodeAsc(product.getId()).isEmpty());

        barcode = barcodeRepository.save(barcode);
        return toBarcodeResponse(barcode);
    }

    @Transactional
    public void removeBarcode(Long productId, Long barcodeId) {
        authorizationService.requirePermission("product:manage");
        UserContext context = authorizationService.requireAuthenticated();

        requireProduct(context.businessId(), productId);
        ProductBarcode barcode = barcodeRepository.findByIdAndBusinessId(barcodeId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Barcode not found"));

        if (!barcode.getProductId().equals(productId)) {
            throw new NotFoundException("Barcode not found for this product");
        }

        barcodeRepository.delete(barcode);
    }

    private Product requireProduct(Long businessId, Long productId) {
        return productRepository.findByIdAndBusinessId(productId, businessId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private void applyProductFields(
            Product product,
            String name,
            String description,
            String brand,
            String unitOfMeasure,
            java.math.BigDecimal costPrice,
            java.math.BigDecimal sellingPrice,
            Boolean taxInclusive,
            Boolean trackInventory,
            Integer reorderLevel,
            String status) {
        product.setName(name.trim());
        product.setDescription(trimToNull(description));
        product.setBrand(trimToNull(brand));
        product.setUnitOfMeasure(unitOfMeasure.trim().toUpperCase(Locale.ROOT));
        product.setCostPrice(costPrice);
        product.setSellingPrice(sellingPrice);
        if (taxInclusive != null) {
            product.setTaxInclusive(taxInclusive);
        }
        if (trackInventory != null) {
            product.setTrackInventory(trackInventory);
        }
        product.setReorderLevel(reorderLevel);
        product.setStatus(status.trim().toUpperCase(Locale.ROOT));
    }

    private void clearPrimaryBarcodes(Long productId) {
        barcodeRepository.findByProductIdOrderByPrimaryBarcodeDescBarcodeAsc(productId).stream()
                .filter(ProductBarcode::isPrimaryBarcode)
                .forEach(existing -> {
                    existing.setPrimaryBarcode(false);
                    barcodeRepository.save(existing);
                });
    }

    private ProductResponse toDetailedResponse(Product product, Long businessId) {
        String currencyCode = requireCurrencyCode(businessId);
        ProductCategory category = product.getCategoryId() != null
                ? categoryService.requireCategory(businessId, product.getCategoryId())
                : null;
        List<ProductBarcode> barcodes = barcodeRepository
                .findByProductIdOrderByPrimaryBarcodeDescBarcodeAsc(product.getId());

        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getBrand(),
                product.getCategoryId(),
                category != null ? category.getName() : null,
                product.getUnitOfMeasure(),
                product.getCostPrice(),
                product.getSellingPrice(),
                currencyCode,
                product.isTaxInclusive(),
                product.isTrackInventory(),
                product.getReorderLevel(),
                product.getStatus(),
                barcodes.stream().map(this::toBarcodeResponse).toList(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }

    private ProductResponse toResponse(
            Product product,
            String currencyCode,
            Map<Long, ProductCategory> categories,
            Map<Long, List<ProductBarcode>> barcodesByProduct) {
        ProductCategory category = product.getCategoryId() != null ? categories.get(product.getCategoryId()) : null;
        List<BarcodeResponse> barcodes = barcodesByProduct.getOrDefault(product.getId(), List.of()).stream()
                .map(this::toBarcodeResponse)
                .toList();

        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getBrand(),
                product.getCategoryId(),
                category != null ? category.getName() : null,
                product.getUnitOfMeasure(),
                product.getCostPrice(),
                product.getSellingPrice(),
                currencyCode,
                product.isTaxInclusive(),
                product.isTrackInventory(),
                product.getReorderLevel(),
                product.getStatus(),
                barcodes,
                product.getCreatedAt(),
                product.getUpdatedAt());
    }

    private Map<Long, ProductCategory> loadCategories(Long businessId, List<Product> products) {
        List<Long> categoryIds = products.stream()
                .map(Product::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        if (categoryIds.isEmpty()) {
            return Map.of();
        }

        return categoryIds.stream()
                .map(id -> categoryService.requireCategory(businessId, id))
                .collect(Collectors.toMap(ProductCategory::getId, Function.identity()));
    }

    private Map<Long, List<ProductBarcode>> loadBarcodes(List<Product> products) {
        List<Long> productIds = products.stream().map(Product::getId).toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }

        return barcodeRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(ProductBarcode::getProductId));
    }

    private String requireCurrencyCode(Long businessId) {
        return businessRepository.findByIdWithCurrency(businessId)
                .orElseThrow(() -> new NotFoundException("Business not found"))
                .getCurrencyCode();
    }

    private BarcodeResponse toBarcodeResponse(ProductBarcode barcode) {
        return new BarcodeResponse(
                barcode.getId(),
                barcode.getBarcode(),
                barcode.getBarcodeType(),
                barcode.isPrimaryBarcode());
    }

    private String normalizeSearch(String search) {
        return search == null ? "" : search.trim();
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
