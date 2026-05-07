package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("hint_record")
public class HintRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long agentRunId;

    private Long submissionId;

    private Long userId;

    private Long problemId;

    private Integer hintLevel;

    private String hintContent;

    private LocalDateTime createdAt;
}
