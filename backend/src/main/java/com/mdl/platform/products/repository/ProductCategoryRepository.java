package com.mdl.platform.products.repository;

import com.mdl.platform.products.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    List<ProductCategory> findByBusinessIdOrderBySortOrderAscNameAsc(Long businessId);

    List<ProductCategory> findByBusinessIdAndStatusOrderBySortOrderAscNameAsc(Long businessId, String status);

    Optional<ProductCategory> findByIdAndBusinessId(Long id, Long businessId);

    Optional<ProductCategory> findByBusinessIdAndCode(Long businessId, String code);

    boolean existsByBusinessIdAndCode(Long businessId, String code);
}
