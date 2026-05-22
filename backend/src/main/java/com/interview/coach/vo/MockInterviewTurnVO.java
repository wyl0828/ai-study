package com.interview.coach.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class MockInterviewTurnVO {

    private Long id;

    private Long knowledgeCardId;

    private Integer turnOrder;

    private String turnType;

    private Long parentTurnId;

    private String question;

    private String userAnswer;

    private Integer score;

    private String feedback;

    private String performanceLevel;

    private String strengthSummary;

    private String gapSummary;

    private String expressionFeedback;

    private String interviewerObservation;

    private String followUpReason;

    private List<String> hitKeyPoints;

    private List<String> missingKeyPoints;

    private String expressionIssue;

    private LocalDateTime createdAt;
}
