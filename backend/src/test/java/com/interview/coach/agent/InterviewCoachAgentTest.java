package com.interview.coach.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.interview.coach.agent.tool.CodeExecutionTool;
import com.interview.coach.agent.tool.ErrorClassifierTool;
import com.interview.coach.agent.tool.HintGeneratorTool;
import com.interview.coach.agent.tool.TrainingPlannerTool;
import com.interview.coach.agent.tool.WeaknessTrackerTool;
import com.interview.coach.dto.AgentExecutionObservation;
import com.interview.coach.dto.AiDiagnosisResult;
import com.interview.coach.dto.HintGenerationResult;
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
    private ErrorClassifierTool errorClassifierTool;

    @Mock
    private HintGeneratorTool hintGeneratorTool;

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
        AiDiagnosisResult diagnosis = new AiDiagnosisResult();
        HintGenerationResult hints = new HintGenerationResult();
        TrainingPlanResult plan = new TrainingPlanResult();
        when(codeExecutionTool.execute(eq(11L), eq(context))).thenReturn(observation);
        when(errorClassifierTool.execute(eq(context), eq(context))).thenReturn(diagnosis);
        when(hintGeneratorTool.execute(eq(context), eq(context))).thenReturn(hints);
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
                AgentState.ERROR_CLASSIFICATION,
                AgentState.HINT_GENERATION,
                AgentState.MEMORY_UPDATE,
                AgentState.TRAINING_PLAN,
                AgentState.COMPLETED);
    }
}
