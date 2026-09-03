package com.mdl.platform.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdl.platform.audit.dto.AuditLogResponse;
import com.mdl.platform.audit.entity.AuditLog;
import com.mdl.platform.audit.repository.AuditLogRepository;
import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

@Service
public class AuditService implements AuditRecorder {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final AuthorizationService authorizationService;
    private final ObjectMapper objectMapper;

    public AuditService(
            AuditLogRepository auditLogRepository,
            AuthorizationService authorizationService,
            ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.authorizationService = authorizationService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void record(
            Long businessId,
            Long userId,
            String action,
            String module,
            String entityType,
            Long entityId,
            String entityRef,
            String summary,
            Map<String, ?> details,
            HttpServletRequest request) {

        AuditLog entry = new AuditLog();
        entry.setBusinessId(businessId);
        entry.setUserId(userId);
        entry.setAction(normalize(action));
        entry.setModule(normalize(module));
        entry.setEntityType(entityType != null ? normalize(entityType) : null);
        entry.setEntityId(entityId);
        entry.setEntityRef(trimToNull(entityRef));
        entry.setSummary(summary.trim());
        entry.setDetails(serializeDetails(details));

        if (request != null) {
            entry.setIpAddress(trimToNull(request.getRemoteAddr()));
            entry.setUserAgent(trimToNull(request.getHeader("User-Agent")));
        }

        auditLogRepository.save(entry);
    }

    @Override
    @Transactional
    public void record(UserContext context, AuditEvent event, HttpServletRequest request) {
        record(
                context.businessId(),
                context.userId(),
                event.action(),
                event.module(),
                event.entityType(),
                event.entityId(),
                event.entityRef(),
                event.summary(),
                event.details(),
                request);
    }

    @Override
    @Transactional
    public void record(UserContext context, AuditEvent event) {
        record(context, event, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> listLogs(
            Long userId,
            String module,
            String action,
            String entityType,
            Long entityId,
            Instant from,
            Instant to,
            int page,
            int size) {

        authorizationService.requirePermission("audit:view");
        UserContext context = authorizationService.requireAuthenticated();

        Page<AuditLog> result = auditLogRepository.search(
                context.businessId(),
                userId,
                normalizeOrNull(module),
                normalizeOrNull(action),
                normalizeOrNull(entityType),
                entityId,
                from,
                to,
                PageRequest.of(Math.max(page, 0), Math.max(size, 1)));

        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private AuditLogResponse toResponse(AuditLog entry) {
        return new AuditLogResponse(
                entry.getId(),
                entry.getUserId(),
                entry.getAction(),
                entry.getModule(),
                entry.getEntityType(),
                entry.getEntityId(),
                entry.getEntityRef(),
                entry.getSummary(),
                entry.getDetails(),
                entry.getIpAddress(),
                entry.getCreatedAt());
    }

    private String serializeDetails(Map<String, ?> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize audit details", ex);
            return null;
        }
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return normalize(value);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record AuditEvent(
            String action,
            String module,
            String entityType,
            Long entityId,
            String entityRef,
            String summary,
            Map<String, ?> details) {
    }
}
