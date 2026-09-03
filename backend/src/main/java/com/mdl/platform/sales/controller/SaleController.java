package com.mdl.platform.sales.controller;

import com.mdl.platform.common.dto.ApiResponse;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.sales.dto.CancelSaleRequest;
import com.mdl.platform.sales.dto.CreateSaleRequest;
import com.mdl.platform.sales.dto.RefundSaleRequest;
import com.mdl.platform.sales.dto.SaleResponse;
import com.mdl.platform.sales.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SaleResponse>>> listSales(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(saleService.listSales(status, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SaleResponse>> getSale(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(saleService.getSale(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SaleResponse>> createSale(
            @Valid @RequestBody CreateSaleRequest request) {
        SaleResponse created = saleService.createSale(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Sale completed", created));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<SaleResponse>> cancelSale(
            @PathVariable Long id,
            @Valid @RequestBody CancelSaleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Sale cancelled", saleService.cancelSale(id, request)));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<SaleResponse>> refundSale(
            @PathVariable Long id,
            @Valid @RequestBody RefundSaleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Sale refunded", saleService.refundSale(id, request)));
    }
}
