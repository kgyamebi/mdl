package com.mdl.platform.approvals.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ApprovalRuleStepRequest(
        @NotNull @Positive
        Integer stepOrder,

        @NotBlank @Size(max = 100)
        String name,

        @NotBlank @Size(max = 100)
        String requiredPermission
) {
}
