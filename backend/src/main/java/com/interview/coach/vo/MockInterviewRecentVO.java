package com.interview.coach.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class MockInterviewRecentVO {

    private Long sessionId;

    private String category;

    private String status;

    private String interviewerStyle;

    private Integer questionCount;

    private Integer answeredMainCount;

    private BigDecimal averageScore;

    private List<String> weaknessTags;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime createdAt;
}
