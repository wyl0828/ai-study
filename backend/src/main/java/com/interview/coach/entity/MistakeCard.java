package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("mistake_card")
public class MistakeCard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long problemId;

    private Long submissionId;

    private Long agentRunId;

    private String errorType;

    private String knowledgePoint;

    private String mistakeSummary;

    private String correctIdea;

    private LocalDateTime createdAt;
}
