package com.mdl.platform.authorization;

import com.mdl.platform.authorization.service.RestrictedWarehouseAccessService;
import com.mdl.platform.authorization.service.TemporaryPermissionService;
import com.mdl.platform.common.exception.ForbiddenException;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.locations.entity.Location;
import com.mdl.platform.locations.repository.LocationRepository;
import com.mdl.platform.locations.repository.UserLocationAssignmentRepository;
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
    private final UserLocationAssignmentRepository locationAssignmentRepository;
    private final RestrictedWarehouseAccessService restrictedWarehouseAccessService;
    private final TemporaryPermissionService temporaryPermissionService;

    public LocationAccessService(
            LocationRepository locationRepository,
            UserLocationAssignmentRepository locationAssignmentRepository,
            RestrictedWarehouseAccessService restrictedWarehouseAccessService,
            TemporaryPermissionService temporaryPermissionService) {
        this.locationRepository = locationRepository;
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

    public void requireLocationAccess(UserContext context, Long locationId) {
        requireAccessibleLocation(context, locationId);
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

        return getAssignedLocationIds(context).contains(locationId);
    }

    private Set<Long> getAssignedLocationIds(UserContext context) {
        Set<Long> ids = new HashSet<>();
        locationAssignmentRepository.findByUserIdAndBusinessId(context.userId(), context.businessId())
                .forEach(assignment -> ids.add(assignment.getLocationId()));
        return ids;
    }
}
