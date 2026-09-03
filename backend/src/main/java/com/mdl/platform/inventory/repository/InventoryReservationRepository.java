package com.mdl.platform.inventory.repository;

import com.mdl.platform.inventory.entity.InventoryReservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    Optional<InventoryReservation> findByIdAndBusinessId(Long id, Long businessId);

    @Query("""
            SELECT r FROM InventoryReservation r
            WHERE r.businessId = :businessId
              AND r.locationId IN :locationIds
              AND (:status IS NULL OR r.status = :status)
            ORDER BY r.createdAt DESC
            """)
    Page<InventoryReservation> search(
            @Param("businessId") Long businessId,
            @Param("locationIds") java.util.List<Long> locationIds,
            @Param("status") String status,
            Pageable pageable);

    long countByBusinessIdAndStatusAndLocationIdIn(Long businessId, String status, java.util.List<Long> locationIds);

    java.util.List<InventoryReservation> findByBusinessIdAndReferenceTypeAndReferenceIdAndStatus(
            Long businessId, String referenceType, Long referenceId, String status);
}
