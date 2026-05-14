package com.interview.coach.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.AgentExecutionObservation;
import com.interview.coach.dto.CodeReviewResult;
import com.interview.coach.dto.RagChunkHit;
import com.interview.coach.dto.RagRetrieveResult;
import com.interview.coach.entity.Problem;
import com.interview.coach.entity.Submission;
import com.interview.coach.integration.ai.AnthropicCompatibleClient;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CodeReviewToolTest {

    @Mock
    private AnthropicCompatibleClient aiClient;

    @InjectMocks
    private CodeReviewTool tool;

    @Test
    void promptIncludesRagEvidenceWithoutTurningReviewIntoAnswerGeneration() {
        CodeReviewResult expected = new CodeReviewResult();
        expected.setComplexity("时间复杂度 O(n)，空间复杂度 O(n)");
        when(aiClient.askJson(anyString(), anyString(), eq(CodeReviewResult.class))).thenReturn(expected);

        AgentContext context = contextWithRagEvidence();

        CodeReviewResult result = tool.execute(context, context);

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(aiClient).askJson(systemPrompt.capture(), userPrompt.capture(), eq(CodeReviewResult.class));
        assertThat(systemPrompt.getValue())
                .contains("Use retrieved evidence only as supporting context")
                .contains("Do not provide a full alternative solution");
        assertThat(userPrompt.getValue())
                .contains("Retrieved evidence:")
                .contains("面试中说明 HashMap 查找顺序和复杂度");
    }

    private AgentContext contextWithRagEvidence() {
        AgentContext context = new AgentContext();
        Problem problem = new Problem();
        problem.setTitle("两数之和");
        problem.setCategory("HashMap");
        context.setProblem(problem);
        context.setKnowledgePoints(List.of("HashMap 基础查找"));
        Submission submission = new Submission();
        submission.setCode("class Solution { }");
        context.setSubmission(submission);
        AgentExecutionObservation observation = new AgentExecutionObservation();
        observation.setStatus("ACCEPTED");
        observation.setPassedCount(3);
        observation.setTotalCount(3);
        observation.setRuntime(12);
        observation.setMemory(1024);
        context.setObservation(observation);
        RagChunkHit hit = new RagChunkHit();
        hit.setSourceType("KNOWLEDGE_CARD");
        hit.setSourceId(7L);
        hit.setScore(90);
        hit.setChunkText("面试中说明 HashMap 查找顺序和复杂度，不需要背完整代码。");
        RagRetrieveResult ragResult = new RagRetrieveResult();
        ragResult.setHits(List.of(hit));
        context.setRagRetrieveResult(ragResult);
        return context;
    }
}
