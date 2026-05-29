package com.interview.coach.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProblemCacheStatusVO {

    private Boolean enabled;

    private String provider;

    private Boolean redisAvailable;

    private String statusLabel;

    private String summary;

    private LocalDateTime checkedAt;

    private String maintenanceAction;

    private String listKey;

    private String detailKeyPattern;

    private String templateKeyPattern;

    private Long listTtlSeconds;

    private Long detailTtlSeconds;

    private Long templateTtlSeconds;

    private Integer cachedKeyCount = 0;

    private Boolean listCached = false;

    private Integer detailCachedKeyCount = 0;

    private Integer templateCachedKeyCount = 0;

    private Long hitCount = 0L;

    private Long missCount = 0L;

    private Long fallbackCount = 0L;

    private Integer hitRate = 0;

    private String lastFallbackReason;

    private String probeWarning;

    private String fallback;
}
