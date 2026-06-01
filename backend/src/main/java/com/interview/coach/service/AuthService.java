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
