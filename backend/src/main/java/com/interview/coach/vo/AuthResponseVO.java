package com.interview.coach.vo;

import lombok.Data;

@Data
public class AuthResponseVO {

    private String token;

    private AuthUserVO user;
}
