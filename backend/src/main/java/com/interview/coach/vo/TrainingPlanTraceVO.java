package com.interview.coach.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class TrainingPlanTraceVO {

    private Long planId;

    private String title;

    private String summary;

    private String status;

    private String statusLabel;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDateTime planCreatedAt;

    private Integer daysSinceCreated = 0;

    private Integer daysRemaining = 0;

    private Boolean overdue = false;

    private Integer itemCount = 0;

    private Integer pendingCount = 0;

    private Integer completedCount = 0;

    private Integer skippedCount = 0;

    private Integer handledCount = 0;

    private BigDecimal completionRate = BigDecimal.ZERO;

    private BigDecimal handledRate = BigDecimal.ZERO;

    private String progressSummary;

    private Map<String, Integer> sourceTypeCounts = Map.of();

    private String sourceTypeSummary;

    private TrainingPlanItemVO nextItem;

    private String nextAction;

    private String nextActionReason;

    private String nextActionPriority;

    private String nextTargetHref;

    private String nextTargetLabel;

    private List<TrainingPlanActivityVO> recentActivities = List.of();

    private String latestActivitySummary;

    private LocalDateTime latestActivityAt;
}
