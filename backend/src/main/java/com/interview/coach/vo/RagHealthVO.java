package com.interview.coach.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class RagHealthVO {

    private Boolean healthy = false;

    private Boolean tablesAvailable = true;

    private Integer systemDocumentCount = 0;

    private Integer systemChunkCount = 0;

    private Integer userMemoryDocumentCount = 0;

    private Integer userMemoryChunkCount = 0;

    private Integer duplicateSystemDocumentCount = 0;

    private Integer staleKnowledgeCardDocumentCount = 0;

    private List<String> warnings = new ArrayList<>();

    private LocalDateTime checkedAt;
}
