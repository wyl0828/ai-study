# Login And User Learning Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a minimal username/password login flow so each tester has isolated submissions, diagnoses, mistake cards, RAG user memory, mock interviews, and training plans.

**Architecture:** Keep authentication small and explainable: Spring Boot issues a signed JWT after login/register, the frontend stores it for the demo, and authenticated API calls resolve `currentUserId` on the backend. Existing learning tables continue to use `user_id`; the main change is replacing hard-coded `DEMO_USER_ID = 1` and request-body `userId` trust with an authenticated user context.

**Tech Stack:** Spring Boot 3, Java 17, MyBatis-Plus, MySQL 8, Next.js 14, TypeScript, localStorage-based demo token, HMAC-SHA256 JWT, BCrypt.

---

## Scope And Non-Goals

This plan intentionally builds a demo-grade account system, not a broad identity platform. It includes username/password registration, login, logout, `GET /api/auth/me`, frontend route guarding, and backend user isolation for learning data.

It excludes email verification, password reset, OAuth, role management, admin panels, refresh-token rotation, account deletion, and public permission management. Those are outside the current AI Interview Coach Agent MVP and would distract from the Agent-driven learning loop.

## Required Execution Order

Execute tasks in this order, even though the detailed sections below keep the auth service and auth context as separate implementation units:

```text
Task 1 DTO / VO
Task 2 PasswordHasher / JwtTokenService
Task 4 CurrentUserContext / AuthInterceptor
Task 3 AuthService / AuthController
Task 5 User isolation hardening
Task 6 Frontend auth client and token handling
Task 7 Login page and AuthGate
Task 8 Replace DEMO_USER_ID and protect learning flows
Task 9 Documentation
Task 10 Verification
```

`CurrentUserContext` must exist before `AuthController` is compiled because `/api/auth/me` depends on it. Treat this order as mandatory when executing the plan.

## Review Corrections Applied Before Execution

- SSE diagnosis requests must send `Authorization: Bearer <token>` because `agentApi.streamDiagnosis(...)` uses a direct `fetch + ReadableStream` path, not the shared `request()` helper.
- `POST /api/agent/analyze` and `GET /api/submissions/{submissionId}/diagnosis/stream` must verify that `submission.user_id == currentUserId` before running Agent analysis.
- Knowledge self-test methods in `UserController` must also be protected by `requireSameUser(userId)` or rewritten to use `currentUserId` directly.
- RAG maintenance, cache refresh, vector retry, and system-index rebuild endpoints require login in this MVP. Admin roles are intentionally deferred.
- Public backend paths are limited to `POST /api/auth/login`, `POST /api/auth/register`, `POST /api/auth/logout`, `GET /api/problems`, `GET /api/problems/{id}`, and `GET /api/problems/{id}/template`.
- Frontend route guarding should protect `/problem/[id]`, `/dashboard`, `/knowledge`, `/mock-interview`, and `/rag-chat`; `/login` remains public.
- `POST /api/auth/logout` is optional server-side state for this JWT demo and should return success even when no token is present, because the real logout action is clearing frontend token storage.

## File Structure

- Create `backend/src/main/java/com/interview/coach/dto/AuthLoginRequest.java`: login request body.
- Create `backend/src/main/java/com/interview/coach/dto/AuthRegisterRequest.java`: register request body.
- Create `backend/src/main/java/com/interview/coach/vo/AuthUserVO.java`: safe user view returned to frontend.
- Create `backend/src/main/java/com/interview/coach/vo/AuthResponseVO.java`: token plus user response.
- Create `backend/src/main/java/com/interview/coach/config/AuthProperties.java`: JWT secret and expiration properties.
- Create `backend/src/main/java/com/interview/coach/auth/PasswordHasher.java`: BCrypt wrapper.
- Create `backend/src/main/java/com/interview/coach/auth/JwtTokenService.java`: HMAC token creation and verification.
- Create `backend/src/main/java/com/interview/coach/auth/CurrentUserContext.java`: request-scoped current user holder.
- Create `backend/src/main/java/com/interview/coach/auth/AuthInterceptor.java`: parse `Authorization: Bearer ...`.
- Create `backend/src/main/java/com/interview/coach/config/WebMvcConfig.java`: register auth interceptor.
- Create `backend/src/main/java/com/interview/coach/service/AuthService.java`: auth service contract.
- Create `backend/src/main/java/com/interview/coach/service/impl/AuthServiceImpl.java`: register/login/me orchestration.
- Create `backend/src/main/java/com/interview/coach/controller/AuthController.java`: `/api/auth` endpoints.
- Modify `backend/pom.xml`: add Spring Security Crypto for BCrypt.
- Modify `backend/src/main/resources/application.yml`: add `coach.auth` properties.
- Modify `backend/src/main/java/com/interview/coach/controller/SubmissionController.java`: use authenticated user id.
- Modify `backend/src/main/java/com/interview/coach/dto/SubmitCodeRequest.java`: make `userId` optional during transition, then unused.
- Modify `backend/src/main/java/com/interview/coach/service/SubmissionService.java`: add owned-submission verification contract.
- Modify `backend/src/main/java/com/interview/coach/service/impl/SubmissionServiceImpl.java`: implement owned-submission verification through existing persistence abstractions.
- Modify `backend/src/main/java/com/interview/coach/controller/AgentController.java`: verify submission ownership for sync and SSE Agent analysis.
- Modify `backend/src/main/java/com/interview/coach/controller/UserController.java`: require path `userId` to match current user.
- Modify `backend/src/main/java/com/interview/coach/controller/RagChatController.java`: use current user id for chat.
- Modify `backend/src/main/java/com/interview/coach/controller/MockInterviewController.java`: ensure create requests use current user id and session reads are owned by current user.
- Modify `backend/src/main/java/com/interview/coach/controller/KnowledgeController.java`: keep read-only knowledge browsing behind AuthGate in the frontend, and require login for cache refresh/status if exposed through backend auth.
- Modify `backend/src/main/java/com/interview/coach/controller/CacheMaintenanceController.java`: require login for status and refresh.
- Create `frontend/lib/auth.ts`: token storage and current-user helpers.
- Modify `frontend/lib/api.ts`: attach auth token and handle 401.
- Create `frontend/app/login/page.tsx`: login/register page.
- Create `frontend/components/AuthGate.tsx`: client-side guard for protected pages.
- Modify `frontend/app/layout.tsx`: keep Navbar but allow auth-aware rendering.
- Modify `frontend/components/Navbar.tsx`: show login/logout and current user.
- Modify `frontend/components/ProblemWorkspace.tsx`: replace `DEMO_USER_ID`.
- Modify `frontend/app/dashboard/page.tsx`: replace `DEMO_USER_ID`.
- Modify `frontend/components/KnowledgeTrainingPage.tsx`: replace `DEMO_USER_ID`.
- Modify `frontend/components/MockInterviewPage.tsx`: replace `DEMO_USER_ID`.
- Modify `frontend/components/RagChatPage.tsx`: replace `DEMO_USER_ID`.
- Create backend tests under `backend/src/test/java/com/interview/coach/auth/` and controller tests beside existing controller tests.
- Create frontend node regression test `frontend/lib/auth-flow.node-test.cjs`.

