package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_weakness")
public class UserWeakness {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String knowledgePoint;

    private String errorType;

    private Integer wrongCount;

    private Integer submitCount;

    private BigDecimal weaknessScore;

    private LocalDateTime lastWrongAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
