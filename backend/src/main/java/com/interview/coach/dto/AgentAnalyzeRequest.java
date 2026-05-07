package com.interview.coach.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgentAnalyzeRequest {

    @NotNull(message = "submissionId is required")
    private Long submissionId;
}
