package com.interview.coach.integration.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.coach.config.AiProperties;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class AnthropicCompatibleClient {

    private final AiProperties properties;

    private final ObjectMapper objectMapper;

    public <T> T askJson(String systemPrompt, String userPrompt, Class<T> responseType) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new AiClientException("AI_API_KEY is required");
        }

        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "max_tokens", properties.getMaxTokens(),
                "system", systemPrompt,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", userPrompt)));

        try {
            JsonNode response = RestClient.builder()
                    .baseUrl(properties.getBaseUrl())
                    .defaultHeader("x-api-key", properties.getApiKey())
                    .defaultHeader("anthropic-version", properties.getAnthropicVersion())
                    .build()
                    .post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String text = firstTextBlock(response);
            return objectMapper.readValue(extractJson(text), responseType);
        } catch (RestClientException ex) {
            throw new AiClientException("AI request failed", ex);
        } catch (Exception ex) {
            throw new AiClientException("AI JSON parsing failed", ex);
        }
    }

    static String extractJson(String text) {
        if (!StringUtils.hasText(text)) {
            throw new AiClientException("AI response text is empty");
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new AiClientException("AI response does not contain a JSON object");
        }
        return text.substring(start, end + 1);
    }

    private static String firstTextBlock(JsonNode response) {
        if (response == null || !response.has("content") || !response.get("content").isArray()) {
            throw new AiClientException("AI response content is missing");
        }
        for (JsonNode block : response.get("content")) {
            if (block.has("text") && StringUtils.hasText(block.get("text").asText())) {
                return block.get("text").asText();
            }
        }
        throw new AiClientException("AI response text block is missing");
    }
}
