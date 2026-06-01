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
