package com.interview.coach.vo;

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
}
