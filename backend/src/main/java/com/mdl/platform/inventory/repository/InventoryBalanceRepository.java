package com.mdl.platform.inventory.repository;

import com.mdl.platform.inventory.entity.InventoryBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, Long> {

    Optional<InventoryBalance> findByIdAndBusinessId(Long id, Long businessId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ib FROM InventoryBalance ib
            WHERE ib.businessId = :businessId
              AND ib.locationId = :locationId
              AND ib.productId = :productId
            """)
    Optional<InventoryBalance> findForUpdate(
            @Param("businessId") Long businessId,
            @Param("locationId") Long locationId,
            @Param("productId") Long productId);

    @Query("""
            SELECT ib FROM InventoryBalance ib
            JOIN Product p ON p.id = ib.productId
            WHERE ib.businessId = :businessId
              AND ib.locationId IN :locationIds
              AND (:locationId IS NULL OR ib.locationId = :locationId)
              AND (:productId IS NULL OR ib.productId = :productId)
              AND (
                    :search IS NULL OR :search = ''
                    OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
              AND (
                    :lowStockOnly = false
                    OR (p.reorderLevel IS NOT NULL AND ib.quantityOnHand <= p.reorderLevel)
                  )
            ORDER BY ib.locationId ASC, p.name ASC
            """)
    Page<InventoryBalance> search(
            @Param("businessId") Long businessId,
            @Param("locationIds") List<Long> locationIds,
            @Param("locationId") Long locationId,
            @Param("productId") Long productId,
            @Param("search") String search,
            @Param("lowStockOnly") boolean lowStockOnly,
            Pageable pageable);

    List<InventoryBalance> findByBusinessIdAndProductIdAndLocationIdIn(
            Long businessId, Long productId, List<Long> locationIds);

    @Query("""
            SELECT COUNT(ib) FROM InventoryBalance ib
            JOIN Product p ON p.id = ib.productId
            WHERE ib.businessId = :businessId
              AND ib.locationId IN :locationIds
              AND p.reorderLevel IS NOT NULL
              AND ib.quantityOnHand <= p.reorderLevel
            """)
    long countLowStock(@Param("businessId") Long businessId, @Param("locationIds") List<Long> locationIds);

    long countByBusinessIdAndLocationIdIn(Long businessId, List<Long> locationIds);
}
