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
