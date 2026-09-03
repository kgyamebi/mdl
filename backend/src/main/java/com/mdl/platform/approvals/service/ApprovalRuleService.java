package com.mdl.platform.approvals.service;

import com.mdl.platform.approvals.dto.ApprovalRuleResponse;
import com.mdl.platform.approvals.dto.ApprovalRuleStepRequest;
import com.mdl.platform.approvals.dto.ApprovalRuleStepResponse;
import com.mdl.platform.approvals.dto.CreateApprovalRuleRequest;
import com.mdl.platform.approvals.dto.UpdateApprovalRuleRequest;
import com.mdl.platform.approvals.entity.ApprovalRule;
import com.mdl.platform.approvals.entity.ApprovalRuleStep;
import com.mdl.platform.approvals.repository.ApprovalRuleRepository;
import com.mdl.platform.approvals.repository.ApprovalRuleStepRepository;
import com.mdl.platform.audit.service.AuditRecorder;
import com.mdl.platform.audit.service.AuditService;
import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.common.exception.ConflictException;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ApprovalRuleService {

    private static final Set<String> ENTITY_TYPES = Set.of(
            "INVENTORY_ADJUSTMENT",
            "STOCK_TRANSFER",
            "IMPORT_ORDER",
            "STOCKTAKE");

    private final AuthorizationService authorizationService;
    private final ApprovalRuleRepository ruleRepository;
    private final ApprovalRuleStepRepository stepRepository;
    private final AuditRecorder auditRecorder;

    public ApprovalRuleService(
            AuthorizationService authorizationService,
            ApprovalRuleRepository ruleRepository,
            ApprovalRuleStepRepository stepRepository,
            AuditRecorder auditRecorder) {
        this.authorizationService = authorizationService;
        this.ruleRepository = ruleRepository;
        this.stepRepository = stepRepository;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public List<ApprovalRuleResponse> listRules() {
        authorizationService.requireAnyPermission("approval:view", "approval:manage");
        UserContext context = authorizationService.requireAuthenticated();

        return ruleRepository.findByBusinessIdOrderByEntityTypeAscPriorityAsc(context.businessId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApprovalRuleResponse getRule(Long id) {
        authorizationService.requireAnyPermission("approval:view", "approval:manage");
        UserContext context = authorizationService.requireAuthenticated();

        return toResponse(requireRule(context.businessId(), id));
    }

    @Transactional
    public ApprovalRuleResponse createRule(CreateApprovalRuleRequest request) {
        authorizationService.requirePermission("approval:manage");
        UserContext context = authorizationService.requireAuthenticated();

        String code = normalizeCode(request.code());
        String entityType = normalizeEntityType(request.entityType());
        if (ruleRepository.existsByBusinessIdAndCode(context.businessId(), code)) {
            throw new ConflictException("Approval rule code already exists: " + code);
        }

        ApprovalRule rule = new ApprovalRule();
        rule.setBusinessId(context.businessId());
        rule.setCode(code);
        rule.setName(request.name().trim());
        rule.setDescription(trimToNull(request.description()));
        rule.setEntityType(entityType);
        rule.setRequiredPermission(request.requiredPermission().trim());
        rule.setMinAbsQuantity(normalizeMinAbsQuantity(request.minAbsQuantity()));
        rule.setEnabled(request.enabled() != null ? request.enabled() : Boolean.TRUE);
        rule.setPriority(request.priority() != null ? request.priority() : 100);

        rule = ruleRepository.save(rule);

        ApprovalRuleStep defaultStep = new ApprovalRuleStep();
        defaultStep.setApprovalRuleId(rule.getId());
        defaultStep.setStepOrder(1);
        defaultStep.setName(rule.getName() + " — step 1");
        defaultStep.setRequiredPermission(rule.getRequiredPermission());
        stepRepository.save(defaultStep);

        auditRecorder.record(context, new AuditService.AuditEvent(
                "RULE_CREATED",
                "APPROVALS",
                "APPROVAL_RULE",
                rule.getId(),
                rule.getCode(),
                "Created approval rule " + rule.getCode(),
                Map.of(
                        "entityType", rule.getEntityType(),
                        "requiredPermission", rule.getRequiredPermission(),
                        "enabled", rule.isEnabled())));

        return toResponse(rule);
    }

    @Transactional
    public ApprovalRuleResponse updateRule(Long id, UpdateApprovalRuleRequest request) {
        authorizationService.requirePermission("approval:manage");
        UserContext context = authorizationService.requireAuthenticated();

        ApprovalRule rule = requireRule(context.businessId(), id);

        if (request.name() != null) {
            rule.setName(request.name().trim());
        }
        if (request.description() != null) {
            rule.setDescription(trimToNull(request.description()));
        }
        if (request.requiredPermission() != null) {
            rule.setRequiredPermission(request.requiredPermission().trim());
        }
        if (request.minAbsQuantity() != null) {
            rule.setMinAbsQuantity(normalizeMinAbsQuantity(request.minAbsQuantity()));
        }
        if (request.enabled() != null) {
            rule.setEnabled(request.enabled());
        }
        if (request.priority() != null) {
            rule.setPriority(request.priority());
        }

        rule = ruleRepository.save(rule);

        auditRecorder.record(context, new AuditService.AuditEvent(
                "RULE_UPDATED",
                "APPROVALS",
                "APPROVAL_RULE",
                rule.getId(),
                rule.getCode(),
                "Updated approval rule " + rule.getCode(),
                Map.of(
                        "entityType", rule.getEntityType(),
                        "requiredPermission", rule.getRequiredPermission(),
                        "enabled", rule.isEnabled())));

        return toResponse(rule);
    }

    @Transactional
    public ApprovalRuleResponse replaceSteps(Long ruleId, List<ApprovalRuleStepRequest> steps) {
        authorizationService.requirePermission("approval:manage");
        UserContext context = authorizationService.requireAuthenticated();

        ApprovalRule rule = requireRule(context.businessId(), ruleId);
        if (steps == null || steps.isEmpty()) {
            throw new ConflictException("At least one approval step is required");
        }

        validateSteps(steps);
        stepRepository.deleteByApprovalRuleId(rule.getId());

        for (ApprovalRuleStepRequest stepRequest : steps) {
            ApprovalRuleStep step = new ApprovalRuleStep();
            step.setApprovalRuleId(rule.getId());
            step.setStepOrder(stepRequest.stepOrder());
            step.setName(stepRequest.name().trim());
            step.setRequiredPermission(stepRequest.requiredPermission().trim());
            stepRepository.save(step);
        }

        auditRecorder.record(context, new AuditService.AuditEvent(
                "RULE_STEPS_UPDATED",
                "APPROVALS",
                "APPROVAL_RULE",
                rule.getId(),
                rule.getCode(),
                "Updated approval steps for rule " + rule.getCode(),
                Map.of("stepCount", steps.size())));

        return toResponse(rule);
    }

    @Transactional(readOnly = true)
    public List<ApprovalRuleStepResponse> listSteps(Long ruleId) {
        authorizationService.requireAnyPermission("approval:view", "approval:manage");
        UserContext context = authorizationService.requireAuthenticated();
        ApprovalRule rule = requireRule(context.businessId(), ruleId);
        return loadStepResponses(rule.getId());
    }

    @Transactional(readOnly = true)
    public Optional<ApprovalRule> resolveMatchingRule(Long businessId, String entityType, BigDecimal absQuantity) {
        return ruleRepository
                .findByBusinessIdAndEntityTypeAndEnabledTrueOrderByPriorityAsc(
                        businessId, normalizeEntityType(entityType))
                .stream()
                .filter(rule -> matchesThreshold(rule, absQuantity))
                .findFirst();
    }

    @Transactional(readOnly = true)
    public String resolveRequiredPermission(Long businessId, String entityType, BigDecimal absQuantity) {
        return resolveMatchingRule(businessId, entityType, absQuantity)
                .map(rule -> loadStepResponses(rule.getId()).stream()
                        .findFirst()
                        .map(ApprovalRuleStepResponse::requiredPermission)
                        .orElse(rule.getRequiredPermission()))
                .orElse(defaultPermission(entityType));
    }

    @Transactional(readOnly = true)
    public String resolveRequiredPermission(Long businessId, String entityType) {
        return resolveRequiredPermission(businessId, entityType, null);
    }

    private ApprovalRule requireRule(Long businessId, Long id) {
        return ruleRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new NotFoundException("Approval rule not found"));
    }

    private String defaultPermission(String entityType) {
        return switch (normalizeEntityType(entityType)) {
            case "INVENTORY_ADJUSTMENT", "STOCKTAKE" -> "inventory:adjust";
            case "STOCK_TRANSFER" -> "transfer:approve";
            case "IMPORT_ORDER" -> "import:approve";
            default -> throw new ConflictException("Unsupported entity type: " + entityType);
        };
    }

    private String normalizeEntityType(String entityType) {
        String normalized = entityType.trim().toUpperCase(Locale.ROOT);
        if (!ENTITY_TYPES.contains(normalized)) {
            throw new ConflictException("Unsupported entity type: " + entityType);
        }
        return normalized;
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal normalizeMinAbsQuantity(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ConflictException("Minimum quantity threshold cannot be negative");
        }
        return value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ApprovalRuleResponse toResponse(ApprovalRule rule) {
        return new ApprovalRuleResponse(
                rule.getId(),
                rule.getCode(),
                rule.getName(),
                rule.getDescription(),
                rule.getEntityType(),
                rule.getRequiredPermission(),
                rule.getMinAbsQuantity(),
                rule.isEnabled(),
                rule.getPriority(),
                loadStepResponses(rule.getId()),
                rule.getCreatedAt(),
                rule.getUpdatedAt());
    }

    private List<ApprovalRuleStepResponse> loadStepResponses(Long ruleId) {
        return stepRepository.findByApprovalRuleIdOrderByStepOrderAsc(ruleId).stream()
                .map(step -> new ApprovalRuleStepResponse(
                        step.getId(),
                        step.getStepOrder(),
                        step.getName(),
                        step.getRequiredPermission()))
                .toList();
    }

    private void validateSteps(List<ApprovalRuleStepRequest> steps) {
        java.util.Set<String> keys = new java.util.HashSet<>();
        java.util.Set<Integer> orders = new java.util.HashSet<>();
        for (ApprovalRuleStepRequest step : steps) {
            String permission = step.requiredPermission().trim();
            String key = step.stepOrder() + "|" + permission;
            if (!keys.add(key)) {
                throw new ConflictException(
                        "Duplicate step order and permission at step " + step.stepOrder());
            }
            orders.add(step.stepOrder());
        }
        int maxOrder = orders.stream().max(Integer::compareTo).orElse(0);
        for (int order = 1; order <= maxOrder; order++) {
            if (!orders.contains(order)) {
                throw new ConflictException("Step orders must be consecutive starting at 1; missing order " + order);
            }
        }
    }

    private boolean matchesThreshold(ApprovalRule rule, BigDecimal absQuantity) {
        if (rule.getMinAbsQuantity() == null) {
            return true;
        }
        if (absQuantity == null) {
            return false;
        }
        return absQuantity.abs().compareTo(rule.getMinAbsQuantity()) >= 0;
    }
}
