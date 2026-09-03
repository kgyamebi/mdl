package com.mdl.platform.authorization.controller;

import com.mdl.platform.authorization.dto.GrantTemporaryPermissionRequest;
import com.mdl.platform.authorization.dto.RevokeTemporaryPermissionRequest;
import com.mdl.platform.authorization.dto.TemporaryPermissionResponse;
import com.mdl.platform.authorization.service.TemporaryPermissionService;
import com.mdl.platform.common.dto.ApiResponse;
import com.mdl.platform.common.dto.PageResponse;
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
@RequestMapping("/api/security/temporary-permissions")
public class TemporaryPermissionController {

    private final TemporaryPermissionService temporaryPermissionService;

    public TemporaryPermissionController(TemporaryPermissionService temporaryPermissionService) {
        this.temporaryPermissionService = temporaryPermissionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TemporaryPermissionResponse>>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(temporaryPermissionService.list(userId, status, page, size)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TemporaryPermissionResponse>> grant(
            @Valid @RequestBody GrantTemporaryPermissionRequest request) {
        TemporaryPermissionResponse created = temporaryPermissionService.grant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Temporary permission granted", created));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<ApiResponse<TemporaryPermissionResponse>> revoke(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) RevokeTemporaryPermissionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Temporary permission revoked", temporaryPermissionService.revoke(id, request)));
    }
}
