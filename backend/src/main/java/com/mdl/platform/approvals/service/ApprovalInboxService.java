package com.mdl.platform.approvals.service;

import com.mdl.platform.approvals.dto.ApprovalInboxResponse;
import com.mdl.platform.approvals.dto.ApprovalInboxResponse.ApprovalInboxItem;
import com.mdl.platform.approvals.dto.ApprovalInboxResponse.ApprovalInboxSummary;
import com.mdl.platform.approvals.service.ApprovalWorkflowService.CurrentStepInfo;
import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.authorization.LocationAccessService;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.imports.entity.ImportOrder;
import com.mdl.platform.imports.repository.ImportOrderRepository;
import com.mdl.platform.inventory.entity.InventoryAdjustmentRequest;
import com.mdl.platform.inventory.entity.Stocktake;
import com.mdl.platform.inventory.repository.InventoryAdjustmentRequestRepository;
import com.mdl.platform.inventory.repository.StocktakeRepository;
import com.mdl.platform.locations.entity.Location;
import com.mdl.platform.security.UserContext;
import com.mdl.platform.transfers.entity.StockTransfer;
import com.mdl.platform.transfers.repository.StockTransferRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ApprovalInboxService {

    private static final int MAX_FETCH_PER_TYPE = 100;

    private final AuthorizationService authorizationService;
    private final LocationAccessService locationAccessService;
    private final ApprovalRuleService approvalRuleService;
    private final ApprovalWorkflowService approvalWorkflowService;
    private final InventoryAdjustmentRequestRepository adjustmentRequestRepository;
    private final StockTransferRepository stockTransferRepository;
    private final ImportOrderRepository importOrderRepository;
    private final StocktakeRepository stocktakeRepository;

    public ApprovalInboxService(
            AuthorizationService authorizationService,
            LocationAccessService locationAccessService,
            ApprovalRuleService approvalRuleService,
            ApprovalWorkflowService approvalWorkflowService,
            InventoryAdjustmentRequestRepository adjustmentRequestRepository,
            StockTransferRepository stockTransferRepository,
            ImportOrderRepository importOrderRepository,
            StocktakeRepository stocktakeRepository) {
        this.authorizationService = authorizationService;
        this.locationAccessService = locationAccessService;
        this.approvalRuleService = approvalRuleService;
        this.approvalWorkflowService = approvalWorkflowService;
        this.adjustmentRequestRepository = adjustmentRequestRepository;
        this.stockTransferRepository = stockTransferRepository;
        this.importOrderRepository = importOrderRepository;
        this.stocktakeRepository = stocktakeRepository;
    }

    public ApprovalInboxResponse getInbox(String entityType, int page, int size) {
        authorizationService.requirePermission("approval:view");
        UserContext context = authorizationService.requireAuthenticated();

        List<Long> locationIds = locationAccessService.getAccessibleLocations(context).stream()
                .map(Location::getId)
                .toList();
        boolean viewAll = locationAccessService.canViewAllLocations(context);
        List<Long> scopedLocationIds = locationIds.isEmpty() ? List.of(-1L) : locationIds;

        List<ApprovalInboxItem> actionableItems = new ArrayList<>();
        long adjustmentCount = 0;
        long transferCount = 0;
        long importCount = 0;
        long stocktakeCount = 0;

        String normalizedEntityType = normalizeEntityTypeFilter(entityType);

        if (includesType(normalizedEntityType, "INVENTORY_ADJUSTMENT")) {
            var adjustments = adjustmentRequestRepository.search(
                    context.businessId(),
                    scopedLocationIds,
                    "PENDING",
                    PageRequest.of(0, MAX_FETCH_PER_TYPE));
            adjustmentCount = adjustments.getTotalElements();
            adjustments.getContent().stream()
                    .map(row -> toAdjustmentItem(context, row))
                    .filter(ApprovalInboxItem::canAct)
                    .forEach(actionableItems::add);
        }

        if (includesType(normalizedEntityType, "STOCK_TRANSFER")) {
            var transfers = stockTransferRepository.search(
                    context.businessId(),
                    scopedLocationIds,
                    viewAll,
                    "REQUESTED",
                    PageRequest.of(0, MAX_FETCH_PER_TYPE));
            transferCount = transfers.getTotalElements();
            transfers.getContent().stream()
                    .map(row -> toTransferItem(context, row))
                    .filter(ApprovalInboxItem::canAct)
                    .forEach(actionableItems::add);
        }

        if (includesType(normalizedEntityType, "IMPORT_ORDER")) {
            var imports = importOrderRepository.search(
                    context.businessId(),
                    scopedLocationIds,
                    context.userId(),
                    viewAll,
                    "PENDING_APPROVAL",
                    PageRequest.of(0, MAX_FETCH_PER_TYPE));
            importCount = imports.getTotalElements();
            imports.getContent().stream()
                    .map(row -> toImportItem(context, row))
                    .filter(ApprovalInboxItem::canAct)
                    .forEach(actionableItems::add);
        }

        if (includesType(normalizedEntityType, "STOCKTAKE")) {
            var stocktakes = stocktakeRepository.search(
                    context.businessId(),
                    scopedLocationIds,
                    "SUBMITTED",
                    PageRequest.of(0, MAX_FETCH_PER_TYPE));
            stocktakeCount = stocktakes.getTotalElements();
            stocktakes.getContent().stream()
                    .map(row -> toStocktakeItem(context, row))
                    .filter(ApprovalInboxItem::canAct)
                    .forEach(actionableItems::add);
        }

        actionableItems.sort(Comparator.comparing(ApprovalInboxItem::submittedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int fromIndex = Math.min(safePage * safeSize, actionableItems.size());
        int toIndex = Math.min(fromIndex + safeSize, actionableItems.size());
        List<ApprovalInboxItem> pageItems = actionableItems.subList(fromIndex, toIndex);

        long totalCount = adjustmentCount + transferCount + importCount + stocktakeCount;
        ApprovalInboxSummary summary = new ApprovalInboxSummary(
                adjustmentCount,
                transferCount,
                importCount,
                stocktakeCount,
                totalCount);

        int totalPages = safeSize == 0 ? 0 : (int) Math.ceil((double) actionableItems.size() / safeSize);
        PageResponse<ApprovalInboxItem> items = new PageResponse<>(
                pageItems,
                safePage,
                safeSize,
                actionableItems.size(),
                totalPages);

        return new ApprovalInboxResponse(summary, items);
    }

    private ApprovalInboxItem toAdjustmentItem(UserContext context, InventoryAdjustmentRequest request) {
        return buildItem(
                context,
                "INVENTORY_ADJUSTMENT",
                request.getId(),
                "ADJ-" + request.getId(),
                "Stock adjustment",
                request.getReason(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getRequestedBy());
    }

    private ApprovalInboxItem toTransferItem(UserContext context, StockTransfer transfer) {
        return buildItem(
                context,
                "STOCK_TRANSFER",
                transfer.getId(),
                transfer.getTransferNumber(),
                "Stock transfer",
                transfer.getNotes() != null ? transfer.getNotes() : "Transfer awaiting approval",
                transfer.getStatus(),
                transfer.getCreatedAt(),
                transfer.getRequestedBy());
    }

    private ApprovalInboxItem toImportItem(UserContext context, ImportOrder importOrder) {
        return buildItem(
                context,
                "IMPORT_ORDER",
                importOrder.getId(),
                importOrder.getImportNumber(),
                "Import order",
                importOrder.getSupplierName(),
                importOrder.getStatus(),
                importOrder.getCreatedAt(),
                importOrder.getCreatedBy());
    }

    private ApprovalInboxItem toStocktakeItem(UserContext context, Stocktake stocktake) {
        Instant submittedAt = stocktake.getSubmittedAt() != null
                ? stocktake.getSubmittedAt()
                : stocktake.getCreatedAt();
        return buildItem(
                context,
                "STOCKTAKE",
                stocktake.getId(),
                stocktake.getStocktakeNumber(),
                "Stocktake",
                stocktake.getVarianceLineCount() + " variance line(s)",
                stocktake.getStatus(),
                submittedAt,
                stocktake.getSubmittedBy());
    }

    private ApprovalInboxItem buildItem(
            UserContext context,
            String entityType,
            Long entityId,
            String reference,
            String title,
            String summary,
            String status,
            Instant submittedAt,
            Long submittedBy) {
        CurrentStepInfo stepInfo = approvalWorkflowService.resolveCurrentStepInfo(
                context.businessId(), entityType, entityId);
        String requiredPermission;
        List<String> requiredPermissions;
        int currentStepOrder;
        int totalSteps;
        String currentStepName;
        boolean parallelStep;
        if (stepInfo != null) {
            requiredPermission = stepInfo.requiredPermission();
            requiredPermissions = stepInfo.requiredPermissions();
            currentStepOrder = stepInfo.currentStepOrder();
            totalSteps = stepInfo.totalSteps();
            currentStepName = stepInfo.stepName();
            parallelStep = stepInfo.parallelStep();
        } else {
            requiredPermission = approvalRuleService.resolveRequiredPermission(context.businessId(), entityType);
            requiredPermissions = List.of(requiredPermission);
            currentStepOrder = 1;
            totalSteps = 1;
            currentStepName = "Approval";
            parallelStep = false;
        }
        boolean canAct = requiredPermissions.stream().anyMatch(context.permissions()::contains);
        return new ApprovalInboxItem(
                entityType,
                entityId,
                reference,
                title,
                summary,
                status,
                requiredPermission,
                requiredPermissions,
                currentStepOrder,
                totalSteps,
                currentStepName,
                parallelStep,
                canAct,
                submittedAt,
                submittedBy);
    }

    private String normalizeEntityTypeFilter(String entityType) {
        if (entityType == null || entityType.isBlank()) {
            return null;
        }
        return entityType.trim().toUpperCase(Locale.ROOT);
    }

    private boolean includesType(String filter, String entityType) {
        return filter == null || filter.equals(entityType);
    }
}
