package com.mdl.platform.copilot.service;

import com.mdl.platform.copilot.dto.CopilotSuggestedPromptResponse;
import com.mdl.platform.security.UserContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CopilotSuggestedPromptService {

    public List<CopilotSuggestedPromptResponse> suggestedPrompts(UserContext context) {
        List<CopilotSuggestedPromptResponse> prompts = new ArrayList<>();

        if (hasPermission(context, "inventory:view")) {
            prompts.add(new CopilotSuggestedPromptResponse("Which products are low on stock?", "inventory"));
            prompts.add(new CopilotSuggestedPromptResponse("What inventory needs replenishment today?", "inventory"));
            prompts.add(new CopilotSuggestedPromptResponse("Show me inventory in Main Warehouse.", "inventory"));
            prompts.add(new CopilotSuggestedPromptResponse("Generate a stock summary.", "inventory"));
        }

        if (hasPermission(context, "approval:view")) {
            prompts.add(new CopilotSuggestedPromptResponse("Which transfers are awaiting approval?", "approvals"));
        }

        if (hasPermission(context, "import:view")) {
            prompts.add(new CopilotSuggestedPromptResponse("What imports are pending?", "imports"));
        }

        if (hasPermission(context, "sale:view") || hasPermission(context, "report:view")) {
            prompts.add(new CopilotSuggestedPromptResponse("What were today's sales?", "sales"));
        }

        if (hasPermission(context, "report:view")) {
            prompts.add(new CopilotSuggestedPromptResponse("Which products are selling fastest?", "sales"));
        }

        prompts.add(new CopilotSuggestedPromptResponse("Summarize pending tasks.", "tasks"));

        return prompts;
    }

    public List<String> followUps(UserContext context) {
        return suggestedPrompts(context).stream()
                .map(CopilotSuggestedPromptResponse::prompt)
                .limit(3)
                .toList();
    }

    private boolean hasPermission(UserContext context, String permission) {
        return context.permissions().contains(permission);
    }
}
