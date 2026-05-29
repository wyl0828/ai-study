package com.interview.coach.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class RagHealthVO {

    private Boolean healthy = false;

    private Boolean tablesAvailable = true;

    private String statusLabel;

    private String maintenanceSummary;

    private String preferredMaintenanceAction;

    private String nextMaintenanceEndpoint;

    private String maintenancePriority;

    private String maintenanceReason;

    private Integer systemDocumentCount = 0;

    private Integer systemChunkCount = 0;

    private Integer enabledProblemCount = 0;

    private Integer enabledKnowledgeCardCount = 0;

    private Integer missingSystemProblemDocumentCount = 0;

    private Integer missingSystemKnowledgeCardDocumentCount = 0;

    private Integer userMemoryDocumentCount = 0;

    private Integer userMemoryChunkCount = 0;

    private Integer duplicateSystemDocumentCount = 0;

    private Integer staleProblemDocumentCount = 0;

    private Integer staleKnowledgeCardDocumentCount = 0;

    private Map<String, Integer> documentSourceTypeCounts = Map.of();

    private Map<String, Integer> chunkSourceTypeCounts = Map.of();

    private Boolean vectorEnabled = false;

    private Integer vectorIndexedChunkCount = 0;

    private Integer vectorFailedChunkCount = 0;

    private Integer vectorPendingChunkCount = 0;

    private List<String> warnings = new ArrayList<>();

    private List<String> maintenanceActions = new ArrayList<>();

    private LocalDateTime checkedAt;
}
