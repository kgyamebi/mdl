package com.mdl.platform.approvals.dto;

public record ApprovalStepActionResult(
        boolean workflowComplete,
        boolean rejected,
        int currentStepOrder,
        int totalSteps
) {
}
