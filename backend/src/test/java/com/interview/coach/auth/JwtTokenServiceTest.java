package com.interview.coach.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.interview.coach.config.AuthProperties;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    @Test
    void createAndVerifyToken() {
        AuthProperties properties = new AuthProperties();
        properties.setJwtSecret("unit-test-secret");
        properties.setJwtExpireHours(1);
        JwtTokenService service = new JwtTokenService(properties);

        String token = service.createToken(7L, "tester");

        JwtTokenService.TokenClaims claims = service.verify(token);
        assertThat(claims).isNotNull();
        assertThat(claims.userId()).isEqualTo(7L);
        assertThat(claims.username()).isEqualTo("tester");
    }

    @Test
    void rejectsTamperedToken() {
        AuthProperties properties = new AuthProperties();
        properties.setJwtSecret("unit-test-secret");
        properties.setJwtExpireHours(1);
        JwtTokenService service = new JwtTokenService(properties);

        String token = service.createToken(7L, "tester") + "x";

        assertThat(service.verify(token)).isNull();
    }
}
