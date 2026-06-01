package com.interview.coach.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.auth.CurrentUserContext;
import com.interview.coach.dto.SubmitCodeRequest;
import com.interview.coach.service.SubmissionService;
import com.interview.coach.vo.SubmissionResultVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmissionControllerTest {

    @Mock
    private SubmissionService submissionService;

    @Mock
    private CurrentUserContext currentUserContext;

    @InjectMocks
    private SubmissionController controller;

    @Test
    void submitOverwritesRequestUserIdWithAuthenticatedUser() {
        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setUserId(99L);
        request.setProblemId(1L);
        request.setLanguage("java");
        request.setCode("class Solution {}");
        when(currentUserContext.requireUserId()).thenReturn(7L);
        when(submissionService.submit(request)).thenReturn(new SubmissionResultVO());

        controller.submit(request);

        assertThat(request.getUserId()).isEqualTo(7L);
        verify(submissionService).submit(request);
    }
}
