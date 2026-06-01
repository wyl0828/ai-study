package com.interview.coach.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.auth.CurrentUserContext;
import com.interview.coach.handler.BusinessException;
import com.interview.coach.service.KnowledgeLearningService;
import com.interview.coach.service.TrainingPlanService;
import com.interview.coach.service.UserLearningService;
import com.interview.coach.vo.ApiResponse;
import com.interview.coach.vo.MockInterviewTraceVO;
import com.interview.coach.vo.TrainingPlanActivityVO;
import com.interview.coach.vo.TrainingPlanHistoryVO;
import com.interview.coach.vo.TrainingPlanTraceVO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserLearningService userLearningService;

    @Mock
    private TrainingPlanService trainingPlanService;

    @Mock
    private KnowledgeLearningService knowledgeLearningService;

    @Mock
    private CurrentUserContext currentUserContext;

    @InjectMocks
    private UserController controller;

    @Test
    void trainingPlanTraceReturnsServiceSummary() {
        when(currentUserContext.requireUserId()).thenReturn(1L);
        TrainingPlanTraceVO trace = new TrainingPlanTraceVO();
        trace.setPlanId(100L);
        trace.setItemCount(3);
        trace.setCompletedCount(1);
        trace.setNextActionReason("优先处理该项，因为它来自失败提交。");
        trace.setNextActionPriority("HIGH");
        when(userLearningService.getTrainingPlanTrace(1L)).thenReturn(trace);

        ApiResponse<TrainingPlanTraceVO> response = controller.getTrainingPlanTrace(1L);

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).isSameAs(trace);
        assertThat(response.getData().getCompletedCount()).isEqualTo(1);
        assertThat(response.getData().getNextActionReason()).contains("失败提交");
        assertThat(response.getData().getNextActionPriority()).isEqualTo("HIGH");
        verify(userLearningService).getTrainingPlanTrace(1L);
    }

    @Test
    void trainingPlanHistoryReturnsServiceSummaries() {
        when(currentUserContext.requireUserId()).thenReturn(1L);
        TrainingPlanHistoryVO active = new TrainingPlanHistoryVO();
        active.setId(100L);
        active.setTitle("第 1 轮训练计划");
        active.setStatus("ACTIVE");
        active.setItemCount(3);
        active.setCompletedCount(1);
        active.setSkippedCount(0);
        TrainingPlanHistoryVO regenerated = new TrainingPlanHistoryVO();
        regenerated.setId(90L);
        regenerated.setTitle("旧训练计划");
        regenerated.setStatus("REGENERATED");
        regenerated.setItemCount(3);
        regenerated.setCompletedCount(3);
        regenerated.setSkippedCount(0);
        when(userLearningService.getTrainingPlanHistory(1L, 5))
                .thenReturn(List.of(active, regenerated));

        ApiResponse<List<TrainingPlanHistoryVO>> response = controller.getTrainingPlanHistory(1L, 5);

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getData().get(1).getStatus()).isEqualTo("REGENERATED");
        verify(userLearningService).getTrainingPlanHistory(1L, 5);
    }

    @Test
    void recentTrainingActivitiesReturnLearningImpactSummaries() {
        when(currentUserContext.requireUserId()).thenReturn(1L);
        TrainingPlanActivityVO completed = new TrainingPlanActivityVO();
        completed.setItemId(10L);
        completed.setPlanId(100L);
        completed.setTaskTitle("两数之和复盘");
        completed.setStatus("COMPLETED");
        completed.setLearningImpactSummary("完成后会形成弱点改善趋势。");
        TrainingPlanActivityVO skipped = new TrainingPlanActivityVO();
        skipped.setItemId(11L);
        skipped.setPlanId(100L);
        skipped.setTaskTitle("Redis 缓存复习");
        skipped.setStatus("SKIPPED");
        skipped.setLearningImpactSummary("跳过只记录训练节奏。");
        when(userLearningService.getRecentTrainingActivities(1L, 5))
                .thenReturn(List.of(completed, skipped));

        ApiResponse<List<TrainingPlanActivityVO>> response = controller.getRecentTrainingActivities(1L, 5);

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getLearningImpactSummary()).contains("改善趋势");
        assertThat(response.getData().get(1).getStatus()).isEqualTo("SKIPPED");
        verify(userLearningService).getRecentTrainingActivities(1L, 5);
    }

    @Test
    void mockInterviewTraceReturnsServiceSummary() {
        when(currentUserContext.requireUserId()).thenReturn(1L);
        MockInterviewTraceVO trace = new MockInterviewTraceVO();
        trace.setLatestSessionId(30L);
        trace.setLatestReportId(7L);
        trace.setLowScoreTurnCount(2);
        trace.setNextActionReason("最近报告已推荐知识卡并接入训练计划。");
        trace.setNextActionPriority("HIGH");
        when(userLearningService.getMockInterviewTrace(1L)).thenReturn(trace);

        ApiResponse<MockInterviewTraceVO> response = controller.getMockInterviewTrace(1L);

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).isSameAs(trace);
        assertThat(response.getData().getLowScoreTurnCount()).isEqualTo(2);
        assertThat(response.getData().getNextActionReason()).contains("推荐知识卡");
        assertThat(response.getData().getNextActionPriority()).isEqualTo("HIGH");
        verify(userLearningService).getMockInterviewTrace(1L);
    }

    @Test
    void rejectsDifferentUserDashboardAccess() {
        when(currentUserContext.requireUserId()).thenReturn(2L);

        assertThatThrownBy(() -> controller.getDashboardStats(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot access another user's learning data");
    }
}
