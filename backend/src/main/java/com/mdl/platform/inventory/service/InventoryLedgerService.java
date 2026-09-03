package com.mdl.platform.inventory.service;

import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.authorization.LocationAccessService;
import com.mdl.platform.common.exception.ConflictException;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.inventory.dto.CreateDamageReportRequest;
import com.mdl.platform.inventory.dto.CreateInventoryAdjustmentRequest;
import com.mdl.platform.inventory.dto.InventoryTransactionResponse;
import com.mdl.platform.inventory.entity.InventoryBalance;
import com.mdl.platform.inventory.entity.InventoryTransaction;
import com.mdl.platform.inventory.repository.InventoryBalanceRepository;
import com.mdl.platform.inventory.repository.InventoryTransactionRepository;
import com.mdl.platform.locations.entity.Location;
import com.mdl.platform.locations.repository.LocationRepository;
import com.mdl.platform.notifications.service.OperationalNotificationService;
import com.mdl.platform.products.entity.Product;
import com.mdl.platform.products.repository.ProductRepository;
import com.mdl.platform.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Single entry point for inventory quantity changes — always writes a ledger row.
 */
@Service
public class InventoryLedgerService {

    private final AuthorizationService authorizationService;
    private final LocationAccessService locationAccessService;
    private final InventoryBalanceRepository balanceRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final OperationalNotificationService operationalNotificationService;

    public InventoryLedgerService(
            AuthorizationService authorizationService,
            LocationAccessService locationAccessService,
            InventoryBalanceRepository balanceRepository,
            InventoryTransactionRepository transactionRepository,
            ProductRepository productRepository,
            LocationRepository locationRepository,
            OperationalNotificationService operationalNotificationService) {
        this.authorizationService = authorizationService;
        this.locationAccessService = locationAccessService;
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
        this.operationalNotificationService = operationalNotificationService;
    }

    @Transactional
    public InventoryTransactionResponse postAdjustment(CreateInventoryAdjustmentRequest request) {
        authorizationService.requirePermission("inventory:adjust");
        UserContext context = authorizationService.requireAuthenticated();

        Location location = locationAccessService.requireAccessibleLocation(context, request.locationId());
        Product product = requireTrackableProduct(context.businessId(), request.productId());

        LedgerMovementResult result = applyOnHandChange(
                context,
                location,
                product,
                request.quantityChange(),
                "ADJUSTMENT",
                "ADJUSTMENT",
                null,
                trimToNull(request.notes()));

        return toResponse(result.transaction(), location, product);
    }

    @Transactional
    public InventoryTransactionResponse reportDamage(CreateDamageReportRequest request) {
        authorizationService.requirePermission("damage:report");
        UserContext context = authorizationService.requireAuthenticated();

        Location location = locationAccessService.requireAccessibleLocation(context, request.locationId());
        Product product = requireTrackableProduct(context.businessId(), request.productId());

        BigDecimal quantityChange = request.quantity().negate();
        LedgerMovementResult result = applyOnHandChange(
                context,
                location,
                product,
                quantityChange,
                "DAMAGE",
                "DAMAGE",
                null,
                trimToNull(request.reason()));

        return toResponse(result.transaction(), location, product);
    }

    /**
     * Core ledger write — all on-hand changes must go through here.
     */
    @Transactional
    public LedgerMovementResult applyOnHandChange(
            UserContext context,
            Location location,
            Product product,
            BigDecimal quantityChange,
            String transactionType,
            String referenceType,
            Long referenceId,
            String notes) {

        if (quantityChange.compareTo(BigDecimal.ZERO) == 0) {
            throw new ConflictException("Quantity change cannot be zero");
        }

        InventoryBalance balance = balanceRepository
                .findForUpdate(context.businessId(), location.getId(), product.getId())
                .orElseGet(() -> createEmptyBalance(context.businessId(), location.getId(), product.getId()));

        BigDecimal available = balance.getQuantityOnHand().subtract(balance.getQuantityReserved());
        BigDecimal newQuantity = balance.getQuantityOnHand().add(quantityChange);

        if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new ConflictException(formatInsufficientStockMessage(
                    product, location, available, quantityChange.abs()));
        }
        if (quantityChange.compareTo(BigDecimal.ZERO) < 0 && available.add(quantityChange).compareTo(BigDecimal.ZERO) < 0) {
            throw new ConflictException(formatInsufficientAvailableStockMessage(
                    product, location, available, balance.getQuantityReserved(), quantityChange.abs()));
        }
        if (newQuantity.compareTo(balance.getQuantityReserved()) < 0) {
            throw new ConflictException("Movement would reduce stock below reserved quantity");
        }

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setBusinessId(context.businessId());
        transaction.setLocationId(location.getId());
        transaction.setProductId(product.getId());
        transaction.setTransactionType(transactionType);
        transaction.setQuantityChange(quantityChange);
        transaction.setQuantityAfter(newQuantity);
        transaction.setReferenceType(referenceType);
        transaction.setReferenceId(referenceId);
        transaction.setNotes(notes);
        transaction.setPerformedBy(context.userId());
        transaction.setTransactionAt(Instant.now());
        transaction = transactionRepository.save(transaction);

