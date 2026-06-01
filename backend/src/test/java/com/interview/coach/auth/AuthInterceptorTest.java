package com.interview.coach.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.interview.coach.config.AuthProperties;
import com.interview.coach.handler.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

class AuthInterceptorTest {

    @Test
    void acceptsValidBearerToken() {
        AuthProperties properties = new AuthProperties();
        properties.setJwtSecret("unit-test-secret");
        JwtTokenService tokenService = new JwtTokenService(properties);
        CurrentUserContext context = new CurrentUserContext();
        AuthInterceptor interceptor = new AuthInterceptor(tokenService, context);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/users/7/dashboard/stats");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + tokenService.createToken(7L, "tester"));

        boolean result = interceptor.preHandle(request, mock(HttpServletResponse.class), new Object());

        assertThat(result).isTrue();
        assertThat(context.getUserId()).isEqualTo(7L);
    }

    @Test
    void rejectsMissingTokenForProtectedPath() {
        AuthProperties properties = new AuthProperties();
        JwtTokenService tokenService = new JwtTokenService(properties);
        AuthInterceptor interceptor = new AuthInterceptor(tokenService, new CurrentUserContext());
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/users/7/dashboard/stats");

        assertThatThrownBy(() -> interceptor.preHandle(request, mock(HttpServletResponse.class), new Object()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("login required");
    }

    @Test
    void passesOptionsPreflightWithoutToken() {
        AuthProperties properties = new AuthProperties();
        JwtTokenService tokenService = new JwtTokenService(properties);
        CurrentUserContext context = new CurrentUserContext();
        AuthInterceptor interceptor = new AuthInterceptor(tokenService, context);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("OPTIONS");
        when(request.getRequestURI()).thenReturn("/api/users/7/dashboard/stats");

        boolean result = interceptor.preHandle(request, mock(HttpServletResponse.class), new Object());

        assertThat(result).isTrue();
        assertThat(context.getUserId()).isNull();
    }

    @Test
    void allowsPublicPathWithoutToken() {
        AuthProperties properties = new AuthProperties();
        JwtTokenService tokenService = new JwtTokenService(properties);
        CurrentUserContext context = new CurrentUserContext();
        AuthInterceptor interceptor = new AuthInterceptor(tokenService, context);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/problems");

        boolean result = interceptor.preHandle(request, mock(HttpServletResponse.class), new Object());

        assertThat(result).isTrue();
        assertThat(context.getUserId()).isNull();
    }
}
