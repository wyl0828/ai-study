package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("training_plan")
public class TrainingPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long agentRunId;

    private String title;

    private String summary;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDateTime createdAt;
}
