package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("training_plan_item")
public class TrainingPlanItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private Integer dayIndex;

    private String knowledgePoint;

    private String problemTitle;

    private String reason;

    private String reviewFocus;

    private String status;
}
