package com.mdl.platform.imports.service;

import com.mdl.platform.approvals.service.ApprovalWorkflowService;
import com.mdl.platform.audit.service.AuditService;
import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.authorization.LocationAccessService;
import com.mdl.platform.authorization.service.TemporaryPermissionService;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.common.exception.ConflictException;
import com.mdl.platform.common.exception.ForbiddenException;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.imports.dto.AddImportEvidenceRequest;
import com.mdl.platform.imports.dto.CreateImportRequest;
import com.mdl.platform.imports.dto.ImportEvidenceResponse;
import com.mdl.platform.imports.dto.ImportItemResponse;
import com.mdl.platform.imports.dto.ImportOrderResponse;
import com.mdl.platform.imports.dto.ReceiveImportRequest;
import com.mdl.platform.imports.entity.ImportEvidence;
import com.mdl.platform.imports.entity.ImportItem;
import com.mdl.platform.imports.entity.ImportOrder;
import com.mdl.platform.imports.repository.ImportEvidenceRepository;
import com.mdl.platform.imports.repository.ImportItemRepository;
import com.mdl.platform.imports.repository.ImportOrderRepository;
import com.mdl.platform.inventory.service.InventoryLedgerService;
import com.mdl.platform.locations.entity.Location;
import com.mdl.platform.locations.entity.Warehouse;
import com.mdl.platform.locations.repository.LocationRepository;
import com.mdl.platform.locations.repository.WarehouseRepository;
import com.mdl.platform.products.entity.Product;
import com.mdl.platform.products.repository.ProductRepository;
import com.mdl.platform.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
public class ImportService {

    private static final Set<String> RECEIVABLE_STATUSES = Set.of("APPROVED", "RECEIVING", "PARTIALLY_RECEIVED");

    private final AuthorizationService authorizationService;
    private final LocationAccessService locationAccessService;
    private final ImportOrderRepository importOrderRepository;
    private final ImportItemRepository importItemRepository;
    private final ImportEvidenceRepository evidenceRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final InventoryLedgerService ledgerService;
    private final TemporaryPermissionService temporaryPermissionService;
    private final AuditService auditService;
    private final ApprovalWorkflowService approvalWorkflowService;

    public ImportService(
            AuthorizationService authorizationService,
            LocationAccessService locationAccessService,
            ImportOrderRepository importOrderRepository,
            ImportItemRepository importItemRepository,
            ImportEvidenceRepository evidenceRepository,
            WarehouseRepository warehouseRepository,
            LocationRepository locationRepository,
            ProductRepository productRepository,
            InventoryLedgerService ledgerService,
            TemporaryPermissionService temporaryPermissionService,
            AuditService auditService,
            ApprovalWorkflowService approvalWorkflowService) {
        this.authorizationService = authorizationService;
        this.locationAccessService = locationAccessService;
        this.importOrderRepository = importOrderRepository;
        this.importItemRepository = importItemRepository;
        this.evidenceRepository = evidenceRepository;
        this.warehouseRepository = warehouseRepository;
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
        this.ledgerService = ledgerService;
        this.temporaryPermissionService = temporaryPermissionService;
        this.auditService = auditService;
        this.approvalWorkflowService = approvalWorkflowService;
    }

