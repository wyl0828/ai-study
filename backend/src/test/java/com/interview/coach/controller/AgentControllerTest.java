package com.interview.coach.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.interview.coach.auth.CurrentUserContext;
import com.interview.coach.dto.AgentAnalyzeRequest;
import com.interview.coach.handler.BusinessException;
import com.interview.coach.service.AgentService;
import com.interview.coach.service.SubmissionService;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentControllerTest {

    @Mock
    private AgentService agentService;

    @Mock
    private SubmissionService submissionService;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private Executor agentTaskExecutor;

    @InjectMocks
    private AgentController controller;

    @Test
    void analyzeRejectsAnotherUsersSubmission() {
        AgentAnalyzeRequest request = new AgentAnalyzeRequest();
        request.setSubmissionId(11L);
        when(currentUserContext.requireUserId()).thenReturn(2L);
        doThrow(new BusinessException(403, "cannot access another user's submission"))
                .when(submissionService)
                .requireOwnedSubmission(11L, 2L);

        assertThatThrownBy(() -> controller.analyze(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot access another user's submission");
    }

    @Test
    void streamRejectsAnotherUsersSubmissionBeforeStartingEmitter() {
        when(currentUserContext.requireUserId()).thenReturn(2L);
        doThrow(new BusinessException(403, "cannot access another user's submission"))
                .when(submissionService)
                .requireOwnedSubmission(11L, 2L);

        assertThatThrownBy(() -> controller.streamDiagnosis(11L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot access another user's submission");
    }
}
