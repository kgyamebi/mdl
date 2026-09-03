package com.mdl.platform.approvals.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateApprovalRuleRequest(
        @Size(max = 100)
        String name,

        @Size(max = 500)
        String description,

        @Size(max = 100)
        String requiredPermission,

        BigDecimal minAbsQuantity,

        Boolean enabled,

        @PositiveOrZero
        Integer priority
) {
}
