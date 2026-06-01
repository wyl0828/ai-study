package com.interview.coach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RagChatRequest {

    private Long userId;

    @NotBlank(message = "question is required")
    private String question;
}
