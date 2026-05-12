package com.interview.coach.vo;

import java.util.List;
import lombok.Data;

@Data
public class ErrorStatsVO {

    private List<ErrorTypeCount> errorTypeDistribution;

    private List<KnowledgeWeakness> topWeakPoints;

    @Data
    public static class ErrorTypeCount {

        private String errorType;

        private Integer count;
    }

    @Data
    public static class KnowledgeWeakness {

        private String knowledgePoint;

        private String errorType;

        private Integer wrongCount;

        private Double weaknessScore;
    }
}
