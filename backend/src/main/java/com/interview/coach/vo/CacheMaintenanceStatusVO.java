package com.interview.coach.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CacheMaintenanceStatusVO {

    private String provider = "Redis";

    private ProblemCacheStatusVO problem;

    private KnowledgeCacheStatusVO knowledge;

    private Boolean allEnabled = false;

    private Boolean allRedisAvailable = false;

    private Integer cachedKeyCount = 0;

    private Long hitCount = 0L;

    private Long missCount = 0L;

    private Long fallbackCount = 0L;

    private Integer hitRate = 0;

    private String lastFallbackReason;

    private String probeWarning;

    private String statusLabel;

    private String summary;

    private String cacheBenefitSummary;

    private String fallbackRiskSummary;

    private String protectedDataSummary;

    private LocalDateTime checkedAt;

    private String maintenanceAction;

    private String boundary = "Only read-mostly problem and knowledge-card responses are cached; learning state remains MySQL-backed.";
}
