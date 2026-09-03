package com.mdl.platform.copilot.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdl.platform.copilot.config.CopilotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Optional OpenAI-compatible client. Falls back to data-grounded answers when the API is unavailable.
 */
@Component
@Primary
@ConditionalOnProperty(name = "app.copilot.provider", havingValue = "openai")
public class OpenAiCompletionClient implements AiCompletionClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompletionClient.class);

    private final CopilotProperties properties;
    private final DataGroundedAiCompletionClient fallbackClient;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public OpenAiCompletionClient(
            CopilotProperties properties,
            DataGroundedAiCompletionClient fallbackClient,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.fallbackClient = fallbackClient;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public AiCompletionResult complete(AiCompletionRequest request) {
        if (!properties.isExternalLlmConfigured()) {
            return fallbackClient.complete(request);
        }

        try {
            return callOpenAi(request);
        } catch (Exception ex) {
            log.warn("OpenAI completion failed, falling back to data-grounded response: {}", ex.getMessage());
            return fallbackClient.complete(request);
        }
    }

    private AiCompletionResult callOpenAi(AiCompletionRequest request) throws Exception {
        String url = properties.getOpenAiBaseUrl().replaceAll("/$", "") + "/chat/completions";
        String model = properties.getOpenAiModel();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getOpenAiApiKey());

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", """
                        You are MDL Copilot, a concise inventory and business operations assistant.
                        Use only the grounded business context provided. Respect permission boundaries.
                        Never invent data. Keep answers short, actionable, and professional."""));
        messages.add(Map.of("role", "system", "content", request.groundedContext()));
        for (ChatTurn turn : request.history()) {
            messages.add(Map.of("role", turn.role().toLowerCase(), "content", turn.content()));
        }
        messages.add(Map.of("role", "user", "content", request.userMessage()));

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.2);

        ResponseEntity<String> response = restTemplate.postForEntity(
                url, new HttpEntity<>(body, headers), String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        String reply = root.path("choices").path(0).path("message").path("content").asText();
        int promptTokens = root.path("usage").path("prompt_tokens").asInt(0);
        int completionTokens = root.path("usage").path("completion_tokens").asInt(0);

        return new AiCompletionResult(reply, promptTokens, completionTokens, model, "openai");
    }
}
