package com.interview.coach.integration.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.interview.coach.config.EmbeddingProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class OpenAiCompatibleEmbeddingClient implements EmbeddingClient {

    private final EmbeddingProperties properties;

    @Override
    public float[] embed(String text) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new AiClientException("EMBEDDING_API_KEY is required");
        }
        if (!StringUtils.hasText(text)) {
            throw new AiClientException("Embedding input text is empty");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("input", text);
        if (properties.getDimensions() > 0) {
            body.put("dimensions", properties.getDimensions());
        }

        try {
            JsonNode response = RestClient.builder()
                    .baseUrl(properties.getBaseUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .build()
                    .post()
                    .uri("/v1/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return parseEmbedding(response);
        } catch (RestClientException ex) {
            throw new AiClientException("Embedding request failed", ex);
        }
    }

    private float[] parseEmbedding(JsonNode response) {
        JsonNode embedding = response == null ? null : response.at("/data/0/embedding");
        if (embedding == null || !embedding.isArray() || embedding.isEmpty()) {
            throw new AiClientException("Embedding response data[0].embedding is missing");
        }
        float[] vector = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            vector[i] = (float) embedding.get(i).asDouble();
        }
        return vector;
    }
}
