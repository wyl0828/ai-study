package com.interview.coach.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TrainingPlanHistoryVO {

    private Long id;

    private String title;

    private String summary;

    private String status;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer itemCount;

    private Integer completedCount;

    private Integer skippedCount;

    private LocalDateTime createdAt;
}
