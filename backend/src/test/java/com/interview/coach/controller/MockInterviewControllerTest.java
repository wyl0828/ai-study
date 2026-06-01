package com.interview.coach.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.auth.CurrentUserContext;
import com.interview.coach.dto.MockInterviewAnswerRequest;
import com.interview.coach.dto.MockInterviewCreateRequest;
import com.interview.coach.service.MockInterviewService;
import com.interview.coach.vo.MockInterviewSessionVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MockInterviewControllerTest {

    @Mock
    private MockInterviewService mockInterviewService;

    @Mock
    private CurrentUserContext currentUserContext;

    @InjectMocks
    private MockInterviewController controller;

    @Test
    void getSessionRequiresOwnedSession() {
        when(currentUserContext.requireUserId()).thenReturn(7L);
        when(mockInterviewService.getSession(12L)).thenReturn(new MockInterviewSessionVO());

        controller.getSession(12L);

        verify(mockInterviewService).requireOwnedSession(12L, 7L);
        verify(mockInterviewService).getSession(12L);
    }

    @Test
    void answerRequiresOwnedSession() {
        when(currentUserContext.requireUserId()).thenReturn(7L);
        MockInterviewAnswerRequest request = new MockInterviewAnswerRequest();
        request.setUserAnswer("我的回答");
        when(mockInterviewService.answer(12L, request)).thenReturn(new MockInterviewSessionVO());

        controller.answer(12L, request);

        verify(mockInterviewService).requireOwnedSession(12L, 7L);
        verify(mockInterviewService).answer(12L, request);
    }

    @Test
    void finishRequiresOwnedSession() {
        when(currentUserContext.requireUserId()).thenReturn(7L);
        when(mockInterviewService.finish(12L)).thenReturn(new MockInterviewSessionVO());

        controller.finish(12L);

        verify(mockInterviewService).requireOwnedSession(12L, 7L);
        verify(mockInterviewService).finish(12L);
    }

    @Test
    void createOverwritesRequestUserId() {
        when(currentUserContext.requireUserId()).thenReturn(7L);
        MockInterviewCreateRequest request = new MockInterviewCreateRequest();
        request.setUserId(99L);
        when(mockInterviewService.create(request)).thenReturn(new MockInterviewSessionVO());

        controller.create(request);

        assertThat(request.getUserId()).isEqualTo(7L);
        verify(mockInterviewService).create(request);
    }
}
