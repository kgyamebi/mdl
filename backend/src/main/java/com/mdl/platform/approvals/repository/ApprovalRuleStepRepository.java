package com.mdl.platform.approvals.repository;

import com.mdl.platform.approvals.entity.ApprovalRuleStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface ApprovalRuleStepRepository extends JpaRepository<ApprovalRuleStep, Long> {

    List<ApprovalRuleStep> findByApprovalRuleIdOrderByStepOrderAsc(Long approvalRuleId);

    @Modifying
    void deleteByApprovalRuleId(Long approvalRuleId);

    long countByApprovalRuleId(Long approvalRuleId);
}
