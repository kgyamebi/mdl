package com.mdl.platform.transfers.controller;

import com.mdl.platform.common.dto.ApiResponse;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.transfers.dto.CreateStockTransferRequest;
import com.mdl.platform.transfers.dto.ReceiveStockTransferRequest;
import com.mdl.platform.transfers.dto.RejectStockTransferRequest;
import com.mdl.platform.transfers.dto.TransferFormOptionsResponse;
import com.mdl.platform.transfers.dto.StockTransferResponse;
import com.mdl.platform.transfers.service.StockTransferService;
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
@RequestMapping("/api/stock-transfers")
public class StockTransferController {

    private final StockTransferService stockTransferService;

    public StockTransferController(StockTransferService stockTransferService) {
        this.stockTransferService = stockTransferService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StockTransferResponse>>> listTransfers(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(stockTransferService.listTransfers(status, page, size)));
    }

    @GetMapping("/form-options")
    public ResponseEntity<ApiResponse<TransferFormOptionsResponse>> getFormOptions() {
        return ResponseEntity.ok(ApiResponse.ok(stockTransferService.getFormOptions()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StockTransferResponse>> getTransfer(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(stockTransferService.getTransfer(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StockTransferResponse>> createTransfer(
            @Valid @RequestBody CreateStockTransferRequest request) {
        StockTransferResponse created = stockTransferService.createTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Stock transfer created", created));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<StockTransferResponse>> approveTransfer(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Transfer approved", stockTransferService.approveTransfer(id)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<StockTransferResponse>> rejectTransfer(
            @PathVariable Long id,
            @Valid @RequestBody RejectStockTransferRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Transfer rejected", stockTransferService.rejectTransfer(id, request)));
    }

    @PostMapping("/{id}/dispatch")
    public ResponseEntity<ApiResponse<StockTransferResponse>> dispatchTransfer(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Transfer dispatched", stockTransferService.dispatchTransfer(id)));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<ApiResponse<StockTransferResponse>> receiveTransfer(
            @PathVariable Long id,
            @Valid @RequestBody ReceiveStockTransferRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Transfer received", stockTransferService.receiveTransfer(id, request)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<StockTransferResponse>> cancelTransfer(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Transfer cancelled", stockTransferService.cancelTransfer(id)));
    }
}
