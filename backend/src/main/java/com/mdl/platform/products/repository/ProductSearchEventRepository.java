package com.mdl.platform.products.repository;

import com.mdl.platform.products.entity.ProductSearchEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSearchEventRepository extends JpaRepository<ProductSearchEvent, Long> {
}
