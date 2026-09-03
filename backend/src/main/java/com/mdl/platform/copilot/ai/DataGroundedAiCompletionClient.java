package com.mdl.platform.copilot.ai;

import org.springframework.stereotype.Component;

@Component
public class DataGroundedAiCompletionClient implements AiCompletionClient {

    private final com.mdl.platform.copilot.service.CopilotDataService copilotDataService;

    public DataGroundedAiCompletionClient(com.mdl.platform.copilot.service.CopilotDataService copilotDataService) {
        this.copilotDataService = copilotDataService;
    }

    @Override
    public AiCompletionResult complete(AiCompletionRequest request) {
        String reply = copilotDataService.answer(request.user(), request.userMessage());
        int promptTokens = estimateTokens(request.userMessage() + request.groundedContext());
        int completionTokens = estimateTokens(reply);
        return new AiCompletionResult(reply, promptTokens, completionTokens, "data-grounded", "data-grounded");
    }

    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, text.length() / 4);
    }
}
