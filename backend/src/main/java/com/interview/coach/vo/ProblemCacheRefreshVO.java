package com.interview.coach.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProblemCacheRefreshVO {

    private Boolean enabled = false;

    private Boolean redisAvailable = false;

    private Boolean listWarmAttempted = false;

    private Integer detailWarmAttemptedCount = 0;

    private Integer templateWarmAttemptedCount = 0;

    private Integer totalWarmAttemptedCount = 0;

    private Integer failedCount = 0;

    private String message;

    private String summary;

    private String statusLabel;

    private String maintenanceAction;

    private LocalDateTime refreshedAt;
}
