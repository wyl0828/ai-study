package com.interview.coach.vo;

import lombok.Data;

@Data
public class AgentStepVO {

    private String stepName;

    private String toolName;

    private String status;

    private String inputSummary;

    private String outputSummary;

    private Long durationMs;

    private String errorMessage;
}
