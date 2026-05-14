package com.interview.coach.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserWeaknessVO {

    private Long id;

    private String knowledgePoint;

    private String errorType;

    private Integer wrongCount;

    private BigDecimal weaknessScore;

    private String trendLabel;

    private BigDecimal lastDeltaScore;

    private LocalDateTime lastEventAt;
}
