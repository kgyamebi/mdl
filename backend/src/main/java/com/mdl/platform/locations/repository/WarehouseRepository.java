package com.mdl.platform.locations.repository;

import com.mdl.platform.locations.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    List<Warehouse> findByBusinessIdAndStatusOrderByNameAsc(Long businessId, String status);

    List<Warehouse> findByBusinessIdAndWarehouseTypeAndStatusOrderByNameAsc(
            Long businessId, String warehouseType, String status);

    Optional<Warehouse> findByIdAndBusinessId(Long id, Long businessId);

    List<Warehouse> findByBusinessIdAndLocationIdInAndStatus(Long businessId, List<Long> locationIds, String status);

    Optional<Warehouse> findByBusinessIdAndLocationIdAndStatus(Long businessId, Long locationId, String status);

    @Query("""
            SELECT w.locationId FROM Warehouse w
            WHERE w.businessId = :businessId
              AND w.restricted = true
              AND w.status = 'ACTIVE'
            """)
    List<Long> findRestrictedLocationIds(@Param("businessId") Long businessId);

    @Query("""
            SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END
            FROM Warehouse w
            WHERE w.businessId = :businessId
              AND w.locationId = :locationId
              AND w.restricted = true
              AND w.status = 'ACTIVE'
            """)
    boolean isRestrictedLocation(@Param("businessId") Long businessId, @Param("locationId") Long locationId);

    boolean existsByBusinessIdAndCode(Long businessId, String code);
}
