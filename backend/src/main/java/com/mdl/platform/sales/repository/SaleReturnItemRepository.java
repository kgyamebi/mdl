package com.mdl.platform.sales.repository;

import com.mdl.platform.sales.entity.SaleReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleReturnItemRepository extends JpaRepository<SaleReturnItem, Long> {

    List<SaleReturnItem> findBySaleReturnIdOrderByIdAsc(Long saleReturnId);

    List<SaleReturnItem> findBySaleReturnIdIn(List<Long> saleReturnIds);
}
