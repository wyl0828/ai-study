package com.interview.coach.agent;

import com.interview.coach.agent.tool.CodeExecutionTool;
import com.interview.coach.agent.tool.CodeReviewTool;
import com.interview.coach.agent.tool.ErrorClassifierTool;
import com.interview.coach.agent.tool.RagRetrieveTool;
import com.interview.coach.agent.tool.TrainingPlannerTool;
import com.interview.coach.agent.tool.WeaknessTrackerTool;
import com.interview.coach.dto.RagRetrieveResult;
import com.interview.coach.entity.AgentRun;
import com.interview.coach.entity.AgentStepEntity;
import com.interview.coach.enums.AgentRunStatusEnum;
import com.interview.coach.enums.AgentState;
import com.interview.coach.enums.AgentStepStatusEnum;
import com.interview.coach.handler.BusinessException;
import com.interview.coach.mapper.AgentRunMapper;
import com.interview.coach.mapper.AgentStepMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j

@Component
@RequiredArgsConstructor
public class InterviewCoachAgent {

    private final CodeExecutionTool codeExecutionTool;

    private final CodeReviewTool codeReviewTool;

    private final ErrorClassifierTool errorClassifierTool;

    private final RagRetrieveTool ragRetrieveTool;

    private final WeaknessTrackerTool weaknessTrackerTool;

    private final TrainingPlannerTool trainingPlannerTool;

    private final AgentStepMapper agentStepMapper;

    private final AgentRunMapper agentRunMapper;

