package com.interview.coach.agent.tool;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.AiDiagnosisResult;
import com.interview.coach.integration.ai.AnthropicCompatibleClient;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ErrorClassifierTool implements Tool<AgentContext, AiDiagnosisResult> {

    private final AnthropicCompatibleClient aiClient;

    @Override
    public String name() {
        return "ErrorClassifierTool";
    }

    @Override
    public AiDiagnosisResult execute(AgentContext input, AgentContext context) {
        AiDiagnosisResult result = aiClient.askJson(systemPrompt(), userPrompt(context), AiDiagnosisResult.class);
        if (result.getConfidence() == null) {
            result.setConfidence(new BigDecimal("0.50"));
        }
        if (result.getWeaknessScoreDelta() == null) {
            result.setWeaknessScoreDelta(new BigDecimal("5"));
        }
        context.setDiagnosis(result);
        return result;
    }

    private String systemPrompt() {
        return """
                You are an interview coach diagnosing Java backend interview submissions.
                Think briefly, then return only one compact JSON object with keys:
                errorType, knowledgePoint, specificError, diagnosis, suggestion, confidence, weaknessScoreDelta.
                errorType must be one of SYNTAX_ERROR, LOGIC_ERROR, BOUNDARY_ERROR, ALGORITHM_ERROR,
                TIMEOUT, RUNTIME_ERROR, SYSTEM_ERROR, ACCEPTED_REVIEW.
                weaknessScoreDelta must be a positive number from 1 to 10 for a failed submission.
                Do not provide a full accepted Java solution.
                Keep every text field under 80 Chinese characters or 40 English words.
                """;
    }

    private String userPrompt(AgentContext context) {
        return """
                Problem title: %s
                Problem category: %s
                Knowledge points: %s
                Submission code:
                %s

                Execution status: %s
                Passed: %s/%s
                Error message: %s
                Failed cases: %s

                Diagnose the most likely interview weakness and return structured JSON.
                """.formatted(
                context.getProblem().getTitle(),
                context.getProblem().getCategory(),
                context.getKnowledgePoints(),
                context.getSubmission().getCode(),
                context.getObservation().getStatus(),
                context.getObservation().getPassedCount(),
                context.getObservation().getTotalCount(),
                context.getObservation().getErrorMessage(),
                context.getObservation().getFailedCases());
    }
}
