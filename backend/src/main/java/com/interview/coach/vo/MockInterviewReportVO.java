package com.interview.coach.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class MockInterviewReportVO {

    private Long id;

    private BigDecimal averageScore;

    private String summary;

    private String strengths;

    private String weaknesses;

    private String expressionAdvice;

    private List<Long> recommendedCardIds;

    private List<String> weaknessTags;

    private LocalDateTime createdAt;
}
