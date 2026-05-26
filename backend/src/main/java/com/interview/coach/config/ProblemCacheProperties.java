package com.interview.coach.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "coach.cache.problem")
public class ProblemCacheProperties {

    private boolean enabled = true;

    private Duration listTtl = Duration.ofMinutes(10);

    private Duration detailTtl = Duration.ofMinutes(30);

    private Duration templateTtl = Duration.ofMinutes(30);
}
