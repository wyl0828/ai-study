package com.interview.coach.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class TrainingPlanResult {

    private String title;

    private String summary;

    private List<TrainingPlanItemResult> items = new ArrayList<>();

    @Data
    public static class TrainingPlanItemResult {

        private String itemType;

        private Long knowledgeCardId;

        private Integer dayIndex;

        private String knowledgePoint;

        private String problemTitle;

        private String knowledgeCardTitle;

        private String reason;

        private String reviewFocus;

        private String sourceType;

        private Long sourceId;

        private String sourceSummary;
    }
}
