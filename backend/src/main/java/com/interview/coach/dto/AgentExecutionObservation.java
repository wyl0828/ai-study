package com.interview.coach.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class AgentExecutionObservation {

    private String status;

    private Integer passedCount;

    private Integer totalCount;

    private Integer runtime;

    private Integer memory;

    private String errorMessage;

    private List<FailedCaseResult> failedCases = new ArrayList<>();
}
