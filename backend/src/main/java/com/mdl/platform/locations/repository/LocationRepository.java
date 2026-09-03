package com.mdl.platform.locations.repository;

import com.mdl.platform.locations.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findByBusinessIdAndStatusOrderByNameAsc(Long businessId, String status);

    Optional<Location> findByIdAndBusinessId(Long id, Long businessId);

    List<Location> findByBusinessIdAndIdInAndStatus(Long businessId, List<Long> ids, String status);
}
