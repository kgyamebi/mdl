package com.mdl.platform.health;

import com.mdl.platform.common.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public health check — used by frontend and deployment monitors.
 * Does not expose sensitive internal details.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DatabaseHealthService databaseHealthService;

    @Value("${app.name:MDL Platform}")
    private String appName;

    @Value("${app.version:0.1.0}")
    private String appVersion;

    public HealthController(DatabaseHealthService databaseHealthService) {
        this.databaseHealthService = databaseHealthService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "OK");
        status.put("application", appName);
        status.put("version", appVersion);
        status.put("timestamp", Instant.now().toString());
        status.put("database", databaseHealthService.isDatabaseUp() ? "UP" : "DOWN");
        return ApiResponse.ok(status);
    }
}
