package com.mdl.platform.notifications.service;

public interface NotificationPublisher {

    void notifyUsersWithPermission(Long businessId, String permissionCode, NotificationEvent event);

    void notifyUser(Long businessId, Long userId, NotificationEvent event);
}
