package com.mdl.platform.products.controller;

import com.mdl.platform.common.dto.ApiResponse;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.products.dto.PosProductHit;
import com.mdl.platform.products.dto.PosQuickAccessResponse;
import com.mdl.platform.products.dto.ProductSearchEventRequest;
import com.mdl.platform.products.service.ProductPosSearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/pos/products")
public class ProductPosController {

    private final ProductPosSearchService productPosSearchService;

    public ProductPosController(ProductPosSearchService productPosSearchService) {
        this.productPosSearchService = productPosSearchService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<PosProductHit>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "40") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                productPosSearchService.search(q, categoryId, locationId, page, size)));
    }

    @GetMapping("/quick-access")
    public ResponseEntity<ApiResponse<PosQuickAccessResponse>> quickAccess(
            @RequestParam(required = false) Long locationId) {
        return ResponseEntity.ok(ApiResponse.ok(productPosSearchService.quickAccess(locationId)));
    }

    @PostMapping("/{productId}/select")
    public ResponseEntity<ApiResponse<Void>> recordSelection(@PathVariable Long productId) {
        productPosSearchService.recordSelection(productId);
        return ResponseEntity.ok(ApiResponse.ok("Product selection recorded", null));
    }

    @PostMapping("/{productId}/favorite")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> toggleFavorite(@PathVariable Long productId) {
        boolean favorite = productPosSearchService.toggleFavorite(productId);
        return ResponseEntity.ok(ApiResponse.ok(
                favorite ? "Added to favorites" : "Removed from favorites",
                Map.of("favorite", favorite)));
    }

    @PostMapping("/search-events")
    public ResponseEntity<ApiResponse<Void>> recordSearchEvent(
            @Valid @RequestBody ProductSearchEventRequest request) {
        productPosSearchService.recordSearchEvent(request);
        return ResponseEntity.ok(ApiResponse.ok("Search event recorded", null));
    }
}
