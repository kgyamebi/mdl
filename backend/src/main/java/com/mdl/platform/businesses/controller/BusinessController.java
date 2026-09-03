package com.mdl.platform.businesses.controller;

import com.mdl.platform.businesses.dto.BusinessResponse;
import com.mdl.platform.businesses.dto.CurrencyResponse;
import com.mdl.platform.businesses.dto.UpdateBusinessRequest;
import com.mdl.platform.businesses.service.BusinessService;
import com.mdl.platform.common.dto.ApiResponse;
import com.mdl.platform.locations.dto.BusinessStructureResponse;
import com.mdl.platform.locations.service.LocationQueryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/business")
public class BusinessController {

    private final BusinessService businessService;
    private final LocationQueryService locationQueryService;

    public BusinessController(BusinessService businessService, LocationQueryService locationQueryService) {
        this.businessService = businessService;
        this.locationQueryService = locationQueryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<BusinessResponse>> getCurrentBusiness() {
        return ResponseEntity.ok(ApiResponse.ok(businessService.getCurrentBusiness()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<BusinessResponse>> updateCurrentBusiness(
            @Valid @RequestBody UpdateBusinessRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Business updated", businessService.updateCurrentBusiness(request)));
    }

    @GetMapping("/currencies")
    public ResponseEntity<ApiResponse<List<CurrencyResponse>>> listCurrencies() {
        return ResponseEntity.ok(ApiResponse.ok(businessService.listSupportedCurrencies()));
    }

    @GetMapping("/structure")
    public ResponseEntity<ApiResponse<BusinessStructureResponse>> getStructure() {
        return ResponseEntity.ok(ApiResponse.ok(locationQueryService.getBusinessStructure()));
    }
}
