package com.interview.coach.vo;

import java.util.List;
import lombok.Data;

@Data
public class SubmissionResultVO {

    private Long submissionId;

    private String status;

    private Integer passedCount;

    private Integer totalCount;

    private Integer runtime;

    private Integer memory;

    private String errorMessage;

    private List<FailedCaseVO> failedCases;
}
