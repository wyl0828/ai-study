package com.interview.coach.vo;

import java.util.List;
import lombok.Data;

@Data
public class KnowledgeCardVO {

    private Long id;

    private String category;

    private String label;

    private String title;

    private String question;

    private String answer;

    private String followUp;

    private List<String> keyPoints;

    private String difficulty;

    private List<String> tags;

    private String sourceName;

    private String sourceUrl;
}
