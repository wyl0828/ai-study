package com.interview.coach.vo;

import lombok.Data;

@Data
public class TestCaseVO {

    private Long id;

    private String input;

    private String expectedOutput;

    private Boolean sample;
}
