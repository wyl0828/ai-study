package com.interview.coach.vo;

import lombok.Data;

@Data
public class FailedCaseVO {

    private Long caseId;

    private String input;

    private String expectedOutput;

    private String actualOutput;
}
