package com.interview.coach.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class MockInterviewSessionVO {

    private Long sessionId;

    private String status;

    private String category;

    private String interviewerStyle;

    private Integer questionCount;

    private Integer answeredMainCount;

    private Long currentKnowledgeCardId;

    private String currentQuestion;

    private String currentTurnType;

    private List<MockInterviewTurnVO> turns;

    private MockInterviewReportVO report;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}
