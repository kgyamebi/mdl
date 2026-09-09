package com.mdl.platform.authorization;

import com.mdl.platform.authorization.service.RestrictedWarehouseAccessService;
import com.mdl.platform.authorization.service.TemporaryPermissionService;
import com.mdl.platform.common.exception.ForbiddenException;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.locations.entity.Location;
import com.mdl.platform.locations.entity.Shop;
import com.mdl.platform.locations.repository.LocationRepository;
import com.mdl.platform.locations.repository.ShopRepository;
import com.mdl.platform.locations.repository.UserLocationAssignmentRepository;
import com.mdl.platform.locations.repository.WarehouseRepository;
import com.mdl.platform.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves which locations a user may see based on role, permissions, assignments,
 * and temporary grants for restricted main warehouses.
 */
@Service
@Transactional(readOnly = true)
public class LocationAccessService {

    private final LocationRepository locationRepository;
    private final ShopRepository shopRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserLocationAssignmentRepository locationAssignmentRepository;
    private final RestrictedWarehouseAccessService restrictedWarehouseAccessService;
    private final TemporaryPermissionService temporaryPermissionService;

    public LocationAccessService(
            LocationRepository locationRepository,
            ShopRepository shopRepository,
            WarehouseRepository warehouseRepository,
            UserLocationAssignmentRepository locationAssignmentRepository,
            RestrictedWarehouseAccessService restrictedWarehouseAccessService,
            TemporaryPermissionService temporaryPermissionService) {
        this.locationRepository = locationRepository;
        this.shopRepository = shopRepository;
        this.warehouseRepository = warehouseRepository;
        this.locationAssignmentRepository = locationAssignmentRepository;
        this.restrictedWarehouseAccessService = restrictedWarehouseAccessService;
        this.temporaryPermissionService = temporaryPermissionService;
    }

    public boolean isOwner(UserContext context) {
        return context.roles().contains("OWNER");
    }

    public boolean canViewAllLocations(UserContext context) {
        return isOwner(context) || context.permissions().contains("inventory:view:all");
    }

    /**
     * Locations the user may look up (stock on hand). Shop floors are visible to
     * anyone with inventory:view so staff can check another shop for a customer.
     * Warehouses are included via {@link #getAccessibleLocations(UserContext)} so
     * staff can cover when a warehouse worker is away.
     * Selling at another shop still uses assigned shop access.
     */
    public List<Location> getViewableLocations(UserContext context) {
        if (canViewAllLocations(context)) {
            return getAccessibleLocations(context);
        }

        Set<Long> viewableIds = new HashSet<>();
        getAccessibleLocations(context).forEach(location -> viewableIds.add(location.getId()));

        if (context.permissions().contains("inventory:view")) {
            addActiveShopFloorLocationIds(context.businessId(), viewableIds);
        }

        if (viewableIds.isEmpty()) {
            return List.of();
        }

        return locationRepository.findByBusinessIdAndIdInAndStatus(
                context.businessId(), List.copyOf(viewableIds), "ACTIVE");
    }

    public List<Location> getAccessibleLocations(UserContext context) {
        if (isOwner(context)) {
            return locationRepository.findByBusinessIdAndStatusOrderByNameAsc(context.businessId(), "ACTIVE");
        }

        Set<Long> restrictedIds = restrictedWarehouseAccessService.getRestrictedLocationIds(context.businessId());
        Set<Long> tempGrantLocationIds = temporaryPermissionService.getActiveLocationIds(
                context.businessId(), context.userId());

        Set<Long> accessibleIds = new HashSet<>();

        if (context.permissions().contains("inventory:view:all")) {
            locationRepository.findByBusinessIdAndStatusOrderByNameAsc(context.businessId(), "ACTIVE").stream()
                    .map(Location::getId)
                    .filter(id -> !restrictedIds.contains(id) || tempGrantLocationIds.contains(id))
                    .forEach(accessibleIds::add);
        }

        for (Long assignedId : getAssignedLocationIds(context)) {
            if (!restrictedIds.contains(assignedId) || tempGrantLocationIds.contains(assignedId)) {
                accessibleIds.add(assignedId);
            }
        }

        accessibleIds.addAll(tempGrantLocationIds);

        if (accessibleIds.isEmpty() && context.roles().contains("SHOP_MANAGER")) {
            addShopStockLocationIds(context.businessId(), accessibleIds);
        }

        if (accessibleIds.isEmpty() && context.roles().contains("WAREHOUSE_MANAGER")) {
            addActiveWarehouseLocationIds(context.businessId(), accessibleIds, restrictedIds, tempGrantLocationIds);
        }

        if (context.permissions().contains("inventory:view")) {
            addActiveWarehouseLocationIds(context.businessId(), accessibleIds, restrictedIds, tempGrantLocationIds);
        }

        if (accessibleIds.isEmpty()) {
            return List.of();
        }

        return locationRepository.findByBusinessIdAndIdInAndStatus(
                context.businessId(), List.copyOf(accessibleIds), "ACTIVE");
    }

