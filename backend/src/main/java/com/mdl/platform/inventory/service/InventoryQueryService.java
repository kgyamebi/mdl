package com.mdl.platform.inventory.service;

import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.authorization.LocationAccessService;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.inventory.dto.InventoryBalanceResponse;
import com.mdl.platform.inventory.dto.InventorySummaryResponse;
import com.mdl.platform.inventory.dto.InventoryTransactionResponse;
import com.mdl.platform.inventory.entity.InventoryBalance;
import com.mdl.platform.inventory.entity.InventoryTransaction;
import com.mdl.platform.inventory.repository.InventoryAdjustmentRequestRepository;
import com.mdl.platform.inventory.repository.InventoryBalanceRepository;
import com.mdl.platform.inventory.repository.InventoryReservationRepository;
import com.mdl.platform.inventory.repository.InventoryTransactionRepository;
import com.mdl.platform.locations.entity.Location;
import com.mdl.platform.products.entity.Product;
import com.mdl.platform.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class InventoryQueryService {

    private final AuthorizationService authorizationService;
    private final LocationAccessService locationAccessService;
    private final InventoryBalanceRepository balanceRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final InventoryAdjustmentRequestRepository adjustmentRequestRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryLedgerService ledgerService;

    public InventoryQueryService(
            AuthorizationService authorizationService,
            LocationAccessService locationAccessService,
            InventoryBalanceRepository balanceRepository,
            InventoryTransactionRepository transactionRepository,
            InventoryAdjustmentRequestRepository adjustmentRequestRepository,
            InventoryReservationRepository reservationRepository,
            InventoryLedgerService ledgerService) {
        this.authorizationService = authorizationService;
        this.locationAccessService = locationAccessService;
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
        this.adjustmentRequestRepository = adjustmentRequestRepository;
        this.reservationRepository = reservationRepository;
        this.ledgerService = ledgerService;
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryBalanceResponse> listBalances(
            Long locationId, Long productId, String search, boolean lowStockOnly, int page, int size) {
        authorizationService.requirePermission("inventory:view");
        UserContext context = authorizationService.requireAuthenticated();

        List<Long> accessibleLocationIds = resolveAccessibleLocationIds(context, locationId);

        Page<InventoryBalance> result = balanceRepository.search(
                context.businessId(),
                accessibleLocationIds,
                locationId,
                productId,
                normalizeSearch(search),
                lowStockOnly,
                PageRequest.of(Math.max(page, 0), Math.max(size, 1)));

        Map<Long, Location> locations = ledgerService.loadLocations(
                context.businessId(), result.map(InventoryBalance::getLocationId).toList());
        Map<Long, Product> products = ledgerService.loadProducts(
                context.businessId(), result.map(InventoryBalance::getProductId).toList());

        List<InventoryBalanceResponse> items = result.getContent().stream()
                .map(balance -> toBalanceResponse(balance, locations, products))
                .toList();

        return new PageResponse<>(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public InventorySummaryResponse getSummary() {
        authorizationService.requirePermission("inventory:view");
        UserContext context = authorizationService.requireAuthenticated();

        List<Long> locationIds = locationAccessService.getAccessibleLocations(context).stream()
                .map(Location::getId)
                .toList();

        if (locationIds.isEmpty()) {
            return new InventorySummaryResponse(0, 0, 0, 0);
        }

        long balanceRows = balanceRepository.countByBusinessIdAndLocationIdIn(context.businessId(), locationIds);
        long lowStock = balanceRepository.countLowStock(context.businessId(), locationIds);
        long pendingRequests = adjustmentRequestRepository.countByBusinessIdAndStatusAndLocationIdIn(
                context.businessId(), "PENDING", locationIds);
        long activeReservations = reservationRepository.countByBusinessIdAndStatusAndLocationIdIn(
                context.businessId(), "ACTIVE", locationIds);

        return new InventorySummaryResponse(balanceRows, lowStock, pendingRequests, activeReservations);
    }

    @Transactional(readOnly = true)
    public InventoryBalanceResponse getBalance(Long balanceId) {
        authorizationService.requirePermission("inventory:view");
        UserContext context = authorizationService.requireAuthenticated();

        InventoryBalance balance = balanceRepository.findByIdAndBusinessId(balanceId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Inventory balance not found"));

        locationAccessService.requireLocationAccess(context, balance.getLocationId());

        Map<Long, Location> locations = ledgerService.loadLocations(
                context.businessId(), List.of(balance.getLocationId()));
        Map<Long, Product> products = ledgerService.loadProducts(
                context.businessId(), List.of(balance.getProductId()));

        return toBalanceResponse(balance, locations, products);
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryTransactionResponse> listTransactions(
            Long locationId, Long productId, int page, int size) {
        authorizationService.requirePermission("inventory:view");
        UserContext context = authorizationService.requireAuthenticated();

        List<Long> accessibleLocationIds = resolveAccessibleLocationIds(context, locationId);

        Page<InventoryTransaction> result = transactionRepository.search(
                context.businessId(),
                accessibleLocationIds,
                locationId,
                productId,
                PageRequest.of(Math.max(page, 0), Math.max(size, 1)));

        Map<Long, Location> locations = ledgerService.loadLocations(
                context.businessId(), result.map(InventoryTransaction::getLocationId).toList());
        Map<Long, Product> products = ledgerService.loadProducts(
                context.businessId(), result.map(InventoryTransaction::getProductId).toList());

        List<InventoryTransactionResponse> items = result.getContent().stream()
                .map(transaction -> toTransactionResponse(transaction, locations, products))
                .toList();

        return new PageResponse<>(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private List<Long> resolveAccessibleLocationIds(UserContext context, Long requestedLocationId) {
        List<Location> accessible = locationAccessService.getAccessibleLocations(context);
        List<Long> ids = accessible.stream().map(Location::getId).toList();

        if (requestedLocationId != null) {
            locationAccessService.requireLocationAccess(context, requestedLocationId);
            if (!ids.contains(requestedLocationId)) {
                throw new NotFoundException("Location not found");
            }
            return List.of(requestedLocationId);
        }

        return ids;
    }

    private InventoryBalanceResponse toBalanceResponse(
            InventoryBalance balance, Map<Long, Location> locations, Map<Long, Product> products) {
        Location location = locations.get(balance.getLocationId());
        Product product = products.get(balance.getProductId());
        BigDecimal available = balance.getQuantityOnHand().subtract(balance.getQuantityReserved());
        Integer reorderLevel = product != null ? product.getReorderLevel() : null;
        boolean belowReorder = reorderLevel != null
                && balance.getQuantityOnHand().compareTo(BigDecimal.valueOf(reorderLevel)) <= 0;

        return new InventoryBalanceResponse(
                balance.getId(),
                balance.getLocationId(),
                location != null ? location.getCode() : null,
                location != null ? location.getName() : null,
                location != null ? location.getLocationType() : null,
                balance.getProductId(),
                product != null ? product.getSku() : null,
                product != null ? product.getName() : null,
                product != null ? product.getUnitOfMeasure() : null,
                balance.getQuantityOnHand(),
                balance.getQuantityReserved(),
                available,
                reorderLevel,
                belowReorder,
                balance.getUpdatedAt());
    }

    private InventoryTransactionResponse toTransactionResponse(
            InventoryTransaction transaction, Map<Long, Location> locations, Map<Long, Product> products) {
        Location location = locations.get(transaction.getLocationId());
        Product product = products.get(transaction.getProductId());

        return new InventoryTransactionResponse(
                transaction.getId(),
                transaction.getLocationId(),
                location != null ? location.getCode() : null,
                location != null ? location.getName() : null,
                product != null ? product.getId() : transaction.getProductId(),
                product != null ? product.getSku() : null,
                product != null ? product.getName() : null,
                transaction.getTransactionType(),
                transaction.getQuantityChange(),
                transaction.getQuantityAfter(),
                transaction.getReferenceType(),
                transaction.getReferenceId(),
                transaction.getNotes(),
                transaction.getPerformedBy(),
                transaction.getTransactionAt());
    }

    private String normalizeSearch(String search) {
        return search == null ? "" : search.trim();
    }
}
