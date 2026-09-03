package com.mdl.platform.approvals.dto;

import java.time.Instant;
import java.util.List;

public record ApprovalInstanceResponse(
        Long id,
        String entityType,
        Long entityId,
        String status,
        int currentStepOrder,
        int totalSteps,
        String currentStepName,
        String currentStepPermission,
        List<String> currentStepPermissions,
        boolean parallelStep,
        Long approvalRuleId,
        String approvalRuleCode,
        Long submittedBy,
        Instant submittedAt,
        Instant completedAt,
        List<ApprovalInstanceActionResponse> actions
) {
    public record ApprovalInstanceActionResponse(
            int stepOrder,
            String action,
            Long actedBy,
            String notes,
            Instant actedAt
    ) {
    }
}
