package com.mdl.platform.approvals.controller;

import com.mdl.platform.approvals.dto.ApprovalInboxResponse;
import com.mdl.platform.approvals.dto.ApprovalInstanceResponse;
import com.mdl.platform.approvals.dto.ApprovalRuleResponse;
import com.mdl.platform.approvals.dto.ApprovalRuleStepRequest;
import com.mdl.platform.approvals.dto.ApprovalRuleStepResponse;
import com.mdl.platform.approvals.dto.CreateApprovalRuleRequest;
import com.mdl.platform.approvals.dto.UpdateApprovalRuleRequest;
import com.mdl.platform.approvals.service.ApprovalInboxService;
import com.mdl.platform.approvals.service.ApprovalRuleService;
import com.mdl.platform.approvals.service.ApprovalWorkflowService;
import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.common.dto.ApiResponse;
import com.mdl.platform.security.UserContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    private final ApprovalRuleService approvalRuleService;
    private final ApprovalInboxService approvalInboxService;
    private final ApprovalWorkflowService approvalWorkflowService;
    private final AuthorizationService authorizationService;

    public ApprovalController(
            ApprovalRuleService approvalRuleService,
            ApprovalInboxService approvalInboxService,
            ApprovalWorkflowService approvalWorkflowService,
            AuthorizationService authorizationService) {
        this.approvalRuleService = approvalRuleService;
        this.approvalInboxService = approvalInboxService;
        this.approvalWorkflowService = approvalWorkflowService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/inbox")
    public ResponseEntity<ApiResponse<ApprovalInboxResponse>> inbox(
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(approvalInboxService.getInbox(entityType, page, size)));
    }

    @GetMapping("/rules")
    public ResponseEntity<ApiResponse<List<ApprovalRuleResponse>>> listRules() {
        return ResponseEntity.ok(ApiResponse.ok(approvalRuleService.listRules()));
    }

    @GetMapping("/rules/{id}")
    public ResponseEntity<ApiResponse<ApprovalRuleResponse>> getRule(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(approvalRuleService.getRule(id)));
    }

    @PostMapping("/rules")
    public ResponseEntity<ApiResponse<ApprovalRuleResponse>> createRule(
            @Valid @RequestBody CreateApprovalRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Approval rule created", approvalRuleService.createRule(request)));
    }

    @PatchMapping("/rules/{id}")
    public ResponseEntity<ApiResponse<ApprovalRuleResponse>> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody UpdateApprovalRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Approval rule updated", approvalRuleService.updateRule(id, request)));
    }

    @GetMapping("/rules/{id}/steps")
    public ResponseEntity<ApiResponse<List<ApprovalRuleStepResponse>>> listSteps(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(approvalRuleService.listSteps(id)));
    }

    @PutMapping("/rules/{id}/steps")
    public ResponseEntity<ApiResponse<ApprovalRuleResponse>> replaceSteps(
            @PathVariable Long id,
            @Valid @RequestBody List<ApprovalRuleStepRequest> steps) {
        return ResponseEntity.ok(ApiResponse.ok("Approval steps updated", approvalRuleService.replaceSteps(id, steps)));
    }

    @GetMapping("/instances/{entityType}/{entityId}")
    public ResponseEntity<ApiResponse<ApprovalInstanceResponse>> getInstance(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        UserContext context = authorizationService.requireAuthenticated();
        authorizationService.requireAnyPermission("approval:view", "approval:manage");
        return ResponseEntity.ok(ApiResponse.ok(
                approvalWorkflowService.getInstance(context, entityType, entityId)));
    }
}
