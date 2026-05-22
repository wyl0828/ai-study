package com.interview.coach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RagChatRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotBlank(message = "question is required")
    private String question;
}
