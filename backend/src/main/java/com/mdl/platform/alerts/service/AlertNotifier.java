package com.mdl.platform.alerts.service;

import java.time.Instant;

public interface AlertNotifier {

    void notifyAccountLocked(Long businessId, Long userId, String userEmail, Instant lockedUntil);

    void checkFailedLoginPattern(Long businessId, Long userId, String userEmail);
}
