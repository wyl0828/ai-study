package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("agent_step")
public class AgentStepEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long agentRunId;

    private String stepName;

    private String toolName;

    private String status;

    private String inputSummary;

    private String outputSummary;

    private Long durationMs;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime createdAt;
}
