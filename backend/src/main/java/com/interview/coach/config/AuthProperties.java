package com.interview.coach.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "coach.auth")
public class AuthProperties {

    private String jwtSecret = "dev-change-this-secret-for-deploy";

    private int jwtExpireHours = 168;
}
