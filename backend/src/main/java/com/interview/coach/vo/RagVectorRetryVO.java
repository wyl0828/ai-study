package com.interview.coach.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RagVectorRetryVO {

    private Boolean enabled = false;

    private Integer requestedLimit = 0;

    private Integer effectiveLimit = 0;

    private Integer attemptedCount = 0;

    private Integer matchedRetryableCount = 0;

    private Integer indexedCount = 0;

    private Integer failedCount = 0;

    private Integer skippedCount = 0;

    private LocalDateTime retriedAt;

    private String statusLabel;

    private String maintenanceAction;

    private String message;

    private String summary;
}
