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
