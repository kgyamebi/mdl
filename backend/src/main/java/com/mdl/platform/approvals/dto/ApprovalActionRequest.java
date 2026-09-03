package com.mdl.platform.approvals.dto;

import jakarta.validation.constraints.Size;

public record ApprovalActionRequest(
        @Size(max = 500)
        String notes
) {
}
