package com.interview.coach.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;

@Data
public class RagRetrieveResult {

    private List<RagChunkHit> hits = new ArrayList<>();

    private boolean vectorEnabled;

    private boolean vectorDowngraded;

    public boolean hasHits() {
        return hits != null && !hits.isEmpty();
    }

    public String summary() {
        if (!hasHits()) {
            return "未检索到 RAG 证据；模式：" + retrievalMode();
        }
        return "已检索到 " + hits.size() + " 条 RAG 证据；来源："
                + sourceTypeSummary() + "；模式：" + retrievalMode();
    }

    public String toPromptBlock() {
        if (!hasHits()) {
            return "没有检索到可用证据。";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            builder.append(hits.get(i).toPromptLine(i + 1)).append("\n");
        }
        return builder.toString();
    }

    private String sourceTypeSummary() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (RagChunkHit hit : hits) {
            counts.merge(sourceTypeGroup(hit.getSourceType()), 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining("、"));
    }

    private String sourceTypeGroup(String sourceType) {
        if ("PROBLEM".equals(sourceType)) {
            return "题目";
        }
        if ("KNOWLEDGE_CARD".equals(sourceType)) {
            return "知识卡";
        }
        if ("AI_DIAGNOSIS".equals(sourceType) || "MISTAKE_CARD".equals(sourceType)) {
            return "用户记忆";
        }
        return sourceType == null || sourceType.isBlank() ? "未知来源" : sourceType;
    }

    private String retrievalMode() {
        if (!vectorEnabled) {
            return "MySQL-only";
        }
        return vectorDowngraded ? "MySQL-only fallback" : "MySQL+Qdrant hybrid";
    }
}
