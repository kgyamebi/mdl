package com.mdl.platform.approvals.service;

import com.mdl.platform.approvals.dto.ApprovalInstanceResponse;
import com.mdl.platform.approvals.dto.ApprovalInstanceResponse.ApprovalInstanceActionResponse;
import com.mdl.platform.approvals.dto.ApprovalStepActionResult;
import com.mdl.platform.approvals.entity.ApprovalInstance;
import com.mdl.platform.approvals.entity.ApprovalInstanceAction;
import com.mdl.platform.approvals.entity.ApprovalRule;
import com.mdl.platform.approvals.entity.ApprovalRuleStep;
import com.mdl.platform.approvals.repository.ApprovalInstanceActionRepository;
import com.mdl.platform.approvals.repository.ApprovalInstanceRepository;
import com.mdl.platform.approvals.repository.ApprovalRuleRepository;
import com.mdl.platform.approvals.repository.ApprovalRuleStepRepository;
import com.mdl.platform.audit.service.AuditRecorder;
import com.mdl.platform.audit.service.AuditService;
import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.common.exception.ConflictException;
import com.mdl.platform.common.exception.ForbiddenException;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ApprovalWorkflowService {

    private static final Set<String> ENTITY_TYPES = Set.of(
            "INVENTORY_ADJUSTMENT",
            "STOCK_TRANSFER",
            "IMPORT_ORDER",
            "STOCKTAKE");

    private final AuthorizationService authorizationService;
    private final ApprovalRuleService approvalRuleService;
    private final ApprovalRuleRepository ruleRepository;
    private final ApprovalRuleStepRepository stepRepository;
    private final ApprovalInstanceRepository instanceRepository;
    private final ApprovalInstanceActionRepository actionRepository;
    private final AuditRecorder auditRecorder;

    public ApprovalWorkflowService(
            AuthorizationService authorizationService,
            ApprovalRuleService approvalRuleService,
            ApprovalRuleRepository ruleRepository,
            ApprovalRuleStepRepository stepRepository,
            ApprovalInstanceRepository instanceRepository,
            ApprovalInstanceActionRepository actionRepository,
            AuditRecorder auditRecorder) {
        this.authorizationService = authorizationService;
        this.approvalRuleService = approvalRuleService;
        this.ruleRepository = ruleRepository;
        this.stepRepository = stepRepository;
        this.instanceRepository = instanceRepository;
        this.actionRepository = actionRepository;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public void startWorkflow(UserContext context, String entityType, Long entityId, BigDecimal routingQuantity) {
        String normalizedType = normalizeEntityType(entityType);
        if (instanceRepository.findByBusinessIdAndEntityTypeAndEntityId(
                context.businessId(), normalizedType, entityId).isPresent()) {
            throw new ConflictException("Approval workflow already exists for this item");
        }

        ApprovalRule rule = approvalRuleService
                .resolveMatchingRule(context.businessId(), normalizedType, routingQuantity)
                .orElse(null);
        if (rule == null) {
            return;
        }

        List<ApprovalRuleStep> steps = loadSteps(rule);
        if (steps.isEmpty()) {
            return;
        }

        ApprovalInstance instance = new ApprovalInstance();
        instance.setBusinessId(context.businessId());
        instance.setApprovalRuleId(rule.getId());
        instance.setEntityType(normalizedType);
        instance.setEntityId(entityId);
        instance.setStatus("PENDING");
        instance.setCurrentStepOrder(1);
        instance.setTotalSteps(countDistinctStepOrders(steps));
        instance.setSubmittedBy(context.userId());
        instance.setSubmittedAt(Instant.now());
        instanceRepository.save(instance);
    }

    @Transactional
    public ApprovalStepActionResult approveCurrentStep(UserContext context, String entityType, Long entityId, String notes) {
        ApprovalInstance instance = requirePendingInstance(context, entityType, entityId);
        List<ApprovalRuleStep> ruleSteps = loadRuleSteps(instance.getApprovalRuleId());
        List<ApprovalRuleStep> currentSteps = stepsAtOrder(ruleSteps, instance.getCurrentStepOrder());
        ApprovalRuleStep matchedStep = requireMatchingStep(context, currentSteps);

        recordAction(instance.getId(), matchedStep.getStepOrder(), "APPROVED", context.userId(), notes);

        int maxStepOrder = maxStepOrder(ruleSteps);
        if (instance.getCurrentStepOrder() >= maxStepOrder) {
            instance.setStatus("APPROVED");
            instance.setCompletedAt(Instant.now());
            instanceRepository.save(instance);
            auditWorkflow(context, instance, "WORKFLOW_APPROVED", "Approval workflow completed");
            return new ApprovalStepActionResult(true, false, instance.getCurrentStepOrder(), instance.getTotalSteps());
        }

        instance.setCurrentStepOrder(instance.getCurrentStepOrder() + 1);
        instanceRepository.save(instance);
        auditWorkflow(context, instance, "WORKFLOW_STEP_APPROVED",
                "Approved step " + matchedStep.getStepOrder() + " of " + instance.getTotalSteps());
        return new ApprovalStepActionResult(false, false, instance.getCurrentStepOrder(), instance.getTotalSteps());
    }

    @Transactional
    public ApprovalStepActionResult rejectCurrentStep(UserContext context, String entityType, Long entityId, String notes) {
        ApprovalInstance instance = requirePendingInstance(context, entityType, entityId);
        List<ApprovalRuleStep> currentSteps = stepsAtOrder(
                loadRuleSteps(instance.getApprovalRuleId()), instance.getCurrentStepOrder());
        ApprovalRuleStep matchedStep = requireMatchingStep(context, currentSteps);

        recordAction(instance.getId(), matchedStep.getStepOrder(), "REJECTED", context.userId(), notes);

        instance.setStatus("REJECTED");
        instance.setCompletedAt(Instant.now());
        instanceRepository.save(instance);
        auditWorkflow(context, instance, "WORKFLOW_REJECTED",
                "Approval workflow rejected at step " + matchedStep.getStepOrder());
        return new ApprovalStepActionResult(true, true, matchedStep.getStepOrder(), instance.getTotalSteps());
    }

    @Transactional(readOnly = true)
    public ApprovalInstanceResponse getInstance(UserContext context, String entityType, Long entityId) {
        authorizationService.requireAnyPermission("approval:view", "approval:manage");
        String normalizedType = normalizeEntityType(entityType);

        ApprovalInstance instance = instanceRepository
                .findByBusinessIdAndEntityTypeAndEntityId(context.businessId(), normalizedType, entityId)
                .orElseThrow(() -> new NotFoundException("Approval workflow not found"));

        return toResponse(instance);
    }

    @Transactional(readOnly = true)
    public boolean canUserActOnCurrentStep(UserContext context, String entityType, Long entityId) {
        return instanceRepository
                .findByBusinessIdAndEntityTypeAndEntityId(
                        context.businessId(), normalizeEntityType(entityType), entityId)
                .filter(instance -> "PENDING".equals(instance.getStatus()))
                .map(instance -> {
                    List<ApprovalRuleStep> currentSteps = stepsAtOrder(
                            loadRuleSteps(instance.getApprovalRuleId()), instance.getCurrentStepOrder());
                    return currentSteps.stream()
                            .anyMatch(step -> context.permissions().contains(step.getRequiredPermission()));
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public String currentStepPermission(Long businessId, String entityType, Long entityId) {
        CurrentStepInfo info = resolveCurrentStepInfo(businessId, entityType, entityId);
        return info != null ? info.requiredPermission() : null;
    }

    @Transactional(readOnly = true)
    public CurrentStepInfo resolveCurrentStepInfo(Long businessId, String entityType, Long entityId) {
        return instanceRepository
                .findByBusinessIdAndEntityTypeAndEntityId(businessId, normalizeEntityType(entityType), entityId)
                .filter(instance -> "PENDING".equals(instance.getStatus()))
                .map(instance -> {
                    List<ApprovalRuleStep> currentSteps = stepsAtOrder(
                            loadRuleSteps(instance.getApprovalRuleId()), instance.getCurrentStepOrder());
                    if (currentSteps.isEmpty()) {
                        throw new NotFoundException("Approval step not found");
                    }
                    List<String> permissions = currentSteps.stream()
                            .map(ApprovalRuleStep::getRequiredPermission)
                            .toList();
                    return new CurrentStepInfo(
                            instance.getCurrentStepOrder(),
                            instance.getTotalSteps(),
                            stepGroupName(currentSteps),
                            permissions.getFirst(),
                            permissions,
                            currentSteps.size() > 1);
                })
                .orElse(null);
    }

    private ApprovalInstance requirePendingInstance(UserContext context, String entityType, Long entityId) {
        ApprovalInstance instance = instanceRepository
                .findByBusinessIdAndEntityTypeAndEntityId(
                        context.businessId(), normalizeEntityType(entityType), entityId)
                .orElseThrow(() -> new NotFoundException("Approval workflow not found"));
        if (!"PENDING".equals(instance.getStatus())) {
            throw new ConflictException("Approval workflow is not pending");
        }
        return instance;
    }

    private ApprovalRuleStep requireMatchingStep(UserContext context, List<ApprovalRuleStep> currentSteps) {
        if (currentSteps.isEmpty()) {
            throw new NotFoundException("Approval step not found");
        }
        return currentSteps.stream()
                .filter(step -> context.permissions().contains(step.getRequiredPermission()))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("You do not have permission to action this approval step"));
    }

    private void recordAction(Long instanceId, int stepOrder, String action, Long actedBy, String notes) {
        ApprovalInstanceAction entry = new ApprovalInstanceAction();
        entry.setApprovalInstanceId(instanceId);
        entry.setStepOrder(stepOrder);
        entry.setAction(action);
        entry.setActedBy(actedBy);
        entry.setNotes(trimToNull(notes));
        actionRepository.save(entry);
    }

    private List<ApprovalRuleStep> loadSteps(ApprovalRule rule) {
        List<ApprovalRuleStep> steps = stepRepository.findByApprovalRuleIdOrderByStepOrderAsc(rule.getId());
        if (!steps.isEmpty()) {
            return steps;
        }
        ApprovalRuleStep fallback = new ApprovalRuleStep();
        fallback.setApprovalRuleId(rule.getId());
        fallback.setStepOrder(1);
        fallback.setName(rule.getName());
        fallback.setRequiredPermission(rule.getRequiredPermission());
        return List.of(fallback);
    }

    private List<ApprovalRuleStep> loadRuleSteps(Long ruleId) {
        return stepRepository.findByApprovalRuleIdOrderByStepOrderAsc(ruleId);
    }

    private List<ApprovalRuleStep> stepsAtOrder(List<ApprovalRuleStep> steps, int stepOrder) {
        return steps.stream()
                .filter(step -> step.getStepOrder() == stepOrder)
                .toList();
    }

    private int countDistinctStepOrders(List<ApprovalRuleStep> steps) {
        return (int) steps.stream()
                .mapToInt(ApprovalRuleStep::getStepOrder)
                .distinct()
                .count();
    }

    private int maxStepOrder(List<ApprovalRuleStep> steps) {
        return steps.stream()
                .mapToInt(ApprovalRuleStep::getStepOrder)
                .max()
                .orElse(1);
    }

    private String stepGroupName(List<ApprovalRuleStep> steps) {
        if (steps.size() == 1) {
            return steps.getFirst().getName();
        }
        return steps.getFirst().getName() + " (any approver)";
    }

    private ApprovalInstanceResponse toResponse(ApprovalInstance instance) {
        ApprovalRule rule = ruleRepository.findById(instance.getApprovalRuleId()).orElse(null);
        List<ApprovalRuleStep> currentSteps = "PENDING".equals(instance.getStatus())
                ? stepsAtOrder(loadRuleSteps(instance.getApprovalRuleId()), instance.getCurrentStepOrder())
                : List.of();
        List<String> currentPermissions = currentSteps.stream()
                .map(ApprovalRuleStep::getRequiredPermission)
                .toList();
        List<ApprovalInstanceActionResponse> actions = actionRepository
                .findByApprovalInstanceIdOrderByActedAtAsc(instance.getId())
                .stream()
                .map(action -> new ApprovalInstanceActionResponse(
                        action.getStepOrder(),
                        action.getAction(),
                        action.getActedBy(),
                        action.getNotes(),
                        action.getActedAt()))
                .toList();

        return new ApprovalInstanceResponse(
                instance.getId(),
                instance.getEntityType(),
                instance.getEntityId(),
                instance.getStatus(),
                instance.getCurrentStepOrder(),
                instance.getTotalSteps(),
                currentSteps.isEmpty() ? null : stepGroupName(currentSteps),
                currentPermissions.isEmpty() ? null : currentPermissions.getFirst(),
                currentPermissions,
                currentSteps.size() > 1,
                instance.getApprovalRuleId(),
                rule != null ? rule.getCode() : null,
                instance.getSubmittedBy(),
                instance.getSubmittedAt(),
                instance.getCompletedAt(),
                actions);
    }

    private void auditWorkflow(UserContext context, ApprovalInstance instance, String action, String summary) {
        auditRecorder.record(context, new AuditService.AuditEvent(
                action,
                "APPROVALS",
                instance.getEntityType(),
                instance.getEntityId(),
                String.valueOf(instance.getId()),
                summary,
                Map.of(
                        "currentStep", instance.getCurrentStepOrder(),
                        "totalSteps", instance.getTotalSteps(),
                        "status", instance.getStatus())));
    }

    private String normalizeEntityType(String entityType) {
        String normalized = entityType.trim().toUpperCase(Locale.ROOT);
        if (!ENTITY_TYPES.contains(normalized)) {
            throw new ConflictException("Unsupported entity type: " + entityType);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record CurrentStepInfo(
            int currentStepOrder,
            int totalSteps,
            String stepName,
            String requiredPermission,
            List<String> requiredPermissions,
            boolean parallelStep
    ) {
    }
}
