package com.mdl.platform.alerts.repository;

import com.mdl.platform.alerts.entity.BusinessAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BusinessAlertRepository extends JpaRepository<BusinessAlert, Long> {

    Optional<BusinessAlert> findByIdAndBusinessId(Long id, Long businessId);

    Optional<BusinessAlert> findByBusinessIdAndDedupeKeyAndStatusIn(
            Long businessId, String dedupeKey, List<String> statuses);

    @Query("""
            SELECT a FROM BusinessAlert a
            WHERE a.businessId = :businessId
              AND (:status IS NULL OR a.status = :status)
              AND (:severity IS NULL OR a.severity = :severity)
              AND (:alertType IS NULL OR a.alertType = :alertType)
              AND (:module IS NULL OR a.module = :module)
            ORDER BY
              CASE a.severity
                WHEN 'CRITICAL' THEN 0
                WHEN 'WARNING' THEN 1
                ELSE 2
              END ASC,
              a.createdAt DESC
            """)
    Page<BusinessAlert> search(
            @Param("businessId") Long businessId,
            @Param("status") String status,
            @Param("severity") String severity,
            @Param("alertType") String alertType,
            @Param("module") String module,
            Pageable pageable);

    long countByBusinessIdAndStatusIn(Long businessId, List<String> statuses);

    long countByBusinessIdAndStatusInAndSeverity(Long businessId, List<String> statuses, String severity);

    List<BusinessAlert> findTop5ByBusinessIdAndStatusInOrderByCreatedAtDesc(
            Long businessId, List<String> statuses);
}
