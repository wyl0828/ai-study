package com.interview.coach.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.AgentExecutionObservation;
import com.interview.coach.dto.FailedCaseResult;
import com.interview.coach.dto.JudgeResult;
import com.interview.coach.enums.SubmissionStatusEnum;
import com.interview.coach.service.SubmissionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CodeExecutionToolTest {

    @Mock
    private SubmissionService submissionService;

    @InjectMocks
    private CodeExecutionTool tool;

    @Test
    void executeCopiesJudgeResultIntoObservationAndContext() {
        JudgeResult judgeResult = new JudgeResult();
        judgeResult.setStatus(SubmissionStatusEnum.WRONG_ANSWER);
        judgeResult.setPassedCount(1);
        judgeResult.setTotalCount(2);
        judgeResult.setRuntime(42);
        judgeResult.setMemory(128);
        judgeResult.setErrorMessage("case failed");
        judgeResult.setFailedCases(List.of(new FailedCaseResult(7L, "[2,2]", "0 1", "-1")));
        when(submissionService.rejudge(11L)).thenReturn(judgeResult);
        AgentContext context = new AgentContext();

        AgentExecutionObservation observation = tool.execute(11L, context);

        assertThat(observation.getStatus()).isEqualTo("WRONG_ANSWER");
        assertThat(observation.getPassedCount()).isEqualTo(1);
        assertThat(observation.getTotalCount()).isEqualTo(2);
        assertThat(observation.getRuntime()).isEqualTo(42);
        assertThat(observation.getMemory()).isEqualTo(128);
        assertThat(observation.getFailedCases()).hasSize(1);
        assertThat(context.getObservation()).isSameAs(observation);
    }
}
