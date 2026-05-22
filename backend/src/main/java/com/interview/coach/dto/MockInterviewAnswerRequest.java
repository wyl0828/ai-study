package com.interview.coach.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MockInterviewAnswerRequest {

    @NotBlank
    private String userAnswer;
}
