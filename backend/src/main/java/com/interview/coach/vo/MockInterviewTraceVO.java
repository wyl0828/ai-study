package com.interview.coach.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class MockInterviewTraceVO {

    private Integer sessionCount = 0;

    private Integer reportedSessionCount = 0;

    private Long latestSessionId;

    private String latestSessionStatus;

    private String latestSessionStatusLabel;

    private String latestCategory;

    private Long latestReportId;

    private BigDecimal latestAverageScore;

    private List<String> latestWeaknessTags = List.of();

    private List<Long> recommendedCardIds = List.of();

    private Integer answeredTurnCount = 0;

    private Integer lowScoreTurnCount = 0;

    private Integer weaknessEventCount = 0;

    private Integer trainingPlanItemCount = 0;

    private Boolean reportTrainingPlanLinked = false;

    private LocalDateTime latestInterviewAt;

    private String closureStatus;

    private String closureStatusLabel;

    private String nextAction;

    private String nextActionReason;

    private String nextActionPriority;

    private String nextTargetHref;

    private String nextTargetLabel;

    private String reportReviewHref;

    private String reportReviewLabel;

    private String closureSummary;

    private String reviewPathSummary;
}
