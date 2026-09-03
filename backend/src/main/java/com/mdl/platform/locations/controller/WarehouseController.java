package com.mdl.platform.locations.controller;

import com.mdl.platform.common.dto.ApiResponse;
import com.mdl.platform.locations.dto.WarehouseResponse;
import com.mdl.platform.locations.service.LocationQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {

    private final LocationQueryService locationQueryService;

    public WarehouseController(LocationQueryService locationQueryService) {
        this.locationQueryService = locationQueryService;
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
}
