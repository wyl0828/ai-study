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
