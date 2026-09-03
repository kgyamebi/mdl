package com.mdl.platform.products.controller;

import com.mdl.platform.common.dto.ApiResponse;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.products.dto.AddBarcodeRequest;
import com.mdl.platform.products.dto.BarcodeResponse;
import com.mdl.platform.products.dto.CreateProductRequest;
import com.mdl.platform.products.dto.ProductResponse;
import com.mdl.platform.products.dto.UpdateProductRequest;
import com.mdl.platform.products.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> listProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(productService.listProducts(search, categoryId, status, page, size)));
    }

    @GetMapping("/lookup")
    public ResponseEntity<ApiResponse<ProductResponse>> lookupByBarcode(
            @RequestParam String barcode) {
        return ResponseEntity.ok(ApiResponse.ok(productService.lookupByBarcode(barcode)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getProduct(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Product created", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Product updated", productService.updateProduct(id, request)));
    }

    @PostMapping("/{id}/barcodes")
    public ResponseEntity<ApiResponse<BarcodeResponse>> addBarcode(
            @PathVariable Long id,
            @Valid @RequestBody AddBarcodeRequest request) {
        BarcodeResponse created = productService.addBarcode(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Barcode added", created));
    }

    @DeleteMapping("/{id}/barcodes/{barcodeId}")
    public ResponseEntity<ApiResponse<Void>> removeBarcode(
            @PathVariable Long id,
            @PathVariable Long barcodeId) {
        productService.removeBarcode(id, barcodeId);
        return ResponseEntity.ok(ApiResponse.ok("Barcode removed", null));
    }
}
