package com.interview.coach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitCodeRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "problemId is required")
    private Long problemId;

    @NotBlank(message = "language is required")
    private String language;

    @NotBlank(message = "code is required")
    private String code;
}
