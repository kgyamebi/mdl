package com.mdl.platform.approvals.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ApprovalRuleResponse(
        Long id,
        String code,
        String name,
        String description,
        String entityType,
        String requiredPermission,
        BigDecimal minAbsQuantity,
        boolean enabled,
        int priority,
        List<ApprovalRuleStepResponse> steps,
        Instant createdAt,
        Instant updatedAt
) {
}
