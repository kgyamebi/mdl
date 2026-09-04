package com.mdl.platform.locations.service;

import com.mdl.platform.locations.entity.Warehouse;
import com.mdl.platform.locations.entity.WarehouseTransferRoute;
import com.mdl.platform.locations.repository.WarehouseRepository;
import com.mdl.platform.locations.repository.WarehouseTransferRouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Ensures authorized transfer routes exist for common stock flows:
 * main warehouse → shop stock, shop stock → main warehouse (returns), and main ↔ main.
 */
@Service
public class TransferRouteProvisioningService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseTransferRouteRepository transferRouteRepository;

    public TransferRouteProvisioningService(
            WarehouseRepository warehouseRepository,
            WarehouseTransferRouteRepository transferRouteRepository) {
        this.warehouseRepository = warehouseRepository;
        this.transferRouteRepository = transferRouteRepository;
    }

    @Transactional
    public void provisionRoutesForShopWarehouse(Long businessId, Long shopWarehouseId) {
        Warehouse shopWarehouse = warehouseRepository.findByIdAndBusinessId(shopWarehouseId, businessId)
                .orElseThrow(() -> new IllegalArgumentException("Shop warehouse not found"));

        if (!"SHOP".equalsIgnoreCase(shopWarehouse.getWarehouseType())) {
            return;
        }

        List<Warehouse> mainWarehouses = warehouseRepository
                .findByBusinessIdAndWarehouseTypeAndStatusOrderByNameAsc(businessId, "MAIN", "ACTIVE");

        for (Warehouse mainWarehouse : mainWarehouses) {
            ensureRoute(
                    businessId,
                    mainWarehouse.getId(),
                    shopWarehouse.getId(),
                    "Distribution: " + mainWarehouse.getName() + " → " + shopWarehouse.getName());
            ensureRoute(
                    businessId,
                    shopWarehouse.getId(),
                    mainWarehouse.getId(),
                    "Return: " + shopWarehouse.getName() + " → " + mainWarehouse.getName());
        }
    }

    @Transactional
    public void provisionRoutesForMainWarehouse(Long businessId, Long mainWarehouseId) {
        Warehouse mainWarehouse = warehouseRepository.findByIdAndBusinessId(mainWarehouseId, businessId)
                .orElseThrow(() -> new IllegalArgumentException("Main warehouse not found"));

        if (!"MAIN".equalsIgnoreCase(mainWarehouse.getWarehouseType())) {
            return;
        }

        List<Warehouse> shopWarehouses = warehouseRepository
                .findByBusinessIdAndWarehouseTypeAndStatusOrderByNameAsc(businessId, "SHOP", "ACTIVE");

        for (Warehouse shopWarehouse : shopWarehouses) {
            ensureRoute(
                    businessId,
                    mainWarehouse.getId(),
                    shopWarehouse.getId(),
                    "Distribution: " + mainWarehouse.getName() + " → " + shopWarehouse.getName());
            ensureRoute(
                    businessId,
                    shopWarehouse.getId(),
                    mainWarehouse.getId(),
                    "Return: " + shopWarehouse.getName() + " → " + mainWarehouse.getName());
        }
    }

    private void ensureRoute(Long businessId, Long fromWarehouseId, Long toWarehouseId, String notes) {
        if (fromWarehouseId.equals(toWarehouseId)) {
            return;
        }
        if (transferRouteRepository.existsByBusinessIdAndFromWarehouseIdAndToWarehouseId(
                businessId, fromWarehouseId, toWarehouseId)) {
            return;
        }

        WarehouseTransferRoute route = new WarehouseTransferRoute();
        route.setBusinessId(businessId);
        route.setFromWarehouseId(fromWarehouseId);
        route.setToWarehouseId(toWarehouseId);
        route.setNotes(notes);
        route.setEnabled(true);
        transferRouteRepository.save(route);
    }
}
