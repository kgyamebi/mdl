package com.mdl.platform.locations.controller;

import com.mdl.platform.common.dto.ApiResponse;
import com.mdl.platform.locations.dto.ShopResponse;
import com.mdl.platform.locations.service.LocationQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shops")
public class ShopController {

    private final LocationQueryService locationQueryService;

    public ShopController(LocationQueryService locationQueryService) {
        this.locationQueryService = locationQueryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShopResponse>>> listShops() {
        return ResponseEntity.ok(ApiResponse.ok(locationQueryService.listShops()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShopResponse>> getShop(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(locationQueryService.getShop(id)));
    }
}
