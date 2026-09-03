package com.mdl.platform.approvals.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateApprovalRuleRequest(
        @NotBlank @Size(max = 50)
        @Pattern(regexp = "[A-Z0-9][A-Z0-9_-]*", message = "Code must be uppercase letters, numbers, hyphens, or underscores")
        String code,

        @NotBlank @Size(max = 100)
        String name,

        @Size(max = 500)
        String description,

        @NotBlank
        String entityType,

        @NotBlank @Size(max = 100)
        String requiredPermission,

        BigDecimal minAbsQuantity,

        @NotNull
        Boolean enabled,

        @PositiveOrZero
        Integer priority
) {
}
