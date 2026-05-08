package com.interview.coach.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class UserWeaknessVO {

    private Long id;

    private String knowledgePoint;

    private String errorType;

    private Integer wrongCount;

    private BigDecimal weaknessScore;
}
