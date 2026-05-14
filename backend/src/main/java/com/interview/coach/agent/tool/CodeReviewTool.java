package com.interview.coach.agent.tool;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.CodeReviewResult;
import com.interview.coach.integration.ai.AnthropicCompatibleClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CodeReviewTool implements Tool<AgentContext, CodeReviewResult> {

    private final AnthropicCompatibleClient aiClient;

    @Override
    public String name() {
        return "CodeReviewTool";
    }

    @Override
    public CodeReviewResult execute(AgentContext input, AgentContext context) {
        CodeReviewResult result = aiClient.askJson(systemPrompt(), userPrompt(context), CodeReviewResult.class);
        context.setCodeReview(result);
        return result;
    }

    private String systemPrompt() {
        return """
                You are a senior Java interview coach reviewing an accepted submission.
                Think briefly, then return only one compact JSON object with keys:
                complexity, codeStyle, interviewSuggestion, optimizationPoints.
                complexity: time and space complexity in Chinese, e.g. "时间复杂度 O(n)，空间复杂度 O(n)".
                codeStyle: brief assessment of code readability, naming, structure (Chinese, under 80 chars).
                interviewSuggestion: what the candidate should proactively explain in an interview about this solution (Chinese, under 100 chars).
                optimizationPoints: array of 2-4 concrete optimization suggestions (Chinese, each under 40 chars).
                All user-facing text fields must be natural Simplified Chinese.
                You may keep technical terms such as Java, HashMap, containsKey, null, O(n).
                Focus on actionable feedback that helps in a real interview setting.
                Use retrieved evidence only as supporting context.
                Do not copy retrieved text verbatim when a short review is enough.
                If retrieved evidence conflicts with execution result, trust execution result.
                Do not provide a full alternative solution.
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
                Runtime: %s ms
                Memory: %s KB

                Retrieved evidence:
                %s

                Review this accepted solution for interview readiness.
                """.formatted(
                context.getProblem().getTitle(),
                context.getProblem().getCategory(),
                context.getKnowledgePoints(),
                context.getSubmission().getCode(),
                context.getObservation().getStatus(),
                context.getObservation().getPassedCount(),
                context.getObservation().getTotalCount(),
                context.getObservation().getRuntime(),
                context.getObservation().getMemory(),
                ragEvidence(context));
    }

    private String ragEvidence(AgentContext context) {
        if (context.getRagRetrieveResult() == null || !context.getRagRetrieveResult().hasHits()) {
            return "没有检索到可用证据。";
        }
        return context.getRagRetrieveResult().toPromptBlock();
    }
}
