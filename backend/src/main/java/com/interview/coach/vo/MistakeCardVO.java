package com.interview.coach.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MistakeCardVO {

    private Long id;

    private Long problemId;

    private String problemTitle;

    private String errorType;

    private String knowledgePoint;

    private String mistakeSummary;

    private String correctIdea;

    private Integer repeatCount;

    private LocalDateTime lastSeenAt;

    private String status;
}
