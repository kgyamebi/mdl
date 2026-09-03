package com.mdl.platform.notifications.controller;

import com.mdl.platform.common.dto.ApiResponse;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.notifications.dto.NotificationResponse;
import com.mdl.platform.notifications.dto.UnreadNotificationCount;
import com.mdl.platform.notifications.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> listNotifications(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.listNotifications(status, category, page, size)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadNotificationCount>> unreadCount() {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.unreadCount()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Notification marked read", notificationService.markRead(id)));
    }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<ApiResponse<NotificationResponse>> dismiss(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Notification dismissed", notificationService.dismiss(id)));
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<Integer>> markAllRead() {
        return ResponseEntity.ok(ApiResponse.ok("Notifications marked read", notificationService.markAllRead()));
    }
}
