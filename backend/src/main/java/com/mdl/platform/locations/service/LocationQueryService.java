package com.mdl.platform.locations.service;

import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.authorization.LocationAccessService;
import com.mdl.platform.common.exception.ConflictException;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.locations.dto.BusinessStructureResponse;
import com.mdl.platform.locations.dto.CreateTransferRouteRequest;
import com.mdl.platform.locations.dto.LocationSummaryResponse;
import com.mdl.platform.locations.dto.ShopResponse;
import com.mdl.platform.locations.dto.TransferRouteResponse;
import com.mdl.platform.locations.dto.UpdateTransferRouteRequest;
import com.mdl.platform.locations.dto.WarehouseResponse;
import com.mdl.platform.locations.entity.Location;
import com.mdl.platform.locations.entity.Shop;
import com.mdl.platform.locations.entity.Warehouse;
import com.mdl.platform.locations.entity.WarehouseTransferRoute;
import com.mdl.platform.locations.repository.ShopRepository;
import com.mdl.platform.locations.repository.WarehouseRepository;
import com.mdl.platform.locations.repository.WarehouseTransferRouteRepository;
import com.mdl.platform.security.UserContext;
import com.mdl.platform.businesses.repository.BusinessRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LocationQueryService {

    private final AuthorizationService authorizationService;
    private final LocationAccessService locationAccessService;
    private final WarehouseRepository warehouseRepository;
    private final ShopRepository shopRepository;
    private final WarehouseTransferRouteRepository transferRouteRepository;
    private final BusinessRepository businessRepository;

    public LocationQueryService(
            AuthorizationService authorizationService,
            LocationAccessService locationAccessService,
            WarehouseRepository warehouseRepository,
            ShopRepository shopRepository,
            WarehouseTransferRouteRepository transferRouteRepository,
            BusinessRepository businessRepository) {
        this.authorizationService = authorizationService;
        this.locationAccessService = locationAccessService;
        this.warehouseRepository = warehouseRepository;
        this.shopRepository = shopRepository;
        this.transferRouteRepository = transferRouteRepository;
        this.businessRepository = businessRepository;
    }

    @Transactional(readOnly = true)
    public List<LocationSummaryResponse> listLocations() {
        authorizationService.requirePermission("inventory:view");
        UserContext context = authorizationService.requireAuthenticated();
        return locationAccessService.getAccessibleLocations(context).stream()
                .map(this::toLocationSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public LocationSummaryResponse getLocation(Long locationId) {
        authorizationService.requirePermission("inventory:view");
        UserContext context = authorizationService.requireAuthenticated();
        Location location = locationAccessService.requireAccessibleLocation(context, locationId);
        return toLocationSummary(location);
    }

    @Transactional(readOnly = true)
    public List<WarehouseResponse> listWarehouses(String warehouseType) {
        authorizationService.requirePermission("inventory:view");
        UserContext context = authorizationService.requireAuthenticated();

        List<Warehouse> warehouses = getAccessibleWarehouses(context, warehouseType);
        Map<Long, Location> locationMap = locationMap(context, warehouses);

        return warehouses.stream()
                .map(warehouse -> toWarehouseResponse(warehouse, locationMap.get(warehouse.getLocationId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouse(Long warehouseId) {
        authorizationService.requirePermission("inventory:view");
        UserContext context = authorizationService.requireAuthenticated();

        Warehouse warehouse = warehouseRepository.findByIdAndBusinessId(warehouseId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Warehouse not found"));

        locationAccessService.requireLocationAccess(context, warehouse.getLocationId());
        Location location = locationAccessService.requireAccessibleLocation(context, warehouse.getLocationId());
        return toWarehouseResponse(warehouse, location);
    }

    @Transactional(readOnly = true)
    public List<ShopResponse> listShops() {
        authorizationService.requirePermission("inventory:view");
        UserContext context = authorizationService.requireAuthenticated();

        List<Shop> shops = getAccessibleShops(context);
        Map<Long, Location> locationMap = shops.stream()
                .map(Shop::getLocationId)
                .distinct()
                .collect(Collectors.toMap(Function.identity(), id ->
                        locationAccessService.requireAccessibleLocation(context, id)));

        Map<Long, Warehouse> warehouseMap = warehouseRepository
                .findByBusinessIdAndStatusOrderByNameAsc(context.businessId(), "ACTIVE").stream()
                .collect(Collectors.toMap(Warehouse::getId, Function.identity()));

        return shops.stream()
                .map(shop -> toShopResponse(shop, locationMap.get(shop.getLocationId()), warehouseMap))
                .toList();
    }

    @Transactional(readOnly = true)
    public ShopResponse getShop(Long shopId) {
        authorizationService.requirePermission("inventory:view");
        UserContext context = authorizationService.requireAuthenticated();

        Shop shop = shopRepository.findByIdAndBusinessId(shopId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Shop not found"));

        locationAccessService.requireLocationAccess(context, shop.getLocationId());
        Location location = locationAccessService.requireAccessibleLocation(context, shop.getLocationId());

        Map<Long, Warehouse> warehouseMap = warehouseRepository
                .findByBusinessIdAndStatusOrderByNameAsc(context.businessId(), "ACTIVE").stream()
                .collect(Collectors.toMap(Warehouse::getId, Function.identity()));

        return toShopResponse(shop, location, warehouseMap);
    }

    @Transactional(readOnly = true)
    public BusinessStructureResponse getBusinessStructure() {
        authorizationService.requirePermission("business:view");
        UserContext context = authorizationService.requireAuthenticated();

        if (!locationAccessService.canViewAllLocations(context)) {
            authorizationService.requirePermission("inventory:view:all");
        }

        var business = businessRepository.findByIdWithCurrency(context.businessId())
                .orElseThrow(() -> new NotFoundException("Business not found"));

        List<WarehouseResponse> allWarehouses = listWarehouses(null);
        List<WarehouseResponse> mainWarehouses = allWarehouses.stream()
                .filter(w -> "MAIN".equals(w.warehouseType()))
                .toList();
        List<WarehouseResponse> shopWarehouses = allWarehouses.stream()
                .filter(w -> "SHOP".equals(w.warehouseType()))
                .toList();

        int routeCount = transferRouteRepository.findByBusinessIdOrderByCreatedAtDesc(context.businessId()).size();

        return new BusinessStructureResponse(
                new BusinessStructureResponse.BusinessOverview(
                        business.getCode(), business.getName(), business.getCurrencyCode()),
                mainWarehouses,
                shopWarehouses,
                listShops(),
                routeCount);
    }

    @Transactional(readOnly = true)
    public List<TransferRouteResponse> listTransferRoutes() {
        authorizationService.requirePermission("transfer:view");
        UserContext context = authorizationService.requireAuthenticated();
        return buildTransferRouteResponses(context);
    }

    @Transactional
    public TransferRouteResponse createTransferRoute(CreateTransferRouteRequest request) {
        authorizationService.requirePermission("business:manage");
        UserContext context = authorizationService.requireAuthenticated();

        if (request.fromWarehouseId().equals(request.toWarehouseId())) {
            throw new ConflictException("Source and destination warehouse must be different");
        }

        Warehouse from = requireWarehouse(context, request.fromWarehouseId());
        Warehouse to = requireWarehouse(context, request.toWarehouseId());

        if (transferRouteRepository.existsByBusinessIdAndFromWarehouseIdAndToWarehouseId(
                context.businessId(), from.getId(), to.getId())) {
            throw new ConflictException("Transfer route already exists");
        }

        WarehouseTransferRoute route = new WarehouseTransferRoute();
        route.setBusinessId(context.businessId());
        route.setFromWarehouseId(from.getId());
        route.setToWarehouseId(to.getId());
        route.setNotes(request.notes());
        route.setEnabled(true);
        transferRouteRepository.save(route);

        return buildTransferRouteResponses(context).stream()
                .filter(r -> r.fromWarehouseId().equals(from.getId()) && r.toWarehouseId().equals(to.getId()))
                .findFirst()
                .orElseThrow();
    }

    @Transactional
    public TransferRouteResponse updateTransferRoute(Long routeId, UpdateTransferRouteRequest request) {
        authorizationService.requirePermission("business:manage");
        UserContext context = authorizationService.requireAuthenticated();

        WarehouseTransferRoute route = transferRouteRepository.findByIdAndBusinessId(routeId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Transfer route not found"));

        route.setEnabled(request.enabled());
        if (request.notes() != null) {
            route.setNotes(request.notes());
        }
        transferRouteRepository.save(route);

        return buildTransferRouteResponses(context).stream()
                .filter(r -> r.id().equals(routeId))
                .findFirst()
                .orElseThrow();
    }

    private List<Warehouse> getAccessibleWarehouses(UserContext context, String warehouseType) {
        List<Location> locations = locationAccessService.getAccessibleLocations(context);
        List<Long> locationIds = locations.stream().map(Location::getId).toList();

        if (locationIds.isEmpty()) {
            return List.of();
        }

        List<Warehouse> warehouses = warehouseRepository.findByBusinessIdAndLocationIdInAndStatus(
                context.businessId(), locationIds, "ACTIVE");

        if (warehouseType != null && !warehouseType.isBlank()) {
            return warehouses.stream()
                    .filter(w -> warehouseType.equalsIgnoreCase(w.getWarehouseType()))
                    .toList();
        }

        return warehouses;
    }

    private List<Shop> getAccessibleShops(UserContext context) {
        List<Long> locationIds = locationAccessService.getAccessibleLocations(context).stream()
                .map(Location::getId)
                .toList();

        if (locationIds.isEmpty()) {
            return List.of();
        }

        return shopRepository.findByBusinessIdAndLocationIdInAndStatus(
                context.businessId(), locationIds, "ACTIVE");
    }

    private Warehouse requireWarehouse(UserContext context, Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findByIdAndBusinessId(warehouseId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Warehouse not found"));
        locationAccessService.requireLocationAccess(context, warehouse.getLocationId());
        return warehouse;
    }

    private Map<Long, Location> locationMap(UserContext context, List<Warehouse> warehouses) {
        return warehouses.stream()
                .map(Warehouse::getLocationId)
                .distinct()
                .collect(Collectors.toMap(Function.identity(), id ->
                        locationAccessService.requireAccessibleLocation(context, id)));
    }

    private List<TransferRouteResponse> buildTransferRouteResponses(UserContext context) {
        Map<Long, Warehouse> warehouseMap = warehouseRepository
                .findByBusinessIdAndStatusOrderByNameAsc(context.businessId(), "ACTIVE").stream()
                .collect(Collectors.toMap(Warehouse::getId, Function.identity()));

        return transferRouteRepository.findByBusinessIdOrderByCreatedAtDesc(context.businessId()).stream()
                .map(route -> {
                    Warehouse from = warehouseMap.get(route.getFromWarehouseId());
                    Warehouse to = warehouseMap.get(route.getToWarehouseId());
                    return new TransferRouteResponse(
                            route.getId(),
                            route.getFromWarehouseId(),
                            from != null ? from.getCode() : null,
                            from != null ? from.getName() : null,
                            route.getToWarehouseId(),
                            to != null ? to.getCode() : null,
                            to != null ? to.getName() : null,
                            route.isEnabled(),
                            route.getNotes());
                })
                .toList();
    }

    private LocationSummaryResponse toLocationSummary(Location location) {
        return new LocationSummaryResponse(
                location.getId(),
                location.getCode(),
                location.getName(),
                location.getLocationType(),
                location.getCity(),
                location.getCountry(),
                location.getStatus());
    }

    private WarehouseResponse toWarehouseResponse(Warehouse warehouse, Location location) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getWarehouseType(),
                warehouse.isRestricted(),
                warehouse.getDescription(),
                warehouse.getStatus(),
                location != null ? toLocationSummary(location) : null);
    }

    private ShopResponse toShopResponse(Shop shop, Location location, Map<Long, Warehouse> warehouseMap) {
        Warehouse warehouse = shop.getWarehouseId() != null ? warehouseMap.get(shop.getWarehouseId()) : null;
        return new ShopResponse(
                shop.getId(),
                shop.getCode(),
                shop.getName(),
                shop.getStatus(),
                toLocationSummary(location),
                shop.getWarehouseId(),
                warehouse != null ? warehouse.getCode() : null,
                warehouse != null ? warehouse.getName() : null,
                warehouse != null ? warehouse.getLocationId() : null);
    }
}