## Backend Authentication Contract

Endpoints:

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
POST /api/auth/logout
```

Request and response shapes:

```json
{
  "username": "tester01",
  "password": "password123"
}
```

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "jwt-token",
    "user": {
      "id": 12,
      "username": "tester01"
    }
  }
}
```

Protected requests must send:

```text
Authorization: Bearer jwt-token
```

## Task 1: Add Backend Auth DTOs And VOs

**Files:**
- Create: `backend/src/main/java/com/interview/coach/dto/AuthLoginRequest.java`
- Create: `backend/src/main/java/com/interview/coach/dto/AuthRegisterRequest.java`
- Create: `backend/src/main/java/com/interview/coach/vo/AuthUserVO.java`
- Create: `backend/src/main/java/com/interview/coach/vo/AuthResponseVO.java`

- [ ] **Step 1: Create login request DTO**

```java
package com.interview.coach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthLoginRequest {

    @NotBlank(message = "username is required")
    @Size(min = 3, max = 32, message = "username length must be 3-32")
    private String username;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 72, message = "password length must be 6-72")
    private String password;
}
```

- [ ] **Step 2: Create register request DTO**

```java
package com.interview.coach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthRegisterRequest {

    @NotBlank(message = "username is required")
    @Size(min = 3, max = 32, message = "username length must be 3-32")
    @Pattern(regexp = "^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$", message = "username may only contain letters, numbers, underscore, or Chinese characters")
    private String username;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 72, message = "password length must be 6-72")
    private String password;
}
```

- [ ] **Step 3: Create safe auth user VO**

```java
package com.interview.coach.vo;

import lombok.Data;

@Data
public class AuthUserVO {

    private Long id;

    private String username;
}
```

- [ ] **Step 4: Create auth response VO**

```java
package com.interview.coach.vo;

import lombok.Data;

@Data
public class AuthResponseVO {

    private String token;

    private AuthUserVO user;
}
```

- [ ] **Step 5: Compile backend**

Run: `cd backend && mvn -q -DskipTests compile`

Expected: compile succeeds.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/interview/coach/dto/AuthLoginRequest.java backend/src/main/java/com/interview/coach/dto/AuthRegisterRequest.java backend/src/main/java/com/interview/coach/vo/AuthUserVO.java backend/src/main/java/com/interview/coach/vo/AuthResponseVO.java
git commit -m "feat: add auth request and response models"
```

## Task 2: Add Password Hashing And JWT Services

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/java/com/interview/coach/config/AuthProperties.java`
- Create: `backend/src/main/java/com/interview/coach/auth/PasswordHasher.java`
- Create: `backend/src/main/java/com/interview/coach/auth/JwtTokenService.java`
- Test: `backend/src/test/java/com/interview/coach/auth/JwtTokenServiceTest.java`

- [ ] **Step 1: Add BCrypt dependency**

Add this dependency inside `backend/pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

- [ ] **Step 2: Add auth properties**

Add under `coach:` in `backend/src/main/resources/application.yml`:

```yaml
  auth:
    jwt-secret: ${AUTH_JWT_SECRET:dev-change-this-secret-for-deploy}
    jwt-expire-hours: ${AUTH_JWT_EXPIRE_HOURS:168}
```

- [ ] **Step 3: Create auth properties class**

```java
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
```

- [ ] **Step 4: Create password hasher**

```java
package com.interview.coach.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String passwordHash) {
        return encoder.matches(rawPassword, passwordHash);
    }
}
```

- [ ] **Step 5: Create JWT token service**

```java
package com.interview.coach.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.coach.config.AuthProperties;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AuthProperties properties;

    public String createToken(Long userId, String username) {
        Instant now = Instant.now();
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", String.valueOf(userId));
        payload.put("username", username);
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plusSeconds(properties.getJwtExpireHours() * 3600L).getEpochSecond());

        String headerPart = base64Url(toJson(header));
        String payloadPart = base64Url(toJson(payload));
        String signaturePart = sign(headerPart + "." + payloadPart);
        return headerPart + "." + payloadPart + "." + signaturePart;
    }

    public TokenClaims verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String expectedSignature = sign(parts[0] + "." + parts[1]);
            if (!constantTimeEquals(expectedSignature, parts[2])) {
                return null;
            }
            Map<?, ?> payload = OBJECT_MAPPER.readValue(base64UrlDecode(parts[1]), Map.class);
            Number exp = (Number) payload.get("exp");
            if (exp == null || Instant.now().getEpochSecond() > exp.longValue()) {
                return null;
            }
            Long userId = Long.valueOf(String.valueOf(payload.get("sub")));
            String username = String.valueOf(payload.get("username"));
            return new TokenClaims(userId, username);
        } catch (Exception ex) {
            return null;
        }
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to serialize jwt part", ex);
        }
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return base64Url(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("failed to sign token", ex);
        }
    }

    private String base64Url(String value) {
        return base64Url(value.getBytes(StandardCharsets.UTF_8));
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private boolean constantTimeEquals(String left, String right) {
        if (left.length() != right.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length(); i++) {
            result |= left.charAt(i) ^ right.charAt(i);
        }
        return result == 0;
    }

    public record TokenClaims(Long userId, String username) {
    }
}
```

- [ ] **Step 6: Add token service tests**

```java
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
```

- [ ] **Step 7: Run tests**

Run: `cd backend && mvn -q -Dtest=JwtTokenServiceTest test`

Expected: two tests pass.

- [ ] **Step 8: Commit**

```bash
git add backend/pom.xml backend/src/main/resources/application.yml backend/src/main/java/com/interview/coach/config/AuthProperties.java backend/src/main/java/com/interview/coach/auth/PasswordHasher.java backend/src/main/java/com/interview/coach/auth/JwtTokenService.java backend/src/test/java/com/interview/coach/auth/JwtTokenServiceTest.java
git commit -m "feat: add password hashing and jwt service"
```

## Task 3: Add Auth Service And Controller

Execution note: implement Task 4 before this task. `AuthController` depends on `CurrentUserContext`, so compiling this task before Task 4 is expected to fail.

**Files:**
- Create: `backend/src/main/java/com/interview/coach/service/AuthService.java`
- Create: `backend/src/main/java/com/interview/coach/service/impl/AuthServiceImpl.java`
- Create: `backend/src/main/java/com/interview/coach/controller/AuthController.java`
- Test: `backend/src/test/java/com/interview/coach/controller/AuthControllerTest.java`

- [ ] **Step 1: Create service interface**

```java
package com.interview.coach.service;