    @Transactional
    public ImportOrderResponse createImport(CreateImportRequest request) {
        authorizationService.requirePermission("import:create");
        UserContext context = authorizationService.requireAuthenticated();

        Warehouse warehouse = requireMainWarehouse(context.businessId(), request.destinationLocationId());
        Location location = locationRepository.findByIdAndBusinessId(
                        request.destinationLocationId(), context.businessId())
                .orElseThrow(() -> new NotFoundException("Destination location not found"));

        validateUniqueProducts(request.items());
        List<Product> products = loadAndValidateProducts(context.businessId(), request.items());

        ImportOrder importOrder = new ImportOrder();
        importOrder.setBusinessId(context.businessId());
        importOrder.setImportNumber(generateImportNumber(context.businessId()));
        importOrder.setSupplierName(request.supplierName().trim());
        importOrder.setSupplierReference(trimToNull(request.supplierReference()));
        importOrder.setDestinationLocationId(location.getId());
        importOrder.setWarehouseId(warehouse.getId());
        importOrder.setStatus("DRAFT");
        importOrder.setExpectedArrivalDate(request.expectedArrivalDate());
        importOrder.setNotes(trimToNull(request.notes()));
        importOrder.setAssignedReceiverUserId(request.assignedReceiverUserId());
        importOrder.setCreatedBy(context.userId());
        importOrder = importOrderRepository.save(importOrder);

        for (CreateImportRequest.CreateImportItemRequest itemRequest : request.items()) {
            ImportItem item = new ImportItem();
            item.setBusinessId(context.businessId());
            item.setImportId(importOrder.getId());
            item.setProductId(itemRequest.productId());
            item.setExpectedQuantity(itemRequest.expectedQuantity());
            item.setUnitCost(itemRequest.unitCost());
            item.setNotes(trimToNull(itemRequest.notes()));
            importItemRepository.save(item);
        }

        return toResponse(importOrder, location, warehouse, products);
    }

    @Transactional(readOnly = true)
    public PageResponse<ImportOrderResponse> listImports(String status, int page, int size) {
        authorizationService.requirePermission("import:view");
        UserContext context = authorizationService.requireAuthenticated();

        List<Long> locationIds = locationAccessService.getAccessibleLocations(context).stream()
                .map(Location::getId)
                .toList();
        boolean viewAll = locationAccessService.canViewAllLocations(context);

        Page<ImportOrder> result = importOrderRepository.search(
                context.businessId(),
                locationIds.isEmpty() ? List.of(-1L) : locationIds,
                context.userId(),
                viewAll,
                normalizeStatus(status),
                PageRequest.of(Math.max(page, 0), Math.max(size, 1)));

        return toPageResponse(context, result);
    }

    @Transactional(readOnly = true)
    public ImportOrderResponse getImport(Long importId) {
        authorizationService.requirePermission("import:view");
        UserContext context = authorizationService.requireAuthenticated();

        ImportOrder importOrder = requireVisibleImport(context, importId);
        return toResponse(context, importOrder);
    }

