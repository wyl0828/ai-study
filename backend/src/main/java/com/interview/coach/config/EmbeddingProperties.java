package com.interview.coach.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "coach.ai.embedding")
public class EmbeddingProperties {

    private String baseUrl = "https://api.openai.com";

    private String apiKey;

    private String model = "text-embedding-3-small";

    private int dimensions = 1536;
}
