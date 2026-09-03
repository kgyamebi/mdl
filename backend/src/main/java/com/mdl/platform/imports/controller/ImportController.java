package com.mdl.platform.imports.controller;

import com.mdl.platform.common.dto.ApiResponse;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.imports.dto.AddImportEvidenceRequest;
import com.mdl.platform.imports.dto.CreateImportRequest;
import com.mdl.platform.imports.dto.ImportEvidenceResponse;
import com.mdl.platform.imports.dto.ImportOrderResponse;
import com.mdl.platform.imports.dto.ReceiveImportRequest;
import com.mdl.platform.imports.service.ImportService;
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

import java.util.List;

@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ImportOrderResponse>>> listImports(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(importService.listImports(status, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ImportOrderResponse>> getImport(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(importService.getImport(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ImportOrderResponse>> createImport(
            @Valid @RequestBody CreateImportRequest request) {
        ImportOrderResponse created = importService.createImport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Import created", created));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<ImportOrderResponse>> submitImport(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Import submitted", importService.submitImport(id)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ImportOrderResponse>> approveImport(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Import approved", importService.approveImport(id)));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<ApiResponse<ImportOrderResponse>> receiveImport(
            @PathVariable Long id,
            @Valid @RequestBody ReceiveImportRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Import received", importService.receiveImport(id, request)));
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<ImportOrderResponse>> verifyImport(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Import verified", importService.verifyImport(id)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ImportOrderResponse>> cancelImport(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Import cancelled", importService.cancelImport(id)));
    }

    @GetMapping("/{id}/evidence")
    public ResponseEntity<ApiResponse<List<ImportEvidenceResponse>>> listEvidence(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(importService.listEvidence(id)));
    }

    @PostMapping("/{id}/evidence")
    public ResponseEntity<ApiResponse<ImportEvidenceResponse>> addEvidence(
            @PathVariable Long id,
            @Valid @RequestBody AddImportEvidenceRequest request) {
        ImportEvidenceResponse created = importService.addEvidence(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Evidence added", created));
    }
}
