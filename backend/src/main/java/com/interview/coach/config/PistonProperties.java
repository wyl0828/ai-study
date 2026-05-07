package com.interview.coach.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "piston")
public class PistonProperties {

    private String baseUrl = "https://emkc.org/api/v2/piston";

    private String apiKey;

    private String javaVersion = "*";

    private Long compileTimeoutMs = 10000L;

    private Long runTimeoutMs = 3000L;
}
