package com.mdl.platform.sales.repository;

import com.mdl.platform.sales.entity.SaleReturnRefund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleReturnRefundRepository extends JpaRepository<SaleReturnRefund, Long> {

    List<SaleReturnRefund> findBySaleReturnIdOrderByIdAsc(Long saleReturnId);

    List<SaleReturnRefund> findBySaleReturnIdIn(List<Long> saleReturnIds);
}
