package com.interview.coach.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class SelfTestRecordVO {

    private Long id;

    private Long knowledgeCardId;

    private Integer score;

    private String feedback;

    private List<String> missingKeyPoints;

    private LocalDateTime createdAt;
}
