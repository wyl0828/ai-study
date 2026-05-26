package com.interview.coach.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TrainingPlanActivityVO {

    private Long itemId;

    private Long planId;

    private String planTitle;

    private String itemType;

    private String taskTitle;

    private String knowledgePoint;

    private String sourceType;

    private String sourceSummary;

    private String status;

    private LocalDateTime statusUpdatedAt;
}
