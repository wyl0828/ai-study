package com.interview.coach.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class MockInterviewAiEvaluationResponse {

    private Integer score;

    private List<String> hitKeyPoints = new ArrayList<>();

    private List<String> missingKeyPoints = new ArrayList<>();

    private String feedback;

    private String expressionIssue;

    private String followUpQuestion;

    private List<String> weaknessTags = new ArrayList<>();

    private List<Long> recommendedCardIds = new ArrayList<>();
}
