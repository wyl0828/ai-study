package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("training_plan_item")
public class TrainingPlanItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private String itemType;

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

    private String status;

    private LocalDateTime statusUpdatedAt;
}
