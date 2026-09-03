package com.mdl.platform.locations.repository;

import com.mdl.platform.locations.entity.WarehouseTransferRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseTransferRouteRepository extends JpaRepository<WarehouseTransferRoute, Long> {

    List<WarehouseTransferRoute> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

    Optional<WarehouseTransferRoute> findByIdAndBusinessId(Long id, Long businessId);

    boolean existsByBusinessIdAndFromWarehouseIdAndToWarehouseId(
            Long businessId, Long fromWarehouseId, Long toWarehouseId);

    boolean existsByBusinessIdAndFromWarehouseIdAndToWarehouseIdAndEnabled(
            Long businessId, Long fromWarehouseId, Long toWarehouseId, boolean enabled);
}
