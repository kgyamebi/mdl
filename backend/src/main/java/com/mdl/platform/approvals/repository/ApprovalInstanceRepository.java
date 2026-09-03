package com.mdl.platform.approvals.repository;

import com.mdl.platform.approvals.entity.ApprovalInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApprovalInstanceRepository extends JpaRepository<ApprovalInstance, Long> {

    Optional<ApprovalInstance> findByIdAndBusinessId(Long id, Long businessId);

    Optional<ApprovalInstance> findByBusinessIdAndEntityTypeAndEntityId(
            Long businessId, String entityType, Long entityId);
}
