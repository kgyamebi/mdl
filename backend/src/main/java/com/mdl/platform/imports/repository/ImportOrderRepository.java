package com.mdl.platform.imports.repository;

import com.mdl.platform.imports.entity.ImportOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ImportOrderRepository extends JpaRepository<ImportOrder, Long> {

    Optional<ImportOrder> findByIdAndBusinessId(Long id, Long businessId);

    long countByBusinessIdAndImportNumberStartingWith(Long businessId, String prefix);

    @Query("""
            SELECT i FROM ImportOrder i
            WHERE i.businessId = :businessId
              AND (:status IS NULL OR i.status = :status)
              AND (
                    i.destinationLocationId IN :locationIds
                    OR i.assignedReceiverUserId = :userId
                    OR :viewAll = true
                  )
            ORDER BY i.createdAt DESC
            """)
    Page<ImportOrder> search(
            @Param("businessId") Long businessId,
            @Param("locationIds") java.util.List<Long> locationIds,
            @Param("userId") Long userId,
            @Param("viewAll") boolean viewAll,
            @Param("status") String status,
            Pageable pageable);

    long countByBusinessIdAndStatus(Long businessId, String status);
}
