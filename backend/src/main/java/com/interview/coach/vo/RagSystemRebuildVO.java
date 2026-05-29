package com.interview.coach.vo;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RagSystemRebuildVO {

    private Boolean attempted = false;

    private Boolean success = false;

    private Boolean vectorEnabled = false;

    private Integer indexedProblemCount = 0;

    private Integer indexedKnowledgeCardCount = 0;

    private Integer beforeSystemDocumentCount = 0;

    private Integer afterSystemDocumentCount = 0;

    private Integer beforeSystemChunkCount = 0;

    private Integer afterSystemChunkCount = 0;

    private Integer beforeUserMemoryDocumentCount = 0;

    private Integer afterUserMemoryDocumentCount = 0;

    private Integer beforeUserMemoryChunkCount = 0;

    private Integer afterUserMemoryChunkCount = 0;

    private List<String> warnings = new ArrayList<>();

    private LocalDateTime rebuiltAt;

    private String statusLabel;

    private String maintenanceAction;

    private String message;

    private String summary;

    private String boundary;
}
