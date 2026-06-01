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
