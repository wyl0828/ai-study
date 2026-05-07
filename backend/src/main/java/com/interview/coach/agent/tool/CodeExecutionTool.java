package com.interview.coach.agent.tool;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.AgentExecutionObservation;
import com.interview.coach.dto.JudgeResult;
import com.interview.coach.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CodeExecutionTool implements Tool<Long, AgentExecutionObservation> {

    private final SubmissionService submissionService;

    @Override
    public String name() {
        return "CodeExecutionTool";
    }

    @Override
    public AgentExecutionObservation execute(Long submissionId, AgentContext context) {
        JudgeResult result = submissionService.rejudge(submissionId);
        AgentExecutionObservation observation = new AgentExecutionObservation();
        observation.setStatus(result.getStatus().name());
        observation.setPassedCount(result.getPassedCount());
        observation.setTotalCount(result.getTotalCount());
        observation.setRuntime(result.getRuntime());
        observation.setMemory(result.getMemory());
        observation.setErrorMessage(result.getErrorMessage());
        observation.setFailedCases(result.getFailedCases());
        context.setObservation(observation);
        return observation;
    }
}
