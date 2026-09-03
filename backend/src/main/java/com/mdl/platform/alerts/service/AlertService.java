package com.mdl.platform.alerts.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdl.platform.alerts.dto.AlertResponse;
import com.mdl.platform.alerts.dto.AttentionCategory;
import com.mdl.platform.alerts.dto.OwnerAttentionReport;
import com.mdl.platform.alerts.entity.BusinessAlert;
import com.mdl.platform.alerts.repository.BusinessAlertRepository;
import com.mdl.platform.audit.repository.AuditLogRepository;
import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.authorization.LocationAccessService;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.common.exception.ConflictException;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.imports.repository.ImportOrderRepository;
import com.mdl.platform.inventory.repository.InventoryAdjustmentRequestRepository;
import com.mdl.platform.inventory.repository.InventoryBalanceRepository;
import com.mdl.platform.locations.entity.Location;
import com.mdl.platform.notifications.service.NotificationEvent;
import com.mdl.platform.notifications.service.NotificationPublisher;
import com.mdl.platform.security.UserContext;
import com.mdl.platform.transfers.repository.StockTransferRepository;
import com.mdl.platform.users.entity.User;
import com.mdl.platform.users.repository.UserBusinessMembershipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AlertService implements AlertNotifier {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);
    private static final List<String> ACTIVE_STATUSES = List.of("OPEN", "ACKNOWLEDGED");
    private static final int FAILED_LOGIN_WINDOW_MINUTES = 15;
    private static final int FAILED_LOGIN_BURST_THRESHOLD = 3;

    private final BusinessAlertRepository alertRepository;
    private final AuthorizationService authorizationService;
    private final LocationAccessService locationAccessService;
    private final InventoryBalanceRepository balanceRepository;
    private final InventoryAdjustmentRequestRepository adjustmentRequestRepository;
    private final StockTransferRepository stockTransferRepository;
    private final ImportOrderRepository importOrderRepository;
    private final UserBusinessMembershipRepository membershipRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final NotificationPublisher notificationPublisher;

    public AlertService(
            BusinessAlertRepository alertRepository,
            AuthorizationService authorizationService,
            LocationAccessService locationAccessService,
            InventoryBalanceRepository balanceRepository,
            InventoryAdjustmentRequestRepository adjustmentRequestRepository,
            StockTransferRepository stockTransferRepository,
            ImportOrderRepository importOrderRepository,
            UserBusinessMembershipRepository membershipRepository,
            AuditLogRepository auditLogRepository,
            ObjectMapper objectMapper,
            NotificationPublisher notificationPublisher) {
        this.alertRepository = alertRepository;
        this.authorizationService = authorizationService;
        this.locationAccessService = locationAccessService;
        this.balanceRepository = balanceRepository;
        this.adjustmentRequestRepository = adjustmentRequestRepository;
        this.stockTransferRepository = stockTransferRepository;
        this.importOrderRepository = importOrderRepository;
        this.membershipRepository = membershipRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.notificationPublisher = notificationPublisher;
    }

    @Transactional(readOnly = true)
    public PageResponse<AlertResponse> listAlerts(
            String status, String severity, String alertType, String module, int page, int size) {
        authorizationService.requirePermission("alert:view");
        UserContext context = authorizationService.requireAuthenticated();

        Page<BusinessAlert> results = alertRepository.search(
                context.businessId(),
                normalizeFilter(status),
                normalizeFilter(severity),
                normalizeFilter(alertType),
                normalizeFilter(module),
                PageRequest.of(page, size));

        List<AlertResponse> items = results.getContent().stream().map(this::toResponse).toList();
        return new PageResponse<>(
                items,
                results.getNumber(),
                results.getSize(),
                results.getTotalElements(),
                results.getTotalPages());
    }

    @Transactional
    public OwnerAttentionReport attentionDashboard() {
        authorizationService.requirePermission("alert:view");
        UserContext context = authorizationService.requireAuthenticated();

        scanAndRefreshAlerts(context);

        long totalOpen = alertRepository.countByBusinessIdAndStatusIn(context.businessId(), ACTIVE_STATUSES);
        long critical = alertRepository.countByBusinessIdAndStatusInAndSeverity(
                context.businessId(), ACTIVE_STATUSES, "CRITICAL");
        long warning = alertRepository.countByBusinessIdAndStatusInAndSeverity(
                context.businessId(), ACTIVE_STATUSES, "WARNING");

        List<AttentionCategory> categories = buildAttentionCategories(context);
        List<AlertResponse> recent = alertRepository
                .findTop5ByBusinessIdAndStatusInOrderByCreatedAtDesc(context.businessId(), ACTIVE_STATUSES)
                .stream()
                .map(this::toResponse)
                .toList();

        return new OwnerAttentionReport(totalOpen, critical, warning, categories, recent);
    }

    @Transactional
    public int scanAlerts() {
        authorizationService.requirePermission("alert:view");
        UserContext context = authorizationService.requireAuthenticated();
        return scanAndRefreshAlerts(context);
    }

    @Transactional
    public AlertResponse acknowledgeAlert(Long alertId) {
        authorizationService.requirePermission("alert:acknowledge");
        UserContext context = authorizationService.requireAuthenticated();

        BusinessAlert alert = alertRepository.findByIdAndBusinessId(alertId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Alert not found"));

        if (!ACTIVE_STATUSES.contains(alert.getStatus())) {
            throw new ConflictException("Alert is not active");
        }

        alert.setStatus("ACKNOWLEDGED");
        alert.setAcknowledgedBy(context.userId());
        alert.setAcknowledgedAt(Instant.now());
        return toResponse(alertRepository.save(alert));
    }

    @Override
    @Transactional
    public void notifyAccountLocked(Long businessId, Long userId, String userEmail, Instant lockedUntil) {
        upsertOpenAlert(
                businessId,
                "ACCOUNT_LOCKED",
                "CRITICAL",
                "SECURITY",
                "Account temporarily locked",
                "User account " + userEmail + " was locked after repeated failed login attempts",
                "USER",
                userId,
                userEmail,
                Map.of("lockedUntil", lockedUntil.toString()),
                "ACCOUNT_LOCKED:" + userId);
    }

    @Override
    @Transactional
    public void checkFailedLoginPattern(Long businessId, Long userId, String userEmail) {
        Instant since = Instant.now().minus(FAILED_LOGIN_WINDOW_MINUTES, ChronoUnit.MINUTES);
        long recentFailures = auditLogRepository.countByBusinessIdAndUserIdAndActionAndCreatedAtAfter(
                businessId, userId, "LOGIN_FAILED", since);

        if (recentFailures >= FAILED_LOGIN_BURST_THRESHOLD) {
            upsertOpenAlert(
                    businessId,
                    "FAILED_LOGIN_BURST",
                    "WARNING",
                    "SECURITY",
                    "Repeated failed login attempts",
                    userEmail + " has " + recentFailures + " failed login attempts in the last "
                            + FAILED_LOGIN_WINDOW_MINUTES + " minutes",
                    "USER",
                    userId,
                    userEmail,
                    Map.of("recentFailures", recentFailures, "windowMinutes", FAILED_LOGIN_WINDOW_MINUTES),
                    "FAILED_LOGIN_BURST:" + userId);
        } else {
            resolveIfOpen(businessId, "FAILED_LOGIN_BURST:" + userId);
        }
    }

    private int scanAndRefreshAlerts(UserContext context) {
        List<Long> locationIds = locationAccessService.getAccessibleLocations(context).stream()
                .map(Location::getId)
                .toList();
        boolean viewAll = locationAccessService.canViewAllLocations(context);
        List<Long> scopedLocationIds = locationIds.isEmpty() ? List.of(-1L) : locationIds;

        int createdOrUpdated = 0;

        long lowStock = locationIds.isEmpty()
                ? 0
                : balanceRepository.countLowStock(context.businessId(), locationIds);
        createdOrUpdated += syncAggregateAlert(
                context.businessId(),
                "LOW_STOCK",
                "WARNING",
                "INVENTORY",
                "Low stock items",
                lowStock,
                "product balance(s) at or below reorder level",
                "LOW_STOCK");

        long pendingAdjustments = adjustmentRequestRepository.countByBusinessIdAndStatusAndLocationIdIn(
                context.businessId(), "PENDING", scopedLocationIds);
        createdOrUpdated += syncAggregateAlert(
                context.businessId(),
                "PENDING_ADJUSTMENTS",
                "WARNING",
                "INVENTORY",
                "Pending stock adjustments",
                pendingAdjustments,
                "adjustment request(s) awaiting manager approval",
                "PENDING_ADJUSTMENTS");

        long pendingTransfers = stockTransferRepository.search(
                context.businessId(),
                scopedLocationIds,
                viewAll,
                "REQUESTED",
                PageRequest.of(0, 1)).getTotalElements();
        createdOrUpdated += syncAggregateAlert(
                context.businessId(),
                "PENDING_TRANSFERS",
                "WARNING",
                "TRANSFERS",
                "Pending transfer requests",
                pendingTransfers,
                "transfer request(s) awaiting approval",
                "PENDING_TRANSFERS");

        long inTransit = stockTransferRepository.search(
                context.businessId(),
                scopedLocationIds,
                viewAll,
                "DISPATCHED",
                PageRequest.of(0, 1)).getTotalElements();
        createdOrUpdated += syncAggregateAlert(
                context.businessId(),
                "TRANSFERS_IN_TRANSIT",
                "INFO",
                "TRANSFERS",
                "Transfers in transit",
                inTransit,
                "transfer(s) dispatched and awaiting receipt",
                "TRANSFERS_IN_TRANSIT");

        if (viewAll) {
            long pendingImports = importOrderRepository.countByBusinessIdAndStatus(
                    context.businessId(), "PENDING_APPROVAL");
            createdOrUpdated += syncAggregateAlert(
                    context.businessId(),
                    "PENDING_IMPORT_APPROVAL",
                    "WARNING",
                    "IMPORTS",
                    "Imports pending approval",
                    pendingImports,
                    "import order(s) awaiting approval",
                    "PENDING_IMPORT_APPROVAL");

            long awaitingReceive = importOrderRepository.countByBusinessIdAndStatus(
                    context.businessId(), "APPROVED");
            createdOrUpdated += syncAggregateAlert(
                    context.businessId(),
                    "IMPORTS_AWAITING_RECEIVE",
                    "INFO",
                    "IMPORTS",
                    "Imports awaiting receive",
                    awaitingReceive,
                    "approved import(s) awaiting receiving",
                    "IMPORTS_AWAITING_RECEIVE");
        }

        List<User> lockedUsers = membershipRepository.findLockedUsersInBusiness(
                context.businessId(), Instant.now());
        for (User user : lockedUsers) {
            upsertOpenAlert(
                    context.businessId(),
                    "ACCOUNT_LOCKED",
                    "CRITICAL",
                    "SECURITY",
                    "Account temporarily locked",
                    "User account " + user.getEmail() + " is locked",
                    "USER",
                    user.getId(),
                    user.getEmail(),
                    Map.of("lockedUntil", user.getLockedUntil().toString()),
                    "ACCOUNT_LOCKED:" + user.getId());
            createdOrUpdated++;
        }

        return createdOrUpdated;
    }

    private int syncAggregateAlert(
            Long businessId,
            String alertType,
            String severity,
            String module,
            String title,
            long count,
            String countLabel,
            String dedupeKey) {
        if (count > 0) {
            upsertOpenAlert(
                    businessId,
                    alertType,
                    severity,
                    module,
                    title,
                    count + " " + countLabel,
                    null,
                    null,
                    null,
                    Map.of("count", count),
                    dedupeKey);
            return 1;
        }
        resolveIfOpen(businessId, dedupeKey);
        return 0;
    }

    private void upsertOpenAlert(
            Long businessId,
            String alertType,
            String severity,
            String module,
            String title,
            String summary,
            String entityType,
            Long entityId,
            String entityRef,
            Map<String, ?> details,
            String dedupeKey) {

        BusinessAlert alert = alertRepository
                .findByBusinessIdAndDedupeKeyAndStatusIn(businessId, dedupeKey, ACTIVE_STATUSES)
                .orElseGet(BusinessAlert::new);

        alert.setBusinessId(businessId);
        alert.setAlertType(alertType);
        alert.setSeverity(severity);
        alert.setModule(module);
        alert.setTitle(title);
        alert.setSummary(summary);
        alert.setEntityType(entityType);
        alert.setEntityId(entityId);
        alert.setEntityRef(entityRef);
        alert.setDetails(serializeDetails(details));
        alert.setDedupeKey(dedupeKey);
        if (alert.getStatus() == null || alert.getStatus().isBlank()) {
            alert.setStatus("OPEN");
        }
        alertRepository.save(alert);
        publishAlertNotification(businessId, alert);
    }

    private void publishAlertNotification(Long businessId, BusinessAlert alert) {
        if (alert.getDedupeKey() == null || alert.getDedupeKey().isBlank()) {
            return;
        }

        String category = "SECURITY".equals(alert.getModule()) ? "SECURITY" : "ALERT";
        notificationPublisher.notifyUsersWithPermission(
                businessId,
                "alert:view",
                new NotificationEvent(
                        "BUSINESS_ALERT",
                        category,
                        alert.getTitle(),
                        alert.getSummary(),
                        alert.getEntityType(),
                        alert.getEntityId(),
                        alert.getEntityRef(),
                        "BUSINESS_ALERT",
                        alert.getId(),
                        "NOTIF:" + alert.getDedupeKey()));
    }

    private void resolveIfOpen(Long businessId, String dedupeKey) {
        alertRepository.findByBusinessIdAndDedupeKeyAndStatusIn(businessId, dedupeKey, ACTIVE_STATUSES)
                .ifPresent(alert -> {
                    alert.setStatus("RESOLVED");
                    alert.setResolvedAt(Instant.now());
                    alertRepository.save(alert);
                });
    }

    private List<AttentionCategory> buildAttentionCategories(UserContext context) {
        List<AttentionCategory> categories = new ArrayList<>();
        List<Long> locationIds = locationAccessService.getAccessibleLocations(context).stream()
                .map(Location::getId)
                .toList();
        List<Long> scopedLocationIds = locationIds.isEmpty() ? List.of(-1L) : locationIds;
        boolean viewAll = locationAccessService.canViewAllLocations(context);

        long lowStock = locationIds.isEmpty()
                ? 0
                : balanceRepository.countLowStock(context.businessId(), locationIds);
        if (lowStock > 0) {
            categories.add(new AttentionCategory(
                    "LOW_STOCK",
                    "Low stock",
                    lowStock,
                    "WARNING",
                    lowStock + " product balance(s) at or below reorder level"));
        }

        long pendingAdjustments = adjustmentRequestRepository.countByBusinessIdAndStatusAndLocationIdIn(
                context.businessId(), "PENDING", scopedLocationIds);
        if (pendingAdjustments > 0) {
            categories.add(new AttentionCategory(
                    "PENDING_ADJUSTMENTS",
                    "Pending adjustments",
                    pendingAdjustments,
                    "WARNING",
                    pendingAdjustments + " adjustment request(s) need approval"));
        }

        long pendingTransfers = stockTransferRepository.search(
                context.businessId(),
                scopedLocationIds,
                viewAll,
                "REQUESTED",
                PageRequest.of(0, 1)).getTotalElements();
        if (pendingTransfers > 0) {
            categories.add(new AttentionCategory(
                    "PENDING_TRANSFERS",
                    "Pending transfers",
                    pendingTransfers,
                    "WARNING",
                    pendingTransfers + " transfer request(s) need approval"));
        }

        long openAlerts = alertRepository.countByBusinessIdAndStatusIn(context.businessId(), ACTIVE_STATUSES);
        long securityAlerts = alertRepository.search(
                context.businessId(),
                null,
                null,
                null,
                "SECURITY",
                PageRequest.of(0, 1)).getTotalElements();
        if (securityAlerts > 0) {
            categories.add(new AttentionCategory(
                    "SECURITY",
                    "Security alerts",
                    securityAlerts,
                    "CRITICAL",
                    securityAlerts + " security alert(s) require review"));
        }

        if (categories.isEmpty() && openAlerts == 0) {
            categories.add(new AttentionCategory(
                    "ALL_CLEAR",
                    "All clear",
                    0,
                    "INFO",
                    "No items need immediate attention"));
        }

        return categories;
    }

    private AlertResponse toResponse(BusinessAlert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getAlertType(),
                alert.getSeverity(),
                alert.getModule(),
                alert.getTitle(),
                alert.getSummary(),
                alert.getEntityType(),
                alert.getEntityId(),
                alert.getEntityRef(),
                alert.getDetails(),
                alert.getStatus(),
                alert.getAcknowledgedBy(),
                alert.getAcknowledgedAt(),
                alert.getResolvedAt(),
                alert.getCreatedAt());
    }

    private String serializeDetails(Map<String, ?> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize alert details", ex);
            return null;
        }
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
