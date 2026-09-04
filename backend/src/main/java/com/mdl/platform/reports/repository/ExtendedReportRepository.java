package com.mdl.platform.reports.repository;

import com.mdl.platform.sales.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ExtendedReportRepository extends JpaRepository<Sale, Long> {

    @Query("""
            SELECT si.productId, p.sku, p.name,
                   COALESCE(SUM(si.quantity), 0),
                   COALESCE(SUM(si.lineTotal), 0)
            FROM SaleItem si
            JOIN Sale s ON s.id = si.saleId
            JOIN Product p ON p.id = si.productId
            WHERE s.businessId = :businessId
              AND s.status = 'COMPLETED'
              AND (:scopedShopIds IS NULL OR s.shopId IN :scopedShopIds)
              AND (:shopId IS NULL OR s.shopId = :shopId)
              AND (:from IS NULL OR s.createdAt >= :from)
              AND (:to IS NULL OR s.createdAt <= :to)
            GROUP BY si.productId, p.sku, p.name
            ORDER BY SUM(si.lineTotal) DESC
            """)
    List<Object[]> salesByProduct(
            @Param("businessId") Long businessId,
            @Param("shopId") Long shopId,
            @Param("scopedShopIds") List<Long> scopedShopIds,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
            SELECT ib.locationId, l.code, l.name,
                   ib.productId, p.sku, p.name,
                   ib.quantityOnHand, p.costPrice, p.sellingPrice
            FROM InventoryBalance ib
            JOIN Product p ON p.id = ib.productId
            JOIN Location l ON l.id = ib.locationId
            WHERE ib.businessId = :businessId
              AND ib.locationId IN :locationIds
              AND ib.quantityOnHand > 0
            ORDER BY l.code ASC, p.name ASC
            """)
    List<Object[]> inventoryValuationRows(
            @Param("businessId") Long businessId,
            @Param("locationIds") List<Long> locationIds);

    @Query("""
            SELECT t.status, COUNT(t)
            FROM StockTransfer t
            WHERE t.businessId = :businessId
              AND (
                    t.fromLocationId IN :locationIds
                    OR t.toLocationId IN :locationIds
                    OR :viewAll = true
                  )
              AND (:from IS NULL OR t.createdAt >= :from)
              AND (:to IS NULL OR t.createdAt <= :to)
            GROUP BY t.status
            ORDER BY t.status ASC
            """)
    List<Object[]> transferStatusCounts(
            @Param("businessId") Long businessId,
            @Param("locationIds") List<Long> locationIds,
            @Param("viewAll") boolean viewAll,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
            SELECT COUNT(t)
            FROM StockTransfer t
            WHERE t.businessId = :businessId
              AND (
                    t.fromLocationId IN :locationIds
                    OR t.toLocationId IN :locationIds
                    OR :viewAll = true
                  )
              AND (:from IS NULL OR t.createdAt >= :from)
              AND (:to IS NULL OR t.createdAt <= :to)
            """)
    long countTransfers(
            @Param("businessId") Long businessId,
            @Param("locationIds") List<Long> locationIds,
            @Param("viewAll") boolean viewAll,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
