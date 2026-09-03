package com.mdl.platform.transfers.repository;

import com.mdl.platform.transfers.entity.StockTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {

    Optional<StockTransfer> findByIdAndBusinessId(Long id, Long businessId);

    long countByBusinessIdAndTransferNumberStartingWith(Long businessId, String prefix);

    @Query("""
            SELECT t FROM StockTransfer t
            WHERE t.businessId = :businessId
              AND (:status IS NULL OR t.status = :status)
              AND (
                    t.fromLocationId IN :locationIds
                    OR t.toLocationId IN :locationIds
                    OR :viewAll = true
                  )
            ORDER BY t.createdAt DESC
            """)
    Page<StockTransfer> search(
            @Param("businessId") Long businessId,
            @Param("locationIds") java.util.List<Long> locationIds,
            @Param("viewAll") boolean viewAll,
            @Param("status") String status,
            Pageable pageable);
}
