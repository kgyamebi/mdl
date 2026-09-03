package com.mdl.platform.copilot.service;

import com.mdl.platform.audit.service.AuditRecorder;
import com.mdl.platform.audit.service.AuditService;
import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.copilot.ai.AiCompletionClient;
import com.mdl.platform.copilot.ai.AiCompletionClient.AiCompletionRequest;
import com.mdl.platform.copilot.ai.AiCompletionClient.AiCompletionResult;
import com.mdl.platform.copilot.ai.AiCompletionClient.ChatTurn;
import com.mdl.platform.copilot.dto.CopilotChatRequest;
import com.mdl.platform.copilot.dto.CopilotChatResponse;
import com.mdl.platform.copilot.dto.CopilotConversationResponse;
import com.mdl.platform.copilot.dto.CopilotMessageResponse;
import com.mdl.platform.copilot.dto.CopilotSuggestedPromptResponse;
import com.mdl.platform.copilot.entity.CopilotConversation;
import com.mdl.platform.copilot.entity.CopilotMessage;
import com.mdl.platform.copilot.entity.CopilotUsageLog;
import com.mdl.platform.copilot.repository.CopilotConversationRepository;
import com.mdl.platform.copilot.repository.CopilotMessageRepository;
import com.mdl.platform.copilot.repository.CopilotUsageLogRepository;
import com.mdl.platform.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class CopilotChatService {

    private final AuthorizationService authorizationService;
    private final CopilotConversationRepository conversationRepository;
    private final CopilotMessageRepository messageRepository;
    private final CopilotUsageLogRepository usageLogRepository;
    private final CopilotDataService copilotDataService;
    private final CopilotSuggestedPromptService suggestedPromptService;
    private final AiCompletionClient aiCompletionClient;
    private final AuditRecorder auditRecorder;

    public CopilotChatService(
            AuthorizationService authorizationService,
            CopilotConversationRepository conversationRepository,
            CopilotMessageRepository messageRepository,
            CopilotUsageLogRepository usageLogRepository,
            CopilotDataService copilotDataService,
            CopilotSuggestedPromptService suggestedPromptService,
            AiCompletionClient aiCompletionClient,
            AuditRecorder auditRecorder) {
        this.authorizationService = authorizationService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.usageLogRepository = usageLogRepository;
        this.copilotDataService = copilotDataService;
        this.suggestedPromptService = suggestedPromptService;
        this.aiCompletionClient = aiCompletionClient;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public CopilotChatResponse chat(CopilotChatRequest request) {
        authorizationService.requirePermission("copilot:use");
        UserContext context = authorizationService.requireAuthenticated();

        CopilotConversation conversation = resolveConversation(context, request.conversationId(), request.message());

        CopilotMessage userMessage = persistMessage(
                conversation.getId(), "USER", request.message(), 0, 0, "user", null);

        List<ChatTurn> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                .filter(message -> !message.getId().equals(userMessage.getId()))
                .map(message -> new ChatTurn(message.getRole(), message.getContent()))
                .toList();

        String groundedContext = copilotDataService.buildGroundedContext(context, request.message());
        AiCompletionResult result = aiCompletionClient.complete(new AiCompletionRequest(
                context,
                request.message(),
                groundedContext,
                history));

        CopilotMessage assistantMessage = persistMessage(
                conversation.getId(),
                "ASSISTANT",
                result.reply(),
                result.promptTokens(),
                result.completionTokens(),
                result.provider(),
                result.model());

        logUsage(context, conversation.getId(), assistantMessage.getId(), result);
        auditRecorder.record(context, new AuditService.AuditEvent(
                "COPILOT_CHAT",
                "COPILOT",
                "CONVERSATION",
                conversation.getId(),
                "CONV-" + conversation.getId(),
                "Copilot answered user question",
                Map.of(
                        "provider", result.provider(),
                        "promptTokens", result.promptTokens(),
                        "completionTokens", result.completionTokens(),
                        "messagePreview", truncate(request.message(), 120))));

        return new CopilotChatResponse(
                conversation.getId(),
                result.reply(),
                result.provider(),
                result.model(),
                result.promptTokens(),
                result.completionTokens(),
                suggestedPromptService.followUps(context));
    }

    @Transactional(readOnly = true)
    public List<CopilotSuggestedPromptResponse> suggestedPrompts() {
        authorizationService.requirePermission("copilot:use");
        UserContext context = authorizationService.requireAuthenticated();
        return suggestedPromptService.suggestedPrompts(context);
    }

    @Transactional(readOnly = true)
    public PageResponse<CopilotConversationResponse> listConversations(int page, int size) {
        authorizationService.requirePermission("copilot:use");
        UserContext context = authorizationService.requireAuthenticated();

        Page<CopilotConversation> results = conversationRepository.findByBusinessIdAndUserIdOrderByUpdatedAtDesc(
                context.businessId(),
                context.userId(),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));

        List<CopilotConversationResponse> items = results.getContent().stream()
                .map(conversation -> new CopilotConversationResponse(
                        conversation.getId(),
                        conversation.getTitle(),
                        conversation.getCreatedAt(),
                        conversation.getUpdatedAt(),
                        List.of()))
                .toList();

        return new PageResponse<>(
                items,
                results.getNumber(),
                results.getSize(),
                results.getTotalElements(),
                results.getTotalPages());
    }

    @Transactional(readOnly = true)
    public CopilotConversationResponse getConversation(Long conversationId) {
        authorizationService.requirePermission("copilot:use");
        UserContext context = authorizationService.requireAuthenticated();

        CopilotConversation conversation = conversationRepository
                .findByIdAndBusinessIdAndUserId(conversationId, context.businessId(), context.userId())
                .orElseThrow(() -> new NotFoundException("Conversation not found"));

        List<CopilotMessageResponse> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                .map(message -> new CopilotMessageResponse(
                        message.getId(),
                        message.getRole(),
                        message.getContent(),
                        message.getCreatedAt()))
                .toList();

        return new CopilotConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messages);
    }

    private CopilotConversation resolveConversation(UserContext context, Long conversationId, String message) {
        if (conversationId != null) {
            return conversationRepository
                    .findByIdAndBusinessIdAndUserId(conversationId, context.businessId(), context.userId())
                    .orElseThrow(() -> new NotFoundException("Conversation not found"));
        }

        CopilotConversation conversation = new CopilotConversation();
        conversation.setBusinessId(context.businessId());
        conversation.setUserId(context.userId());
        conversation.setTitle(truncate(message, 120));
        return conversationRepository.save(conversation);
    }

    private CopilotMessage persistMessage(
            Long conversationId,
            String role,
            String content,
            int promptTokens,
            int completionTokens,
            String provider,
            String model) {
        CopilotMessage message = new CopilotMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setPromptTokens(promptTokens);
        message.setCompletionTokens(completionTokens);
        message.setProvider(provider);
        message.setModel(model);
        return messageRepository.save(message);
    }

    private void logUsage(UserContext context, Long conversationId, Long messageId, AiCompletionResult result) {
        CopilotUsageLog log = new CopilotUsageLog();
        log.setBusinessId(context.businessId());
        log.setUserId(context.userId());
        log.setConversationId(conversationId);
        log.setMessageId(messageId);
        log.setProvider(result.provider());
        log.setModel(result.model());
        log.setPromptTokens(result.promptTokens());
        log.setCompletionTokens(result.completionTokens());
        usageLogRepository.save(log);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength - 1) + "…";
    }
}
