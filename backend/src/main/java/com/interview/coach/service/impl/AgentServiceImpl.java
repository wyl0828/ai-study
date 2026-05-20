package com.interview.coach.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.coach.agent.AgentContext;
import com.interview.coach.agent.AgentStep;
import com.interview.coach.agent.InterviewCoachAgent;
import com.interview.coach.dto.AgentExecutionObservation;
import com.interview.coach.dto.AiDiagnosisResult;
import com.interview.coach.dto.FailedCaseResult;
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
import org.springframework.util.StringUtils;

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
            vo.setSuggestion(diagnosis.getSuggestion());
            vo.setFailurePhenomenon(firstText(
                    failurePhenomenonFromObservation(context.getObservation()),
                    diagnosis.getFailurePhenomenon(),
                    diagnosis.getSpecificError(),
                    errorMessage(context.getObservation())));
            vo.setRootCause(firstText(diagnosis.getRootCause(), diagnosis.getDiagnosis(), diagnosis.getSpecificError()));
            vo.setRepairDirection(firstText(diagnosis.getRepairDirection(), diagnosis.getSuggestion(), diagnosis.getDiagnosis()));
            vo.setInterviewReminder(firstText(
                    diagnosis.getInterviewReminder(),
                    interviewReminderFallback(diagnosis.getKnowledgePoint())));
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

    private String failurePhenomenonFromObservation(AgentExecutionObservation observation) {
        if (observation == null) {
            return null;
        }
        if (observation.getFailedCases() != null && !observation.getFailedCases().isEmpty()) {
            FailedCaseResult failedCase = observation.getFailedCases().get(0);
            return "在用例 %s 中，期望输出 %s，实际输出 %s。".formatted(
                    compact(failedCase.getInput()),
                    compact(failedCase.getExpectedOutput()),
                    compact(failedCase.getActualOutput()));
        }
        return errorMessage(observation);
    }

    private String errorMessage(AgentExecutionObservation observation) {
        return observation == null ? null : summarizeExecutionError(observation.getErrorMessage());
    }

    private String interviewReminderFallback(String knowledgePoint) {
        if (StringUtils.hasText(knowledgePoint)) {
            return "面试中要主动说明「%s」的关键边界和容易出错的用例。".formatted(knowledgePoint.trim());
        }
        return "面试中要主动说明失败用例暴露的边界条件和修正思路。";
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String compact(String value) {
        if (!StringUtils.hasText(value)) {
            return "空输出";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String summarizeExecutionError(String errorMessage) {
        if (!StringUtils.hasText(errorMessage)) {
            return null;
        }
        String firstLine = errorMessage.lines()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(errorMessage.trim());
        String compactLine = compact(firstLine)
                .replaceFirst("^Exception in thread \"[^\"]+\"\\s+", "");
        if (compactLine.contains("OutOfMemoryError")) {
            return "运行时异常：%s。程序在遍历或输出时没有正常终止，常见原因是链表成环、递归未收敛或循环条件错误。".formatted(compactLine);
        }
        if (compactLine.matches(".*(Exception|Error)(:.*)?")) {
            return "运行时异常：%s。请结合测试结果中的堆栈定位触发位置。".formatted(compactLine);
        }
        return compactLine;
    }
}
