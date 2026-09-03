package com.mdl.platform.inventory.repository;

import com.mdl.platform.inventory.entity.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    @Query("""
            SELECT t FROM InventoryTransaction t
            WHERE t.businessId = :businessId
              AND t.locationId IN :locationIds
              AND (:locationId IS NULL OR t.locationId = :locationId)
              AND (:productId IS NULL OR t.productId = :productId)
            ORDER BY t.transactionAt DESC, t.id DESC
            """)
    Page<InventoryTransaction> search(
            @Param("businessId") Long businessId,
            @Param("locationIds") List<Long> locationIds,
            @Param("locationId") Long locationId,
            @Param("productId") Long productId,
            Pageable pageable);
}
