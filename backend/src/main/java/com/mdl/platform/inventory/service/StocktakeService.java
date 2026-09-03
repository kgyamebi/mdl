package com.mdl.platform.inventory.service;

import com.mdl.platform.approvals.service.ApprovalWorkflowService;
import com.mdl.platform.audit.service.AuditRecorder;
import com.mdl.platform.audit.service.AuditService;
import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.authorization.LocationAccessService;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.common.exception.ConflictException;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.inventory.dto.CancelStocktakeRequest;
import com.mdl.platform.inventory.dto.CreateStocktakeRequest;
import com.mdl.platform.inventory.dto.ReviewStocktakeRequest;
import com.mdl.platform.inventory.dto.StocktakeLineResponse;
import com.mdl.platform.inventory.dto.StocktakeResponse;
import com.mdl.platform.inventory.dto.UpsertStocktakeLineRequest;
import com.mdl.platform.inventory.entity.InventoryBalance;
import com.mdl.platform.inventory.entity.Stocktake;
import com.mdl.platform.inventory.entity.StocktakeLine;
import com.mdl.platform.inventory.repository.InventoryBalanceRepository;
import com.mdl.platform.inventory.repository.StocktakeLineRepository;
import com.mdl.platform.inventory.repository.StocktakeRepository;
import com.mdl.platform.locations.entity.Location;
import com.mdl.platform.notifications.service.NotificationEvent;
import com.mdl.platform.notifications.service.NotificationPublisher;
import com.mdl.platform.products.entity.Product;
import com.mdl.platform.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class StocktakeService {

    private final AuthorizationService authorizationService;
    private final LocationAccessService locationAccessService;
    private final StocktakeRepository stocktakeRepository;
    private final StocktakeLineRepository stocktakeLineRepository;
    private final InventoryBalanceRepository balanceRepository;
    private final InventoryLedgerService ledgerService;
    private final AuditRecorder auditRecorder;
    private final NotificationPublisher notificationPublisher;
    private final ApprovalWorkflowService approvalWorkflowService;

    public StocktakeService(
            AuthorizationService authorizationService,
            LocationAccessService locationAccessService,
            StocktakeRepository stocktakeRepository,
            StocktakeLineRepository stocktakeLineRepository,
            InventoryBalanceRepository balanceRepository,
            InventoryLedgerService ledgerService,
            AuditRecorder auditRecorder,
            NotificationPublisher notificationPublisher,
            ApprovalWorkflowService approvalWorkflowService) {
        this.authorizationService = authorizationService;
        this.locationAccessService = locationAccessService;
        this.stocktakeRepository = stocktakeRepository;
        this.stocktakeLineRepository = stocktakeLineRepository;
        this.balanceRepository = balanceRepository;
        this.ledgerService = ledgerService;
        this.auditRecorder = auditRecorder;
        this.notificationPublisher = notificationPublisher;
        this.approvalWorkflowService = approvalWorkflowService;
    }

    @Transactional
    public StocktakeResponse createStocktake(CreateStocktakeRequest request) {
        authorizationService.requirePermission("stock:count");
        UserContext context = authorizationService.requireAuthenticated();

        Location location = locationAccessService.requireAccessibleLocation(context, request.locationId());

        Stocktake stocktake = new Stocktake();
        stocktake.setBusinessId(context.businessId());
        stocktake.setStocktakeNumber(generateStocktakeNumber(context.businessId()));
        stocktake.setLocationId(location.getId());
        stocktake.setStatus("IN_PROGRESS");
        stocktake.setNotes(trimToNull(request.notes()));
        stocktake.setStartedBy(context.userId());
        stocktakeRepository.save(stocktake);

        boolean preload = request.preloadBalances() == null || request.preloadBalances();
        if (preload) {
            preloadLinesFromBalances(context, stocktake, location.getId());
        }

        return toResponse(stocktake, location);
    }

    @Transactional(readOnly = true)
    public PageResponse<StocktakeResponse> listStocktakes(String status, int page, int size) {
        authorizationService.requirePermission("stock:count");
        UserContext context = authorizationService.requireAuthenticated();

        List<Long> locationIds = locationAccessService.getAccessibleLocations(context).stream()
                .map(Location::getId)
                .toList();
        if (locationIds.isEmpty()) {
            return new PageResponse<>(List.of(), page, size, 0, 0);
        }

        Page<Stocktake> result = stocktakeRepository.search(
                context.businessId(),
                locationIds,
                normalizeStatus(status),
                PageRequest.of(Math.max(page, 0), Math.max(size, 1)));

        Map<Long, Location> locations = ledgerService.loadLocations(
                context.businessId(), result.map(Stocktake::getLocationId).toList());

        List<StocktakeResponse> items = result.getContent().stream()
                .map(stocktake -> toResponse(stocktake, locations.get(stocktake.getLocationId()), List.of()))
                .toList();

        return new PageResponse<>(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public StocktakeResponse getStocktake(Long stocktakeId) {
        authorizationService.requirePermission("stock:count");
        UserContext context = authorizationService.requireAuthenticated();

        Stocktake stocktake = requireVisibleStocktake(context, stocktakeId);
        Location location = locationAccessService.requireAccessibleLocation(context, stocktake.getLocationId());
        return toResponse(stocktake, location);
    }

    @Transactional
    public StocktakeResponse upsertLine(Long stocktakeId, UpsertStocktakeLineRequest request) {
        authorizationService.requirePermission("stock:count");
        UserContext context = authorizationService.requireAuthenticated();

        Stocktake stocktake = requireEditableStocktake(context, stocktakeId);
        Location location = locationAccessService.requireAccessibleLocation(context, stocktake.getLocationId());
        Product product = ledgerService.requireTrackableProduct(context.businessId(), request.productId());

        if (request.countedQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new ConflictException("Counted quantity cannot be negative");
        }

        StocktakeLine line = stocktakeLineRepository
                .findByStocktakeIdAndProductId(stocktake.getId(), product.getId())
                .orElseGet(() -> {
                    InventoryBalance balance = balanceRepository
                            .findForUpdate(context.businessId(), location.getId(), product.getId())
                            .orElse(null);
                    BigDecimal expected = balance != null ? balance.getQuantityOnHand() : BigDecimal.ZERO;

                    StocktakeLine newLine = new StocktakeLine();
                    newLine.setBusinessId(context.businessId());
                    newLine.setStocktakeId(stocktake.getId());
                    newLine.setProductId(product.getId());
                    newLine.setExpectedQuantity(expected);
                    return newLine;
                });

        line.setCountedQuantity(request.countedQuantity());
        line.setNotes(trimToNull(request.notes()));
        stocktakeLineRepository.save(line);

        refreshLineCount(stocktake);
        return toResponse(stocktake, location);
    }

    @Transactional
    public StocktakeResponse submitStocktake(Long stocktakeId) {
        authorizationService.requirePermission("stock:count");
        UserContext context = authorizationService.requireAuthenticated();

        Stocktake stocktake = requireEditableStocktake(context, stocktakeId);
        Location location = locationAccessService.requireAccessibleLocation(context, stocktake.getLocationId());

        List<StocktakeLine> lines = stocktakeLineRepository.findByStocktakeIdOrderByIdAsc(stocktake.getId());
        if (lines.isEmpty()) {
            throw new ConflictException("Stocktake must have at least one count line");
        }

        int varianceLines = 0;
        BigDecimal totalVariance = BigDecimal.ZERO;
        for (StocktakeLine line : lines) {
            if (line.getCountedQuantity() == null) {
                throw new ConflictException("All lines must have a counted quantity before submit");
            }
            BigDecimal variance = line.getCountedQuantity().subtract(line.getExpectedQuantity())
                    .setScale(4, RoundingMode.HALF_UP);
            line.setVariance(variance);
            stocktakeLineRepository.save(line);

            if (variance.compareTo(BigDecimal.ZERO) != 0) {
                varianceLines++;
            }
            totalVariance = totalVariance.add(variance);
        }

        stocktake.setStatus("SUBMITTED");
        stocktake.setSubmittedBy(context.userId());
        stocktake.setSubmittedAt(Instant.now());
        stocktake.setLineCount(lines.size());
        stocktake.setVarianceLineCount(varianceLines);
        stocktake.setTotalVariance(totalVariance.setScale(4, RoundingMode.HALF_UP));
        stocktakeRepository.save(stocktake);

        approvalWorkflowService.startWorkflow(
                context,
                "STOCKTAKE",
                stocktake.getId(),
                stocktake.getTotalVariance().abs());

        auditRecorder.record(context, new AuditService.AuditEvent(
                "STOCKTAKE_SUBMITTED",
                "INVENTORY",
                "STOCKTAKE",
                stocktake.getId(),
                stocktake.getStocktakeNumber(),
                "Submitted stocktake " + stocktake.getStocktakeNumber(),
                Map.of("varianceLineCount", varianceLines, "totalVariance", stocktake.getTotalVariance())));

        notificationPublisher.notifyUsersWithPermission(
                context.businessId(),
                "inventory:adjust",
                new NotificationEvent(
                        "STOCKTAKE_SUBMITTED",
                        "APPROVAL",
                        "Stocktake awaiting approval",
                        stocktake.getStocktakeNumber() + " at " + location.getCode()
                                + " — " + varianceLines + " variance line(s)",
                        "STOCKTAKE",
                        stocktake.getId(),
                        stocktake.getStocktakeNumber(),
                        "STOCKTAKE",
                        stocktake.getId(),
                        "NOTIF:STOCKTAKE:" + stocktake.getId()));

        return toResponse(stocktake, location);
    }

    @Transactional
    public StocktakeResponse approveStocktake(Long stocktakeId, ReviewStocktakeRequest review) {
        UserContext context = authorizationService.requireAuthenticated();

        Stocktake stocktake = requireSubmittedStocktake(context, stocktakeId);
        Location location = locationAccessService.requireAccessibleLocation(context, stocktake.getLocationId());

        String reviewNotes = review != null ? review.reviewNotes() : null;
        var workflowResult = approvalWorkflowService.approveCurrentStep(
                context, "STOCKTAKE", stocktake.getId(), reviewNotes);
        if (!workflowResult.workflowComplete()) {
            return toResponse(stocktake, location);
        }

        List<StocktakeLine> lines = stocktakeLineRepository.findByStocktakeIdOrderByIdAsc(stocktake.getId());
        for (StocktakeLine line : lines) {
            if (line.getVariance() == null || line.getVariance().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            Product product = ledgerService.requireTrackableProduct(context.businessId(), line.getProductId());
            String notes = "Stocktake " + stocktake.getStocktakeNumber();
            if (review != null && review.reviewNotes() != null && !review.reviewNotes().isBlank()) {
                notes = notes + " — " + review.reviewNotes().trim();
            }

            LedgerMovementResult movement = ledgerService.applyOnHandChange(
                    context,
                    location,
                    product,
                    line.getVariance(),
                    "STOCKTAKE",
                    "STOCKTAKE",
                    stocktake.getId(),
                    notes);

            line.setResultTransactionId(movement.transaction().getId());
            stocktakeLineRepository.save(line);
        }

        stocktake.setStatus("COMPLETED");
        stocktake.setApprovedBy(context.userId());
        stocktake.setApprovedAt(Instant.now());
        stocktakeRepository.save(stocktake);

        auditRecorder.record(context, new AuditService.AuditEvent(
                "STOCKTAKE_APPROVED",
                "INVENTORY",
                "STOCKTAKE",
                stocktake.getId(),
                stocktake.getStocktakeNumber(),
                "Approved stocktake " + stocktake.getStocktakeNumber(),
                Map.of("totalVariance", stocktake.getTotalVariance())));

        return toResponse(stocktake, location);
    }

    @Transactional
    public StocktakeResponse cancelStocktake(Long stocktakeId, CancelStocktakeRequest request) {
        authorizationService.requirePermission("stock:count");
        UserContext context = authorizationService.requireAuthenticated();

        Stocktake stocktake = stocktakeRepository.findByIdAndBusinessId(stocktakeId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Stocktake not found"));
        locationAccessService.requireLocationAccess(context, stocktake.getLocationId());

        if (!"IN_PROGRESS".equals(stocktake.getStatus()) && !"SUBMITTED".equals(stocktake.getStatus())) {
            throw new ConflictException("Stocktake cannot be cancelled in status: " + stocktake.getStatus());
        }

        stocktake.setStatus("CANCELLED");
        stocktake.setCancelledBy(context.userId());
        stocktake.setCancelledAt(Instant.now());
        stocktake.setCancelReason(request.reason().trim());
        stocktakeRepository.save(stocktake);

        Location location = locationAccessService.requireAccessibleLocation(context, stocktake.getLocationId());
        return toResponse(stocktake, location);
    }

    private void preloadLinesFromBalances(UserContext context, Stocktake stocktake, Long locationId) {
        Page<InventoryBalance> balances = balanceRepository.search(
                context.businessId(),
                List.of(locationId),
                locationId,
                null,
                "",
                false,
                PageRequest.of(0, 500));

        int count = 0;
        for (InventoryBalance balance : balances.getContent()) {
            Product product = ledgerService.requireTrackableProduct(context.businessId(), balance.getProductId());

            StocktakeLine line = new StocktakeLine();
            line.setBusinessId(context.businessId());
            line.setStocktakeId(stocktake.getId());
            line.setProductId(product.getId());
            line.setExpectedQuantity(balance.getQuantityOnHand());
            stocktakeLineRepository.save(line);
            count++;
        }

        stocktake.setLineCount(count);
        stocktakeRepository.save(stocktake);
    }

    private void refreshLineCount(Stocktake stocktake) {
        stocktake.setLineCount((int) stocktakeLineRepository.countByStocktakeId(stocktake.getId()));
        stocktakeRepository.save(stocktake);
    }

    private Stocktake requireEditableStocktake(UserContext context, Long stocktakeId) {
        Stocktake stocktake = requireVisibleStocktake(context, stocktakeId);
        if (!"IN_PROGRESS".equals(stocktake.getStatus())) {
            throw new ConflictException("Stocktake is not in progress");
        }
        return stocktake;
    }

    private Stocktake requireSubmittedStocktake(UserContext context, Long stocktakeId) {
        Stocktake stocktake = requireVisibleStocktake(context, stocktakeId);
        if (!"SUBMITTED".equals(stocktake.getStatus())) {
            throw new ConflictException("Stocktake is not submitted for approval");
        }
        return stocktake;
    }

    private Stocktake requireVisibleStocktake(UserContext context, Long stocktakeId) {
        Stocktake stocktake = stocktakeRepository.findByIdAndBusinessId(stocktakeId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Stocktake not found"));
        locationAccessService.requireLocationAccess(context, stocktake.getLocationId());
        return stocktake;
    }

    private StocktakeResponse toResponse(Stocktake stocktake, Location location) {
        List<StocktakeLine> lines = stocktakeLineRepository.findByStocktakeIdOrderByIdAsc(stocktake.getId());
        return toResponse(stocktake, location, lines);
    }

    private StocktakeResponse toResponse(Stocktake stocktake, Location location, List<StocktakeLine> lines) {
        Map<Long, Product> products = ledgerService.loadProducts(
                stocktake.getBusinessId(), lines.stream().map(StocktakeLine::getProductId).toList());

        List<StocktakeLineResponse> lineResponses = lines.stream()
                .map(line -> toLineResponse(line, products.get(line.getProductId())))
                .toList();

        return new StocktakeResponse(
                stocktake.getId(),
                stocktake.getStocktakeNumber(),
                stocktake.getLocationId(),
                location != null ? location.getCode() : null,
                location != null ? location.getName() : null,
                stocktake.getStatus(),
                stocktake.getNotes(),
                stocktake.getLineCount(),
                stocktake.getVarianceLineCount(),
                stocktake.getTotalVariance(),
                stocktake.getStartedBy(),
                stocktake.getSubmittedBy(),
                stocktake.getSubmittedAt(),
                stocktake.getApprovedBy(),
                stocktake.getApprovedAt(),
                stocktake.getCancelledBy(),
                stocktake.getCancelledAt(),
                stocktake.getCancelReason(),
                lineResponses,
                stocktake.getCreatedAt(),
                stocktake.getUpdatedAt());
    }

    private StocktakeLineResponse toLineResponse(StocktakeLine line, Product product) {
        return new StocktakeLineResponse(
                line.getId(),
                line.getProductId(),
                product != null ? product.getSku() : null,
                product != null ? product.getName() : null,
                product != null ? product.getUnitOfMeasure() : null,
                line.getExpectedQuantity(),
                line.getCountedQuantity(),
                line.getVariance(),
                line.getNotes(),
                line.getResultTransactionId());
    }

    private String generateStocktakeNumber(Long businessId) {
        String prefix = "STK-" + Year.now().getValue() + "-";
        long count = stocktakeRepository.countByBusinessIdAndStocktakeNumberStartingWith(businessId, prefix);
        return prefix + String.format("%04d", count + 1);
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
