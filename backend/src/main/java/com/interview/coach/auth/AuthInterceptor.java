package com.interview.coach.auth;

import com.interview.coach.handler.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtTokenService jwtTokenService;

    private final CurrentUserContext currentUserContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        String path = request.getRequestURI();
        if (isPublicPath(path)) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BusinessException(401, "login required");
        }
        JwtTokenService.TokenClaims claims = jwtTokenService.verify(header.substring("Bearer ".length()).trim());
        if (claims == null) {
            throw new BusinessException(401, "invalid or expired token");
        }
        currentUserContext.set(claims.userId(), claims.username());
        return true;
    }

    private boolean isPublicPath(String path) {
        return path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.equals("/api/auth/logout")
                || path.equals("/api/problems")
                || path.matches("^/api/problems/\\d+$")
                || path.matches("^/api/problems/\\d+/template$");
    }
}
