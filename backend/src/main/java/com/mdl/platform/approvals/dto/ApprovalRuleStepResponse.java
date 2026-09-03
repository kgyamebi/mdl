package com.mdl.platform.approvals.dto;

public record ApprovalRuleStepResponse(
        Long id,
        int stepOrder,
        String name,
        String requiredPermission
) {
}
