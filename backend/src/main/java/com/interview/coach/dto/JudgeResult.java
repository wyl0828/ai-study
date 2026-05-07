package com.interview.coach.dto;

import com.interview.coach.enums.SubmissionStatusEnum;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class JudgeResult {

    private SubmissionStatusEnum status;

    private Integer passedCount = 0;

    private Integer totalCount = 0;

    private Integer runtime;

    private Integer memory;

    private String errorMessage;

    private List<FailedCaseResult> failedCases = new ArrayList<>();
}
