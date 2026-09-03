package com.mdl.platform.transfers.service;

import com.mdl.platform.approvals.service.ApprovalWorkflowService;
import com.mdl.platform.audit.service.AuditService;
import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.authorization.LocationAccessService;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.common.exception.ConflictException;
import com.mdl.platform.common.exception.ForbiddenException;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.inventory.service.InventoryLedgerService;
import com.mdl.platform.inventory.service.InventoryReservationService;
import com.mdl.platform.locations.entity.Location;
import com.mdl.platform.locations.entity.Warehouse;
import com.mdl.platform.locations.repository.LocationRepository;
import com.mdl.platform.locations.repository.WarehouseRepository;
import com.mdl.platform.locations.repository.WarehouseTransferRouteRepository;
import com.mdl.platform.products.entity.Product;
import com.mdl.platform.products.repository.ProductRepository;
import com.mdl.platform.security.UserContext;
import com.mdl.platform.transfers.dto.CreateStockTransferRequest;
import com.mdl.platform.transfers.dto.ReceiveStockTransferRequest;
import com.mdl.platform.transfers.dto.RejectStockTransferRequest;
import com.mdl.platform.transfers.dto.StockTransferItemResponse;
import com.mdl.platform.transfers.dto.StockTransferResponse;
import com.mdl.platform.transfers.entity.StockTransfer;
import com.mdl.platform.transfers.entity.StockTransferItem;
import com.mdl.platform.transfers.repository.StockTransferItemRepository;
import com.mdl.platform.transfers.repository.StockTransferRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StockTransferService {

    private static final Set<String> RECEIVABLE_STATUSES = Set.of("DISPATCHED", "PARTIALLY_RECEIVED");

    private final AuthorizationService authorizationService;
    private final LocationAccessService locationAccessService;
    private final StockTransferRepository transferRepository;
    private final StockTransferItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;
    private final WarehouseTransferRouteRepository routeRepository;
    private final ProductRepository productRepository;
    private final InventoryLedgerService ledgerService;
    private final InventoryReservationService reservationService;
    private final AuditService auditService;
    private final ApprovalWorkflowService approvalWorkflowService;

    public StockTransferService(
            AuthorizationService authorizationService,
            LocationAccessService locationAccessService,
            StockTransferRepository transferRepository,
            StockTransferItemRepository itemRepository,
            WarehouseRepository warehouseRepository,
            LocationRepository locationRepository,
            WarehouseTransferRouteRepository routeRepository,
            ProductRepository productRepository,
            InventoryLedgerService ledgerService,
            InventoryReservationService reservationService,
            AuditService auditService,
            ApprovalWorkflowService approvalWorkflowService) {
        this.authorizationService = authorizationService;
        this.locationAccessService = locationAccessService;
        this.transferRepository = transferRepository;
        this.itemRepository = itemRepository;
        this.warehouseRepository = warehouseRepository;
        this.locationRepository = locationRepository;
        this.routeRepository = routeRepository;
        this.productRepository = productRepository;
        this.ledgerService = ledgerService;
        this.reservationService = reservationService;
        this.auditService = auditService;
        this.approvalWorkflowService = approvalWorkflowService;
    }

    @Transactional
    public StockTransferResponse createTransfer(CreateStockTransferRequest request) {
        UserContext context = authorizationService.requireAuthenticated();
        boolean directCreate = context.permissions().contains("transfer:create");
        if (!directCreate && !context.permissions().contains("stock:request")) {
            throw new ForbiddenException("You do not have permission to create stock transfers");
        }

        Warehouse fromWarehouse = requireWarehouse(context.businessId(), request.fromWarehouseId());
        Warehouse toWarehouse = requireWarehouse(context.businessId(), request.toWarehouseId());
        requireEnabledRoute(context.businessId(), fromWarehouse.getId(), toWarehouse.getId());
        locationAccessService.requireLocationAccess(context, toWarehouse.getLocationId());

        validateUniqueProducts(request.items());
        List<Product> products = loadAndValidateProducts(context.businessId(), request.items());

        StockTransfer transfer = new StockTransfer();
        transfer.setBusinessId(context.businessId());
        transfer.setTransferNumber(generateTransferNumber(context.businessId()));
        transfer.setFromWarehouseId(fromWarehouse.getId());
        transfer.setToWarehouseId(toWarehouse.getId());
        transfer.setFromLocationId(fromWarehouse.getLocationId());
        transfer.setToLocationId(toWarehouse.getLocationId());
        transfer.setStatus(directCreate ? "APPROVED" : "REQUESTED");
        transfer.setNotes(trimToNull(request.notes()));
        transfer.setRequestedBy(context.userId());
        if (directCreate) {
            transfer.setApprovedBy(context.userId());
            transfer.setApprovedAt(Instant.now());
        }
        transfer = transferRepository.save(transfer);

        for (CreateStockTransferRequest.CreateStockTransferItemRequest itemRequest : request.items()) {
            StockTransferItem item = new StockTransferItem();
            item.setBusinessId(context.businessId());
            item.setTransferId(transfer.getId());
            item.setProductId(itemRequest.productId());
            item.setRequestedQuantity(itemRequest.quantity());
            item.setNotes(trimToNull(itemRequest.notes()));
            itemRepository.save(item);
        }

        if (directCreate) {
            reserveStockForTransfer(context, transfer);
        } else {
            BigDecimal totalQuantity = request.items().stream()
                    .map(CreateStockTransferRequest.CreateStockTransferItemRequest::quantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            approvalWorkflowService.startWorkflow(
                    context, "STOCK_TRANSFER", transfer.getId(), totalQuantity);
        }

        return toResponse(context, transfer, products);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockTransferResponse> listTransfers(String status, int page, int size) {
        authorizationService.requirePermission("transfer:view");
        UserContext context = authorizationService.requireAuthenticated();

        List<Long> locationIds = locationAccessService.getAccessibleLocations(context).stream()
                .map(Location::getId)
                .toList();
        boolean viewAll = locationAccessService.canViewAllLocations(context);

        Page<StockTransfer> result = transferRepository.search(
                context.businessId(),
                locationIds.isEmpty() ? List.of(-1L) : locationIds,
                viewAll,
                normalizeStatus(status),
                PageRequest.of(Math.max(page, 0), Math.max(size, 1)));

        return toPageResponse(context, result);
    }

    @Transactional(readOnly = true)
    public StockTransferResponse getTransfer(Long transferId) {
        authorizationService.requirePermission("transfer:view");
        UserContext context = authorizationService.requireAuthenticated();

        StockTransfer transfer = requireVisibleTransfer(context, transferId);
        return toResponse(context, transfer);
    }

    @Transactional
    public StockTransferResponse approveTransfer(Long transferId) {
        UserContext context = authorizationService.requireAuthenticated();

        StockTransfer transfer = requireTransfer(context, transferId);
        if (!"REQUESTED".equals(transfer.getStatus())) {
            throw new ConflictException("Transfer is not pending approval");
        }

        var workflowResult = approvalWorkflowService.approveCurrentStep(
                context, "STOCK_TRANSFER", transfer.getId(), null);
        if (!workflowResult.workflowComplete()) {
            return toResponse(context, transfer);
        }

        transfer.setStatus("APPROVED");
        transfer.setApprovedBy(context.userId());
        transfer.setApprovedAt(Instant.now());
        transferRepository.save(transfer);

        reserveStockForTransfer(context, transfer);
        auditService.record(context, new AuditService.AuditEvent(
                "TRANSFER_APPROVED",
                "TRANSFERS",
                "STOCK_TRANSFER",
                transfer.getId(),
                transfer.getTransferNumber(),
                "Approved transfer " + transfer.getTransferNumber(),
                null));
        return toResponse(context, transfer);
    }

    @Transactional
    public StockTransferResponse rejectTransfer(Long transferId, RejectStockTransferRequest request) {
        UserContext context = authorizationService.requireAuthenticated();

        StockTransfer transfer = requireTransfer(context, transferId);
        if (!"REQUESTED".equals(transfer.getStatus())) {
            throw new ConflictException("Transfer is not pending approval");
        }

        approvalWorkflowService.rejectCurrentStep(
                context, "STOCK_TRANSFER", transfer.getId(), request.reason());

        transfer.setStatus("REJECTED");
        transfer.setRejectedBy(context.userId());
        transfer.setRejectedAt(Instant.now());
        transfer.setRejectReason(request.reason().trim());
        transferRepository.save(transfer);
        return toResponse(context, transfer);
    }

    @Transactional
    public StockTransferResponse dispatchTransfer(Long transferId) {
        authorizationService.requirePermission("transfer:dispatch");
        UserContext context = authorizationService.requireAuthenticated();

        StockTransfer transfer = requireTransfer(context, transferId);
        if (!"APPROVED".equals(transfer.getStatus())) {
            throw new ConflictException("Transfer must be approved before dispatch");
        }

        Location sourceLocation = locationRepository.findByIdAndBusinessId(
                        transfer.getFromLocationId(), context.businessId())
                .orElseThrow(() -> new NotFoundException("Source location not found"));
        locationAccessService.requireLocationAccess(context, sourceLocation.getId());

        List<StockTransferItem> items = itemRepository.findByTransferIdOrderByIdAsc(transfer.getId());
        for (StockTransferItem item : items) {
            Product product = ledgerService.requireTrackableProduct(context.businessId(), item.getProductId());
            ledgerService.applyOnHandChange(
                    context,
                    sourceLocation,
                    product,
                    item.getRequestedQuantity().negate(),
                    "TRANSFER_OUT",
                    "TRANSFER",
                    transfer.getId(),
                    "Transfer " + transfer.getTransferNumber());

            item.setDispatchedQuantity(item.getRequestedQuantity());
            itemRepository.save(item);
        }

        reservationService.consumeReservationsForTransfer(context, transfer.getId());

        transfer.setStatus("DISPATCHED");
        transfer.setDispatchedBy(context.userId());
        transfer.setDispatchedAt(Instant.now());
        transferRepository.save(transfer);

        auditService.record(context, new AuditService.AuditEvent(
                "TRANSFER_DISPATCHED",
                "TRANSFERS",
                "STOCK_TRANSFER",
                transfer.getId(),
                transfer.getTransferNumber(),
                "Dispatched transfer " + transfer.getTransferNumber(),
                null));

        return toResponse(context, transfer);
    }

    @Transactional
    public StockTransferResponse receiveTransfer(Long transferId, ReceiveStockTransferRequest request) {
        authorizationService.requirePermission("transfer:receive");
        UserContext context = authorizationService.requireAuthenticated();

        StockTransfer transfer = requireTransfer(context, transferId);
        if (!RECEIVABLE_STATUSES.contains(transfer.getStatus())) {
            throw new ConflictException("Transfer is not ready for receiving");
        }

        Location destination = locationRepository.findByIdAndBusinessId(
                        transfer.getToLocationId(), context.businessId())
                .orElseThrow(() -> new NotFoundException("Destination location not found"));
        locationAccessService.requireLocationAccess(context, destination.getId());

        Map<Long, StockTransferItem> itemsById = itemRepository.findByTransferIdOrderByIdAsc(transfer.getId()).stream()
                .collect(Collectors.toMap(StockTransferItem::getId, Function.identity()));

        for (ReceiveStockTransferRequest.ReceiveStockTransferItemRequest line : request.items()) {
            StockTransferItem item = itemsById.get(line.itemId());
            if (item == null) {
                throw new NotFoundException("Transfer item not found: " + line.itemId());
            }

            BigDecimal remaining = item.getDispatchedQuantity().subtract(item.getReceivedQuantity());
            if (line.quantityReceived().compareTo(remaining) > 0) {
                throw new ConflictException("Received quantity exceeds remaining dispatched for item " + line.itemId());
            }

            Product product = ledgerService.requireTrackableProduct(context.businessId(), item.getProductId());
            ledgerService.applyOnHandChange(
                    context,
                    destination,
                    product,
                    line.quantityReceived(),
                    "TRANSFER_IN",
                    "TRANSFER",
                    transfer.getId(),
                    "Transfer " + transfer.getTransferNumber());

            item.setReceivedQuantity(item.getReceivedQuantity().add(line.quantityReceived()));
            if (line.notes() != null && !line.notes().isBlank()) {
                item.setNotes(line.notes().trim());
            }
            itemRepository.save(item);
        }

        refreshReceivingStatus(transfer);
        transferRepository.save(transfer);
        return toResponse(context, transfer);
    }

    @Transactional
    public StockTransferResponse cancelTransfer(Long transferId) {
        authorizationService.requireAnyPermission("transfer:create", "transfer:approve", "stock:request");
        UserContext context = authorizationService.requireAuthenticated();

        StockTransfer transfer = requireTransfer(context, transferId);
        if (!Set.of("REQUESTED", "APPROVED").contains(transfer.getStatus())) {
            throw new ConflictException("Transfer cannot be cancelled in status: " + transfer.getStatus());
        }

        if ("APPROVED".equals(transfer.getStatus())) {
            reservationService.releaseReservationsForTransfer(context, transfer.getId());
        }

        transfer.setStatus("CANCELLED");
        transfer.setCancelledBy(context.userId());
        transfer.setCancelledAt(Instant.now());
        transferRepository.save(transfer);
        return toResponse(context, transfer);
    }

    private void reserveStockForTransfer(UserContext context, StockTransfer transfer) {
        locationAccessService.requireLocationAccess(context, transfer.getFromLocationId());
        List<StockTransferItem> items = itemRepository.findByTransferIdOrderByIdAsc(transfer.getId());
        for (StockTransferItem item : items) {
            reservationService.reserveForTransfer(
                    context,
                    transfer.getFromLocationId(),
                    item.getProductId(),
                    item.getRequestedQuantity(),
                    transfer.getId(),
                    "Reserved for transfer " + transfer.getTransferNumber());
        }
    }

    private void refreshReceivingStatus(StockTransfer transfer) {
        List<StockTransferItem> items = itemRepository.findByTransferIdOrderByIdAsc(transfer.getId());
        boolean anyReceived = items.stream().anyMatch(i -> i.getReceivedQuantity().compareTo(BigDecimal.ZERO) > 0);
        boolean allReceived = items.stream().allMatch(i ->
                i.getReceivedQuantity().compareTo(i.getDispatchedQuantity()) >= 0);

        if (allReceived) {
            transfer.setStatus("RECEIVED");
        } else if (anyReceived) {
            transfer.setStatus("PARTIALLY_RECEIVED");
        }
    }

    private void requireEnabledRoute(Long businessId, Long fromWarehouseId, Long toWarehouseId) {
        if (fromWarehouseId.equals(toWarehouseId)) {
            throw new ConflictException("Source and destination warehouses must differ");
        }
        if (!routeRepository.existsByBusinessIdAndFromWarehouseIdAndToWarehouseIdAndEnabled(
                businessId, fromWarehouseId, toWarehouseId, true)) {
            throw new ConflictException("No authorized transfer route exists between these warehouses");
        }
    }

    private Warehouse requireWarehouse(Long businessId, Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findByIdAndBusinessId(warehouseId, businessId)
                .orElseThrow(() -> new NotFoundException("Warehouse not found"));
        if (!"ACTIVE".equals(warehouse.getStatus())) {
            throw new ConflictException("Warehouse is not active");
        }
        return warehouse;
    }

    private StockTransfer requireTransfer(UserContext context, Long transferId) {
        return transferRepository.findByIdAndBusinessId(transferId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Transfer not found"));
    }

    private StockTransfer requireVisibleTransfer(UserContext context, Long transferId) {
        StockTransfer transfer = requireTransfer(context, transferId);
        if (locationAccessService.canViewAllLocations(context)) {
            return transfer;
        }
        Set<Long> accessible = locationAccessService.getAccessibleLocations(context).stream()
                .map(Location::getId)
                .collect(Collectors.toSet());
        if (accessible.contains(transfer.getFromLocationId()) || accessible.contains(transfer.getToLocationId())) {
            return transfer;
        }
        throw new ForbiddenException("You do not have access to this transfer");
    }

    private String generateTransferNumber(Long businessId) {
        String prefix = "TRF-" + Year.now().getValue() + "-";
        long count = transferRepository.countByBusinessIdAndTransferNumberStartingWith(businessId, prefix);
        return prefix + String.format("%04d", count + 1);
    }

    private void validateUniqueProducts(List<CreateStockTransferRequest.CreateStockTransferItemRequest> items) {
        Set<Long> productIds = new HashSet<>();
        for (CreateStockTransferRequest.CreateStockTransferItemRequest item : items) {
            if (!productIds.add(item.productId())) {
                throw new ConflictException("Duplicate product in transfer items: " + item.productId());
            }
        }
    }

    private List<Product> loadAndValidateProducts(
            Long businessId, List<CreateStockTransferRequest.CreateStockTransferItemRequest> items) {
        return items.stream()
                .map(item -> ledgerService.requireTrackableProduct(businessId, item.productId()))
                .toList();
    }

    private PageResponse<StockTransferResponse> toPageResponse(UserContext context, Page<StockTransfer> page) {
        Map<Long, Warehouse> warehouses = loadWarehouses(context.businessId(), page.stream()
                .flatMap(t -> java.util.stream.Stream.of(t.getFromWarehouseId(), t.getToWarehouseId()))
                .distinct()
                .toList());
        Map<Long, Location> locations = loadLocations(context.businessId(), page.stream()
                .flatMap(t -> java.util.stream.Stream.of(t.getFromLocationId(), t.getToLocationId()))
                .distinct()
                .toList());
        Map<Long, List<StockTransferItem>> itemsByTransfer = itemRepository
                .findByTransferIdIn(page.map(StockTransfer::getId).toList()).stream()
                .collect(Collectors.groupingBy(StockTransferItem::getTransferId));
        Map<Long, Product> products = loadProducts(context.businessId(), itemsByTransfer.values().stream()
                .flatMap(List::stream)
                .map(StockTransferItem::getProductId)
                .distinct()
                .toList());

        List<StockTransferResponse> items = page.getContent().stream()
                .map(transfer -> toResponse(
                        transfer,
                        warehouses.get(transfer.getFromWarehouseId()),
                        warehouses.get(transfer.getToWarehouseId()),
                        locations.get(transfer.getFromLocationId()),
                        locations.get(transfer.getToLocationId()),
                        itemsByTransfer.getOrDefault(transfer.getId(), List.of()),
                        products))
                .toList();

        return new PageResponse<>(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private StockTransferResponse toResponse(UserContext context, StockTransfer transfer) {
        Warehouse fromWarehouse = warehouseRepository.findByIdAndBusinessId(
                transfer.getFromWarehouseId(), context.businessId()).orElse(null);
        Warehouse toWarehouse = warehouseRepository.findByIdAndBusinessId(
                transfer.getToWarehouseId(), context.businessId()).orElse(null);
        Location fromLocation = locationRepository.findByIdAndBusinessId(
                transfer.getFromLocationId(), context.businessId()).orElse(null);
        Location toLocation = locationRepository.findByIdAndBusinessId(
                transfer.getToLocationId(), context.businessId()).orElse(null);
        List<StockTransferItem> items = itemRepository.findByTransferIdOrderByIdAsc(transfer.getId());
        Map<Long, Product> products = loadProducts(context.businessId(),
                items.stream().map(StockTransferItem::getProductId).toList());
        return toResponse(transfer, fromWarehouse, toWarehouse, fromLocation, toLocation, items, products);
    }

    private StockTransferResponse toResponse(
            UserContext context, StockTransfer transfer, List<Product> products) {
        Warehouse fromWarehouse = warehouseRepository.findByIdAndBusinessId(
                transfer.getFromWarehouseId(), context.businessId()).orElse(null);
        Warehouse toWarehouse = warehouseRepository.findByIdAndBusinessId(
                transfer.getToWarehouseId(), context.businessId()).orElse(null);
        Location fromLocation = locationRepository.findByIdAndBusinessId(
                transfer.getFromLocationId(), context.businessId()).orElse(null);
        Location toLocation = locationRepository.findByIdAndBusinessId(
                transfer.getToLocationId(), context.businessId()).orElse(null);
        List<StockTransferItem> items = itemRepository.findByTransferIdOrderByIdAsc(transfer.getId());
        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        return toResponse(transfer, fromWarehouse, toWarehouse, fromLocation, toLocation, items, productMap);
    }

    private StockTransferResponse toResponse(
            StockTransfer transfer,
            Warehouse fromWarehouse,
            Warehouse toWarehouse,
            Location fromLocation,
            Location toLocation,
            List<StockTransferItem> items,
            Map<Long, Product> products) {
        List<StockTransferItemResponse> itemResponses = items.stream()
                .map(item -> toItemResponse(item, products.get(item.getProductId())))
                .toList();

        return new StockTransferResponse(
                transfer.getId(),
                transfer.getTransferNumber(),
                transfer.getFromWarehouseId(),
                fromWarehouse != null ? fromWarehouse.getCode() : null,
                fromWarehouse != null ? fromWarehouse.getName() : null,
                transfer.getToWarehouseId(),
                toWarehouse != null ? toWarehouse.getCode() : null,
                toWarehouse != null ? toWarehouse.getName() : null,
                transfer.getFromLocationId(),
                fromLocation != null ? fromLocation.getCode() : null,
                transfer.getToLocationId(),
                toLocation != null ? toLocation.getCode() : null,
                transfer.getStatus(),
                transfer.getNotes(),
                transfer.getRequestedBy(),
                transfer.getApprovedBy(),
                transfer.getApprovedAt(),
                transfer.getDispatchedBy(),
                transfer.getDispatchedAt(),
                transfer.getRejectedBy(),
                transfer.getRejectedAt(),
                transfer.getRejectReason(),
                itemResponses,
                transfer.getCreatedAt(),
                transfer.getUpdatedAt());
    }

    private StockTransferItemResponse toItemResponse(StockTransferItem item, Product product) {
        BigDecimal remaining = item.getDispatchedQuantity().subtract(item.getReceivedQuantity());
        return new StockTransferItemResponse(
                item.getId(),
                item.getProductId(),
                product != null ? product.getSku() : null,
                product != null ? product.getName() : null,
                product != null ? product.getUnitOfMeasure() : null,
                item.getRequestedQuantity(),
                item.getDispatchedQuantity(),
                item.getReceivedQuantity(),
                remaining.max(BigDecimal.ZERO),
                item.getNotes());
    }

    private Map<Long, Location> loadLocations(Long businessId, List<Long> locationIds) {
        if (locationIds.isEmpty()) {
            return Map.of();
        }
        return locationRepository.findAllById(locationIds).stream()
                .filter(location -> location.getBusinessId().equals(businessId))
                .collect(Collectors.toMap(Location::getId, Function.identity()));
    }

    private Map<Long, Warehouse> loadWarehouses(Long businessId, List<Long> warehouseIds) {
        if (warehouseIds.isEmpty()) {
            return Map.of();
        }
        return warehouseRepository.findAllById(warehouseIds).stream()
                .filter(warehouse -> warehouse.getBusinessId().equals(businessId))
                .collect(Collectors.toMap(Warehouse::getId, Function.identity()));
    }

    private Map<Long, Product> loadProducts(Long businessId, List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productRepository.findAllById(productIds).stream()
                .filter(product -> product.getBusinessId().equals(businessId))
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
