package com.mdl.platform.inventory.controller;

import com.mdl.platform.common.dto.ApiResponse;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.inventory.dto.AdjustmentRequestResponse;
import com.mdl.platform.inventory.dto.CreateAdjustmentRequestRequest;
import com.mdl.platform.inventory.dto.CreateDamageReportRequest;
import com.mdl.platform.inventory.dto.CreateInventoryAdjustmentRequest;
import com.mdl.platform.inventory.dto.CreateReservationRequest;
import com.mdl.platform.inventory.dto.InventoryBalanceResponse;
import com.mdl.platform.inventory.dto.InventorySummaryResponse;
import com.mdl.platform.inventory.dto.InventoryTransactionResponse;
import com.mdl.platform.inventory.dto.RecordWarehouseStockRequest;
import com.mdl.platform.inventory.dto.ReservationResponse;
import com.mdl.platform.inventory.dto.ReviewAdjustmentRequestRequest;
import com.mdl.platform.inventory.dto.CancelStocktakeRequest;
import com.mdl.platform.inventory.dto.CreateStocktakeRequest;
import com.mdl.platform.inventory.dto.ReviewStocktakeRequest;
import com.mdl.platform.inventory.dto.StocktakeResponse;
import com.mdl.platform.inventory.dto.UpsertStocktakeLineRequest;
import com.mdl.platform.inventory.service.InventoryAdjustmentRequestService;
import com.mdl.platform.inventory.service.InventoryLedgerService;
import com.mdl.platform.inventory.service.InventoryQueryService;
import com.mdl.platform.inventory.service.InventoryReservationService;
import com.mdl.platform.inventory.service.StocktakeService;
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
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryQueryService queryService;
    private final InventoryLedgerService ledgerService;
    private final InventoryAdjustmentRequestService adjustmentRequestService;
    private final InventoryReservationService reservationService;
    private final StocktakeService stocktakeService;

    public InventoryController(
            InventoryQueryService queryService,
            InventoryLedgerService ledgerService,
            InventoryAdjustmentRequestService adjustmentRequestService,
            InventoryReservationService reservationService,
            StocktakeService stocktakeService) {
        this.queryService = queryService;
        this.ledgerService = ledgerService;
        this.adjustmentRequestService = adjustmentRequestService;
        this.reservationService = reservationService;
        this.stocktakeService = stocktakeService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<InventorySummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.ok(queryService.getSummary()));
    }

    @GetMapping("/balances")
    public ResponseEntity<ApiResponse<PageResponse<InventoryBalanceResponse>>> listBalances(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean lowStockOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                queryService.listBalances(locationId, productId, search, lowStockOnly, page, size)));
    }

    @GetMapping("/balances/{id}")
    public ResponseEntity<ApiResponse<InventoryBalanceResponse>> getBalance(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(queryService.getBalance(id)));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<PageResponse<InventoryTransactionResponse>>> listTransactions(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                queryService.listTransactions(locationId, productId, page, size)));
    }

    @PostMapping("/adjustments")
    public ResponseEntity<ApiResponse<InventoryTransactionResponse>> postAdjustment(
            @Valid @RequestBody CreateInventoryAdjustmentRequest request) {
        InventoryTransactionResponse created = ledgerService.postAdjustment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Adjustment posted", created));
    }

    @PostMapping("/warehouse-stock")
    public ResponseEntity<ApiResponse<InventoryTransactionResponse>> recordWarehouseStock(
            @Valid @RequestBody RecordWarehouseStockRequest request) {
        InventoryTransactionResponse created = ledgerService.recordWarehouseStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Stock updated", created));
    }

    @PostMapping("/damage-reports")
    public ResponseEntity<ApiResponse<InventoryTransactionResponse>> reportDamage(
            @Valid @RequestBody CreateDamageReportRequest request) {
        InventoryTransactionResponse created = ledgerService.reportDamage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Damage reported", created));
    }

    @PostMapping("/adjustment-requests")
    public ResponseEntity<ApiResponse<AdjustmentRequestResponse>> createAdjustmentRequest(
            @Valid @RequestBody CreateAdjustmentRequestRequest request) {
        AdjustmentRequestResponse created = adjustmentRequestService.createRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Adjustment request submitted", created));
    }

    @GetMapping("/adjustment-requests")
    public ResponseEntity<ApiResponse<PageResponse<AdjustmentRequestResponse>>> listAdjustmentRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(adjustmentRequestService.listRequests(status, page, size)));
    }

    @PostMapping("/adjustment-requests/{id}/approve")
    public ResponseEntity<ApiResponse<AdjustmentRequestResponse>> approveAdjustmentRequest(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ReviewAdjustmentRequestRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Adjustment approved",
                adjustmentRequestService.approveRequest(id, request)));
    }

    @PostMapping("/adjustment-requests/{id}/reject")
    public ResponseEntity<ApiResponse<AdjustmentRequestResponse>> rejectAdjustmentRequest(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ReviewAdjustmentRequestRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Adjustment rejected",
                adjustmentRequestService.rejectRequest(id, request)));
    }

    @PostMapping("/reservations")
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(
            @Valid @RequestBody CreateReservationRequest request) {
        ReservationResponse created = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Stock reserved", created));
    }

    @GetMapping("/reservations")
    public ResponseEntity<ApiResponse<PageResponse<ReservationResponse>>> listReservations(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(reservationService.listReservations(status, page, size)));
    }

    @PostMapping("/reservations/{id}/release")
    public ResponseEntity<ApiResponse<ReservationResponse>> releaseReservation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Reservation released", reservationService.releaseReservation(id)));
    }

    @PostMapping("/stocktakes")
    public ResponseEntity<ApiResponse<StocktakeResponse>> createStocktake(
            @Valid @RequestBody CreateStocktakeRequest request) {
        StocktakeResponse created = stocktakeService.createStocktake(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Stocktake started", created));
    }

    @GetMapping("/stocktakes")
    public ResponseEntity<ApiResponse<PageResponse<StocktakeResponse>>> listStocktakes(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(stocktakeService.listStocktakes(status, page, size)));
    }

    @GetMapping("/stocktakes/{id}")
    public ResponseEntity<ApiResponse<StocktakeResponse>> getStocktake(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(stocktakeService.getStocktake(id)));
    }

    @PostMapping("/stocktakes/{id}/lines")
    public ResponseEntity<ApiResponse<StocktakeResponse>> upsertStocktakeLine(
            @PathVariable Long id,
            @Valid @RequestBody UpsertStocktakeLineRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Count recorded", stocktakeService.upsertLine(id, request)));
    }

    @PostMapping("/stocktakes/{id}/submit")
    public ResponseEntity<ApiResponse<StocktakeResponse>> submitStocktake(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Stocktake submitted", stocktakeService.submitStocktake(id)));
    }

    @PostMapping("/stocktakes/{id}/approve")
    public ResponseEntity<ApiResponse<StocktakeResponse>> approveStocktake(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ReviewStocktakeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Stocktake approved", stocktakeService.approveStocktake(id, request)));
    }

    @PostMapping("/stocktakes/{id}/cancel")
    public ResponseEntity<ApiResponse<StocktakeResponse>> cancelStocktake(
            @PathVariable Long id,
            @Valid @RequestBody CancelStocktakeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Stocktake cancelled", stocktakeService.cancelStocktake(id, request)));
    }
}
