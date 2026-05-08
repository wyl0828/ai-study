package com.interview.coach.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SubmissionHistoryVO {

    private Long problemId;

    private String problemTitle;

    private String status;

    private Integer passedCount;

    private Integer totalCount;

    private LocalDateTime createdAt;
}
