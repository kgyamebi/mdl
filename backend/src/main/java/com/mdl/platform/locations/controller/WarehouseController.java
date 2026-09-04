package com.mdl.platform.locations.controller;

import com.mdl.platform.common.dto.ApiResponse;
import com.mdl.platform.locations.dto.CreateMainWarehouseRequest;
import com.mdl.platform.locations.dto.WarehouseResponse;
import com.mdl.platform.locations.service.LocationManagementService;
import com.mdl.platform.locations.service.LocationQueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {

    private final LocationQueryService locationQueryService;
    private final LocationManagementService locationManagementService;

    public WarehouseController(
            LocationQueryService locationQueryService,
            LocationManagementService locationManagementService) {
        this.locationQueryService = locationQueryService;
        this.locationManagementService = locationManagementService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WarehouseResponse>>> listWarehouses(
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponse.ok(locationQueryService.listWarehouses(type)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseResponse>> getWarehouse(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(locationQueryService.getWarehouse(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WarehouseResponse>> createMainWarehouse(
            @Valid @RequestBody CreateMainWarehouseRequest request) {
        WarehouseResponse created = locationManagementService.createMainWarehouse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Warehouse created", created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseResponse>> deactivateWarehouse(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Warehouse deactivated", locationManagementService.deactivateWarehouse(id)));
    }
}
