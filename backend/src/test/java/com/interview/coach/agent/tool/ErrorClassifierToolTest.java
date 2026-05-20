package com.interview.coach.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.AgentExecutionObservation;
import com.interview.coach.dto.AiDiagnosisResult;
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
class ErrorClassifierToolTest {

    @Mock
    private AnthropicCompatibleClient aiClient;

    @InjectMocks
    private ErrorClassifierTool tool;

    @Test
    void promptIncludesRagEvidenceAndAnswerBoundary() {
        AiDiagnosisResult expected = new AiDiagnosisResult();
        expected.setErrorType("LOGIC_ERROR");
        expected.setKnowledgePoint("HashMap 基础查找");
        when(aiClient.askJson(anyString(), anyString(), eq(AiDiagnosisResult.class))).thenReturn(expected);

        AgentContext context = contextWithRagEvidence();

        AiDiagnosisResult result = tool.execute(context, context);

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(aiClient).askJson(systemPrompt.capture(), userPrompt.capture(), eq(AiDiagnosisResult.class));
        assertThat(systemPrompt.getValue())
                .contains("Use retrieved evidence only as supporting context")
                .contains("Do not provide a full accepted Java solution")
                .contains("failurePhenomenon")
                .contains("rootCause")
                .contains("repairDirection")
                .contains("interviewReminder");
        assertThat(userPrompt.getValue())
                .contains("Retrieved evidence:")
                .contains("HashMap 需要先查找 complement 再写入当前元素");
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
        observation.setStatus("WRONG_ANSWER");
        observation.setPassedCount(1);
        observation.setTotalCount(3);
        observation.setErrorMessage("wrong answer");
        context.setObservation(observation);
        RagChunkHit hit = new RagChunkHit();
        hit.setSourceType("KNOWLEDGE_CARD");
        hit.setSourceId(7L);
        hit.setScore(120);
        hit.setChunkText("HashMap 需要先查找 complement 再写入当前元素，避免自匹配。");
        RagRetrieveResult ragResult = new RagRetrieveResult();
        ragResult.setHits(List.of(hit));
        context.setRagRetrieveResult(ragResult);
        return context;
    }
}
