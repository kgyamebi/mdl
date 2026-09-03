package com.mdl.platform.approvals.repository;

import com.mdl.platform.approvals.entity.ApprovalRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalRuleRepository extends JpaRepository<ApprovalRule, Long> {

    Optional<ApprovalRule> findByIdAndBusinessId(Long id, Long businessId);

    Optional<ApprovalRule> findByBusinessIdAndCode(Long businessId, String code);

    boolean existsByBusinessIdAndCode(Long businessId, String code);

    List<ApprovalRule> findByBusinessIdOrderByEntityTypeAscPriorityAsc(Long businessId);

    List<ApprovalRule> findByBusinessIdAndEnabledTrueOrderByPriorityAsc(Long businessId);

    List<ApprovalRule> findByBusinessIdAndEntityTypeAndEnabledTrueOrderByPriorityAsc(
            Long businessId, String entityType);
}
