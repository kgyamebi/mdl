package com.mdl.platform.alerts.controller;

import com.mdl.platform.alerts.dto.AlertResponse;
import com.mdl.platform.alerts.dto.OwnerAttentionReport;
import com.mdl.platform.alerts.service.AlertService;
import com.mdl.platform.common.dto.ApiResponse;
import com.mdl.platform.common.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AlertResponse>>> listAlerts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                alertService.listAlerts(status, severity, alertType, module, page, size)));
    }

    @GetMapping("/attention")
    public ResponseEntity<ApiResponse<OwnerAttentionReport>> attentionDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(alertService.attentionDashboard()));
    }

    @PostMapping("/scan")
    public ResponseEntity<ApiResponse<Integer>> scanAlerts() {
        return ResponseEntity.ok(ApiResponse.ok("Alert scan completed", alertService.scanAlerts()));
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<ApiResponse<AlertResponse>> acknowledge(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Alert acknowledged", alertService.acknowledgeAlert(id)));
    }
}
