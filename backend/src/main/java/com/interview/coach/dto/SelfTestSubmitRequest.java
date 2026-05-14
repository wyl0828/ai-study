package com.interview.coach.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class SelfTestSubmitRequest {

    @NotBlank(message = "userAnswer is required")
    private String userAnswer;

    @NotNull(message = "score is required")
    @Min(value = 0, message = "score must be between 0 and 100")
    @Max(value = 100, message = "score must be between 0 and 100")
    private Integer score;

    private String feedback;

    private List<String> missingKeyPoints;
}
