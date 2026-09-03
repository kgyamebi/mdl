package com.mdl.platform.authorization.service;

import com.mdl.platform.locations.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class RestrictedWarehouseAccessService {

    private final WarehouseRepository warehouseRepository;

    public RestrictedWarehouseAccessService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    public boolean isRestrictedLocation(Long businessId, Long locationId) {
        return warehouseRepository.isRestrictedLocation(businessId, locationId);
    }

    public Set<Long> getRestrictedLocationIds(Long businessId) {
        return new HashSet<>(warehouseRepository.findRestrictedLocationIds(businessId));
    }
}
