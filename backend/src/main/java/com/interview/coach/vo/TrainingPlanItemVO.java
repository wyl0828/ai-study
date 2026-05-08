package com.interview.coach.vo;

import lombok.Data;

@Data
public class TrainingPlanItemVO {

    private Integer dayIndex;

    private String knowledgePoint;

    private String problemTitle;

    private String reason;

    private String reviewFocus;

    private String status;
}
