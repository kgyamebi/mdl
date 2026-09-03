package com.mdl.platform.approvals.repository;

import com.mdl.platform.approvals.entity.ApprovalInstanceAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalInstanceActionRepository extends JpaRepository<ApprovalInstanceAction, Long> {

    List<ApprovalInstanceAction> findByApprovalInstanceIdOrderByActedAtAsc(Long approvalInstanceId);
}
