package com.interview.coach.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JudgeCase {

    private Long caseId;

    private String input;

    private String expectedOutput;
}
