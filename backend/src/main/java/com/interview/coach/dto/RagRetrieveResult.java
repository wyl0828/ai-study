package com.interview.coach.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class RagRetrieveResult {

    private List<RagChunkHit> hits = new ArrayList<>();

    public boolean hasHits() {
        return hits != null && !hits.isEmpty();
    }

    public String summary() {
        if (!hasHits()) {
            return "未检索到 RAG 证据";
        }
        return "已检索到 " + hits.size() + " 条 RAG 证据";
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
}
