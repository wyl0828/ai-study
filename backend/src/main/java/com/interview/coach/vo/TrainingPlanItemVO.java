package com.interview.coach.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TrainingPlanItemVO {

    private Long id;

    private String itemType;

    private Long problemId;

    private Long knowledgeCardId;

    private Integer dayIndex;

    private String knowledgePoint;

    private String problemTitle;

    private String knowledgeCardTitle;

    private String reason;

    private String reviewFocus;

    private String sourceType;

    private Long sourceId;

    private String sourceSummary;

    private String targetHref;

    private String targetLabel;

    private String status;

    private LocalDateTime statusUpdatedAt;
}
