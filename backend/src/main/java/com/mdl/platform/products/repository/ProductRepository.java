package com.mdl.platform.products.repository;

import com.mdl.platform.products.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndBusinessId(Long id, Long businessId);

    Optional<Product> findByBusinessIdAndSku(Long businessId, String sku);

    boolean existsByBusinessIdAndSku(Long businessId, String sku);

    @Query("""
            SELECT p FROM Product p
            WHERE p.businessId = :businessId
              AND (:status IS NULL OR p.status = :status)
              AND (:categoryId IS NULL OR p.categoryId = :categoryId)
              AND (
                    :search IS NULL OR :search = ''
                    OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(COALESCE(p.brand, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
            ORDER BY p.name ASC
            """)
    Page<Product> search(
            @Param("businessId") Long businessId,
            @Param("status") String status,
            @Param("categoryId") Long categoryId,
            @Param("search") String search,
            Pageable pageable);
}
