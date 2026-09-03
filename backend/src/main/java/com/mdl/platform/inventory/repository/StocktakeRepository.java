package com.mdl.platform.inventory.repository;

import com.mdl.platform.inventory.entity.Stocktake;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StocktakeRepository extends JpaRepository<Stocktake, Long> {

    Optional<Stocktake> findByIdAndBusinessId(Long id, Long businessId);

    long countByBusinessIdAndStocktakeNumberStartingWith(Long businessId, String prefix);

    @Query("""
            SELECT s FROM Stocktake s
            WHERE s.businessId = :businessId
              AND s.locationId IN :locationIds
              AND (:status IS NULL OR s.status = :status)
            ORDER BY s.createdAt DESC
            """)
    Page<Stocktake> search(
            @Param("businessId") Long businessId,
            @Param("locationIds") java.util.List<Long> locationIds,
            @Param("status") String status,
            Pageable pageable);
}
