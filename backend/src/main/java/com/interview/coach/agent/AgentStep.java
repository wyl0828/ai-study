package com.interview.coach.agent;

import com.interview.coach.enums.AgentState;
import com.interview.coach.enums.AgentStepStatusEnum;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AgentStep {

    private Long id;

    private AgentState state;

    private String toolName;

    private AgentStepStatusEnum status;

    private String inputSummary;

    private String outputSummary;

    private Long durationMs;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}
