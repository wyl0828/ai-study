package com.interview.coach.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class RagRetrieveQuery {

    private Long userId;

    private Long problemId;

    private String problemTitle;

    private String problemCategory;

    private String errorType;

    private String knowledgePoint;

    private String executionStatus;

    private String errorMessage;

    private List<String> keywords = new ArrayList<>();

    private int limit = 5;
}
