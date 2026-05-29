package com.interview.coach.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.service.ProblemCacheService;
import com.interview.coach.service.ProblemService;
import com.interview.coach.vo.ApiResponse;
import com.interview.coach.vo.ProblemCacheRefreshVO;
import com.interview.coach.vo.ProblemCacheStatusVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProblemControllerTest {

    @Mock
    private ProblemService problemService;

    @Mock
    private ProblemCacheService problemCacheService;

    @Test
    void cacheStatusReturnsCacheBoundarySummary() {
        ProblemCacheStatusVO status = new ProblemCacheStatusVO();
        status.setEnabled(true);
        status.setProvider("Redis");
        status.setRedisAvailable(true);
        status.setListKey("coach:problem:list:v1");
        when(problemCacheService.status()).thenReturn(status);
        ProblemController controller = new ProblemController(problemService, problemCacheService);

        ApiResponse<ProblemCacheStatusVO> response = controller.cacheStatus();

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).isSameAs(status);
        assertThat(response.getData().getListKey()).isEqualTo("coach:problem:list:v1");
        verify(problemCacheService).status();
    }

    @Test
    void refreshCacheUsesProblemServiceMaintenanceAction() {
        ProblemCacheRefreshVO refresh = new ProblemCacheRefreshVO();
        refresh.setEnabled(true);
        refresh.setListWarmAttempted(true);
        refresh.setDetailWarmAttemptedCount(20);
        when(problemService.refreshProblemCache()).thenReturn(refresh);
        ProblemController controller = new ProblemController(problemService, problemCacheService);

        ApiResponse<ProblemCacheRefreshVO> response = controller.refreshCache();

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).isSameAs(refresh);
        assertThat(response.getData().getDetailWarmAttemptedCount()).isEqualTo(20);
        verify(problemService).refreshProblemCache();
    }
}
