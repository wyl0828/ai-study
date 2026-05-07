package com.interview.coach.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "coach.ai")
public class AiProperties {

    private String baseUrl = "https://api.anthropic.com";

    private String apiKey;

    private String model = "claude-3-5-sonnet-latest";

    private Integer maxTokens = 3000;

    private String anthropicVersion = "2023-06-01";
}
