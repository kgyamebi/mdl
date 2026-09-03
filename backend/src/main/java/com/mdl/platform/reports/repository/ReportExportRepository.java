package com.mdl.platform.reports.repository;

import com.mdl.platform.reports.entity.ReportExport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReportExportRepository extends JpaRepository<ReportExport, Long> {

    Optional<ReportExport> findByIdAndBusinessId(Long id, Long businessId);

    @Query("""
            SELECT e FROM ReportExport e
            WHERE e.businessId = :businessId
              AND (:reportType IS NULL OR e.reportType = :reportType)
            ORDER BY e.createdAt DESC
            """)
    Page<ReportExport> search(
            @Param("businessId") Long businessId,
            @Param("reportType") String reportType,
            Pageable pageable);
}
