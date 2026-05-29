package com.interview.coach.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class KnowledgeCacheStatusVO {

    private Boolean enabled;

    private String provider;

    private Boolean redisAvailable;

    private String statusLabel;

    private String summary;

    private LocalDateTime checkedAt;

    private String maintenanceAction;

    private String categoryKey;

    private String listKeyPattern;

    private String detailKeyPattern;

    private Long categoryTtlSeconds;

    private Long listTtlSeconds;

    private Long detailTtlSeconds;

    private Integer cachedKeyCount = 0;

    private Boolean categoryCached = false;

    private Integer listCachedKeyCount = 0;

    private Integer detailCachedKeyCount = 0;

    private Long hitCount = 0L;

    private Long missCount = 0L;

    private Long fallbackCount = 0L;

    private Integer hitRate = 0;

    private String lastFallbackReason;

    private String probeWarning;

    private String fallback;
}
