package com.interview.coach.vo;

import java.util.List;
import lombok.Data;

@Data
public class ProblemDetailVO {

    private Long id;

    private String title;

    private String description;

    private String difficulty;

    private String category;

    private String inputFormat;

    private String outputFormat;

    private List<String> knowledgePoints;

    private List<TestCaseVO> sampleCases;

    private String solutionOutline;

    private PresetHintsVO presetHints;

    @Data
    public static class PresetHintsVO {

        private String level1;

        private String level2;

        private String level3;
    }
}
