package com.mdl.platform.reports.repository;

import com.mdl.platform.sales.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;

public interface SalesReportRepository extends JpaRepository<Sale, Long> {

    @Query("""
            SELECT COUNT(s) FROM Sale s
            WHERE s.businessId = :businessId
              AND s.status = :status
              AND (:shopId IS NULL OR s.shopId = :shopId)
              AND (:from IS NULL OR s.createdAt >= :from)
              AND (:to IS NULL OR s.createdAt <= :to)
            """)
    long countByStatus(
            @Param("businessId") Long businessId,
            @Param("status") String status,
            @Param("shopId") Long shopId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
            SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s
            WHERE s.businessId = :businessId
              AND s.status = :status
              AND (:shopId IS NULL OR s.shopId = :shopId)
              AND (:from IS NULL OR s.createdAt >= :from)
              AND (:to IS NULL OR s.createdAt <= :to)
            """)
    BigDecimal sumTotalAmountByStatus(
            @Param("businessId") Long businessId,
            @Param("status") String status,
            @Param("shopId") Long shopId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
            SELECT COALESCE(SUM(si.quantity), 0)
            FROM SaleItem si
            JOIN Sale s ON s.id = si.saleId
            WHERE s.businessId = :businessId
              AND s.status = 'COMPLETED'
              AND (:shopId IS NULL OR s.shopId = :shopId)
              AND (:from IS NULL OR s.createdAt >= :from)
              AND (:to IS NULL OR s.createdAt <= :to)
            """)
    BigDecimal sumCompletedItemsSold(
            @Param("businessId") Long businessId,
            @Param("shopId") Long shopId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
