package com.interview.coach.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TrainingPlanItemStatusRequest {

    @NotBlank(message = "status is required")
    private String status;
}
