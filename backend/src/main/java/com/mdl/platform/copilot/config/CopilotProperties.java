package com.mdl.platform.copilot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CopilotProperties {

    @Value("${app.copilot.provider:data-grounded}")
    private String provider;

    @Value("${app.copilot.openai.api-key:}")
    private String openAiApiKey;

    @Value("${app.copilot.openai.model:gpt-4o-mini}")
    private String openAiModel;

    @Value("${app.copilot.openai.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    @Value("${app.copilot.azure.enabled:false}")
    private boolean azureEnabled;

    @Value("${app.copilot.azure.endpoint:}")
    private String azureEndpoint;

    @Value("${app.copilot.azure.deployment:}")
    private String azureDeployment;

    @Value("${app.copilot.azure.api-key:}")
    private String azureApiKey;

    public String getProvider() {
        return provider;
    }

    public String getOpenAiApiKey() {
        return openAiApiKey;
    }

    public String getOpenAiModel() {
        return openAiModel;
    }

    public String getOpenAiBaseUrl() {
        return openAiBaseUrl;
    }

    public boolean isAzureEnabled() {
        return azureEnabled;
    }

    public String getAzureEndpoint() {
        return azureEndpoint;
    }

    public String getAzureDeployment() {
        return azureDeployment;
    }

    public String getAzureApiKey() {
        return azureApiKey;
    }

    public boolean isExternalLlmConfigured() {
        if (azureEnabled && azureEndpoint != null && !azureEndpoint.isBlank()
                && azureApiKey != null && !azureApiKey.isBlank()) {
            return true;
        }
        return openAiApiKey != null && !openAiApiKey.isBlank();
    }
}
