package com.mdl.platform.locations.controller;

import com.mdl.platform.common.dto.ApiResponse;
import com.mdl.platform.locations.dto.LocationSummaryResponse;
import com.mdl.platform.locations.service.LocationQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationQueryService locationQueryService;

    public LocationController(LocationQueryService locationQueryService) {
        this.locationQueryService = locationQueryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LocationSummaryResponse>>> listLocations() {
        return ResponseEntity.ok(ApiResponse.ok(locationQueryService.listLocations()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LocationSummaryResponse>> getLocation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(locationQueryService.getLocation(id)));
    }
}
