package com.mdl.platform.locations.controller;

import com.mdl.platform.common.dto.ApiResponse;
import com.mdl.platform.locations.dto.CreateTransferRouteRequest;
import com.mdl.platform.locations.dto.TransferRouteResponse;
import com.mdl.platform.locations.dto.UpdateTransferRouteRequest;
import com.mdl.platform.locations.service.LocationManagementService;
import com.mdl.platform.locations.service.LocationQueryService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transfer-routes")
public class TransferRouteController {

    private final LocationQueryService locationQueryService;
    private final LocationManagementService locationManagementService;

    public TransferRouteController(
            LocationQueryService locationQueryService,
            LocationManagementService locationManagementService) {
        this.locationQueryService = locationQueryService;
        this.locationManagementService = locationManagementService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransferRouteResponse>>> listRoutes() {
        return ResponseEntity.ok(ApiResponse.ok(locationQueryService.listTransferRoutes()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransferRouteResponse>> createRoute(
            @Valid @RequestBody CreateTransferRouteRequest request) {
        TransferRouteResponse created = locationQueryService.createTransferRoute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Transfer route created", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransferRouteResponse>> updateRoute(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTransferRouteRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Transfer route updated",
                locationQueryService.updateTransferRoute(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoute(@PathVariable Long id) {
        locationManagementService.deleteTransferRoute(id);
        return ResponseEntity.ok(ApiResponse.ok("Transfer route deleted", null));
    }
}
