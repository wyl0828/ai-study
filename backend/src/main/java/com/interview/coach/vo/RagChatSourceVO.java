package com.interview.coach.vo;

import lombok.Data;

@Data
public class RagChatSourceVO {

    private String sourceType;

    private Long sourceId;

    private String title;

    private int score;

    private String snippet;

    private String matchReason;
}
