package com.interview.coach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MockInterviewCreateRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String category;

    private Integer questionCount;

    private String interviewerStyle;
}
