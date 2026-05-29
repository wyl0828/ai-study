package com.interview.coach.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CacheMaintenanceRefreshVO {

    private ProblemCacheRefreshVO problem;

    private KnowledgeCacheRefreshVO knowledge;

    private Integer totalWarmAttemptedCount = 0;

    private Integer failedCount = 0;

    private String statusLabel;

    private String maintenanceAction;

    private String message;

    private String summary;

    private String refreshScopeSummary;

    private String warmupResultSummary;

    private String protectedDataSummary;

    private LocalDateTime refreshedAt;

    private String boundary;
}
