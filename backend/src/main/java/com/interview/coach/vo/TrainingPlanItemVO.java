package com.interview.coach.vo;

import lombok.Data;

@Data
public class TrainingPlanItemVO {

    private Long id;

    private String itemType;

    private Long knowledgeCardId;

    private Integer dayIndex;

    private String knowledgePoint;

    private String problemTitle;

    private String knowledgeCardTitle;

    private String reason;

    private String reviewFocus;

    private String status;
}
