package com.interview.coach.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.interview.coach.agent.tool.CodeExecutionTool;
import com.interview.coach.agent.tool.CodeReviewTool;
import com.interview.coach.agent.tool.ErrorClassifierTool;
import com.interview.coach.agent.tool.RagRetrieveTool;
import com.interview.coach.agent.tool.TrainingPlannerTool;
import com.interview.coach.agent.tool.WeaknessTrackerTool;
import com.interview.coach.dto.AgentExecutionObservation;
import com.interview.coach.dto.AiDiagnosisResult;
import com.interview.coach.dto.RagRetrieveResult;
import com.interview.coach.dto.TrainingPlanResult;
import com.interview.coach.enums.AgentState;
import com.interview.coach.enums.AgentStepStatusEnum;
import com.interview.coach.mapper.AgentRunMapper;
import com.interview.coach.mapper.AgentStepMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterviewCoachAgentTest {

    @Mock
    private CodeExecutionTool codeExecutionTool;

    @Mock
    private CodeReviewTool codeReviewTool;

    @Mock
    private ErrorClassifierTool errorClassifierTool;

    @Mock
    private RagRetrieveTool ragRetrieveTool;

    @Mock
    private WeaknessTrackerTool weaknessTrackerTool;

    @Mock
    private TrainingPlannerTool trainingPlannerTool;

    @Mock
    private AgentStepMapper agentStepMapper;

    @Mock
    private AgentRunMapper agentRunMapper;

    @InjectMocks
    private InterviewCoachAgent agent;

    @Test
    void runEmitsSuccessfulStatesInInterviewCoachOrder() {
        AgentContext context = new AgentContext();
        context.setAgentRunId(99L);
        context.setSubmissionId(11L);
        AgentExecutionObservation observation = new AgentExecutionObservation();
        observation.setStatus("WRONG_ANSWER");
        context.setObservation(observation);
        AiDiagnosisResult diagnosis = new AiDiagnosisResult();
        context.setDiagnosis(diagnosis);
        RagRetrieveResult ragResult = new RagRetrieveResult();
        TrainingPlanResult plan = new TrainingPlanResult();
        when(codeExecutionTool.execute(eq(11L), eq(context))).thenReturn(observation);
        when(ragRetrieveTool.name()).thenReturn("RagRetrieveTool");
        when(ragRetrieveTool.execute(eq(context), eq(context))).thenReturn(ragResult);
        when(errorClassifierTool.execute(eq(context), eq(context))).thenAnswer(invocation -> {
            context.setDiagnosis(diagnosis);
            return diagnosis;
        });
        when(weaknessTrackerTool.execute(eq(context), eq(context))).thenReturn(context);
        when(trainingPlannerTool.execute(eq(context), eq(context))).thenReturn(plan);

        List<AgentState> successStates = new ArrayList<>();
        agent.run(context, step -> {
            if (step.getStatus() == AgentStepStatusEnum.SUCCESS) {
                successStates.add(step.getState());
            }
        });

        assertThat(successStates).containsExactly(
                AgentState.PLANNING,
                AgentState.CODE_EXECUTION,
                AgentState.OBSERVATION,
                AgentState.RAG_RETRIEVAL,
                AgentState.ERROR_CLASSIFICATION,
                AgentState.MEMORY_UPDATE,
                AgentState.TRAINING_PLAN,
                AgentState.COMPLETED);
    }

    @Test
    void acceptedSubmissionContinuesWhenCodeReviewFails() {
        AgentContext context = new AgentContext();
        context.setAgentRunId(100L);
        context.setSubmissionId(12L);
        AgentExecutionObservation observation = new AgentExecutionObservation();
        observation.setStatus("ACCEPTED");
        context.setObservation(observation);
        when(codeExecutionTool.execute(eq(12L), eq(context))).thenReturn(observation);
        when(ragRetrieveTool.name()).thenReturn("RagRetrieveTool");
        when(ragRetrieveTool.execute(eq(context), eq(context))).thenReturn(new RagRetrieveResult());
        when(codeReviewTool.execute(eq(context), eq(context))).thenThrow(new RuntimeException("AI unavailable"));

        List<AgentStep> emittedSteps = new ArrayList<>();
        AgentContext result = agent.run(context, emittedSteps::add);

        assertThat(result.getCodeReview()).isNull();
        assertThat(emittedSteps)
                .anySatisfy(step -> {
                    assertThat(step.getState()).isEqualTo(AgentState.CODE_REVIEW);
                    assertThat(step.getStatus()).isEqualTo(AgentStepStatusEnum.FAILED);
                    assertThat(step.getErrorMessage()).isEqualTo("AI unavailable");
                });
        assertThat(result.getSteps())
                .extracting(AgentStep::getState)
                .contains(AgentState.RAG_RETRIEVAL, AgentState.CODE_REVIEW, AgentState.COMPLETED);
    }

    @Test
    void ragRetrievalFailureDoesNotBlockDiagnosis() {
        AgentContext context = new AgentContext();
        context.setAgentRunId(101L);
        context.setSubmissionId(13L);
        AgentExecutionObservation observation = new AgentExecutionObservation();
        observation.setStatus("WRONG_ANSWER");
        context.setObservation(observation);
        AiDiagnosisResult diagnosis = new AiDiagnosisResult();
        TrainingPlanResult plan = new TrainingPlanResult();
        when(codeExecutionTool.execute(eq(13L), eq(context))).thenReturn(observation);
        when(ragRetrieveTool.name()).thenReturn("RagRetrieveTool");
        when(ragRetrieveTool.execute(eq(context), eq(context))).thenThrow(new RuntimeException("RAG unavailable"));
        when(errorClassifierTool.execute(eq(context), eq(context))).thenAnswer(invocation -> {
            context.setDiagnosis(diagnosis);
            return diagnosis;
        });
        when(weaknessTrackerTool.execute(eq(context), eq(context))).thenReturn(context);
        when(trainingPlannerTool.execute(eq(context), eq(context))).thenReturn(plan);

        List<AgentStep> emittedSteps = new ArrayList<>();
        AgentContext result = agent.run(context, emittedSteps::add);

        assertThat(result.getDiagnosis()).isSameAs(diagnosis);
        assertThat(result.getSteps())
                .extracting(AgentStep::getState)
                .contains(AgentState.RAG_RETRIEVAL, AgentState.ERROR_CLASSIFICATION, AgentState.COMPLETED);
        assertThat(emittedSteps)
                .anySatisfy(step -> {
                    assertThat(step.getState()).isEqualTo(AgentState.RAG_RETRIEVAL);
                    assertThat(step.getStatus()).isEqualTo(AgentStepStatusEnum.FAILED);
                    assertThat(step.getErrorMessage()).isEqualTo("RAG unavailable");
                });
    }
}
