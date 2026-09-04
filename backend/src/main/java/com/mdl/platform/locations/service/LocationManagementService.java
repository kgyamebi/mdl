package com.mdl.platform.locations.service;

import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.common.exception.ConflictException;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.locations.dto.CreateMainWarehouseRequest;
import com.mdl.platform.locations.dto.CreateShopRequest;
import com.mdl.platform.locations.dto.ShopResponse;
import com.mdl.platform.locations.dto.WarehouseResponse;
import com.mdl.platform.locations.entity.Location;
import com.mdl.platform.locations.entity.Shop;
import com.mdl.platform.locations.entity.Warehouse;
import com.mdl.platform.locations.repository.LocationRepository;
import com.mdl.platform.locations.repository.ShopRepository;
import com.mdl.platform.locations.repository.WarehouseRepository;
import com.mdl.platform.locations.repository.WarehouseTransferRouteRepository;
import com.mdl.platform.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class LocationManagementService {

    private static final Pattern NON_ALNUM = Pattern.compile("[^A-Z0-9]+");

    private final AuthorizationService authorizationService;
    private final LocationRepository locationRepository;
    private final WarehouseRepository warehouseRepository;
    private final ShopRepository shopRepository;
    private final WarehouseTransferRouteRepository transferRouteRepository;
    private final LocationQueryService locationQueryService;

    public LocationManagementService(
            AuthorizationService authorizationService,
            LocationRepository locationRepository,
            WarehouseRepository warehouseRepository,
            ShopRepository shopRepository,
            WarehouseTransferRouteRepository transferRouteRepository,
            LocationQueryService locationQueryService) {
        this.authorizationService = authorizationService;
        this.locationRepository = locationRepository;
        this.warehouseRepository = warehouseRepository;
        this.shopRepository = shopRepository;
        this.transferRouteRepository = transferRouteRepository;
        this.locationQueryService = locationQueryService;
    }

    @Transactional
    public ShopResponse createShop(CreateShopRequest request) {
        UserContext context = requireManage();

        String name = request.name().trim();
        String shopCode = resolveCode(request.code(), name, "SHOP");
        String shopLocCode = "LOC-" + shopCode;
        String whCode = "WH-" + shopCode;
        String whLocCode = "LOC-" + whCode;

        ensureUniqueCodes(context.businessId(), shopCode, shopLocCode, whCode, whLocCode);

        String city = defaultCity(request.city());
        String country = defaultCountry(request.country());

        Location shopLocation = saveLocation(context.businessId(), shopLocCode, name, "SHOP", city, country);
        Location warehouseLocation = saveLocation(
                context.businessId(), whLocCode, name + " Warehouse", "WAREHOUSE", city, country);

        Warehouse warehouse = new Warehouse();
        warehouse.setBusinessId(context.businessId());
        warehouse.setLocationId(warehouseLocation.getId());
        warehouse.setName(name + " Warehouse");
        warehouse.setCode(whCode);
        warehouse.setWarehouseType("SHOP");
        warehouse.setRestricted(false);
        warehouse.setDescription("Shop stock storage");
        warehouse.setStatus("ACTIVE");
        warehouse = warehouseRepository.save(warehouse);

        Shop shop = new Shop();
        shop.setBusinessId(context.businessId());
        shop.setLocationId(shopLocation.getId());
        shop.setWarehouseId(warehouse.getId());
        shop.setName(name);
        shop.setCode(shopCode);
        shop.setStatus("ACTIVE");
        shop = shopRepository.save(shop);

        return locationQueryService.getShop(shop.getId());
    }

    @Transactional
    public WarehouseResponse createMainWarehouse(CreateMainWarehouseRequest request) {
        UserContext context = requireManage();

        String name = request.name().trim();
        String warehouseType = normalizeWarehouseType(request.warehouseType());
        String prefix = "MAIN".equals(warehouseType) ? "WH-MAIN" : "WH-SHOP";
        String whCode = resolveCode(request.code(), name, prefix);
        String locCode = "LOC-" + whCode;

        ensureUniqueCodes(context.businessId(), whCode, locCode);

        String city = defaultCity(request.city());
        String country = defaultCountry(request.country());
        boolean restricted = "MAIN".equals(warehouseType)
                && (request.restricted() == null || request.restricted());

        Location location = saveLocation(context.businessId(), locCode, name, "WAREHOUSE", city, country);

        Warehouse warehouse = new Warehouse();
        warehouse.setBusinessId(context.businessId());
        warehouse.setLocationId(location.getId());
        warehouse.setName(name);
        warehouse.setCode(whCode);
        warehouse.setWarehouseType(warehouseType);
        warehouse.setRestricted(restricted);
        warehouse.setDescription(request.description());
        warehouse.setStatus("ACTIVE");
        warehouse = warehouseRepository.save(warehouse);

        return locationQueryService.getWarehouse(warehouse.getId());
    }

    private String normalizeWarehouseType(String warehouseType) {
        if (warehouseType == null || warehouseType.isBlank()) {
            return "MAIN";
        }
        String normalized = warehouseType.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("MAIN", "SHOP").contains(normalized)) {
            throw new IllegalArgumentException("Warehouse type must be MAIN or SHOP");
        }
        return normalized;
    }

    @Transactional
    public ShopResponse deactivateShop(Long shopId) {
        UserContext context = requireManage();
        Shop shop = shopRepository.findByIdAndBusinessId(shopId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Shop not found"));

        shop.setStatus("INACTIVE");
        shopRepository.save(shop);

        deactivateLocation(context.businessId(), shop.getLocationId());

        if (shop.getWarehouseId() != null) {
            warehouseRepository.findByIdAndBusinessId(shop.getWarehouseId(), context.businessId())
                    .ifPresent(warehouse -> {
                        warehouse.setStatus("INACTIVE");
                        warehouseRepository.save(warehouse);
                        deactivateLocation(context.businessId(), warehouse.getLocationId());
                    });
        }

        return locationQueryService.getShop(shopId);
    }

    @Transactional
    public WarehouseResponse deactivateWarehouse(Long warehouseId) {
        UserContext context = requireManage();
        Warehouse warehouse = warehouseRepository.findByIdAndBusinessId(warehouseId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Warehouse not found"));

        long linkedShops = shopRepository.findByBusinessIdAndStatusOrderByNameAsc(context.businessId(), "ACTIVE")
                .stream()
                .filter(shop -> warehouseId.equals(shop.getWarehouseId()))
                .count();
        if (linkedShops > 0) {
            throw new ConflictException("Cannot remove warehouse while an active shop is linked to it");
        }

        warehouse.setStatus("INACTIVE");
        warehouseRepository.save(warehouse);
        deactivateLocation(context.businessId(), warehouse.getLocationId());

        return locationQueryService.getWarehouse(warehouseId);
    }

    @Transactional
    public void deleteTransferRoute(Long routeId) {
        UserContext context = requireManage();
        var route = transferRouteRepository.findByIdAndBusinessId(routeId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Transfer route not found"));
        transferRouteRepository.delete(route);
    }

    private UserContext requireManage() {
        authorizationService.requirePermission("business:manage");
        return authorizationService.requireAuthenticated();
    }

    private Location saveLocation(
            Long businessId, String code, String name, String type, String city, String country) {
        Location location = new Location();
        location.setBusinessId(businessId);
        location.setCode(code);
        location.setName(name);
        location.setLocationType(type);
        location.setCity(city);
        location.setCountry(country);
        location.setStatus("ACTIVE");
        return locationRepository.save(location);
    }

    private void deactivateLocation(Long businessId, Long locationId) {
        locationRepository.findByIdAndBusinessId(locationId, businessId).ifPresent(location -> {
            location.setStatus("INACTIVE");
            locationRepository.save(location);
        });
    }

    private void ensureUniqueCodes(Long businessId, String... codes) {
        for (String code : codes) {
            if (locationRepository.existsByBusinessIdAndCode(businessId, code)
                    || warehouseRepository.existsByBusinessIdAndCode(businessId, code)
                    || shopRepository.existsByBusinessIdAndCode(businessId, code)) {
                throw new ConflictException("Location code already exists: " + code);
            }
        }
    }

    private String resolveCode(String requestedCode, String name, String prefix) {
        if (requestedCode != null && !requestedCode.isBlank()) {
            return requestedCode.trim().toUpperCase(Locale.ROOT);
        }
        String slug = NON_ALNUM.matcher(name.trim().toUpperCase(Locale.ROOT)).replaceAll("-");
        slug = slug.replaceAll("^-|-$", "");
        if (slug.isBlank()) {
            slug = "NEW";
        }
        slug = slug.substring(0, Math.min(slug.length(), 20));
        return prefix + "-" + slug;
    }

    private String defaultCity(String city) {
        return city == null || city.isBlank() ? "Accra" : city.trim();
    }

    private String defaultCountry(String country) {
        return country == null || country.isBlank() ? "Ghana" : country.trim();
    }
}
