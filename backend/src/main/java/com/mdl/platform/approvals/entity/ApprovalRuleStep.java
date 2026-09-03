package com.mdl.platform.approvals.entity;

import com.mdl.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "approval_rule_steps")
public class ApprovalRuleStep extends BaseEntity {

    @Column(name = "approval_rule_id", nullable = false)
    private Long approvalRuleId;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "required_permission", nullable = false, length = 100)
    private String requiredPermission;

    public Long getApprovalRuleId() {
        return approvalRuleId;
    }

    public void setApprovalRuleId(Long approvalRuleId) {
        this.approvalRuleId = approvalRuleId;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(int stepOrder) {
        this.stepOrder = stepOrder;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }

    public void setRequiredPermission(String requiredPermission) {
        this.requiredPermission = requiredPermission;
    }
}
