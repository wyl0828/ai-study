package com.interview.coach.vo;

import com.interview.coach.dto.CodeReviewResult;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class AgentAnalyzeVO {

    private Long agentRunId;

    private Long submissionId;

    private String errorType;

    private String knowledgePoint;

    private String specificError;

    private String diagnosis;

    private CodeReviewResult codeReview;

    private String hintLevel1;

    private String hintLevel2;

    private String hintLevel3;

    private String trainingPlanTitle;

    private List<AgentStepVO> steps = new ArrayList<>();
}
