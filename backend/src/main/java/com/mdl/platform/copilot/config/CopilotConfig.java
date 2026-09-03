package com.mdl.platform.copilot.config;

import com.mdl.platform.copilot.ai.AiCompletionClient;
import com.mdl.platform.copilot.ai.DataGroundedAiCompletionClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CopilotConfig {

    @Bean
    @ConditionalOnMissingBean(AiCompletionClient.class)
    AiCompletionClient dataGroundedAiCompletionClient(DataGroundedAiCompletionClient client) {
        return client;
    }
}
