package com.mdl.platform.sales.repository;

import com.mdl.platform.sales.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    Optional<Sale> findByIdAndBusinessId(Long id, Long businessId);

    long countByBusinessIdAndSaleNumberStartingWith(Long businessId, String prefix);

    @Query("""
            SELECT s FROM Sale s
            WHERE s.businessId = :businessId
              AND (:status IS NULL OR s.status = :status)
              AND (
                    s.shopLocationId IN :locationIds
                    OR s.warehouseLocationId IN :locationIds
                    OR :viewAll = true
                  )
            ORDER BY s.createdAt DESC
            """)
    Page<Sale> search(
            @Param("businessId") Long businessId,
            @Param("locationIds") java.util.List<Long> locationIds,
            @Param("viewAll") boolean viewAll,
            @Param("status") String status,
            Pageable pageable);
}
