package com.interview.coach.agent.tool;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.HintGenerationResult;
import com.interview.coach.integration.ai.AnthropicCompatibleClient;
import lombok.RequiredArgsConstructor;

/**
 * Legacy compatibility tool. Current product flow uses preset problem hints from the problem table,
 * and InterviewCoachAgent does not invoke this tool.
 */
@Deprecated
@RequiredArgsConstructor
public class HintGeneratorTool implements Tool<AgentContext, HintGenerationResult> {

    private final AnthropicCompatibleClient aiClient;

    @Override
    public String name() {
        return "HintGeneratorTool";
    }

    @Override
    public HintGenerationResult execute(AgentContext input, AgentContext context) {
        HintGenerationResult result = aiClient.askJson(systemPrompt(), userPrompt(context), HintGenerationResult.class);
        context.setHints(result);
        return result;
    }

    private String systemPrompt() {
        return """
                You are an interview coach. Generate layered hints, not answers.
                Think briefly, then return only one compact JSON object with keys hintLevel1, hintLevel2, hintLevel3.
                Absolutely do not reveal a full accepted Java solution.
                All hint text must be natural Simplified Chinese.
                You may keep technical terms such as Java, HashMap, containsKey, null.
                hintLevel1: direction only.
                hintLevel2: related knowledge point and likely issue.
                hintLevel3: pseudocode or key idea only, no complete Java code.
                Keep each hint under 80 Chinese characters.
                """;
    }

    private String userPrompt(AgentContext context) {
        return """
                Problem title: %s
                Category: %s
                Knowledge points: %s
                Diagnosis error type: %s
                Diagnosis: %s
                Specific error: %s
                Failed cases: %s
                Code:
                %s

                Generate three layered hints for this failed Java submission.
                """.formatted(
                context.getProblem().getTitle(),
                context.getProblem().getCategory(),
                context.getKnowledgePoints(),
                context.getDiagnosis().getErrorType(),
                context.getDiagnosis().getDiagnosis(),
                context.getDiagnosis().getSpecificError(),
                context.getObservation().getFailedCases(),
                context.getSubmission().getCode());
    }
}
