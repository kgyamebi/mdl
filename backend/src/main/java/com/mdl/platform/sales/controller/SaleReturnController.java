package com.mdl.platform.sales.controller;

import com.mdl.platform.common.dto.ApiResponse;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.sales.dto.CreateSaleReturnRequest;
import com.mdl.platform.sales.dto.SaleReturnResponse;
import com.mdl.platform.sales.service.SaleReturnService;
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

import java.util.List;

@RestController
@RequestMapping("/api")
public class SaleReturnController {

    private final SaleReturnService saleReturnService;

    public SaleReturnController(SaleReturnService saleReturnService) {
        this.saleReturnService = saleReturnService;
    }

    @GetMapping("/sale-returns")
    public ResponseEntity<ApiResponse<PageResponse<SaleReturnResponse>>> listReturns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(saleReturnService.listReturns(page, size)));
    }

    @GetMapping("/sale-returns/{id}")
    public ResponseEntity<ApiResponse<SaleReturnResponse>> getReturn(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(saleReturnService.getReturn(id)));
    }

    @GetMapping("/sales/{saleId}/returns")
    public ResponseEntity<ApiResponse<List<SaleReturnResponse>>> listReturnsForSale(@PathVariable Long saleId) {
        return ResponseEntity.ok(ApiResponse.ok(saleReturnService.listReturnsForSale(saleId)));
    }

    @PostMapping("/sales/{saleId}/returns")
    public ResponseEntity<ApiResponse<SaleReturnResponse>> createReturn(
            @PathVariable Long saleId,
            @Valid @RequestBody CreateSaleReturnRequest request) {
        SaleReturnResponse created = saleReturnService.createReturn(saleId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Return processed", created));
    }
}
