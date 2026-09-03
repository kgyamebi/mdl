package com.mdl.platform.sales.repository;

import com.mdl.platform.sales.entity.SalePayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalePaymentRepository extends JpaRepository<SalePayment, Long> {

    List<SalePayment> findBySaleIdOrderByIdAsc(Long saleId);

    List<SalePayment> findBySaleIdIn(List<Long> saleIds);
}