import com.interview.coach.dto.AuthLoginRequest;
import com.interview.coach.dto.AuthRegisterRequest;
import com.interview.coach.vo.AuthResponseVO;
import com.interview.coach.vo.AuthUserVO;

public interface AuthService {

    AuthResponseVO register(AuthRegisterRequest request);

    AuthResponseVO login(AuthLoginRequest request);

    AuthUserVO me(Long userId);
}
```

- [ ] **Step 2: Create service implementation**

```java
package com.interview.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.coach.auth.JwtTokenService;
import com.interview.coach.auth.PasswordHasher;
import com.interview.coach.dto.AuthLoginRequest;
import com.interview.coach.dto.AuthRegisterRequest;
import com.interview.coach.entity.User;
import com.interview.coach.handler.BusinessException;
import com.interview.coach.mapper.UserMapper;
import com.interview.coach.service.AuthService;
import com.interview.coach.vo.AuthResponseVO;
import com.interview.coach.vo.AuthUserVO;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;

    private final PasswordHasher passwordHasher;

    private final JwtTokenService jwtTokenService;

    @Override
    public AuthResponseVO register(AuthRegisterRequest request) {
        String username = normalizeUsername(request.getUsername());
        User existing = findByUsername(username);
        if (existing != null) {
            throw new BusinessException(400, "username already exists");
        }
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHasher.hash(request.getPassword()));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return toAuthResponse(user);
    }

    @Override
    public AuthResponseVO login(AuthLoginRequest request) {
        String username = normalizeUsername(request.getUsername());
        User user = findByUsername(username);
        if (user == null || !passwordHasher.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(401, "invalid username or password");
        }
        return toAuthResponse(user);
    }

    @Override
    public AuthUserVO me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "login required");
        }
        return toUserVO(user);
    }

    private User findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(400, "username is required");
        }
        return normalized;
    }

    private AuthResponseVO toAuthResponse(User user) {
        AuthResponseVO vo = new AuthResponseVO();
        vo.setToken(jwtTokenService.createToken(user.getId(), user.getUsername()));
        vo.setUser(toUserVO(user));
        return vo;
    }

    private AuthUserVO toUserVO(User user) {
        AuthUserVO vo = new AuthUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        return vo;
    }
}
```

- [ ] **Step 3: Create auth controller**

```java
package com.interview.coach.controller;