    public AgentContext run(AgentContext context, Consumer<AgentStep> stepSink) {
        Consumer<AgentStep> sink = stepSink == null ? step -> { } : stepSink;
        try {
            // 核心步骤：失败则终止
            runStep(context, AgentState.PLANNING, null, "Prepare agent context", "Context ready", sink, () -> context);
            runStep(context, AgentState.CODE_EXECUTION, codeExecutionTool.name(), "submissionId=" + context.getSubmissionId(),
                    "Execution observation ready", sink,
                    () -> codeExecutionTool.execute(context.getSubmissionId(), context));
            runStep(context, AgentState.OBSERVATION, null, "Read execution result", "Observation captured", sink, () -> context.getObservation());

            runOptionalStep(context, AgentState.RAG_RETRIEVAL, ragRetrieveTool.name(),
                    "Retrieve problem knowledge and user learning memory",
                    "RAG evidence ready", sink,
                    () -> ragRetrieveTool.execute(context, context));

            boolean accepted = "ACCEPTED".equals(context.getObservation().getStatus());
            if (accepted) {
                // AC 走代码点评
                runOptionalStep(context, AgentState.CODE_REVIEW, codeReviewTool.name(), "Review accepted code",
                        "Code review ready", sink,
                        () -> codeReviewTool.execute(context, context));
            } else {
                // 失败走错误诊断
                runStep(context, AgentState.ERROR_CLASSIFICATION, errorClassifierTool.name(), "Classify execution observation",
                        "Diagnosis ready", sink,
                        () -> errorClassifierTool.execute(context, context));

                // 非核心步骤：失败只 warn，不阻塞后续流程
                runOptionalStep(context, AgentState.MEMORY_UPDATE, weaknessTrackerTool.name(), "Persist diagnosis and weakness memory",
                        "Learning memory updated", sink,
                        () -> weaknessTrackerTool.execute(context, context));
                runOptionalStep(context, AgentState.TRAINING_PLAN, trainingPlannerTool.name(), "Create short training plan",
                        "Training plan ready", sink,
                        () -> trainingPlannerTool.execute(context, context));
            }

            // 最终步骤：始终执行
            runStep(context, AgentState.COMPLETED, null, "Finalize agent run", "Agent run completed", sink, () -> context);
            markRunSuccess(context);
            return context;
        } catch (RuntimeException ex) {
            markRunFailed(context, ex.getMessage());
            if (ex instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(500, "agent analysis failed");
        }
    }

    private Object runStep(AgentContext context, AgentState state, String toolName, String inputSummary,
            String fallbackOutputSummary, Consumer<AgentStep> sink, StepAction action) {
        markRunState(context, state);
        AgentStep step = new AgentStep();
        step.setState(state);
        step.setToolName(toolName);
        step.setStatus(AgentStepStatusEnum.RUNNING);
        step.setInputSummary(inputSummary);
        step.setStartedAt(LocalDateTime.now());
        persistStartedStep(context, step);
        sink.accept(step);
        try {
            Object output = action.execute();
            step.setStatus(AgentStepStatusEnum.SUCCESS);
            step.setOutputSummary(summarize(output, fallbackOutputSummary));
            step.setFinishedAt(LocalDateTime.now());
            step.setDurationMs(Duration.between(step.getStartedAt(), step.getFinishedAt()).toMillis());
            updateStep(step);
            context.getSteps().add(step);
            sink.accept(step);
            return output;
        } catch (RuntimeException ex) {
            step.setStatus(AgentStepStatusEnum.FAILED);
            step.setErrorMessage(ex.getMessage());
            step.setFinishedAt(LocalDateTime.now());
            step.setDurationMs(Duration.between(step.getStartedAt(), step.getFinishedAt()).toMillis());
            updateStep(step);
            context.getSteps().add(step);
            sink.accept(step);
            throw ex;
        }
    }

    private void runOptionalStep(AgentContext context, AgentState state, String toolName, String inputSummary,
            String fallbackOutputSummary, Consumer<AgentStep> sink, StepAction action) {
        try {
            runStep(context, state, toolName, inputSummary, fallbackOutputSummary, sink, action);
        } catch (Exception ex) {
            log.warn("Optional step {} failed, continuing: {}", state.name(), ex.getMessage());
        }
    }

    private void persistStartedStep(AgentContext context, AgentStep step) {
        AgentStepEntity entity = toEntity(context.getAgentRunId(), step);
        agentStepMapper.insert(entity);
        step.setId(entity.getId());
    }

    private void updateStep(AgentStep step) {
        AgentStepEntity entity = toEntity(null, step);
        entity.setId(step.getId());
        agentStepMapper.updateById(entity);
    }

    private AgentStepEntity toEntity(Long agentRunId, AgentStep step) {
        AgentStepEntity entity = new AgentStepEntity();
        entity.setAgentRunId(agentRunId);
        entity.setStepName(step.getState().name());
        entity.setToolName(step.getToolName());
        entity.setStatus(step.getStatus().name());
        entity.setInputSummary(step.getInputSummary());
        entity.setOutputSummary(step.getOutputSummary());
        entity.setDurationMs(step.getDurationMs());
        entity.setErrorMessage(step.getErrorMessage());
        entity.setStartedAt(step.getStartedAt());
        entity.setFinishedAt(step.getFinishedAt());
        entity.setCreatedAt(step.getStartedAt());
        return entity;
    }

    private void markRunState(AgentContext context, AgentState state) {
        AgentRun run = new AgentRun();
        run.setId(context.getAgentRunId());
        run.setStatus(AgentRunStatusEnum.RUNNING.name());
        run.setCurrentState(state.name());
        run.setUpdatedAt(LocalDateTime.now());
        agentRunMapper.updateById(run);
    }

    private void markRunSuccess(AgentContext context) {
        AgentRun run = new AgentRun();
        run.setId(context.getAgentRunId());
        run.setStatus(AgentRunStatusEnum.SUCCESS.name());
        run.setCurrentState(AgentState.COMPLETED.name());
        run.setFinishedAt(LocalDateTime.now());
        run.setUpdatedAt(LocalDateTime.now());
        agentRunMapper.updateById(run);
    }

    private void markRunFailed(AgentContext context, String message) {
        AgentRun run = new AgentRun();
        run.setId(context.getAgentRunId());
        run.setStatus(AgentRunStatusEnum.FAILED.name());
        run.setCurrentState(AgentState.FAILED.name());
        run.setErrorMessage(message);
        run.setFinishedAt(LocalDateTime.now());
        run.setUpdatedAt(LocalDateTime.now());
        agentRunMapper.updateById(run);
    }

    private String summarize(Object output, String fallback) {
        if (output instanceof RagRetrieveResult ragRetrieveResult) {
            return ragRetrieveResult.summary();
        }
        return output == null ? fallback : fallback;
    }

    @FunctionalInterface
    private interface StepAction {

        Object execute();
    }
}
