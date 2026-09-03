package com.mdl.platform.authorization.service;

import com.mdl.platform.audit.service.AuditService;
import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.authorization.dto.GrantTemporaryPermissionRequest;
import com.mdl.platform.authorization.dto.RevokeTemporaryPermissionRequest;
import com.mdl.platform.authorization.dto.TemporaryPermissionResponse;
import com.mdl.platform.authorization.entity.TemporaryPermission;
import com.mdl.platform.authorization.repository.TemporaryPermissionRepository;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.common.exception.ConflictException;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.locations.entity.Location;
import com.mdl.platform.locations.repository.LocationRepository;
import com.mdl.platform.security.UserContext;
import com.mdl.platform.users.repository.UserBusinessMembershipRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TemporaryPermissionService {

    private final AuthorizationService authorizationService;
    private final TemporaryPermissionRepository temporaryPermissionRepository;
    private final RestrictedWarehouseAccessService restrictedWarehouseAccessService;
    private final LocationRepository locationRepository;
    private final UserBusinessMembershipRepository membershipRepository;
    private final AuditService auditService;

    public TemporaryPermissionService(
            AuthorizationService authorizationService,
            TemporaryPermissionRepository temporaryPermissionRepository,
            RestrictedWarehouseAccessService restrictedWarehouseAccessService,
            LocationRepository locationRepository,
            UserBusinessMembershipRepository membershipRepository,
            AuditService auditService) {
        this.authorizationService = authorizationService;
        this.temporaryPermissionRepository = temporaryPermissionRepository;
        this.restrictedWarehouseAccessService = restrictedWarehouseAccessService;
        this.locationRepository = locationRepository;
        this.membershipRepository = membershipRepository;
        this.auditService = auditService;
    }

    @Transactional
    public TemporaryPermissionResponse grant(GrantTemporaryPermissionRequest request) {
        authorizationService.requirePermission("permission:grant");
        UserContext context = authorizationService.requireAuthenticated();

        validateUserInBusiness(context.businessId(), request.userId());
        Location location = locationRepository.findByIdAndBusinessId(request.locationId(), context.businessId())
                .orElseThrow(() -> new NotFoundException("Location not found"));

        if (!restrictedWarehouseAccessService.isRestrictedLocation(context.businessId(), location.getId())) {
            throw new ConflictException("Temporary permissions apply only to restricted main warehouse locations");
        }

        TemporaryPermission grant = new TemporaryPermission();
        grant.setBusinessId(context.businessId());
        grant.setUserId(request.userId());
        grant.setPermissionCode(request.permissionCode().trim());
        grant.setLocationId(location.getId());
        grant.setReferenceType(normalizeReferenceType(request.referenceType()));
        grant.setReferenceId(request.referenceId());
        grant.setReason(trimToNull(request.reason()));
        grant.setGrantedBy(context.userId());
        grant.setExpiresAt(request.expiresAt());
        grant.setStatus("ACTIVE");

        grant = temporaryPermissionRepository.save(grant);
        auditService.record(context, new AuditService.AuditEvent(
                "PERMISSION_GRANTED",
                "SECURITY",
                "TEMPORARY_PERMISSION",
                grant.getId(),
                null,
                "Granted temporary permission " + grant.getPermissionCode(),
                Map.of(
                        "userId", grant.getUserId(),
                        "locationId", grant.getLocationId(),
                        "expiresAt", grant.getExpiresAt().toString())));
        return toResponse(grant, location);
    }

    @Transactional
    public TemporaryPermissionResponse grantTaskPermission(
            Long businessId,
            Long userId,
            String permissionCode,
            Long locationId,
            String referenceType,
            Long referenceId,
            Long grantedBy,
            String reason,
            Instant expiresAt) {

        TemporaryPermission grant = new TemporaryPermission();
        grant.setBusinessId(businessId);
        grant.setUserId(userId);
        grant.setPermissionCode(permissionCode);
        grant.setLocationId(locationId);
        grant.setReferenceType(referenceType);
        grant.setReferenceId(referenceId);
        grant.setReason(reason);
        grant.setGrantedBy(grantedBy);
        grant.setExpiresAt(expiresAt);
        grant.setStatus("ACTIVE");

        grant = temporaryPermissionRepository.save(grant);
        Location location = locationRepository.findByIdAndBusinessId(locationId, businessId).orElse(null);
        return toResponse(grant, location);
    }

    @Transactional
    public void revokeByReference(Long businessId, String referenceType, Long referenceId, Long revokedBy, String reason) {
        List<TemporaryPermission> grants = temporaryPermissionRepository
                .findByBusinessIdAndReferenceTypeAndReferenceIdAndStatus(
                        businessId, referenceType, referenceId, "ACTIVE");

        Instant now = Instant.now();
        for (TemporaryPermission grant : grants) {
            if (grant.isCurrentlyActive(now)) {
                grant.setStatus("REVOKED");
                grant.setRevokedAt(now);
                grant.setRevokedBy(revokedBy);
                grant.setRevokeReason(reason);
                temporaryPermissionRepository.save(grant);
            }
        }
    }

    @Transactional
    public TemporaryPermissionResponse revoke(Long grantId, RevokeTemporaryPermissionRequest request) {
        authorizationService.requirePermission("permission:grant");
        UserContext context = authorizationService.requireAuthenticated();

        TemporaryPermission grant = temporaryPermissionRepository.findByIdAndBusinessId(grantId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Temporary permission not found"));

        if (!"ACTIVE".equals(grant.getStatus()) || grant.getRevokedAt() != null) {
            throw new ConflictException("Temporary permission is not active");
        }

        grant.setStatus("REVOKED");
        grant.setRevokedAt(Instant.now());
        grant.setRevokedBy(context.userId());
        grant.setRevokeReason(request != null ? trimToNull(request.reason()) : null);
        grant = temporaryPermissionRepository.save(grant);

        Location location = locationRepository.findByIdAndBusinessId(grant.getLocationId(), context.businessId())
                .orElse(null);
        Map<String, Object> revokeDetails = new java.util.HashMap<>();
        revokeDetails.put("userId", grant.getUserId());
        if (grant.getRevokeReason() != null) {
            revokeDetails.put("reason", grant.getRevokeReason());
        }
        auditService.record(context, new AuditService.AuditEvent(
                "PERMISSION_REVOKED",
                "SECURITY",
                "TEMPORARY_PERMISSION",
                grant.getId(),
                null,
                "Revoked temporary permission " + grant.getPermissionCode(),
                revokeDetails));

        return toResponse(grant, location);
    }

    @Transactional(readOnly = true)
    public PageResponse<TemporaryPermissionResponse> list(Long userId, String status, int page, int size) {
        authorizationService.requireAnyPermission("permission:grant", "security:view");
        UserContext context = authorizationService.requireAuthenticated();

        Page<TemporaryPermission> result = temporaryPermissionRepository.search(
                context.businessId(),
                userId,
                normalizeStatus(status),
                PageRequest.of(Math.max(page, 0), Math.max(size, 1)));

        Map<Long, Location> locations = loadLocations(context.businessId(), result.map(TemporaryPermission::getLocationId).toList());

        List<TemporaryPermissionResponse> items = result.getContent().stream()
                .map(grant -> toResponse(grant, locations.get(grant.getLocationId())))
                .toList();

        return new PageResponse<>(items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public Set<Long> getActiveLocationIds(Long businessId, Long userId) {
        return temporaryPermissionRepository.findActiveForUser(businessId, userId, Instant.now()).stream()
                .map(TemporaryPermission::getLocationId)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public boolean hasActivePermission(
            Long businessId,
            Long userId,
            Long locationId,
            String permissionCode,
            String referenceType,
            Long referenceId) {
        return !temporaryPermissionRepository.findActiveGrants(
                businessId,
                userId,
                locationId,
                permissionCode,
                referenceType,
                referenceId,
                Instant.now()).isEmpty();
    }

    @Transactional(readOnly = true)
    public boolean hasActiveLocationAccess(Long businessId, Long userId, Long locationId) {
        return temporaryPermissionRepository.findActiveForUser(businessId, userId, Instant.now()).stream()
                .anyMatch(grant -> grant.getLocationId().equals(locationId));
    }

    public Instant defaultTaskExpiry() {
        return Instant.now().plus(7, ChronoUnit.DAYS);
    }

    private void validateUserInBusiness(Long businessId, Long userId) {
        if (!membershipRepository.existsByUserIdAndBusinessIdAndStatus(userId, businessId, "ACTIVE")) {
            throw new NotFoundException("User is not a member of this business");
        }
    }

    private TemporaryPermissionResponse toResponse(TemporaryPermission grant, Location location) {
        return new TemporaryPermissionResponse(
                grant.getId(),
                grant.getUserId(),
                grant.getPermissionCode(),
                grant.getLocationId(),
                location != null ? location.getCode() : null,
                location != null ? location.getName() : null,
                grant.getReferenceType(),
                grant.getReferenceId(),
                grant.getReason(),
                grant.getGrantedBy(),
                grant.getExpiresAt(),
                grant.getStatus(),
                grant.getRevokedAt(),
                grant.getRevokedBy(),
                grant.getRevokeReason(),
                grant.getCreatedAt());
    }

    private Map<Long, Location> loadLocations(Long businessId, Iterable<Long> locationIds) {
        return locationRepository.findAllById(
                        java.util.stream.StreamSupport.stream(locationIds.spliterator(), false).toList())
                .stream()
                .filter(location -> location.getBusinessId().equals(businessId))
                .collect(Collectors.toMap(Location::getId, Function.identity()));
    }

    private String normalizeReferenceType(String referenceType) {
        if (referenceType == null || referenceType.isBlank()) {
            return null;
        }
        return referenceType.trim().toUpperCase(Locale.ROOT);
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
