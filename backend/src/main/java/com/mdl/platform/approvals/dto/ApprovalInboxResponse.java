package com.mdl.platform.approvals.dto;

import com.mdl.platform.common.dto.PageResponse;

import java.time.Instant;
import java.util.List;

public record ApprovalInboxResponse(
        ApprovalInboxSummary summary,
        PageResponse<ApprovalInboxItem> items
) {
    public record ApprovalInboxSummary(
            long adjustmentCount,
            long transferCount,
            long importCount,
            long stocktakeCount,
            long totalCount
    ) {
    }

    public record ApprovalInboxItem(
            String entityType,
            Long entityId,
            String reference,
            String title,
            String summary,
            String status,
            String requiredPermission,
            List<String> requiredPermissions,
            int currentStepOrder,
            int totalSteps,
            String currentStepName,
            boolean parallelStep,
            boolean canAct,
            Instant submittedAt,
            Long submittedBy
    ) {
    }
}
