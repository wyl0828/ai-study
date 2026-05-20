package com.interview.coach.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class AiDiagnosisResult {

    private String errorType;

    private String knowledgePoint;

    private String specificError;

    private String diagnosis;

    private String suggestion;

    private String failurePhenomenon;

    private String rootCause;

    private String repairDirection;

    private String interviewReminder;

    private BigDecimal confidence;

    private BigDecimal weaknessScoreDelta;
}