    @Transactional
    public ImportOrderResponse submitImport(Long importId) {
        authorizationService.requirePermission("import:create");
        UserContext context = authorizationService.requireAuthenticated();

        ImportOrder importOrder = requireOwnedDraft(context, importId);
        importOrder.setStatus("PENDING_APPROVAL");
        importOrderRepository.save(importOrder);

        BigDecimal totalQuantity = importItemRepository.findByImportIdOrderByIdAsc(importOrder.getId()).stream()
                .map(ImportItem::getExpectedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        approvalWorkflowService.startWorkflow(
                context, "IMPORT_ORDER", importOrder.getId(), totalQuantity);

        return toResponse(context, importOrder);
    }

    @Transactional
    public ImportOrderResponse approveImport(Long importId) {
        UserContext context = authorizationService.requireAuthenticated();

        ImportOrder importOrder = requireImport(context, importId);
        if (!"PENDING_APPROVAL".equals(importOrder.getStatus())) {
            throw new ConflictException("Import is not pending approval");
        }

        var workflowResult = approvalWorkflowService.approveCurrentStep(
                context, "IMPORT_ORDER", importOrder.getId(), null);
        if (!workflowResult.workflowComplete()) {
            return toResponse(context, importOrder);
        }

        importOrder.setStatus("APPROVED");
        importOrder.setApprovedBy(context.userId());
        importOrder.setApprovedAt(Instant.now());
        importOrderRepository.save(importOrder);

        if (importOrder.getAssignedReceiverUserId() != null) {
            temporaryPermissionService.grantTaskPermission(
                    context.businessId(),
                    importOrder.getAssignedReceiverUserId(),
                    "import:receive:task",
                    importOrder.getDestinationLocationId(),
                    "IMPORT",
                    importOrder.getId(),
                    context.userId(),
                    "Auto-granted for import " + importOrder.getImportNumber(),
                    temporaryPermissionService.defaultTaskExpiry());
        }

        Map<String, Object> approvalDetails = new java.util.HashMap<>();
        if (importOrder.getAssignedReceiverUserId() != null) {
            approvalDetails.put("assignedReceiverUserId", importOrder.getAssignedReceiverUserId());
        }
        auditService.record(context, new AuditService.AuditEvent(
                "IMPORT_APPROVED",
                "IMPORTS",
                "IMPORT",
                importOrder.getId(),
                importOrder.getImportNumber(),
                "Approved import " + importOrder.getImportNumber(),
                approvalDetails));

        return toResponse(context, importOrder);
    }

    @Transactional
    public ImportOrderResponse receiveImport(Long importId, ReceiveImportRequest request) {
        UserContext context = authorizationService.requireAuthenticated();

        ImportOrder importOrder = requireImport(context, importId);
        requireReceivePermission(context, importOrder);

        if (!RECEIVABLE_STATUSES.contains(importOrder.getStatus())) {
            throw new ConflictException("Import is not ready for receiving");
        }

        Location location = locationRepository.findByIdAndBusinessId(
                        importOrder.getDestinationLocationId(), context.businessId())
                .orElseThrow(() -> new NotFoundException("Destination location not found"));

        Map<Long, ImportItem> itemsById = importItemRepository.findByImportIdOrderByIdAsc(importOrder.getId()).stream()
                .collect(Collectors.toMap(ImportItem::getId, Function.identity()));

        for (ReceiveImportRequest.ReceiveImportItemRequest line : request.items()) {
            ImportItem item = itemsById.get(line.itemId());
            if (item == null) {
                throw new NotFoundException("Import item not found: " + line.itemId());
            }

            BigDecimal remaining = item.getExpectedQuantity().subtract(item.getReceivedQuantity());
            if (line.quantityReceived().compareTo(remaining) > 0) {
                throw new ConflictException("Received quantity exceeds remaining expected for item " + line.itemId());
            }

            Product product = ledgerService.requireTrackableProduct(context.businessId(), item.getProductId());
            ledgerService.applyOnHandChange(
                    context,
                    location,
                    product,
                    line.quantityReceived(),
                    "IMPORT_RECEIVE",
                    "IMPORT",
                    importOrder.getId(),
                    "Import " + importOrder.getImportNumber());

            item.setReceivedQuantity(item.getReceivedQuantity().add(line.quantityReceived()));
            if (line.notes() != null && !line.notes().isBlank()) {
                item.setNotes(line.notes().trim());
            }
            importItemRepository.save(item);
        }

        refreshReceivingStatus(importOrder);
        importOrderRepository.save(importOrder);
        return toResponse(context, importOrder);
    }

    @Transactional
    public ImportOrderResponse verifyImport(Long importId) {
        authorizationService.requirePermission("import:verify");
        UserContext context = authorizationService.requireAuthenticated();

        ImportOrder importOrder = requireImport(context, importId);
        if (!"RECEIVED".equals(importOrder.getStatus())) {
            throw new ConflictException("Import must be fully received before verification");
        }

        importOrder.setStatus("VERIFIED");
        importOrder.setVerifiedBy(context.userId());
        importOrder.setVerifiedAt(Instant.now());
        importOrderRepository.save(importOrder);

        temporaryPermissionService.revokeByReference(
                context.businessId(), "IMPORT", importOrder.getId(), context.userId(),
                "Import verified");

        return toResponse(context, importOrder);
    }

    @Transactional
    public ImportOrderResponse cancelImport(Long importId) {
        authorizationService.requireAnyPermission("import:create", "import:approve");
        UserContext context = authorizationService.requireAuthenticated();

        ImportOrder importOrder = requireImport(context, importId);
        if (Set.of("RECEIVED", "VERIFIED", "CANCELLED").contains(importOrder.getStatus())) {
            throw new ConflictException("Import cannot be cancelled in status: " + importOrder.getStatus());
        }

        boolean hasReceived = importItemRepository.findByImportIdOrderByIdAsc(importOrder.getId()).stream()
                .anyMatch(item -> item.getReceivedQuantity().compareTo(BigDecimal.ZERO) > 0);
        if (hasReceived) {
            throw new ConflictException("Cannot cancel an import that has received stock");
        }

        importOrder.setStatus("CANCELLED");
        importOrder.setCancelledBy(context.userId());
        importOrder.setCancelledAt(Instant.now());
        importOrderRepository.save(importOrder);

        temporaryPermissionService.revokeByReference(
                context.businessId(), "IMPORT", importOrder.getId(), context.userId(),
                "Import cancelled");

        return toResponse(context, importOrder);
    }

    @Transactional
    public ImportEvidenceResponse addEvidence(Long importId, AddImportEvidenceRequest request) {
        authorizationService.requirePermission("import:view");
        UserContext context = authorizationService.requireAuthenticated();

        ImportOrder importOrder = requireVisibleImport(context, importId);

        ImportEvidence evidence = new ImportEvidence();
        evidence.setBusinessId(context.businessId());
        evidence.setImportId(importOrder.getId());
        evidence.setEvidenceType(request.evidenceType().trim().toUpperCase(Locale.ROOT));
        evidence.setDescription(request.description().trim());
        evidence.setReferenceUri(trimToNull(request.referenceUri()));
        evidence.setUploadedBy(context.userId());
        evidence = evidenceRepository.save(evidence);

        return toEvidenceResponse(evidence);
    }

    @Transactional(readOnly = true)
    public List<ImportEvidenceResponse> listEvidence(Long importId) {
        authorizationService.requirePermission("import:view");
        UserContext context = authorizationService.requireAuthenticated();

        requireVisibleImport(context, importId);
        return evidenceRepository.findByImportIdOrderByCreatedAtDesc(importId).stream()
                .map(this::toEvidenceResponse)
                .toList();
    }

    private void refreshReceivingStatus(ImportOrder importOrder) {
        List<ImportItem> items = importItemRepository.findByImportIdOrderByIdAsc(importOrder.getId());
        boolean anyReceived = items.stream().anyMatch(i -> i.getReceivedQuantity().compareTo(BigDecimal.ZERO) > 0);
        boolean allReceived = items.stream().allMatch(i ->
                i.getReceivedQuantity().compareTo(i.getExpectedQuantity()) >= 0);

        if (allReceived) {
            importOrder.setStatus("RECEIVED");
        } else if (anyReceived) {
            importOrder.setStatus("PARTIALLY_RECEIVED");
        } else {
            importOrder.setStatus("RECEIVING");
        }
    }

    private Warehouse requireMainWarehouse(Long businessId, Long locationId) {
        Warehouse warehouse = warehouseRepository
                .findByBusinessIdAndLocationIdAndStatus(businessId, locationId, "ACTIVE")
                .orElseThrow(() -> new ConflictException("Destination must be an active warehouse location"));

        if (!"MAIN".equals(warehouse.getWarehouseType())) {
            throw new ConflictException("Imports must be destined for a MAIN warehouse");
        }
        return warehouse;
    }

    private void requireReceivePermission(UserContext context, ImportOrder importOrder) {
        if (context.permissions().contains("import:receive")) {
            locationAccessService.requireLocationAccess(context, importOrder.getDestinationLocationId());
            return;
        }
        if (context.permissions().contains("import:receive:task")) {
            if (importOrder.getAssignedReceiverUserId() == null
                    || !importOrder.getAssignedReceiverUserId().equals(context.userId())) {
                throw new ForbiddenException("You are not assigned to receive this import");
            }
            if (!temporaryPermissionService.hasActivePermission(
                    context.businessId(),
                    context.userId(),
                    importOrder.getDestinationLocationId(),
                    "import:receive:task",
                    "IMPORT",
                    importOrder.getId())) {
                throw new ForbiddenException("You do not have an active task grant for this import");
            }
            return;
        }
        throw new ForbiddenException("You do not have permission to receive imports");
    }

    private ImportOrder requireImport(UserContext context, Long importId) {
        return importOrderRepository.findByIdAndBusinessId(importId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Import not found"));
    }

    private ImportOrder requireVisibleImport(UserContext context, Long importId) {
        ImportOrder importOrder = requireImport(context, importId);
        if (locationAccessService.canViewAllLocations(context)) {
            return importOrder;
        }
        if (importOrder.getAssignedReceiverUserId() != null
                && importOrder.getAssignedReceiverUserId().equals(context.userId())) {
            return importOrder;
        }
        locationAccessService.requireLocationAccess(context, importOrder.getDestinationLocationId());
        return importOrder;
    }

    private ImportOrder requireOwnedDraft(UserContext context, Long importId) {
        ImportOrder importOrder = requireImport(context, importId);
        if (!"DRAFT".equals(importOrder.getStatus())) {
            throw new ConflictException("Only draft imports can be submitted");
        }
        return importOrder;
    }

    private String generateImportNumber(Long businessId) {
        String prefix = "IMP-" + Year.now().getValue() + "-";
        long count = importOrderRepository.countByBusinessIdAndImportNumberStartingWith(businessId, prefix);
        return prefix + String.format("%04d", count + 1);
    }

    private void validateUniqueProducts(List<CreateImportRequest.CreateImportItemRequest> items) {
        Set<Long> productIds = new HashSet<>();
        for (CreateImportRequest.CreateImportItemRequest item : items) {
            if (!productIds.add(item.productId())) {
                throw new ConflictException("Duplicate product in import items: " + item.productId());
            }
        }
    }

    private List<Product> loadAndValidateProducts(
            Long businessId, List<CreateImportRequest.CreateImportItemRequest> items) {
        return items.stream()
                .map(item -> ledgerService.requireTrackableProduct(businessId, item.productId()))
                .toList();
    }

    private PageResponse<ImportOrderResponse> toPageResponse(UserContext context, Page<ImportOrder> page) {
        Map<Long, Location> locations = loadLocations(context.businessId(), page.map(ImportOrder::getDestinationLocationId).toList());
        Map<Long, Warehouse> warehouses = loadWarehouses(context.businessId(), page.map(ImportOrder::getWarehouseId).toList());
        Map<Long, List<ImportItem>> itemsByImport = importItemRepository
                .findByImportIdIn(page.map(ImportOrder::getId).toList()).stream()
                .collect(Collectors.groupingBy(ImportItem::getImportId));
        Map<Long, Product> products = loadProducts(context.businessId(), itemsByImport.values().stream()
                .flatMap(List::stream)
                .map(ImportItem::getProductId)
                .distinct()
                .toList());

        List<ImportOrderResponse> items = page.getContent().stream()
                .map(importOrder -> toResponse(
                        importOrder,
                        locations.get(importOrder.getDestinationLocationId()),
                        importOrder.getWarehouseId() != null ? warehouses.get(importOrder.getWarehouseId()) : null,
                        itemsByImport.getOrDefault(importOrder.getId(), List.of()),
                        products))
                .toList();

        return new PageResponse<>(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private ImportOrderResponse toResponse(UserContext context, ImportOrder importOrder) {
        Location location = locationRepository.findByIdAndBusinessId(
                        importOrder.getDestinationLocationId(), context.businessId())
                .orElse(null);
        Warehouse warehouse = importOrder.getWarehouseId() != null
                ? warehouseRepository.findByIdAndBusinessId(importOrder.getWarehouseId(), context.businessId()).orElse(null)
                : null;
        List<ImportItem> items = importItemRepository.findByImportIdOrderByIdAsc(importOrder.getId());
        Map<Long, Product> products = loadProducts(context.businessId(),
                items.stream().map(ImportItem::getProductId).toList());
        return toResponse(importOrder, location, warehouse, items, products);
    }

    private ImportOrderResponse toResponse(
            ImportOrder importOrder,
            Location location,
            Warehouse warehouse,
            List<Product> products) {
        List<ImportItem> items = importItemRepository.findByImportIdOrderByIdAsc(importOrder.getId());
        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        return toResponse(importOrder, location, warehouse, items, productMap);
    }

    private ImportOrderResponse toResponse(
            ImportOrder importOrder,
            Location location,
            Warehouse warehouse,
            List<ImportItem> items,
            Map<Long, Product> products) {
        List<ImportItemResponse> itemResponses = items.stream()
                .map(item -> toItemResponse(item, products.get(item.getProductId())))
                .toList();

        return new ImportOrderResponse(
                importOrder.getId(),
                importOrder.getImportNumber(),
                importOrder.getSupplierName(),
                importOrder.getSupplierReference(),
                importOrder.getDestinationLocationId(),
                location != null ? location.getCode() : null,
                location != null ? location.getName() : null,
                importOrder.getWarehouseId(),
                warehouse != null ? warehouse.getCode() : null,
                warehouse != null ? warehouse.getName() : null,
                importOrder.getStatus(),
                importOrder.getExpectedArrivalDate(),
                importOrder.getNotes(),
                importOrder.getAssignedReceiverUserId(),
                importOrder.getCreatedBy(),
                importOrder.getApprovedBy(),
                importOrder.getApprovedAt(),
                importOrder.getVerifiedBy(),
                importOrder.getVerifiedAt(),
                itemResponses,
                importOrder.getCreatedAt(),
                importOrder.getUpdatedAt());
    }

    private ImportItemResponse toItemResponse(ImportItem item, Product product) {
        BigDecimal remaining = item.getExpectedQuantity().subtract(item.getReceivedQuantity());
        return new ImportItemResponse(
                item.getId(),
                item.getProductId(),
                product != null ? product.getSku() : null,
                product != null ? product.getName() : null,
                product != null ? product.getUnitOfMeasure() : null,
                item.getExpectedQuantity(),
                item.getReceivedQuantity(),
                remaining.max(BigDecimal.ZERO),
                item.getUnitCost(),
                item.getNotes());
    }

    private ImportEvidenceResponse toEvidenceResponse(ImportEvidence evidence) {
        return new ImportEvidenceResponse(
                evidence.getId(),
                evidence.getEvidenceType(),
                evidence.getDescription(),
                evidence.getReferenceUri(),
                evidence.getUploadedBy(),
                evidence.getCreatedAt());
    }

    private Map<Long, Location> loadLocations(Long businessId, Iterable<Long> locationIds) {
        return locationRepository.findAllById(
                        java.util.stream.StreamSupport.stream(locationIds.spliterator(), false)
                                .filter(id -> id != null)
                                .toList())
                .stream()
                .filter(location -> location.getBusinessId().equals(businessId))
                .collect(Collectors.toMap(Location::getId, Function.identity()));
    }

    private Map<Long, Warehouse> loadWarehouses(Long businessId, Iterable<Long> warehouseIds) {
        return warehouseRepository.findAllById(
                        java.util.stream.StreamSupport.stream(warehouseIds.spliterator(), false)
                                .filter(id -> id != null)
                                .toList())
                .stream()
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