import com.interview.coach.auth.CurrentUserContext;
import com.interview.coach.dto.AuthLoginRequest;
import com.interview.coach.dto.AuthRegisterRequest;
import com.interview.coach.service.AuthService;
import com.interview.coach.vo.ApiResponse;
import com.interview.coach.vo.AuthResponseVO;
import com.interview.coach.vo.AuthUserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    private final CurrentUserContext currentUserContext;

    @PostMapping("/register")
    public ApiResponse<AuthResponseVO> register(@Valid @RequestBody AuthRegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponseVO> login(@Valid @RequestBody AuthLoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<AuthUserVO> me() {
        return ApiResponse.success(authService.me(currentUserContext.requireUserId()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success(null);
    }
}
```

- [ ] **Step 4: Create controller test for service delegation**

```java
package com.interview.coach.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.interview.coach.auth.CurrentUserContext;
import com.interview.coach.dto.AuthLoginRequest;
import com.interview.coach.service.AuthService;
import com.interview.coach.vo.ApiResponse;
import com.interview.coach.vo.AuthResponseVO;
import com.interview.coach.vo.AuthUserVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private CurrentUserContext currentUserContext;

    @InjectMocks
    private AuthController controller;

    @Test
    void loginReturnsAuthResponse() {
        AuthLoginRequest request = new AuthLoginRequest();
        request.setUsername("tester");
        request.setPassword("password123");
        AuthResponseVO expected = new AuthResponseVO();
        AuthUserVO user = new AuthUserVO();
        user.setId(3L);
        user.setUsername("tester");
        expected.setUser(user);
        expected.setToken("token");
        when(authService.login(request)).thenReturn(expected);

        ApiResponse<AuthResponseVO> response = controller.login(request);

        assertThat(response.getData().getUser().getId()).isEqualTo(3L);
        assertThat(response.getData().getToken()).isEqualTo("token");
    }

    @Test
    void meUsesCurrentUser() {
        AuthUserVO expected = new AuthUserVO();
        expected.setId(5L);
        expected.setUsername("tester5");
        when(currentUserContext.requireUserId()).thenReturn(5L);
        when(authService.me(5L)).thenReturn(expected);

        ApiResponse<AuthUserVO> response = controller.me();

        assertThat(response.getData().getUsername()).isEqualTo("tester5");
    }
}
```

- [ ] **Step 5: Run controller test**

Run: `cd backend && mvn -q -Dtest=AuthControllerTest test`

Expected: tests pass. Run this only after Task 4 has created `CurrentUserContext`.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/interview/coach/service/AuthService.java backend/src/main/java/com/interview/coach/service/impl/AuthServiceImpl.java backend/src/main/java/com/interview/coach/controller/AuthController.java backend/src/test/java/com/interview/coach/controller/AuthControllerTest.java
git commit -m "feat: add auth service and endpoints"
```

## Task 4: Add Current User Context And Auth Interceptor

Execution note: run this task before Task 3.

**Files:**
- Create: `backend/src/main/java/com/interview/coach/auth/CurrentUserContext.java`
- Create: `backend/src/main/java/com/interview/coach/auth/AuthInterceptor.java`
- Create: `backend/src/main/java/com/interview/coach/config/WebMvcConfig.java`
- Test: `backend/src/test/java/com/interview/coach/auth/AuthInterceptorTest.java`

- [ ] **Step 1: Create current user context**

```java
package com.interview.coach.auth;

import com.interview.coach.handler.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class CurrentUserContext {

    private Long userId;

    private String username;

    public void set(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Long requireUserId() {
        if (userId == null) {
            throw new BusinessException(401, "login required");
        }
        return userId;
    }
}
```

- [ ] **Step 2: Create auth interceptor**

```java
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
```

- [ ] **Step 3: Register interceptor**

```java
package com.interview.coach.config;

import com.interview.coach.auth.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
    }
}
```

- [ ] **Step 4: Create interceptor unit test**

```java
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
        when(request.getRequestURI()).thenReturn("/api/users/7/dashboard/stats");

        assertThatThrownBy(() -> interceptor.preHandle(request, mock(HttpServletResponse.class), new Object()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("login required");
    }
}
```

- [ ] **Step 5: Run auth tests**

Run: `cd backend && mvn -q -Dtest=JwtTokenServiceTest,AuthInterceptorTest,AuthControllerTest test`

Expected: tests pass.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/interview/coach/auth/CurrentUserContext.java backend/src/main/java/com/interview/coach/auth/AuthInterceptor.java backend/src/main/java/com/interview/coach/config/WebMvcConfig.java backend/src/test/java/com/interview/coach/auth/AuthInterceptorTest.java
git commit -m "feat: add authenticated user context"
```

## Task 5: Enforce User Isolation In Learning Controllers

**Files:**
- Modify: `backend/src/main/java/com/interview/coach/controller/SubmissionController.java`
- Modify: `backend/src/main/java/com/interview/coach/dto/SubmitCodeRequest.java`
- Modify: `backend/src/main/java/com/interview/coach/service/SubmissionService.java`
- Modify: `backend/src/main/java/com/interview/coach/service/impl/SubmissionServiceImpl.java`
- Modify: `backend/src/main/java/com/interview/coach/controller/AgentController.java`
- Modify: `backend/src/main/java/com/interview/coach/controller/UserController.java`
- Modify: `backend/src/main/java/com/interview/coach/controller/RagChatController.java`
- Modify: `backend/src/main/java/com/interview/coach/controller/MockInterviewController.java`
- Modify: `backend/src/main/java/com/interview/coach/controller/KnowledgeController.java`
- Modify: `backend/src/main/java/com/interview/coach/controller/CacheMaintenanceController.java`
- Test: `backend/src/test/java/com/interview/coach/controller/UserControllerTest.java`
- Test: `backend/src/test/java/com/interview/coach/controller/SubmissionControllerTest.java`
- Test: `backend/src/test/java/com/interview/coach/controller/AgentControllerTest.java`

- [ ] **Step 1: Make `SubmitCodeRequest.userId` transition-safe**

Change the field in `SubmitCodeRequest` to remove `@NotNull`:

```java
private Long userId;
```

Keep the field temporarily so older frontend code compiles during the transition, but controllers must overwrite it from authenticated context.

- [ ] **Step 2: Update submission controller**

```java
package com.interview.coach.controller;

import com.interview.coach.auth.CurrentUserContext;
import com.interview.coach.dto.SubmitCodeRequest;
import com.interview.coach.service.SubmissionService;
import com.interview.coach.vo.ApiResponse;
import com.interview.coach.vo.SubmissionResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    private final CurrentUserContext currentUserContext;

    @PostMapping
    public ApiResponse<SubmissionResultVO> submit(@Valid @RequestBody SubmitCodeRequest request) {
        request.setUserId(currentUserContext.requireUserId());
        return ApiResponse.success(submissionService.submit(request));
    }
}
```

- [ ] **Step 3: Add helper to `UserController`**

Add dependency and helper:

```java
private final CurrentUserContext currentUserContext;

private void requireSameUser(Long userId) {
    Long currentUserId = currentUserContext.requireUserId();
    if (!currentUserId.equals(userId)) {
        throw new BusinessException(403, "cannot access another user's learning data");
    }
}
```

Call `requireSameUser(userId);` at the start of every `UserController` method before reading or mutating learning data. This includes dashboard stats, weaknesses, weakness events, mistake cards, training plan reads, training plan item status updates, manual training-plan regeneration, recent submissions, mock interview summaries, and knowledge-card self-test endpoints.

For self-test methods, the protected shape must be:

```java
@PostMapping("/{userId}/knowledge/cards/{cardId}/self-tests")
public ApiResponse<SelfTestRecordVO> submitSelfTest(
        @PathVariable Long userId,
        @PathVariable Long cardId,
        @Valid @RequestBody SelfTestSubmitRequest request) {
    requireSameUser(userId);
    return ApiResponse.success(knowledgeLearningService.submitSelfTest(userId, cardId, request));
}

@GetMapping("/{userId}/knowledge/cards/{cardId}/self-tests/recent")
public ApiResponse<List<SelfTestRecordVO>> getRecentSelfTests(
        @PathVariable Long userId,
        @PathVariable Long cardId,
        @RequestParam(defaultValue = "5") int limit) {
    requireSameUser(userId);
    return ApiResponse.success(knowledgeLearningService.getRecentSelfTests(userId, cardId, limit));
}
```

- [ ] **Step 4: Update RAG chat controller**

Change chat to ignore request `userId`:

```java
@PostMapping("/chat")
public ApiResponse<RagChatResponseVO> chat(@Valid @RequestBody RagChatRequest request) {
    return ApiResponse.success(ragChatService.ask(currentUserContext.requireUserId(), request.getQuestion()));
}
```

- [ ] **Step 5: Add Agent submission ownership checks**

`AgentController` currently accepts only `submissionId`. Before `agentService.analyze(...)` is called, the controller or service must verify that the submission belongs to the authenticated user. Add a `SubmissionService.requireOwnedSubmission(submissionId, currentUserId)` method or equivalent service-layer helper; the controller should not query mappers directly.

Service contract:

```java
void requireOwnedSubmission(Long submissionId, Long userId);
```

Service implementation shape:

```java
@Override
public void requireOwnedSubmission(Long submissionId, Long userId) {
    Submission submission = submissionMapper.selectById(submissionId);
    if (submission == null) {
        throw new BusinessException(404, "submission not found");
    }
    if (!userId.equals(submission.getUserId())) {
        throw new BusinessException(403, "cannot access another user's submission");
    }
}
```

Controller shape:

```java
private final CurrentUserContext currentUserContext;

private final SubmissionService submissionService;

@PostMapping("/agent/analyze")
public ApiResponse<AgentAnalyzeVO> analyze(@Valid @RequestBody AgentAnalyzeRequest request) {
    Long currentUserId = currentUserContext.requireUserId();
    submissionService.requireOwnedSubmission(request.getSubmissionId(), currentUserId);
    return ApiResponse.success(agentService.analyze(request.getSubmissionId()));
}

@GetMapping("/submissions/{submissionId}/diagnosis/stream")
public SseEmitter streamDiagnosis(@PathVariable Long submissionId) {
    Long currentUserId = currentUserContext.requireUserId();
    submissionService.requireOwnedSubmission(submissionId, currentUserId);
    SseEmitter emitter = new SseEmitter(0L);
    CompletableFuture.runAsync(() -> runDiagnosisStream(submissionId, emitter), agentTaskExecutor);
    return emitter;
}
```

`requireOwnedSubmission` should throw `BusinessException(403, "cannot access another user's submission")` when `submission.user_id` does not equal the authenticated user id.

- [ ] **Step 6: Update mock interview create flow**

In `MockInterviewController.create`, overwrite the request user id:

```java
@PostMapping
public ApiResponse<MockInterviewSessionVO> create(@Valid @RequestBody MockInterviewCreateRequest request) {
    request.setUserId(currentUserContext.requireUserId());
    return ApiResponse.success(mockInterviewService.create(request));
}
```

For `getSession`, `answer`, and `finish`, add ownership enforcement in `MockInterviewService` if not already present. The service should load the session by id and compare `session.getUserId()` with `currentUserContext.requireUserId()` before returning or mutating it.

- [ ] **Step 7: Require login for RAG maintenance and cache maintenance**

Because `AuthInterceptor` now protects every `/api/**` path except login/register/logout and problem reads, these endpoints automatically require a token:

```text
GET  /api/rag/health
POST /api/rag/vector/retry-failed
POST /api/rag/system-index/rebuild
GET  /api/cache/status
POST /api/cache/refresh
GET  /api/problems/cache/status
POST /api/problems/cache/refresh
GET  /api/knowledge/cache/status
POST /api/knowledge/cache/refresh
```

Do not add admin roles in this task. Document that these are authenticated maintenance endpoints for the demo; admin separation is out of MVP scope.

- [ ] **Step 8: Add controller tests for user mismatch**

Add a test to `UserControllerTest`:

```java
@Test
void rejectsDifferentUserDashboardAccess() {
    when(currentUserContext.requireUserId()).thenReturn(2L);

    assertThatThrownBy(() -> controller.getDashboardStats(1L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("cannot access another user's learning data");
}
```

Add a test to `SubmissionControllerTest`:

```java
@Test
void submitOverwritesRequestUserIdWithAuthenticatedUser() {
    SubmitCodeRequest request = new SubmitCodeRequest();
    request.setUserId(99L);
    request.setProblemId(1L);
    request.setLanguage("java");
    request.setCode("class Solution {}");
    when(currentUserContext.requireUserId()).thenReturn(7L);

    controller.submit(request);

    assertThat(request.getUserId()).isEqualTo(7L);
    verify(submissionService).submit(request);
}
```

Add tests to `AgentControllerTest`:

```java
@Test
void analyzeRejectsAnotherUsersSubmission() {
    AgentAnalyzeRequest request = new AgentAnalyzeRequest();
    request.setSubmissionId(11L);
    when(currentUserContext.requireUserId()).thenReturn(2L);
    doThrow(new BusinessException(403, "cannot access another user's submission"))
            .when(submissionService)
            .requireOwnedSubmission(11L, 2L);

    assertThatThrownBy(() -> controller.analyze(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("cannot access another user's submission");
}

@Test
void streamRejectsAnotherUsersSubmissionBeforeStartingEmitter() {
    when(currentUserContext.requireUserId()).thenReturn(2L);
    doThrow(new BusinessException(403, "cannot access another user's submission"))
            .when(submissionService)
            .requireOwnedSubmission(11L, 2L);

    assertThatThrownBy(() -> controller.streamDiagnosis(11L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("cannot access another user's submission");
}
```

- [ ] **Step 9: Run controller tests**

Run: `cd backend && mvn -q -Dtest=UserControllerTest,SubmissionControllerTest,AgentControllerTest test`

Expected: tests pass; user mismatch and submission mismatch access throw 403.

- [ ] **Step 10: Commit**

```bash
git add backend/src/main/java/com/interview/coach/controller/SubmissionController.java backend/src/main/java/com/interview/coach/dto/SubmitCodeRequest.java backend/src/main/java/com/interview/coach/service/SubmissionService.java backend/src/main/java/com/interview/coach/service/impl/SubmissionServiceImpl.java backend/src/main/java/com/interview/coach/controller/AgentController.java backend/src/main/java/com/interview/coach/controller/UserController.java backend/src/main/java/com/interview/coach/controller/RagChatController.java backend/src/main/java/com/interview/coach/controller/MockInterviewController.java backend/src/main/java/com/interview/coach/controller/KnowledgeController.java backend/src/main/java/com/interview/coach/controller/CacheMaintenanceController.java backend/src/test/java/com/interview/coach/controller/UserControllerTest.java backend/src/test/java/com/interview/coach/controller/SubmissionControllerTest.java backend/src/test/java/com/interview/coach/controller/AgentControllerTest.java
git commit -m "feat: enforce authenticated learning data isolation"
```

## Task 6: Add Frontend Auth Client And API Token Handling

**Files:**
- Create: `frontend/lib/auth.ts`
- Modify: `frontend/lib/api.ts`
- Modify: `frontend/lib/types.ts`
- Test: `frontend/lib/auth-flow.node-test.cjs`

- [ ] **Step 1: Add auth types**

Add to `frontend/lib/types.ts`:

```ts
export interface AuthUser {
  id: number;
  username: string;
}

export interface AuthResponse {
  token: string;
  user: AuthUser;
}
```

- [ ] **Step 2: Create frontend auth helper**

```ts
import type { AuthUser } from "./types";

const TOKEN_KEY = "ai-study.auth.token";
const USER_KEY = "ai-study.auth.user";

export function getAuthToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(TOKEN_KEY);
}

export function getStoredUser(): AuthUser | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthUser;
  } catch {
    return null;
  }
}

export function saveAuthSession(token: string, user: AuthUser) {
  window.localStorage.setItem(TOKEN_KEY, token);
  window.localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearAuthSession() {
  window.localStorage.removeItem(TOKEN_KEY);
  window.localStorage.removeItem(USER_KEY);
}
```

- [ ] **Step 3: Attach token in API client**

In `frontend/lib/api.ts`, import `getAuthToken` and add auth headers:

```ts
import { clearAuthSession, getAuthToken } from "./auth";
```

Inside `request<T>` before `fetch`:

```ts
const token = typeof window === "undefined" ? null : getAuthToken();
const headers = new Headers(init?.headers);
if (token) {
  headers.set("Authorization", `Bearer ${token}`);
}
```

Use the headers in fetch:

```ts
res = await fetch(`${API_BASE}${url}`, {
  ...init,
  headers,
  cache: "no-store",
});
```

Handle 401:

```ts
if (res.status === 401) {
  if (typeof window !== "undefined") {
    clearAuthSession();
    window.location.href = "/login";
  }
  throw new Error("登录状态已过期，请重新登录。");
}
```

- [ ] **Step 4: Add auth API functions**

Add to `frontend/lib/api.ts`:

```ts
export const authApi = {
  register: (username: string, password: string) =>
    request<ApiResponse<AuthResponse>>("/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    }),
  login: (username: string, password: string) =>
    request<ApiResponse<AuthResponse>>("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    }),
  me: () => request<ApiResponse<AuthUser>>("/api/auth/me"),
  logout: () =>
    request<ApiResponse<null>>("/api/auth/logout", {
      method: "POST",
    }),
};
```

- [ ] **Step 5: Ensure SSE helper can receive token**

`agentApi.streamDiagnosis(...)` already has an `options?: { token?: string }` parameter. Keep that parameter and ensure the implementation sets the bearer header:

```ts
fetch(`${API_BASE}/api/submissions/${submissionId}/diagnosis/stream`, {
  method: "GET",
  headers: {
    Accept: "text/event-stream",
    ...(options?.token ? { Authorization: `Bearer ${options.token}` } : {}),
  },
  signal: controller.signal,
});
```

This is required because SSE diagnosis uses direct `fetch + ReadableStream` and does not call the shared `request()` helper.

- [ ] **Step 6: Add static regression test**

```js
const fs = require("fs");
const assert = require("assert");

const api = fs.readFileSync("frontend/lib/api.ts", "utf8");
const auth = fs.readFileSync("frontend/lib/auth.ts", "utf8");

assert.match(auth, /ai-study\.auth\.token/);
assert.match(auth, /saveAuthSession/);
assert.match(api, /Authorization/);
assert.match(api, /authApi/);
assert.match(api, /window\.location\.href = "\/login"/);
assert.match(api, /streamDiagnosis/);
assert.match(api, /Authorization: `Bearer \$\{options\.token\}`/);

console.log("auth-flow checks passed");
```

- [ ] **Step 7: Run frontend check**

Run: `node frontend/lib/auth-flow.node-test.cjs`

Expected: prints `auth-flow checks passed`.

- [ ] **Step 8: Commit**

```bash
git add frontend/lib/auth.ts frontend/lib/api.ts frontend/lib/types.ts frontend/lib/auth-flow.node-test.cjs
git commit -m "feat: add frontend auth client"
```

## Task 7: Add Login Page And Route Guard

**Files:**
- Create: `frontend/app/login/page.tsx`
- Create: `frontend/components/AuthGate.tsx`
- Modify: `frontend/components/Navbar.tsx`
- Modify: `frontend/app/problem/[id]/page.tsx`
- Modify: `frontend/app/dashboard/page.tsx`
- Modify: `frontend/app/knowledge/page.tsx`
- Modify: `frontend/app/mock-interview/page.tsx`
- Modify: `frontend/app/rag-chat/page.tsx`

- [ ] **Step 1: Create login page**

```tsx
"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { authApi, formatApiError } from "@/lib/api";
import { saveAuthSession } from "@/lib/auth";

export default function LoginPage() {
  const router = useRouter();
  const [mode, setMode] = useState<"login" | "register">("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const response =
        mode === "login"
          ? await authApi.login(username, password)
          : await authApi.register(username, password);
      saveAuthSession(response.data.token, response.data.user);
      router.push("/");
    } catch (err) {
      setError(formatApiError(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center px-6 py-12">
      <section className="rounded-lg border border-border bg-surface p-6 shadow-sm">
        <h1 className="text-xl font-semibold text-on-surface">
          {mode === "login" ? "登录 AI 面试教练" : "创建测试账号"}
        </h1>
        <form onSubmit={submit} className="mt-6 space-y-4">
          <label className="block text-sm">
            <span className="text-on-surface-muted">用户名</span>
            <input
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              className="mt-1 w-full rounded-md border border-border bg-background px-3 py-2"
              autoComplete="username"
            />
          </label>
          <label className="block text-sm">
            <span className="text-on-surface-muted">密码</span>
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="mt-1 w-full rounded-md border border-border bg-background px-3 py-2"
              autoComplete={mode === "login" ? "current-password" : "new-password"}
            />
          </label>
          {error && <p className="text-sm text-error">{error}</p>}
          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-md bg-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-60"
          >
            {loading ? "处理中..." : mode === "login" ? "登录" : "注册并登录"}
          </button>
        </form>
        <button
          type="button"
          onClick={() => setMode(mode === "login" ? "register" : "login")}
          className="mt-4 text-sm text-primary"
        >
          {mode === "login" ? "没有账号？创建一个测试账号" : "已有账号？返回登录"}
        </button>
      </section>
    </main>
  );
}
```

- [ ] **Step 2: Create AuthGate**

```tsx
"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { authApi } from "@/lib/api";
import { getStoredUser, getAuthToken, saveAuthSession } from "@/lib/auth";
import type { AuthUser } from "@/lib/types";

interface AuthGateProps {
  children: (user: AuthUser) => React.ReactNode;
}

export default function AuthGate({ children }: AuthGateProps) {
  const router = useRouter();
  const [user, setUser] = useState<AuthUser | null>(() => getStoredUser());
  const [checking, setChecking] = useState(true);

  useEffect(() => {
    const token = getAuthToken();
    if (!token) {
      router.replace("/login");
      return;
    }
    authApi
      .me()
      .then((response) => {
        saveAuthSession(token, response.data);
        setUser(response.data);
      })
      .finally(() => setChecking(false));
  }, [router]);

  if (checking || !user) {
    return <main className="px-6 py-10 text-sm text-on-surface-muted">正在确认登录状态...</main>;
  }

  return <>{children(user)}</>;
}
```

- [ ] **Step 3: Use AuthGate in protected pages**

Protect these routes and keep `/login` public:

```text
/problem/[id]
/dashboard
/knowledge
/mock-interview
/rag-chat
```

Route pattern:

```tsx
import AuthGate from "@/components/AuthGate";

export default function ProtectedPage() {
  return (
    <AuthGate>
      {(user) => <ProtectedPageContent userId={user.id} />}
    </AuthGate>
  );
}
```

For files where splitting is simpler, keep the exported page as the wrapper and move existing logic into an inner component that accepts `userId: number`. Problem list `/` may remain public, but any page that submits code, reads learning records, asks RAG chat, runs a self-test, or starts mock interview must be protected.

- [ ] **Step 4: Update Navbar logout behavior**

Add a logout button that calls:

```ts
clearAuthSession();
window.location.href = "/login";
```

- [ ] **Step 5: Run frontend build**

Run: `cd frontend && npm run build`

Expected: Next.js build succeeds.

- [ ] **Step 6: Commit**

```bash
git add frontend/app/login/page.tsx frontend/components/AuthGate.tsx frontend/components/Navbar.tsx frontend/app frontend/components
git commit -m "feat: add login page and auth guard"
```

## Task 8: Replace Demo User IDs Across Frontend Flows

**Files:**
- Modify: `frontend/components/ProblemWorkspace.tsx`
- Modify: `frontend/app/dashboard/page.tsx`
- Modify: `frontend/components/KnowledgeTrainingPage.tsx`
- Modify: `frontend/components/MockInterviewPage.tsx`
- Modify: `frontend/components/RagChatPage.tsx`
- Modify: `frontend/lib/draft.ts` only if its public API requires a clearer user-id call site.

- [ ] **Step 1: Replace problem workspace constant**

Change component props:

```ts
interface ProblemWorkspaceProps {
  problemId: number;
  userId: number;
}
```

Replace all `DEMO_USER_ID` and literal `userId: 1` with the prop:

```ts
loadDraft(userId, problemId)
saveDraft(userId, problemId, { code, language: "java" })
clearDraft(userId, problemId)
submissionApi.submit({ problemId, language: "java", code })
```

The submission request no longer sends `userId`; backend fills it from the token.

- [ ] **Step 2: Pass token to diagnosis SSE**

Import `getAuthToken` in `ProblemWorkspace.tsx`:

```ts
import { getAuthToken } from "@/lib/auth";
```

When calling `agentApi.streamDiagnosis(...)`, pass the current token:

```ts
const token = getAuthToken();

streamControllerRef.current = agentApi.streamDiagnosis(
  resultWithSnapshot.submissionId,
  {
    onStep: (step) => {
      if (!isCurrentAgentStream(streamId, currentStreamIdRef.current)) return;
      setAgentSteps((prev) => {
        const idx = prev.findIndex((s) => s.stepName === step.stepName);
        const next = idx >= 0 ? prev.map((s, i) => (i === idx ? step : s)) : [...prev, step];
        return next.sort((a, b) => getStepRank(a.stepName) - getStepRank(b.stepName));
      });
    },
    onDone: (result) => {
      if (!isCurrentAgentStream(streamId, currentStreamIdRef.current)) return;
      doneReceived = true;
      applyDiagnosisResult(result);
      setIsAnalyzing(false);
    },
    onError: (message) => {
      if (!isCurrentAgentStream(streamId, currentStreamIdRef.current)) return;
      doneReceived = true;
      if (shouldRunSyncFallback("error", doneReceived)) {
        runSyncFallback(message);
      }
    },
    onEnd: () => {
      if (!isCurrentAgentStream(streamId, currentStreamIdRef.current)) return;
      if (shouldRunSyncFallback("end", doneReceived)) {
        runSyncFallback("SSE ended without done event");
      } else {
        setIsAnalyzing(false);
      }
    },
  },
  { token: token ?? undefined }
);
```

Without this, code submission can succeed while the diagnosis stream returns 401 and the Agent timeline stays stuck.

- [ ] **Step 3: Replace dashboard constant**

Move dashboard page logic into an inner component:

```tsx
export default function DashboardPage() {
  return <AuthGate>{(user) => <DashboardContent userId={user.id} />}</AuthGate>;
}
```

Replace calls:

```ts
userApi.stats(userId)
userApi.weaknesses(userId)
userApi.latestPlan(userId)
userApi.updateTrainingPlanItemStatus(userId, itemId, status)
```

- [ ] **Step 4: Replace knowledge training constant**

Use authenticated user id:

```tsx
<KnowledgeCard userId={userId} topic={topic} />
```

- [ ] **Step 5: Replace mock interview constant**

Use authenticated user id for create request:

```ts
await mockInterviewApi.create({ userId, questionCount, category })
```

This can still include `userId` during transition; backend overwrites it with current user id.

- [ ] **Step 6: Replace RAG chat constant**

Send current user id during transition:

```ts
await ragChatApi.ask({ userId, question })
```

Backend uses current user id and ignores the request user id.

- [ ] **Step 7: Update regression test**

Extend `frontend/lib/auth-flow.node-test.cjs`:

```js
const files = [
  "frontend/components/ProblemWorkspace.tsx",
  "frontend/app/dashboard/page.tsx",
  "frontend/components/KnowledgeTrainingPage.tsx",
  "frontend/components/MockInterviewPage.tsx",
  "frontend/components/RagChatPage.tsx",
];

for (const file of files) {
  const content = fs.readFileSync(file, "utf8");
  assert.doesNotMatch(content, /DEMO_USER_ID/);
  assert.doesNotMatch(content, /userId:\s*1/);
}

const problemWorkspace = fs.readFileSync("frontend/components/ProblemWorkspace.tsx", "utf8");
assert.match(problemWorkspace, /getAuthToken/);
assert.match(problemWorkspace, /streamDiagnosis\([\s\S]*\{ token: token \?\? undefined \}/);
```

- [ ] **Step 8: Run frontend checks**

Run:

```bash
node frontend/lib/auth-flow.node-test.cjs
cd frontend && npm run build
```

Expected: static auth checks pass and build succeeds.

- [ ] **Step 9: Commit**

```bash
git add frontend/components/ProblemWorkspace.tsx frontend/app/dashboard/page.tsx frontend/components/KnowledgeTrainingPage.tsx frontend/components/MockInterviewPage.tsx frontend/components/RagChatPage.tsx frontend/lib/auth-flow.node-test.cjs
git commit -m "feat: use authenticated user in frontend learning flows"
```

## Task 9: Update API Docs And Deployment Notes

**Files:**
- Modify: `docs/API.md`
- Modify: `README.md`
- Modify: `docs/PROJECT_STATUS.md`

- [ ] **Step 1: Document auth APIs in `docs/API.md`**

Add:

```markdown
## Auth

### POST /api/auth/register

Creates a tester account and returns a bearer token.

### POST /api/auth/login

Logs in with username and password and returns a bearer token.

### GET /api/auth/me

Returns the current authenticated user. Requires `Authorization: Bearer <token>`.

### POST /api/auth/logout

Client-side logout helper endpoint. The frontend clears local token storage.
```

- [ ] **Step 2: Document deployment environment variable**

Add to README environment section:

```bash
# Auth
AUTH_JWT_SECRET=replace_with_a_long_random_secret
AUTH_JWT_EXPIRE_HOURS=168
```

- [ ] **Step 3: Document product boundary**

Add to `docs/PROJECT_STATUS.md`:

```markdown
Login is a demo-grade tester account system used to isolate learning records. It is not a full identity platform: email verification, password reset, OAuth, role management, and admin permission controls are intentionally out of MVP scope.
```

- [ ] **Step 4: Commit**

```bash
git add docs/API.md README.md docs/PROJECT_STATUS.md
git commit -m "docs: describe login and user isolation"
```

## Task 10: End-To-End Verification

**Files:**
- No new files.

- [ ] **Step 1: Run backend tests**

Run: `cd backend && mvn test`

Expected: all backend tests pass.

- [ ] **Step 2: Run frontend build and auth regression**

Run:

```bash
node frontend/lib/auth-flow.node-test.cjs
cd frontend && npm run build
```

Expected: auth regression passes and frontend build succeeds.

- [ ] **Step 3: Start local dependencies**

Run:

```bash
docker compose up -d redis qdrant
```

Expected: Redis and Qdrant containers are running.

- [ ] **Step 4: Start backend and frontend**

Run backend:

```bash
cd backend
mvn spring-boot:run
```

Run frontend in another terminal:

```bash
cd frontend
npm run dev
```

Expected: backend listens on `http://localhost:8080`, frontend listens on `http://127.0.0.1:4000`.

- [ ] **Step 5: Manual user isolation smoke**

Use browser:

```text
1. Register user alpha with password password123.
2. Submit a known wrong Two Sum solution.
3. Wait for SSE diagnosis and training plan.
4. Open dashboard and confirm alpha has recent submission and plan.
5. Logout.
6. Register user beta with password password123.
7. Open dashboard and confirm beta does not see alpha's submission, mistake card, or training plan.
8. Ask RAG chat "我最近的薄弱点是什么".
9. Confirm beta receives empty or beta-only learning memory.
10. Use beta's token to call `POST /api/agent/analyze` with alpha's `submissionId`; confirm response code is 403.
11. Use beta's token to call `GET /api/submissions/{alphaSubmissionId}/diagnosis/stream`; confirm response code is 403 and no SSE stream starts.
12. Use beta's token to call alpha's training-plan item status update path; confirm response code is 403.
```

Expected: learning data is isolated by authenticated user id.

- [ ] **Step 6: Run existing demo smoke**

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\local_dependency_preflight.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\e2e_demo_smoke.ps1
```

Expected: preflight reports the next required local action or `READY_FOR_E2E_SMOKE`; full smoke passes once the script includes login token support. If the smoke script still assumes unauthenticated APIs, update it to register/login a smoke user first and send the bearer token on protected requests.

- [ ] **Step 7: Final commit**

```bash
git status --short
git add .
git commit -m "feat: add login-based learning record isolation"
```

## Rollback Plan

If login blocks a live demo, temporarily allow the demo account through an environment flag:

```yaml
coach:
  auth:
    demo-user-fallback-enabled: ${AUTH_DEMO_USER_FALLBACK_ENABLED:false}
```

The fallback should only apply in local development and should set `currentUserId=1` when no token is provided. Keep the default `false` in deployment.

## Acceptance Criteria

- New users can register and immediately enter the app.
- Existing users can log in and recover their own learning records.
- Frontend no longer contains `DEMO_USER_ID` or `userId: 1` in learning flows.
- Backend does not trust frontend-provided `userId` for submissions, RAG chat, self-tests, training plans, or mock interviews.
- User A cannot read or mutate User B's dashboard, weaknesses, mistake cards, training plans, self-tests, mock interview records, or RAG user-memory answers.
- User B cannot call `POST /api/agent/analyze` or `GET /api/submissions/{submissionId}/diagnosis/stream` for User A's submission.
- SSE diagnosis fetch sends `Authorization: Bearer <token>`.
- Problem list/detail/template can remain public at the backend level, but frontend protects `/problem/[id]`, `/dashboard`, `/knowledge`, `/mock-interview`, and `/rag-chat` for this multi-user test deployment.
- RAG health, RAG vector retry, RAG system-index rebuild, cache status, and cache refresh endpoints require login in this MVP.
- Deployment docs include `AUTH_JWT_SECRET`.
- Existing Agent workflow remains unchanged after `currentUserId` is resolved.

## Self-Review

- Spec coverage: The plan covers registration, login, current-user resolution, frontend token storage, route guarding, user-id replacement, SSE token handling, Agent submission ownership checks, knowledge self-test isolation, maintenance endpoint protection, documentation, and verification.
- Placeholder scan: The plan contains no unresolved placeholder markers, no open task without concrete files, and no vague test-only instruction.
- Type consistency: Backend uses `AuthUserVO`, `AuthResponseVO`, `CurrentUserContext`, `JwtTokenService.TokenClaims`, and `SubmissionService.requireOwnedSubmission(...)` consistently. Frontend uses `AuthUser`, `AuthResponse`, `authApi`, `AuthGate`, `getAuthToken`, and `agentApi.streamDiagnosis(..., { token })` consistently.
- MVP discipline: The plan keeps auth minimal and protects the Agent learning loop without adding broad account-system features.
