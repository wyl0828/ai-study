package com.interview.coach.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.coach.agent.AgentContext;
import com.interview.coach.agent.AgentStep;
import com.interview.coach.agent.InterviewCoachAgent;
import com.interview.coach.dto.AiDiagnosisResult;
import com.interview.coach.dto.HintGenerationResult;
import com.interview.coach.dto.TrainingPlanResult;
import com.interview.coach.entity.AgentRun;
import com.interview.coach.entity.Problem;
import com.interview.coach.entity.Submission;
import com.interview.coach.enums.AgentRunStatusEnum;
import com.interview.coach.enums.AgentState;
import com.interview.coach.handler.BusinessException;
import com.interview.coach.mapper.AgentRunMapper;
import com.interview.coach.service.AgentService;
import com.interview.coach.service.ProblemService;
import com.interview.coach.service.SubmissionService;
import com.interview.coach.vo.AgentAnalyzeVO;
import com.interview.coach.vo.AgentStepVO;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final SubmissionService submissionService;

    private final ProblemService problemService;

    private final InterviewCoachAgent interviewCoachAgent;

    private final AgentRunMapper agentRunMapper;

    private final ObjectMapper objectMapper;

    @Override
    public AgentAnalyzeVO analyze(Long submissionId) {
        return analyze(submissionId, null);
    }

    @Override
    public AgentAnalyzeVO analyze(Long submissionId, Consumer<String> eventSink) {
        AgentContext context = buildContext(submissionId);
        Consumer<AgentStep> stepSink = step -> {
            if (eventSink != null) {
                eventSink.accept(toJson(toStepVO(step)));
            }
        };
        AgentContext result = interviewCoachAgent.run(context, stepSink);
        return toAnalyzeVO(result);
    }

    private AgentContext buildContext(Long submissionId) {
        Submission submission = submissionService.getSubmissionOrThrow(submissionId);
        Problem problem = problemService.getEnabledProblem(submission.getProblemId());
        AgentRun run = createRun(submission);

        AgentContext context = new AgentContext();
        context.setAgentRunId(run.getId());
        context.setSubmissionId(submission.getId());
        context.setUserId(submission.getUserId());
        context.setProblemId(submission.getProblemId());
        context.setSubmission(submission);
        context.setProblem(problem);
        context.setKnowledgePoints(problemService.listKnowledgePointNames(problem.getId()));
        return context;
    }

    private AgentRun createRun(Submission submission) {
        LocalDateTime now = LocalDateTime.now();
        AgentRun run = new AgentRun();
        run.setSubmissionId(submission.getId());
        run.setUserId(submission.getUserId());
        run.setProblemId(submission.getProblemId());
        run.setStatus(AgentRunStatusEnum.RUNNING.name());
        run.setCurrentState(AgentState.PLANNING.name());
        run.setStartedAt(now);
        run.setCreatedAt(now);
        run.setUpdatedAt(now);
        agentRunMapper.insert(run);
        return run;
    }

    private AgentAnalyzeVO toAnalyzeVO(AgentContext context) {
        AgentAnalyzeVO vo = new AgentAnalyzeVO();
        vo.setAgentRunId(context.getAgentRunId());
        vo.setSubmissionId(context.getSubmissionId());
        AiDiagnosisResult diagnosis = context.getDiagnosis();
        if (diagnosis != null) {
            vo.setErrorType(diagnosis.getErrorType());
            vo.setKnowledgePoint(diagnosis.getKnowledgePoint());
            vo.setSpecificError(diagnosis.getSpecificError());
            vo.setDiagnosis(diagnosis.getDiagnosis());
        }
        if (context.getCodeReview() != null) {
            vo.setCodeReview(context.getCodeReview());
        }
        HintGenerationResult hints = context.getHints();
        if (hints != null) {
            vo.setHintLevel1(hints.getHintLevel1());
            vo.setHintLevel2(hints.getHintLevel2());
            vo.setHintLevel3(hints.getHintLevel3());
        }
        TrainingPlanResult trainingPlan = context.getTrainingPlan();
        if (trainingPlan != null) {
            vo.setTrainingPlanTitle(trainingPlan.getTitle());
        }
        vo.setSteps(context.getSteps().stream().map(this::toStepVO).toList());
        return vo;
    }

    private AgentStepVO toStepVO(AgentStep step) {
        AgentStepVO vo = new AgentStepVO();
        vo.setStepName(step.getState().name());
        vo.setToolName(step.getToolName());
        vo.setStatus(step.getStatus().name());
        vo.setInputSummary(step.getInputSummary());
        vo.setOutputSummary(step.getOutputSummary());
        vo.setDurationMs(step.getDurationMs());
        vo.setErrorMessage(step.getErrorMessage());
        return vo;
    }

    private String toJson(AgentStepVO step) {
        try {
            return objectMapper.writeValueAsString(step);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "failed to serialize agent step");
        }
    }
}
