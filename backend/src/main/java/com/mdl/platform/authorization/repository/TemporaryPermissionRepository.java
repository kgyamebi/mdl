package com.mdl.platform.authorization.repository;

import com.mdl.platform.authorization.entity.TemporaryPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TemporaryPermissionRepository extends JpaRepository<TemporaryPermission, Long> {

    Optional<TemporaryPermission> findByIdAndBusinessId(Long id, Long businessId);

    @Query("""
            SELECT tp FROM TemporaryPermission tp
            WHERE tp.businessId = :businessId
              AND tp.userId = :userId
              AND tp.status = 'ACTIVE'
              AND tp.revokedAt IS NULL
              AND tp.expiresAt > :now
            """)
    List<TemporaryPermission> findActiveForUser(
            @Param("businessId") Long businessId,
            @Param("userId") Long userId,
            @Param("now") Instant now);

    @Query("""
            SELECT tp FROM TemporaryPermission tp
            WHERE tp.businessId = :businessId
              AND tp.userId = :userId
              AND tp.locationId = :locationId
              AND tp.permissionCode = :permissionCode
              AND tp.status = 'ACTIVE'
              AND tp.revokedAt IS NULL
              AND tp.expiresAt > :now
              AND (:referenceType IS NULL OR tp.referenceType = :referenceType)
              AND (:referenceId IS NULL OR tp.referenceId = :referenceId)
            """)
    List<TemporaryPermission> findActiveGrants(
            @Param("businessId") Long businessId,
            @Param("userId") Long userId,
            @Param("locationId") Long locationId,
            @Param("permissionCode") String permissionCode,
            @Param("referenceType") String referenceType,
            @Param("referenceId") Long referenceId,
            @Param("now") Instant now);

    List<TemporaryPermission> findByBusinessIdAndReferenceTypeAndReferenceIdAndStatus(
            Long businessId, String referenceType, Long referenceId, String status);

    @Query("""
            SELECT tp FROM TemporaryPermission tp
            WHERE tp.businessId = :businessId
              AND (:userId IS NULL OR tp.userId = :userId)
              AND (:status IS NULL OR tp.status = :status)
            ORDER BY tp.createdAt DESC
            """)
    Page<TemporaryPermission> search(
            @Param("businessId") Long businessId,
            @Param("userId") Long userId,
            @Param("status") String status,
            Pageable pageable);
}
