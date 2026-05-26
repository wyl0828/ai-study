package com.interview.coach.dto;

import lombok.Data;

@Data
public class RagChunkHit {

    private Long chunkId;

    private Long documentId;

    private String sourceType;

    private Long sourceId;

    private Long userId;

    private Long problemId;

    private String title;

    private String knowledgePoint;

    private String errorType;

    private String chunkText;

    private int score;

    private String matchReason;

    public String toPromptLine(int index) {
        return "%d. [%s#%s score=%d reason=%s] %s".formatted(
                index, sourceType, sourceId, score, compact(matchReason), compact(chunkText));
    }

    private String compact(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 180) + "...";
    }
}
