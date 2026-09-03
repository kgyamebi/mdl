package com.mdl.platform.sales.repository;

import com.mdl.platform.sales.entity.SaleReturn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SaleReturnRepository extends JpaRepository<SaleReturn, Long> {

    Optional<SaleReturn> findByIdAndBusinessId(Long id, Long businessId);

    List<SaleReturn> findBySaleIdAndBusinessIdOrderByCreatedAtDesc(Long saleId, Long businessId);

    long countByBusinessIdAndReturnNumberStartingWith(Long businessId, String prefix);

    @Query("""
            SELECT r FROM SaleReturn r
            JOIN Sale s ON s.id = r.saleId
            WHERE r.businessId = :businessId
              AND (
                    s.shopLocationId IN :locationIds
                    OR s.warehouseLocationId IN :locationIds
                    OR :viewAll = true
                  )
            ORDER BY r.createdAt DESC
            """)
    Page<SaleReturn> search(
            @Param("businessId") Long businessId,
            @Param("locationIds") List<Long> locationIds,
            @Param("viewAll") boolean viewAll,
            Pageable pageable);
}
