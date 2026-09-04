package com.mdl.platform.locations.controller;

import com.mdl.platform.common.dto.ApiResponse;
import com.mdl.platform.locations.dto.CreateShopRequest;
import com.mdl.platform.locations.dto.ShopResponse;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shops")
public class ShopController {

    private final LocationQueryService locationQueryService;
    private final LocationManagementService locationManagementService;

    public ShopController(
            LocationQueryService locationQueryService,
            LocationManagementService locationManagementService) {
        this.locationQueryService = locationQueryService;
        this.locationManagementService = locationManagementService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShopResponse>>> listShops() {
        return ResponseEntity.ok(ApiResponse.ok(locationQueryService.listShops()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShopResponse>> getShop(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(locationQueryService.getShop(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShopResponse>> createShop(@Valid @RequestBody CreateShopRequest request) {
        ShopResponse created = locationManagementService.createShop(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Shop created", created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ShopResponse>> deactivateShop(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Shop deactivated", locationManagementService.deactivateShop(id)));
    }
}
