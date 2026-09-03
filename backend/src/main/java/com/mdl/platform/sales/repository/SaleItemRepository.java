package com.mdl.platform.sales.repository;

import com.mdl.platform.sales.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    Optional<SaleItem> findByIdAndBusinessId(Long id, Long businessId);

    List<SaleItem> findBySaleIdOrderByIdAsc(Long saleId);

    List<SaleItem> findBySaleIdIn(List<Long> saleIds);
}
