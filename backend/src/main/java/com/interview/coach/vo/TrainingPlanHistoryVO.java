package com.interview.coach.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class TrainingPlanHistoryVO {

    private Long id;

    private String title;

    private String summary;

    private String status;

    private String statusLabel;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer itemCount;

    private Integer completedCount;

    private Integer skippedCount;

    private Integer pendingCount;

    private Integer handledCount;

    private BigDecimal completionRate = BigDecimal.ZERO;

    private BigDecimal handledRate = BigDecimal.ZERO;

    private LocalDateTime createdAt;
}
