package com.interview.coach.dto;

import java.util.List;
import lombok.Data;

@Data
public class CodeReviewResult {

    private String complexity;

    private String codeStyle;

    private String interviewSuggestion;

    private List<String> optimizationPoints;
}
