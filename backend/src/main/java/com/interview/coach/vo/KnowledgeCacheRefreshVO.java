package com.interview.coach.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class KnowledgeCacheRefreshVO {

    private Boolean enabled = false;

    private Boolean redisAvailable = false;

    private Boolean categoryWarmAttempted = false;

    private Integer listWarmAttemptedCount = 0;

    private Integer detailWarmAttemptedCount = 0;

    private Integer totalWarmAttemptedCount = 0;

    private Integer failedCount = 0;

    private String message;

    private String summary;

    private String statusLabel;

    private String maintenanceAction;

    private LocalDateTime refreshedAt;
}
