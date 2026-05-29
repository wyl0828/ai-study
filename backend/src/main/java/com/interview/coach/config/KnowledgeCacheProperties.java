package com.interview.coach.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "coach.cache.knowledge")
public class KnowledgeCacheProperties {

    private boolean enabled = true;

    private Duration categoryTtl = Duration.ofMinutes(30);

    private Duration listTtl = Duration.ofMinutes(30);

    private Duration detailTtl = Duration.ofHours(2);
}
