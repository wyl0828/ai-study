package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_weakness_event")
public class UserWeaknessEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String knowledgePoint;

    private String errorType;

    private String sourceType;

    private Long sourceId;

    private BigDecimal deltaScore;

    private BigDecimal beforeScore;

    private BigDecimal afterScore;

    private String reason;

    private LocalDateTime createdAt;
}
