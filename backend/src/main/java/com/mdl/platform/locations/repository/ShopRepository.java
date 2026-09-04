package com.mdl.platform.locations.repository;

import com.mdl.platform.locations.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    List<Shop> findByBusinessIdAndStatusOrderByNameAsc(Long businessId, String status);

    Optional<Shop> findByIdAndBusinessId(Long id, Long businessId);

    List<Shop> findByBusinessIdAndLocationIdInAndStatus(Long businessId, List<Long> locationIds, String status);

    boolean existsByBusinessIdAndCode(Long businessId, String code);
}
