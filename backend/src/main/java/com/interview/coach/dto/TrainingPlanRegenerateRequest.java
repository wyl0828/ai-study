package com.interview.coach.dto;

import lombok.Data;

@Data
public class TrainingPlanRegenerateRequest {

    private Boolean replaceCurrentPlan = true;

    private String reason;
}
