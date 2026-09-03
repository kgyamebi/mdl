package com.mdl.platform.copilot.controller;

import com.mdl.platform.common.dto.ApiResponse;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.copilot.dto.CopilotChatRequest;
import com.mdl.platform.copilot.dto.CopilotChatResponse;
import com.mdl.platform.copilot.dto.CopilotConversationResponse;
import com.mdl.platform.copilot.dto.CopilotSuggestedPromptResponse;
import com.mdl.platform.copilot.service.CopilotChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/copilot")
public class CopilotController {

    private final CopilotChatService copilotChatService;

    public CopilotController(CopilotChatService copilotChatService) {
        this.copilotChatService = copilotChatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<CopilotChatResponse>> chat(@Valid @RequestBody CopilotChatRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(copilotChatService.chat(request)));
    }

    @GetMapping("/suggested-prompts")
    public ResponseEntity<ApiResponse<List<CopilotSuggestedPromptResponse>>> suggestedPrompts() {
        return ResponseEntity.ok(ApiResponse.ok(copilotChatService.suggestedPrompts()));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<PageResponse<CopilotConversationResponse>>> listConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(copilotChatService.listConversations(page, size)));
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<ApiResponse<CopilotConversationResponse>> getConversation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(copilotChatService.getConversation(id)));
    }
}
