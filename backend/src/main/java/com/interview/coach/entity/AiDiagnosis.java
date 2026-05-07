package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("ai_diagnosis")
public class AiDiagnosis {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long agentRunId;

    private Long submissionId;

    private Long userId;

    private Long problemId;

    private String errorType;

    private String knowledgePoint;

    private String specificError;

    private String diagnosis;

    private String suggestion;

    private BigDecimal confidence;

    private LocalDateTime createdAt;
}
