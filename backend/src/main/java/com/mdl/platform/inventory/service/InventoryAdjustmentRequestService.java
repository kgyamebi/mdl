package com.mdl.platform.inventory.service;

import com.mdl.platform.approvals.service.ApprovalWorkflowService;
import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.authorization.LocationAccessService;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.common.exception.ConflictException;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.inventory.dto.AdjustmentRequestResponse;
import com.mdl.platform.inventory.dto.CreateAdjustmentRequestRequest;
import com.mdl.platform.inventory.dto.ReviewAdjustmentRequestRequest;
import com.mdl.platform.inventory.entity.InventoryAdjustmentRequest;
import com.mdl.platform.inventory.repository.InventoryAdjustmentRequestRepository;
import com.mdl.platform.locations.entity.Location;
import com.mdl.platform.notifications.service.NotificationEvent;
import com.mdl.platform.notifications.service.NotificationPublisher;
import com.mdl.platform.products.entity.Product;
import com.mdl.platform.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class InventoryAdjustmentRequestService {

    private final AuthorizationService authorizationService;
    private final LocationAccessService locationAccessService;
    private final InventoryAdjustmentRequestRepository requestRepository;
    private final InventoryLedgerService ledgerService;
    private final NotificationPublisher notificationPublisher;
    private final ApprovalWorkflowService approvalWorkflowService;

    public InventoryAdjustmentRequestService(
            AuthorizationService authorizationService,
            LocationAccessService locationAccessService,
            InventoryAdjustmentRequestRepository requestRepository,
            InventoryLedgerService ledgerService,
            NotificationPublisher notificationPublisher,
            ApprovalWorkflowService approvalWorkflowService) {
        this.authorizationService = authorizationService;
        this.locationAccessService = locationAccessService;
        this.requestRepository = requestRepository;
        this.ledgerService = ledgerService;
        this.notificationPublisher = notificationPublisher;
        this.approvalWorkflowService = approvalWorkflowService;
    }

    @Transactional
    public AdjustmentRequestResponse createRequest(CreateAdjustmentRequestRequest request) {
        authorizationService.requirePermission("inventory:adjust:request");
        UserContext context = authorizationService.requireAuthenticated();

        if (request.requestedChange().compareTo(java.math.BigDecimal.ZERO) == 0) {
            throw new ConflictException("Requested change cannot be zero");
        }

        Location location = locationAccessService.requireAccessibleLocation(context, request.locationId());
        Product product = ledgerService.requireTrackableProduct(context.businessId(), request.productId());

        InventoryAdjustmentRequest entity = new InventoryAdjustmentRequest();
        entity.setBusinessId(context.businessId());
        entity.setLocationId(location.getId());
        entity.setProductId(product.getId());
        entity.setRequestedChange(request.requestedChange());
        entity.setReason(request.reason().trim());
        entity.setStatus("PENDING");
        entity.setRequestedBy(context.userId());

        entity = requestRepository.save(entity);

        approvalWorkflowService.startWorkflow(
                context,
                "INVENTORY_ADJUSTMENT",
                entity.getId(),
                request.requestedChange().abs());

        notificationPublisher.notifyUsersWithPermission(
                context.businessId(),
                "inventory:adjust",
                new NotificationEvent(
                        "ADJUSTMENT_REQUEST_PENDING",
                        "APPROVAL",
                        "Stock adjustment awaiting approval",
                        product.getSku() + " at " + location.getCode() + ": " + entity.getReason(),
                        "ADJUSTMENT_REQUEST",
                        entity.getId(),
                        null,
                        "ADJUSTMENT_REQUEST",
                        entity.getId(),
                        "NOTIF:ADJUSTMENT:" + entity.getId()));

        return toResponse(entity, location, product);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdjustmentRequestResponse> listRequests(String status, int page, int size) {
        authorizationService.requireAnyPermission("inventory:adjust:request", "inventory:adjust");
        UserContext context = authorizationService.requireAuthenticated();

        List<Long> locationIds = locationAccessService.getAccessibleLocations(context).stream()
                .map(Location::getId)
                .toList();

        if (locationIds.isEmpty()) {
            return new PageResponse<>(List.of(), page, size, 0, 0);
        }

        String normalizedStatus = normalizeStatus(status);
        Page<InventoryAdjustmentRequest> result = requestRepository.search(
                context.businessId(),
                locationIds,
                normalizedStatus,
                PageRequest.of(Math.max(page, 0), Math.max(size, 1)));

        Map<Long, Location> locations = ledgerService.loadLocations(
                context.businessId(), result.map(InventoryAdjustmentRequest::getLocationId).toList());
        Map<Long, Product> products = ledgerService.loadProducts(
                context.businessId(), result.map(InventoryAdjustmentRequest::getProductId).toList());

        List<AdjustmentRequestResponse> items = result.getContent().stream()
                .map(row -> toResponse(
                        row,
                        locations.get(row.getLocationId()),
                        products.get(row.getProductId())))
                .toList();

        return new PageResponse<>(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional
    public AdjustmentRequestResponse approveRequest(Long requestId, ReviewAdjustmentRequestRequest review) {
        UserContext context = authorizationService.requireAuthenticated();

        InventoryAdjustmentRequest entity = requirePendingRequest(context, requestId);
        Location location = locationAccessService.requireAccessibleLocation(context, entity.getLocationId());
        Product product = ledgerService.requireTrackableProduct(context.businessId(), entity.getProductId());

        var workflowResult = approvalWorkflowService.approveCurrentStep(
                context,
                "INVENTORY_ADJUSTMENT",
                entity.getId(),
                review != null ? review.reviewNotes() : null);

        if (!workflowResult.workflowComplete()) {
            return toResponse(entity, location, product);
        }

        LedgerMovementResult movement = ledgerService.applyOnHandChange(
                context,
                location,
                product,
                entity.getRequestedChange(),
                "ADJUSTMENT",
                "ADJUSTMENT_REQUEST",
                entity.getId(),
                entity.getReason());

        entity.setStatus("APPROVED");
        entity.setReviewedBy(context.userId());
        entity.setReviewedAt(Instant.now());
        entity.setReviewNotes(review != null ? trimToNull(review.reviewNotes()) : null);
        entity.setResultTransactionId(movement.transaction().getId());
        entity = requestRepository.save(entity);

        return toResponse(entity, location, product);
    }

    @Transactional
    public AdjustmentRequestResponse rejectRequest(Long requestId, ReviewAdjustmentRequestRequest review) {
        UserContext context = authorizationService.requireAuthenticated();

        InventoryAdjustmentRequest entity = requirePendingRequest(context, requestId);
        locationAccessService.requireLocationAccess(context, entity.getLocationId());

        approvalWorkflowService.rejectCurrentStep(
                context,
                "INVENTORY_ADJUSTMENT",
                entity.getId(),
                review != null ? review.reviewNotes() : null);

        entity.setStatus("REJECTED");
        entity.setReviewedBy(context.userId());
        entity.setReviewedAt(Instant.now());
        entity.setReviewNotes(review != null ? trimToNull(review.reviewNotes()) : null);
        entity = requestRepository.save(entity);

        Map<Long, Location> locations = ledgerService.loadLocations(
                context.businessId(), List.of(entity.getLocationId()));
        Map<Long, Product> products = ledgerService.loadProducts(
                context.businessId(), List.of(entity.getProductId()));

        return toResponse(entity, locations.get(entity.getLocationId()), products.get(entity.getProductId()));
    }

    private InventoryAdjustmentRequest requirePendingRequest(UserContext context, Long requestId) {
        InventoryAdjustmentRequest entity = requestRepository.findByIdAndBusinessId(requestId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Adjustment request not found"));
        if (!"PENDING".equals(entity.getStatus())) {
            throw new ConflictException("Adjustment request is not pending");
        }
        return entity;
    }

    private AdjustmentRequestResponse toResponse(
            InventoryAdjustmentRequest entity, Location location, Product product) {
        return new AdjustmentRequestResponse(
                entity.getId(),
                entity.getLocationId(),
                location != null ? location.getCode() : null,
                location != null ? location.getName() : null,
                entity.getProductId(),
                product != null ? product.getSku() : null,
                product != null ? product.getName() : null,
                entity.getRequestedChange(),
                entity.getReason(),
                entity.getStatus(),
                entity.getRequestedBy(),
                entity.getReviewedBy(),
                entity.getReviewedAt(),
                entity.getReviewNotes(),
                entity.getResultTransactionId(),
                entity.getCreatedAt());
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