    public Location requireAccessibleLocation(UserContext context, Long locationId) {
        Location location = locationRepository.findByIdAndBusinessId(locationId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Location not found"));

        if (!canAccessLocation(context, locationId)) {
            throw new ForbiddenException("You do not have access to this location");
        }

        return location;
    }

    public Location requireViewableLocation(UserContext context, Long locationId) {
        Location location = locationRepository.findByIdAndBusinessId(locationId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Location not found"));

        if (!canViewLocation(context, locationId)) {
            throw new ForbiddenException("You do not have access to this location");
        }

        return location;
    }

    public void requireLocationAccess(UserContext context, Long locationId) {
        requireAccessibleLocation(context, locationId);
    }

    public Shop requireAccessibleShop(UserContext context, Long shopId) {
        Shop shop = shopRepository.findByIdAndBusinessId(shopId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Shop not found"));

        if (!isOwner(context) && !canAccessLocation(context, shop.getLocationId())) {
            throw new ForbiddenException("You do not have access to this shop");
        }

        return shop;
    }

    public List<Long> getAccessibleShopIds(UserContext context) {
        if (canViewAllLocations(context)) {
            return shopRepository.findByBusinessIdAndStatusOrderByNameAsc(context.businessId(), "ACTIVE").stream()
                    .map(Shop::getId)
                    .toList();
        }

        List<Long> locationIds = getAccessibleLocations(context).stream()
                .map(Location::getId)
                .toList();
        if (locationIds.isEmpty()) {
            return List.of();
        }

        return shopRepository.findByBusinessIdAndLocationIdInAndStatus(
                        context.businessId(), locationIds, "ACTIVE").stream()
                .map(Shop::getId)
                .toList();
    }

    /**
     * Operational stock recording at any active shop/warehouse in the business.
     */
    public Location requireBusinessStockLocation(UserContext context, Long locationId) {
        Location location = locationRepository.findByIdAndBusinessId(locationId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Location not found"));

        if (!"ACTIVE".equals(location.getStatus())) {
            throw new NotFoundException("Location not found");
        }
        if (!Set.of("WAREHOUSE", "SHOP").contains(location.getLocationType())) {
            throw new NotFoundException("Location is not a stock location");
        }
        return location;
    }

    public boolean canViewLocation(UserContext context, Long locationId) {
        if (canAccessLocation(context, locationId)) {
            return true;
        }
        if (!context.permissions().contains("inventory:view")) {
            return false;
        }

        Location location = locationRepository.findByIdAndBusinessId(locationId, context.businessId())
                .orElse(null);
        return location != null
                && "ACTIVE".equals(location.getStatus())
                && "SHOP".equals(location.getLocationType());
    }

    public boolean canAccessLocation(UserContext context, Long locationId) {
        if (isOwner(context)) {
            return true;
        }

        if (restrictedWarehouseAccessService.isRestrictedLocation(context.businessId(), locationId)) {
            return temporaryPermissionService.hasActiveLocationAccess(
                    context.businessId(), context.userId(), locationId);
        }

        if (context.permissions().contains("inventory:view:all")) {
            return true;
        }

        Set<Long> assignedIds = getAssignedLocationIds(context);
        if (assignedIds.contains(locationId)) {
            return true;
        }

        if (assignedIds.isEmpty() && context.roles().contains("SHOP_MANAGER")) {
            Set<Long> shopStockLocationIds = new HashSet<>();
            addShopStockLocationIds(context.businessId(), shopStockLocationIds);
            return shopStockLocationIds.contains(locationId);
        }

        if (assignedIds.isEmpty() && context.roles().contains("WAREHOUSE_MANAGER")) {
            Set<Long> warehouseLocationIds = new HashSet<>();
            Set<Long> restrictedIds = restrictedWarehouseAccessService.getRestrictedLocationIds(context.businessId());
            Set<Long> tempGrantLocationIds = temporaryPermissionService.getActiveLocationIds(
                    context.businessId(), context.userId());
            addActiveWarehouseLocationIds(context.businessId(), warehouseLocationIds, restrictedIds, tempGrantLocationIds);
            return warehouseLocationIds.contains(locationId);
        }

        if (context.permissions().contains("inventory:view") && isActiveWarehouseLocation(context.businessId(), locationId)) {
            return true;
        }

        return false;
    }

    private boolean isActiveWarehouseLocation(Long businessId, Long locationId) {
        return warehouseRepository.findByBusinessIdAndLocationIdAndStatus(businessId, locationId, "ACTIVE")
                .isPresent();
    }

    private void addActiveShopFloorLocationIds(Long businessId, Set<Long> target) {
        shopRepository.findByBusinessIdAndStatusOrderByNameAsc(businessId, "ACTIVE")
                .forEach(shop -> target.add(shop.getLocationId()));
    }

    private void addShopStockLocationIds(Long businessId, Set<Long> target) {
        List<Shop> shops = shopRepository.findByBusinessIdAndStatusOrderByNameAsc(businessId, "ACTIVE");
        for (Shop shop : shops) {
            target.add(shop.getLocationId());
            if (shop.getWarehouseId() != null) {
                warehouseRepository.findByIdAndBusinessId(shop.getWarehouseId(), businessId)
                        .ifPresent(warehouse -> target.add(warehouse.getLocationId()));
            }
        }
    }

    private void addActiveWarehouseLocationIds(
            Long businessId,
            Set<Long> target,
            Set<Long> restrictedIds,
            Set<Long> tempGrantLocationIds) {
        warehouseRepository.findByBusinessIdAndStatusOrderByNameAsc(businessId, "ACTIVE").forEach(warehouse -> {
            Long locationId = warehouse.getLocationId();
            if (!restrictedIds.contains(locationId) || tempGrantLocationIds.contains(locationId)) {
                target.add(locationId);
            }
        });
    }

    private Set<Long> getAssignedLocationIds(UserContext context) {
        Set<Long> ids = new HashSet<>();
        locationAssignmentRepository.findByUserIdAndBusinessId(context.userId(), context.businessId())
                .forEach(assignment -> ids.add(assignment.getLocationId()));
        return ids;
    }
}
