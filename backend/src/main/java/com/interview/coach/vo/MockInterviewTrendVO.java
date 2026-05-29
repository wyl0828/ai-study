package com.interview.coach.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MockInterviewTrendVO {

    private Long knowledgeCardId;

    private String knowledgePoint;

    private String category;

    private Long latestSessionId;

    private BigDecimal latestScore;

    private BigDecimal previousScore;

    private BigDecimal deltaScore;

    private String trendLabel;

    private Integer interviewCount;

    private String latestIssue;

    private String latestIssueType;

    private String latestIssueTypeLabel;

    private LocalDateTime lastInterviewAt;
}
