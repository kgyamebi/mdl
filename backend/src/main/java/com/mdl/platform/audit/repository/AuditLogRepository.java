package com.mdl.platform.audit.repository;

import com.mdl.platform.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.businessId = :businessId
              AND (:userId IS NULL OR a.userId = :userId)
              AND (:module IS NULL OR a.module = :module)
              AND (:action IS NULL OR a.action = :action)
              AND (:entityType IS NULL OR a.entityType = :entityType)
              AND (:entityId IS NULL OR a.entityId = :entityId)
              AND (:from IS NULL OR a.createdAt >= :from)
              AND (:to IS NULL OR a.createdAt <= :to)
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> search(
            @Param("businessId") Long businessId,
            @Param("userId") Long userId,
            @Param("module") String module,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    long countByBusinessIdAndUserIdAndActionAndCreatedAtAfter(
            Long businessId, Long userId, String action, Instant createdAt);
}
