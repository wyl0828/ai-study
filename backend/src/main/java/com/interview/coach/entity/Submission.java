package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("submission")
public class Submission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long problemId;

    private String language;

    private String code;

    private String status;

    private Integer passedCount;

    private Integer totalCount;

    private Integer executionTime;

    private Integer memoryUsage;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
