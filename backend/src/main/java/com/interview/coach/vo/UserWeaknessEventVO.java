package com.interview.coach.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserWeaknessEventVO {

    private Long id;

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
