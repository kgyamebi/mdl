package com.mdl.platform.audit.service;

import com.mdl.platform.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface AuditRecorder {

    void record(
            Long businessId,
            Long userId,
            String action,
            String module,
            String entityType,
            Long entityId,
            String entityRef,
            String summary,
            Map<String, ?> details,
            HttpServletRequest request);

    void record(UserContext context, AuditService.AuditEvent event, HttpServletRequest request);

    void record(UserContext context, AuditService.AuditEvent event);
}
