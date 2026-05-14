package com.interview.coach.agent;

import com.interview.coach.dto.AgentExecutionObservation;
import com.interview.coach.dto.AiDiagnosisResult;
import com.interview.coach.dto.CodeReviewResult;
import com.interview.coach.dto.HintGenerationResult;
import com.interview.coach.dto.RagRetrieveResult;
import com.interview.coach.dto.TrainingPlanResult;
import com.interview.coach.entity.Problem;
import com.interview.coach.entity.Submission;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class AgentContext {

    private Long agentRunId;

    private Long submissionId;

    private Long userId;

    private Long problemId;

    private Submission submission;

    private Problem problem;

    private List<String> knowledgePoints = new ArrayList<>();

    private AgentExecutionObservation observation;

    private AiDiagnosisResult diagnosis;

    private CodeReviewResult codeReview;

    private HintGenerationResult hints;

    private RagRetrieveResult ragRetrieveResult;

    private TrainingPlanResult trainingPlan;

    private List<AgentStep> steps = new ArrayList<>();
}