        balance.setQuantityOnHand(newQuantity);
        balance.setLastTransactionId(transaction.getId());
        balance = balanceRepository.save(balance);

        operationalNotificationService.notifyInventoryMovement(
                context, location, product, quantityChange, transaction);

        return new LedgerMovementResult(transaction, balance);
    }

    private InventoryBalance createEmptyBalance(Long businessId, Long locationId, Long productId) {
        InventoryBalance balance = new InventoryBalance();
        balance.setBusinessId(businessId);
        balance.setLocationId(locationId);
        balance.setProductId(productId);
        balance.setQuantityOnHand(BigDecimal.ZERO);
        balance.setQuantityReserved(BigDecimal.ZERO);
        return balanceRepository.save(balance);
    }

    public Product requireTrackableProduct(Long businessId, Long productId) {
        Product product = productRepository.findByIdAndBusinessId(productId, businessId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        if (!product.isTrackInventory()) {
            throw new ConflictException("Product is not tracked in inventory");
        }
        if (!"ACTIVE".equals(product.getStatus())) {
            throw new ConflictException("Product is not active");
        }
        return product;
    }

    private InventoryTransactionResponse toResponse(
            InventoryTransaction transaction, Location location, Product product) {
        return new InventoryTransactionResponse(
                transaction.getId(),
                location.getId(),
                location.getCode(),
                location.getName(),
                product.getId(),
                product.getSku(),
                product.getName(),
                transaction.getTransactionType(),
                transaction.getQuantityChange(),
                transaction.getQuantityAfter(),
                transaction.getReferenceType(),
                transaction.getReferenceId(),
                transaction.getNotes(),
                transaction.getPerformedBy(),
                transaction.getTransactionAt());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String formatInsufficientStockMessage(
            Product product, Location location, BigDecimal available, BigDecimal requested) {
        return "Insufficient stock for %s at %s: %s %s available, %s %s requested"
                .formatted(
                        product.getSku(),
                        location.getName(),
                        formatQuantity(available),
                        product.getUnitOfMeasure(),
                        formatQuantity(requested),
                        product.getUnitOfMeasure());
    }

    private String formatInsufficientAvailableStockMessage(
            Product product,
            Location location,
            BigDecimal available,
            BigDecimal reserved,
            BigDecimal requested) {
        return "Insufficient available stock for %s at %s: %s %s available (%s reserved), %s %s requested"
                .formatted(
                        product.getSku(),
                        location.getName(),
                        formatQuantity(available),
                        product.getUnitOfMeasure(),
                        formatQuantity(reserved),
                        formatQuantity(requested),
                        product.getUnitOfMeasure());
    }

    private String formatQuantity(BigDecimal quantity) {
        return quantity.stripTrailingZeros().toPlainString();
    }

    Map<Long, Location> loadLocations(Long businessId, Iterable<Long> locationIds) {
        return locationRepository.findAllById(
                        java.util.stream.StreamSupport.stream(locationIds.spliterator(), false).toList())
                .stream()
                .filter(location -> location.getBusinessId().equals(businessId))
                .collect(Collectors.toMap(Location::getId, Function.identity()));
    }

    Map<Long, Product> loadProducts(Long businessId, Iterable<Long> productIds) {
        return productRepository.findAllById(
                        java.util.stream.StreamSupport.stream(productIds.spliterator(), false).toList())
                .stream()
                .filter(product -> product.getBusinessId().equals(businessId))
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }
}
