package com.mdl.platform.notifications.service;

import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.authorization.repository.UserAuthProfileRepository;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.common.exception.ConflictException;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.notifications.dto.NotificationResponse;
import com.mdl.platform.notifications.dto.UnreadNotificationCount;
import com.mdl.platform.notifications.entity.Notification;
import com.mdl.platform.notifications.repository.NotificationRepository;
import com.mdl.platform.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class NotificationService implements NotificationPublisher {

    private static final Set<String> CATEGORIES = Set.of("ALERT", "SECURITY", "APPROVAL", "SYSTEM");
    private static final Set<String> ACTIVE_STATUSES = Set.of("UNREAD", "READ");

    private final NotificationRepository notificationRepository;
    private final AuthorizationService authorizationService;
    private final UserAuthProfileRepository userAuthProfileRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            AuthorizationService authorizationService,
            UserAuthProfileRepository userAuthProfileRepository) {
        this.notificationRepository = notificationRepository;
        this.authorizationService = authorizationService;
        this.userAuthProfileRepository = userAuthProfileRepository;
    }

    @Override
    @Transactional
    public void notifyUsersWithPermission(Long businessId, String permissionCode, NotificationEvent event) {
        List<Long> userIds = userAuthProfileRepository.findUserIdsWithPermission(businessId, permissionCode);
        for (Long userId : userIds) {
            notifyUser(businessId, userId, event);
        }
    }

    @Override
    @Transactional
    public void notifyUser(Long businessId, Long userId, NotificationEvent event) {
        validateEvent(event);

        Notification notification = null;
        if (event.dedupeKey() != null && !event.dedupeKey().isBlank()) {
            notification = notificationRepository
                    .findByBusinessIdAndUserIdAndDedupeKeyAndStatus(
                            businessId, userId, event.dedupeKey(), "UNREAD")
                    .orElse(null);
        }

        if (notification == null) {
            notification = new Notification();
            notification.setBusinessId(businessId);
            notification.setUserId(userId);
            notification.setStatus("UNREAD");
        }

        notification.setNotificationType(event.notificationType());
        notification.setCategory(event.category());
        notification.setTitle(event.title().trim());
        notification.setMessage(event.message().trim());
        notification.setEntityType(event.entityType());
        notification.setEntityId(event.entityId());
        notification.setEntityRef(trimToNull(event.entityRef()));
        notification.setSourceType(event.sourceType());
        notification.setSourceId(event.sourceId());
        notification.setDedupeKey(trimToNull(event.dedupeKey()));

        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listNotifications(
            String status, String category, int page, int size) {
        UserContext context = authorizationService.requireAuthenticated();

        Page<Notification> results = notificationRepository.search(
                context.businessId(),
                context.userId(),
                normalizeFilter(status),
                normalizeFilter(category),
                PageRequest.of(Math.max(page, 0), Math.max(size, 1)));

        List<NotificationResponse> items = results.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(
                items,
                results.getNumber(),
                results.getSize(),
                results.getTotalElements(),
                results.getTotalPages());
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCount unreadCount() {
        UserContext context = authorizationService.requireAuthenticated();
        long count = notificationRepository.countByBusinessIdAndUserIdAndStatus(
                context.businessId(), context.userId(), "UNREAD");
        return new UnreadNotificationCount(count);
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId) {
        UserContext context = authorizationService.requireAuthenticated();
        Notification notification = requireOwnedNotification(context, notificationId);

        if ("UNREAD".equals(notification.getStatus())) {
            notification.setStatus("READ");
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }

        return toResponse(notification);
    }

    @Transactional
    public NotificationResponse dismiss(Long notificationId) {
        UserContext context = authorizationService.requireAuthenticated();
        Notification notification = requireOwnedNotification(context, notificationId);

        if ("DISMISSED".equals(notification.getStatus())) {
            return toResponse(notification);
        }

        notification.setStatus("DISMISSED");
        notification.setDismissedAt(Instant.now());
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }
        notificationRepository.save(notification);
        return toResponse(notification);
    }

    @Transactional
    public int markAllRead() {
        UserContext context = authorizationService.requireAuthenticated();
        return notificationRepository.markAllRead(
                context.businessId(), context.userId(), Instant.now());
    }

    private Notification requireOwnedNotification(UserContext context, Long notificationId) {
        return notificationRepository.findByIdAndBusinessIdAndUserId(
                        notificationId, context.businessId(), context.userId())
                .orElseThrow(() -> new NotFoundException("Notification not found"));
    }

    private void validateEvent(NotificationEvent event) {
        if (event.title() == null || event.title().isBlank()) {
            throw new ConflictException("Notification title is required");
        }
        if (event.message() == null || event.message().isBlank()) {
            throw new ConflictException("Notification message is required");
        }
        if (!CATEGORIES.contains(event.category())) {
            throw new ConflictException("Invalid notification category: " + event.category());
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getNotificationType(),
                notification.getCategory(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getEntityType(),
                notification.getEntityId(),
                notification.getEntityRef(),
                notification.getSourceType(),
                notification.getSourceId(),
                notification.getStatus(),
                notification.getReadAt(),
                notification.getDismissedAt(),
                notification.getCreatedAt());
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
