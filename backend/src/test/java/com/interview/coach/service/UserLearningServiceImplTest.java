package com.interview.coach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.interview.coach.entity.MistakeCard;
import com.interview.coach.entity.KnowledgeCard;
import com.interview.coach.entity.MockInterviewReport;
import com.interview.coach.entity.MockInterviewSession;
import com.interview.coach.entity.MockInterviewTurn;
import com.interview.coach.entity.Problem;
import com.interview.coach.entity.Submission;
import com.interview.coach.entity.TrainingPlan;
import com.interview.coach.entity.TrainingPlanItem;
import com.interview.coach.entity.UserWeakness;
import com.interview.coach.entity.UserWeaknessEvent;
import com.interview.coach.mapper.MistakeCardMapper;
import com.interview.coach.mapper.KnowledgeCardMapper;
import com.interview.coach.mapper.MockInterviewReportMapper;
import com.interview.coach.mapper.MockInterviewSessionMapper;
import com.interview.coach.mapper.MockInterviewTurnMapper;
import com.interview.coach.mapper.ProblemMapper;
import com.interview.coach.mapper.SubmissionMapper;
import com.interview.coach.mapper.TrainingPlanItemMapper;
import com.interview.coach.mapper.TrainingPlanMapper;
import com.interview.coach.mapper.UserWeaknessEventMapper;
import com.interview.coach.mapper.UserWeaknessMapper;
import com.interview.coach.service.impl.UserLearningServiceImpl;
import com.interview.coach.vo.DashboardStatsVO;
import com.interview.coach.vo.MistakeCardVO;
import com.interview.coach.vo.MockInterviewRecentVO;
import com.interview.coach.vo.MockInterviewTraceVO;
import com.interview.coach.vo.MockInterviewTrendVO;
import com.interview.coach.vo.SubmissionHistoryVO;
import com.interview.coach.vo.TrainingPlanActivityVO;
import com.interview.coach.vo.TrainingPlanHistoryVO;
import com.interview.coach.vo.TrainingPlanTraceVO;
import com.interview.coach.vo.TrainingPlanVO;
import com.interview.coach.vo.UserWeaknessVO;
import com.interview.coach.vo.ErrorStatsVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserLearningServiceImplTest {

    @Mock
    private SubmissionMapper submissionMapper;

    @Mock
    private UserWeaknessMapper userWeaknessMapper;

    @Mock
    private UserWeaknessEventMapper userWeaknessEventMapper;

    @Mock
    private MistakeCardMapper mistakeCardMapper;

    @Mock
    private TrainingPlanMapper trainingPlanMapper;

    @Mock
    private TrainingPlanItemMapper trainingPlanItemMapper;

    @Mock
    private ProblemMapper problemMapper;

    @Mock
    private MockInterviewSessionMapper mockInterviewSessionMapper;

    @Mock
    private MockInterviewReportMapper mockInterviewReportMapper;

    @Mock
    private MockInterviewTurnMapper mockInterviewTurnMapper;

    @Mock
    private KnowledgeCardMapper knowledgeCardMapper;

    @InjectMocks
    private UserLearningServiceImpl userLearningService;

    @Test
    void getDashboardStatsCountsAcceptedDistinctProblems() {
        when(submissionMapper.selectCount(any())).thenReturn(4L);
        when(mistakeCardMapper.selectCount(any())).thenReturn(2L);
        when(userWeaknessMapper.selectList(any())).thenReturn(List.of(
                weakness(1L, "HashMap 基础查找", "LOGIC_ERROR", 1, "8.0"),
                weakness(2L, "链表指针操作", "RUNTIME_ERROR", 1, "6.0"),
                weakness(3L, "二叉树递归", "LOGIC_ERROR", 1, "5.0")));

        Submission firstAccepted = submission(101L, "ACCEPTED");
        Submission duplicateAccepted = submission(101L, "ACCEPTED");
        Submission secondAccepted = submission(102L, "ACCEPTED");
        when(submissionMapper.selectList(any()))
                .thenReturn(List.of(firstAccepted, duplicateAccepted, secondAccepted));

        DashboardStatsVO stats = userLearningService.getDashboardStats(1L);

        assertThat(stats.getTotalSubmissions()).isEqualTo(4);
        assertThat(stats.getPassedProblems()).isEqualTo(2);
        assertThat(stats.getWeakPointCount()).isEqualTo(3);
        assertThat(stats.getMistakeCount()).isEqualTo(2);
    }

    @Test
    void getDashboardStatsCountsAggregatedWeakPoints() {
        when(submissionMapper.selectCount(any())).thenReturn(4L);
        when(mistakeCardMapper.selectCount(any())).thenReturn(2L);
        when(submissionMapper.selectList(any())).thenReturn(List.of(submission(101L, "ACCEPTED")));
        when(userWeaknessMapper.selectList(any())).thenReturn(List.of(
                weakness(1L, "HashMap 在两数之和中的应用", "LOGIC_ERROR", 6, "38.0"),
                weakness(2L, "HashMap in Two Sum", "ALGORITHM_ERROR", 4, "31.0"),
                weakness(3L, "链表指针操作", "RUNTIME_ERROR", 1, "8.0")));

        DashboardStatsVO stats = userLearningService.getDashboardStats(1L);

        assertThat(stats.getWeakPointCount()).isEqualTo(2);
    }

    @Test
    void getLatestTrainingPlanReturnsNullWhenUserHasNoPlan() {
        when(trainingPlanMapper.selectOne(any())).thenReturn(null);

        assertThat(userLearningService.getLatestTrainingPlan(1L)).isNull();
    }

    @Test
    void getLatestTrainingPlanDefaultsOldItemsToProblemType() {
        TrainingPlan plan = trainingPlan();
        TrainingPlanItem item = new TrainingPlanItem();
        item.setDayIndex(1);
        item.setKnowledgePoint("HashMap Lookup");
        item.setProblemTitle("Two Sum");
        item.setReason("Replay the failed case.");
        item.setStatus("PENDING");
        when(trainingPlanMapper.selectOne(any())).thenReturn(plan);
        when(trainingPlanItemMapper.selectList(any())).thenReturn(List.of(item));

        TrainingPlanVO result = userLearningService.getLatestTrainingPlan(1L);

        assertThat(trainingPlanValue(result, "statusLabel")).isEqualTo("进行中");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getItemType()).isEqualTo("PROBLEM");
        assertThat(result.getItems().get(0).getProblemTitle()).isEqualTo("Two Sum");
        assertThat(result.getItems().get(0).getProblemId()).isEqualTo(1L);
        assertThat(result.getItems().get(0).getSourceType()).isEqualTo("LEGACY_TRAINING_PLAN");
        assertThat(result.getItems().get(0).getSourceSummary()).contains("历史训练计划");
        assertThat(result.getItems().get(0).getReviewFocus()).contains("HashMap Lookup");
        assertThat(result.getItems().get(0).getTargetHref()).isEqualTo("/problem/1");
        assertThat(result.getItems().get(0).getTargetLabel()).isEqualTo("去做题");
    }

    @Test
    void getLatestTrainingPlanKeepsProblemNavigationForCompletedAndSkippedItems() {
        TrainingPlan plan = trainingPlan();
        TrainingPlanItem completed = new TrainingPlanItem();
        completed.setItemType("PROBLEM");
        completed.setDayIndex(1);
        completed.setKnowledgePoint("HashMap 在两数之和中的应用");
        completed.setProblemTitle("两数之和");
        completed.setReason("Replay the failed case.");
        completed.setReviewFocus("Check complement before insert.");
        completed.setStatus("COMPLETED");

        TrainingPlanItem skipped = new TrainingPlanItem();
        skipped.setItemType("PROBLEM");
        skipped.setDayIndex(2);
        skipped.setKnowledgePoint("链表指针");
        skipped.setProblemTitle("反转链表");
        skipped.setReason("Replay pointer updates.");
        skipped.setReviewFocus("Check next pointer order.");
        skipped.setStatus("SKIPPED");

        when(trainingPlanMapper.selectOne(any())).thenReturn(plan);
        when(trainingPlanItemMapper.selectList(any())).thenReturn(List.of(completed, skipped));

        TrainingPlanVO result = userLearningService.getLatestTrainingPlan(1L);

        assertThat(result.getItems())
                .extracting(item -> item.getProblemId())
                .containsExactly(1L, 206L);
    }

    @Test
    void getLatestTrainingPlanReturnsKnowledgeCardItemFields() {
        TrainingPlan plan = trainingPlan();
        TrainingPlanItem item = new TrainingPlanItem();
        item.setItemType("KNOWLEDGE_CARD");
        item.setKnowledgeCardId(7L);
        item.setKnowledgeCardTitle("HashMap 底层结构");
        item.setDayIndex(2);
        item.setKnowledgePoint("Java 集合");
        item.setReason("复习 Java 后端高频知识点。");
        item.setReviewFocus("数组、链表、红黑树、扩容。");
        item.setSourceType("RAG_KNOWLEDGE_CARD");
        item.setSourceId(7L);
        item.setSourceSummary("来自 RAG 命中的知识卡片。");
        item.setStatus("PENDING");
        when(trainingPlanMapper.selectOne(any())).thenReturn(plan);
        when(trainingPlanItemMapper.selectList(any())).thenReturn(List.of(item));

        TrainingPlanVO result = userLearningService.getLatestTrainingPlan(1L);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getItemType()).isEqualTo("KNOWLEDGE_CARD");
        assertThat(result.getItems().get(0).getKnowledgeCardId()).isEqualTo(7L);
        assertThat(result.getItems().get(0).getKnowledgeCardTitle()).isEqualTo("HashMap 底层结构");
        assertThat(result.getItems().get(0).getSourceType()).isEqualTo("RAG_KNOWLEDGE_CARD");
        assertThat(result.getItems().get(0).getSourceId()).isEqualTo(7L);
        assertThat(result.getItems().get(0).getSourceSummary()).contains("RAG");
    }

    @Test
    void getTrainingPlanHistoryReturnsPlanSummariesWithItemCounts() {
        TrainingPlan active = trainingPlan();
        active.setId(100L);
        active.setTitle("当前训练计划");
        active.setStatus("ACTIVE");
        TrainingPlan completed = trainingPlan();
        completed.setId(99L);
        completed.setTitle("上一轮训练计划");
        completed.setStatus("COMPLETED");
        when(trainingPlanMapper.selectList(any())).thenReturn(List.of(active, completed));

        TrainingPlanItem pending = new TrainingPlanItem();
        pending.setPlanId(100L);
        pending.setStatus("PENDING");
        TrainingPlanItem done = new TrainingPlanItem();
        done.setPlanId(100L);
        done.setStatus("COMPLETED");
        TrainingPlanItem skipped = new TrainingPlanItem();
        skipped.setPlanId(99L);
        skipped.setStatus("SKIPPED");
        when(trainingPlanItemMapper.selectList(any())).thenReturn(List.of(pending, done, skipped));

        List<TrainingPlanHistoryVO> result = userLearningService.getTrainingPlanHistory(1L, 5);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(100L);
        assertThat(result.get(0).getTitle()).isEqualTo("当前训练计划");
        assertThat(result.get(0).getStatus()).isEqualTo("ACTIVE");
        assertThat(trainingPlanHistoryValue(result.get(0), "statusLabel")).isEqualTo("进行中");
        assertThat(result.get(0).getItemCount()).isEqualTo(2);
        assertThat(result.get(0).getCompletedCount()).isEqualTo(1);
        assertThat(result.get(0).getSkippedCount()).isEqualTo(0);
        assertThat(result.get(0).getPendingCount()).isEqualTo(1);
        assertThat(result.get(0).getHandledCount()).isEqualTo(1);
        assertThat(result.get(0).getCompletionRate()).isEqualByComparingTo("50.0");
        assertThat(result.get(0).getHandledRate()).isEqualByComparingTo("50.0");
        assertThat(result.get(1).getId()).isEqualTo(99L);
        assertThat(trainingPlanHistoryValue(result.get(1), "statusLabel")).isEqualTo("已完成");
        assertThat(result.get(1).getItemCount()).isEqualTo(1);
        assertThat(result.get(1).getCompletedCount()).isEqualTo(0);
        assertThat(result.get(1).getSkippedCount()).isEqualTo(1);
        assertThat(result.get(1).getPendingCount()).isEqualTo(0);
        assertThat(result.get(1).getHandledCount()).isEqualTo(1);
        assertThat(result.get(1).getCompletionRate()).isEqualByComparingTo("0.0");
        assertThat(result.get(1).getHandledRate()).isEqualByComparingTo("100.0");
    }

    @Test
    void getRecentTrainingActivitiesReturnsCompletedAndSkippedItemsWithPlanContext() {
        TrainingPlan plan = trainingPlan();
        plan.setId(100L);
        plan.setTitle("3 天当前弱点专项训练");
        plan.setCreatedAt(LocalDateTime.of(2026, 5, 23, 9, 30));
        when(trainingPlanMapper.selectList(any())).thenReturn(List.of(plan));

        TrainingPlanItem completed = new TrainingPlanItem();
        completed.setId(7L);
        completed.setPlanId(100L);
        completed.setItemType("PROBLEM");
        completed.setKnowledgePoint("HashMap 在两数之和中的应用");
        completed.setProblemTitle("两数之和专项复盘");
        completed.setSourceSummary("来自失败提交 #42 的 AI 诊断。");
        completed.setStatus("COMPLETED");
        completed.setStatusUpdatedAt(LocalDateTime.of(2026, 5, 24, 20, 0));

        TrainingPlanItem skipped = new TrainingPlanItem();
        skipped.setId(8L);
        skipped.setPlanId(100L);
        skipped.setItemType("KNOWLEDGE_CARD");
        skipped.setKnowledgePoint("Java 集合");
        skipped.setKnowledgeCardTitle("HashMap 底层结构");
        skipped.setStatus("SKIPPED");
        skipped.setStatusUpdatedAt(null);
        when(trainingPlanItemMapper.selectList(any())).thenReturn(List.of(completed, skipped));

        List<TrainingPlanActivityVO> result = userLearningService.getRecentTrainingActivities(1L, 5);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getItemId()).isEqualTo(7L);
        assertThat(result.get(0).getPlanTitle()).isEqualTo("3 天当前弱点专项训练");
        assertThat(result.get(0).getTaskTitle()).isEqualTo("两数之和专项复盘");
        assertThat(result.get(0).getStatus()).isEqualTo("COMPLETED");
        assertThat(trainingPlanActivityValue(result.get(0), "statusLabel")).isEqualTo("已完成");
        assertThat(result.get(0).getSourceType()).isEqualTo("LEGACY_TRAINING_PLAN");
        assertThat(result.get(0).getSourceSummary()).contains("失败提交");
        assertThat(result.get(0).getLearningImpactSummary())
                .contains("已完成", "HashMap 在两数之和中的应用", "薄弱点会记录为改善趋势");
        assertThat(result.get(0).getStatusUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 24, 20, 0));
        assertThat(result.get(1).getTaskTitle()).isEqualTo("HashMap 底层结构");
        assertThat(result.get(1).getStatus()).isEqualTo("SKIPPED");
        assertThat(trainingPlanActivityValue(result.get(1), "statusLabel")).isEqualTo("已跳过");
        assertThat(result.get(1).getSourceType()).isEqualTo("LEGACY_TRAINING_PLAN");
        assertThat(result.get(1).getSourceSummary()).contains("历史训练计划", "Java 集合");
        assertThat(result.get(1).getLearningImpactSummary())
                .contains("已跳过", "Java 集合", "不会降低薄弱点分数");
        assertThat(result.get(1).getStatusUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 23, 9, 30));
    }

    @Test
    void getTrainingPlanTraceReturnsProgressSourcesAndNextItem() {
        TrainingPlan plan = trainingPlan();
        plan.setId(100L);
        plan.setTitle("3 天当前弱点专项训练");
        plan.setSummary("围绕 HashMap 和 Java 集合补强。");
        plan.setStatus("ACTIVE");
        plan.setStartDate(LocalDate.now().minusDays(1));
        plan.setEndDate(LocalDate.now().plusDays(1));
        plan.setCreatedAt(LocalDateTime.now().minusDays(1));
        when(trainingPlanMapper.selectOne(any())).thenReturn(plan);

        TrainingPlanItem completed = new TrainingPlanItem();
        completed.setId(7L);
        completed.setPlanId(100L);
        completed.setItemType("PROBLEM");
        completed.setKnowledgePoint("HashMap 在两数之和中的应用");
        completed.setProblemTitle("两数之和专项复盘");
        completed.setSourceType("SUBMISSION_FAILED");
        completed.setSourceSummary("来自失败提交 #42 的 AI 诊断。");
        completed.setStatus("COMPLETED");
        completed.setStatusUpdatedAt(LocalDateTime.of(2026, 5, 24, 20, 0));

        TrainingPlanItem pending = new TrainingPlanItem();
        pending.setId(8L);
        pending.setPlanId(100L);
        pending.setItemType("KNOWLEDGE_CARD");
        pending.setKnowledgeCardId(9L);
        pending.setKnowledgePoint("Java 集合");
        pending.setKnowledgeCardTitle("HashMap 底层结构");
        pending.setSourceType("RAG_KNOWLEDGE_CARD");
        pending.setStatus("PENDING");

        TrainingPlanItem skipped = new TrainingPlanItem();
        skipped.setId(9L);
        skipped.setPlanId(100L);
        skipped.setItemType("PROBLEM");
        skipped.setKnowledgePoint("链表指针");
        skipped.setProblemTitle("反转链表");
        skipped.setSourceType("MOCK_INTERVIEW_REPORT");
        skipped.setStatus("SKIPPED");
        skipped.setStatusUpdatedAt(LocalDateTime.of(2026, 5, 24, 19, 0));
        when(trainingPlanItemMapper.selectList(any())).thenReturn(List.of(completed, pending, skipped));

        TrainingPlanTraceVO result = userLearningService.getTrainingPlanTrace(1L);

        assertThat(result.getPlanId()).isEqualTo(100L);
        assertThat(trainingPlanTraceValue(result, "statusLabel")).isEqualTo("进行中");
        assertThat(result.getStartDate()).isEqualTo(LocalDate.now().minusDays(1));
        assertThat(result.getEndDate()).isEqualTo(LocalDate.now().plusDays(1));
        assertThat(result.getPlanCreatedAt()).isNotNull();
        assertThat(result.getDaysSinceCreated()).isEqualTo(1);
        assertThat(result.getDaysRemaining()).isEqualTo(1);
        assertThat(result.getOverdue()).isFalse();
        assertThat(result.getItemCount()).isEqualTo(3);
        assertThat(result.getCompletedCount()).isEqualTo(1);
        assertThat(result.getPendingCount()).isEqualTo(1);
        assertThat(result.getSkippedCount()).isEqualTo(1);
        assertThat(result.getHandledCount()).isEqualTo(2);
        assertThat(result.getCompletionRate()).isEqualByComparingTo("33.3");
        assertThat(result.getHandledRate()).isEqualByComparingTo("66.7");
        assertThat(result.getSourceTypeCounts())
                .containsEntry("SUBMISSION_FAILED", 1)
                .containsEntry("RAG_KNOWLEDGE_CARD", 1)
                .containsEntry("MOCK_INTERVIEW_REPORT", 1);
        assertThat(result.getSourceTypeSummary())
                .contains("失败提交 1", "RAG 知识卡 1", "模拟面试报告 1");
        assertThat(result.getProgressSummary())
                .contains("已完成 1/3", "已处理 2/3", "剩余 1 天", "推荐来源");
        assertThat(result.getNextItem().getId()).isEqualTo(8L);
        assertThat(result.getNextItem().getItemType()).isEqualTo("KNOWLEDGE_CARD");
        assertThat(result.getNextItem().getSourceType()).isEqualTo("RAG_KNOWLEDGE_CARD");
        assertThat(result.getNextItem().getReason()).isNotBlank();
        assertThat(result.getNextItem().getTargetHref()).isEqualTo("/knowledge?cardId=9");
        assertThat(result.getNextItem().getTargetLabel()).isEqualTo("去复习");
        assertThat(result.getNextAction()).contains("下一步复习知识卡", "HashMap 底层结构");
        assertThat(result.getNextActionReason()).contains("RAG 知识卡", "Java 集合");
        assertThat(result.getNextActionPriority()).isEqualTo("HIGH");
        assertThat(result.getNextTargetHref()).isEqualTo("/knowledge?cardId=9");
        assertThat(result.getNextTargetLabel()).isEqualTo("去复习");
        assertThat(result.getRecentActivities()).hasSize(2);
        assertThat(result.getRecentActivities().get(0).getTaskTitle()).isEqualTo("两数之和专项复盘");
        assertThat(result.getRecentActivities().get(0).getLearningImpactSummary())
                .contains("已完成", "HashMap 在两数之和中的应用");
        assertThat(result.getLatestActivitySummary()).isEqualTo("最近完成：两数之和专项复盘");
        assertThat(result.getLatestActivityAt()).isEqualTo(LocalDateTime.of(2026, 5, 24, 20, 0));
    }

    @Test
    void getTrainingPlanTraceReturnsEmptyTraceWhenNoPlanExists() {
        when(trainingPlanMapper.selectOne(any())).thenReturn(null);

        TrainingPlanTraceVO result = userLearningService.getTrainingPlanTrace(1L);

        assertThat(result.getPlanId()).isNull();
        assertThat(result.getItemCount()).isZero();
        assertThat(result.getCompletionRate()).isEqualByComparingTo("0");
        assertThat(result.getHandledRate()).isEqualByComparingTo("0");
        assertThat(result.getNextAction()).contains("暂无训练计划");
        assertThat(result.getNextActionReason()).contains("训练闭环还没有起点", "提交诊断");
        assertThat(result.getNextActionPriority()).isEqualTo("HIGH");
        assertThat(result.getNextTargetHref()).isEqualTo("/problem/1");
        assertThat(result.getNextTargetLabel()).isEqualTo("去做题");
        assertThat(result.getProgressSummary()).contains("暂无训练计划");
        assertThat(result.getSourceTypeSummary()).isEqualTo("暂无推荐来源");
        assertThat(result.getRecentActivities()).isEmpty();
        assertThat(result.getLatestActivitySummary()).isNull();
    }

    @Test
    void getTrainingPlanTraceSuggestsRegenerationWhenNoPendingItemsRemain() {
        TrainingPlan plan = trainingPlan();
        plan.setId(100L);
        plan.setEndDate(LocalDate.now().minusDays(1));
        when(trainingPlanMapper.selectOne(any())).thenReturn(plan);
        TrainingPlanItem completed = new TrainingPlanItem();
        completed.setId(7L);
        completed.setPlanId(100L);
        completed.setItemType("PROBLEM");
        completed.setProblemTitle("两数之和专项复盘");
        completed.setStatus("COMPLETED");
        completed.setStatusUpdatedAt(LocalDateTime.of(2026, 5, 24, 20, 0));
        when(trainingPlanItemMapper.selectList(any())).thenReturn(List.of(completed));

        TrainingPlanTraceVO result = userLearningService.getTrainingPlanTrace(1L);

        assertThat(result.getNextItem()).isNull();
        assertThat(result.getHandledCount()).isEqualTo(1);
        assertThat(result.getHandledRate()).isEqualByComparingTo("100.0");
        assertThat(result.getDaysRemaining()).isEqualTo(-1);
        assertThat(result.getOverdue()).isFalse();
        assertThat(result.getLatestActivitySummary()).isEqualTo("最近完成：两数之和专项复盘");
        assertThat(result.getNextAction()).contains("暂无待完成项", "重新生成下一轮训练计划");
        assertThat(result.getNextActionReason()).contains("没有待完成项", "重新生成下一轮计划");
        assertThat(result.getNextActionPriority()).isEqualTo("MEDIUM");
        assertThat(result.getNextTargetHref()).isEqualTo("/dashboard");
        assertThat(result.getNextTargetLabel()).isEqualTo("查看计划");
    }

    @Test
    void getTrainingPlanTraceTreatsNullStatusAsPending() {
        TrainingPlan plan = trainingPlan();
        plan.setId(100L);
        plan.setEndDate(LocalDate.now().minusDays(1));
        when(trainingPlanMapper.selectOne(any())).thenReturn(plan);
        TrainingPlanItem pendingWithoutStatus = new TrainingPlanItem();
        pendingWithoutStatus.setId(8L);
        pendingWithoutStatus.setPlanId(100L);
        pendingWithoutStatus.setItemType("KNOWLEDGE_CARD");
        pendingWithoutStatus.setKnowledgeCardTitle("HashMap 底层结构");
        pendingWithoutStatus.setKnowledgePoint("Java 集合");
        when(trainingPlanItemMapper.selectList(any())).thenReturn(List.of(pendingWithoutStatus));

        TrainingPlanTraceVO result = userLearningService.getTrainingPlanTrace(1L);

        assertThat(result.getPendingCount()).isEqualTo(1);
        assertThat(result.getDaysRemaining()).isEqualTo(-1);
        assertThat(result.getOverdue()).isTrue();
        assertThat(result.getProgressSummary()).contains("已逾期 1 天");
        assertThat(result.getHandledCount()).isZero();
        assertThat(result.getHandledRate()).isEqualByComparingTo("0.0");
        assertThat(result.getNextItem().getId()).isEqualTo(8L);
        assertThat(result.getNextAction()).contains("下一步复习知识卡", "HashMap 底层结构");
        assertThat(result.getNextActionReason()).contains("学习记录", "Java 集合");
        assertThat(result.getNextActionPriority()).isEqualTo("HIGH");
        assertThat(result.getNextTargetHref()).isEqualTo("/knowledge");
        assertThat(result.getNextTargetLabel()).isEqualTo("去复习");
    }

    @Test
    void getWeaknessesMergesSameKnowledgePointAcrossLegacyNamesAndErrorTypes() {
        when(userWeaknessMapper.selectList(any())).thenReturn(List.of(
                weakness(1L, "HashMap 在两数之和中的应用", "LOGIC_ERROR", 6, "38.0"),
                weakness(2L, "HashMap in Two Sum", "ALGORITHM_ERROR", 4, "31.0"),
                weakness(3L, "HashMap 基础查找", "LOGIC_ERROR", 3, "20.0")));

        List<UserWeaknessVO> result = userLearningService.getWeaknesses(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getKnowledgePoint()).isEqualTo("HashMap 在两数之和中的应用");
        assertThat(result.get(0).getWrongCount()).isEqualTo(10);
        assertThat(result.get(0).getWeaknessScore()).isEqualByComparingTo("69.0");
        assertThat(result.get(0).getErrorType()).isEqualTo("LOGIC_ERROR");
        assertThat(result.get(1).getKnowledgePoint()).isEqualTo("HashMap 基础查找");
    }

    @Test
    void getWeaknessesUsesTrainingCompletionEventAsImprovementTrend() {
        when(userWeaknessMapper.selectList(any())).thenReturn(List.of(
                weakness(1L, "HashMap 在两数之和中的应用", "LOGIC_ERROR", 3, "36.0")));
        UserWeaknessEvent event = new UserWeaknessEvent();
        event.setKnowledgePoint("HashMap 在两数之和中的应用");
        event.setSourceType("TRAINING_PLAN_COMPLETED");
        event.setDeltaScore(new BigDecimal("-2"));
        event.setCreatedAt(LocalDateTime.of(2026, 5, 24, 21, 0));
        when(userWeaknessEventMapper.selectList(any())).thenReturn(List.of(event));

        List<UserWeaknessVO> result = userLearningService.getWeaknesses(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTrendLabel()).isEqualTo("最近改善");
        assertThat(result.get(0).getLastDeltaScore()).isEqualByComparingTo("-2");
        assertThat(result.get(0).getLastEventAt()).isEqualTo(LocalDateTime.of(2026, 5, 24, 21, 0));
    }

    @Test
    void getWeaknessesDistinguishesWorseningAndSustainedWeakTrend() {
        when(userWeaknessMapper.selectList(any())).thenReturn(List.of(
                weakness(1L, "HashMap 在两数之和中的应用", "LOGIC_ERROR", 4, "42.0"),
                weakness(2L, "链表指针操作", "RUNTIME_ERROR", 5, "45.0")));
        UserWeaknessEvent worseningEvent = new UserWeaknessEvent();
        worseningEvent.setKnowledgePoint("HashMap 在两数之和中的应用");
        worseningEvent.setSourceType("SUBMISSION_FAILED");
        worseningEvent.setDeltaScore(new BigDecimal("6"));
        worseningEvent.setCreatedAt(LocalDateTime.of(2026, 5, 24, 21, 0));
        when(userWeaknessEventMapper.selectList(any())).thenReturn(List.of(worseningEvent));

        List<UserWeaknessVO> result = userLearningService.getWeaknesses(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getKnowledgePoint()).isEqualTo("链表指针操作");
        assertThat(result.get(0).getTrendLabel()).isEqualTo("持续薄弱");
        assertThat(result.get(1).getKnowledgePoint()).isEqualTo("HashMap 在两数之和中的应用");
        assertThat(result.get(1).getTrendLabel()).isEqualTo("最近加重");
        assertThat(result.get(1).getLastDeltaScore()).isEqualByComparingTo("6");
    }

    @Test
    void getErrorStatsAggregatesTopWeakPointsWithSameDisplayKnowledgePoint() {
        when(userWeaknessMapper.selectList(any())).thenReturn(List.of(
                weakness(1L, "HashMap 在两数之和中的应用", "LOGIC_ERROR", 6, "38.0"),
                weakness(2L, "HashMap in Two Sum", "ALGORITHM_ERROR", 4, "31.0"),
                weakness(3L, "HashMap 字符串计数", "SYSTEM_ERROR", 2, "16.0")));

        ErrorStatsVO result = userLearningService.getErrorStats(1L);

        assertThat(result.getTopWeakPoints()).hasSize(2);
        assertThat(result.getTopWeakPoints().get(0).getKnowledgePoint())
                .isEqualTo("HashMap 在两数之和中的应用");
        assertThat(result.getTopWeakPoints().get(0).getWrongCount()).isEqualTo(10);
        assertThat(result.getTopWeakPoints().get(0).getWeaknessScore()).isEqualTo(69.0);
        assertThat(result.getErrorTypeDistribution())
                .extracting(ErrorStatsVO.ErrorTypeCount::getCount)
                .containsExactly(6, 4, 2);
    }

    @Test
    void getMistakesAddsProblemTitleFromBatchProblemLookup() {
        MistakeCard mistake = new MistakeCard();
        mistake.setId(9L);
        mistake.setProblemId(101L);
        mistake.setErrorType("LOGIC_ERROR");
        mistake.setKnowledgePoint("HashMap");
        mistake.setMistakeSummary("Checked complement after insert.");
        mistake.setCorrectIdea("Check complement before insert.");
        when(mistakeCardMapper.selectList(any())).thenReturn(List.of(mistake));
        when(problemMapper.selectBatchIds(any())).thenReturn(List.of(problem(101L, "Two Sum")));

        List<MistakeCardVO> mistakes = userLearningService.getMistakes(1L);

        assertThat(mistakes).hasSize(1);
        assertThat(mistakes.get(0).getProblemTitle()).isEqualTo("Two Sum");
        assertThat(mistakes.get(0).getMistakeSummary()).isEqualTo("Checked complement after insert.");
        assertThat(mistakes.get(0).getCorrectIdea()).isEqualTo("Check complement before insert.");
    }

    @Test
    void getRecentSubmissionsAddsProblemTitleFromBatchProblemLookup() {
        Submission submission = submission(102L, "WRONG_ANSWER");
        submission.setPassedCount(1);
        submission.setTotalCount(3);
        when(submissionMapper.selectList(any())).thenReturn(List.of(submission));
        when(problemMapper.selectBatchIds(any())).thenReturn(List.of(problem(102L, "Reverse Linked List")));

        List<SubmissionHistoryVO> submissions = userLearningService.getRecentSubmissions(1L);

        assertThat(submissions).hasSize(1);
        assertThat(submissions.get(0).getProblemTitle()).isEqualTo("Reverse Linked List");
        assertThat(submissions.get(0).getStatus()).isEqualTo("WRONG_ANSWER");
        assertThat(submissions.get(0).getPassedCount()).isEqualTo(1);
        assertThat(submissions.get(0).getTotalCount()).isEqualTo(3);
    }

    @Test
    void getRecentMockInterviewsReturnsSessionAndReportSummary() {
        MockInterviewSession reported = mockSession(30L, "REPORTED");
        MockInterviewSession active = mockSession(31L, "ASKING_FOLLOW_UP");
        when(mockInterviewSessionMapper.selectList(any())).thenReturn(List.of(reported, active));
        MockInterviewReport report = new MockInterviewReport();
        report.setSessionId(30L);
        report.setAverageScore(new BigDecimal("76.50"));
        report.setWeaknessTags("BeanPostProcessor,销毁");
        report.setCreatedAt(LocalDateTime.now());
        when(mockInterviewReportMapper.selectList(any())).thenReturn(List.of(report));

        List<MockInterviewRecentVO> result = userLearningService.getRecentMockInterviews(1L, 5);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSessionId()).isEqualTo(30L);
        assertThat(result.get(0).getStatus()).isEqualTo("REPORTED");
        assertThat(result.get(0).getCategory()).isEqualTo("SPRING");
        assertThat(result.get(0).getAverageScore()).isEqualByComparingTo("76.50");
        assertThat(result.get(0).getWeaknessTags()).containsExactly("BeanPostProcessor", "销毁");
        assertThat(result.get(1).getSessionId()).isEqualTo(31L);
        assertThat(result.get(1).getAverageScore()).isNull();
        assertThat(result.get(1).getWeaknessTags()).isEmpty();
    }

    @Test
    void getMockInterviewTraceConnectsReportWeaknessAndTrainingPlanItems() {
        MockInterviewSession latest = mockSession(30L, "REPORTED");
        latest.setFinishedAt(LocalDateTime.of(2026, 5, 24, 21, 0));
        MockInterviewSession active = mockSession(31L, "ASKING_FOLLOW_UP");
        when(mockInterviewSessionMapper.selectList(any())).thenReturn(List.of(latest, active));

        MockInterviewReport report = new MockInterviewReport();
        report.setId(70L);
        report.setSessionId(30L);
        report.setUserId(1L);
        report.setAverageScore(new BigDecimal("58.50"));
        report.setWeaknessTags("BeanPostProcessor,表达结构");
        report.setRecommendedCardIds("11,12");
        report.setCreatedAt(LocalDateTime.of(2026, 5, 24, 21, 5));
        when(mockInterviewReportMapper.selectList(any())).thenReturn(List.of(report));

        MockInterviewTurn lowScore = mockTurn(1L, 30L, 11L, 45,
                "缺少 BeanPostProcessor 生命周期",
                LocalDateTime.of(2026, 5, 24, 20, 30));
        MockInterviewTurn okScore = mockTurn(2L, 30L, 12L, 80,
                "表达结构清晰",
                LocalDateTime.of(2026, 5, 24, 20, 40));
        when(mockInterviewTurnMapper.selectList(any())).thenReturn(List.of(lowScore, okScore));
        when(userWeaknessEventMapper.selectCount(any())).thenReturn(2L);

        TrainingPlan plan = trainingPlan();
        plan.setId(100L);
        when(trainingPlanMapper.selectList(any())).thenReturn(List.of(plan));
        when(trainingPlanItemMapper.selectCount(any())).thenReturn(2L);

        MockInterviewTraceVO result = userLearningService.getMockInterviewTrace(1L);

        assertThat(result.getSessionCount()).isEqualTo(2);
        assertThat(result.getReportedSessionCount()).isEqualTo(1);
        assertThat(result.getLatestSessionId()).isEqualTo(30L);
        assertThat(mockInterviewTraceValue(result, "latestSessionStatusLabel")).isEqualTo("已生成报告");
        assertThat(result.getLatestReportId()).isEqualTo(70L);
        assertThat(result.getLatestAverageScore()).isEqualByComparingTo("58.50");
        assertThat(result.getLatestWeaknessTags()).containsExactly("BeanPostProcessor", "表达结构");
        assertThat(result.getRecommendedCardIds()).containsExactly(11L, 12L);
        assertThat(result.getAnsweredTurnCount()).isEqualTo(2);
        assertThat(result.getLowScoreTurnCount()).isEqualTo(1);
        assertThat(result.getWeaknessEventCount()).isEqualTo(2);
        assertThat(result.getTrainingPlanItemCount()).isEqualTo(2);
        assertThat(result.getReportTrainingPlanLinked()).isTrue();
        assertThat(result.getLatestInterviewAt()).isEqualTo(LocalDateTime.of(2026, 5, 24, 21, 0));
        assertThat(result.getClosureStatus()).isEqualTo("REVIEW_CARD_REQUIRED");
        assertThat(result.getClosureStatusLabel()).isEqualTo("复盘推荐卡");
        assertThat(result.getNextAction()).contains("复盘最近面试推荐知识卡 #11");
        assertThat(result.getNextActionReason()).contains("报告已推荐知识卡", "接入训练计划");
        assertThat(result.getNextActionPriority()).isEqualTo("HIGH");
        assertThat(result.getClosureSummary()).contains("已报告 1/2", "低分回答 1", "报告已接入训练计划");
        assertThat(result.getReviewPathSummary())
                .contains("报告 #70", "推荐知识卡 2 张", "训练计划 2 项", "/knowledge?cardId=11");
        assertThat(result.getNextTargetHref()).isEqualTo("/knowledge?cardId=11");
        assertThat(result.getNextTargetLabel()).isEqualTo("去复盘");
        assertThat(result.getReportReviewHref()).isEqualTo("/mock-interview?sessionId=30");
        assertThat(result.getReportReviewLabel()).isEqualTo("查看报告");
    }

    @Test
    void getMockInterviewTraceReturnsEmptyTraceWhenNoSessionExists() {
        when(mockInterviewSessionMapper.selectList(any())).thenReturn(List.of());

        MockInterviewTraceVO result = userLearningService.getMockInterviewTrace(1L);

        assertThat(result.getSessionCount()).isZero();
        assertThat(result.getLatestSessionId()).isNull();
        assertThat(result.getLatestWeaknessTags()).isEmpty();
        assertThat(result.getRecommendedCardIds()).isEmpty();
        assertThat(result.getReportTrainingPlanLinked()).isFalse();
        assertThat(result.getClosureStatus()).isEqualTo("NO_SESSION");
        assertThat(result.getClosureStatusLabel()).isEqualTo("尚未开始");
        assertThat(result.getNextAction()).contains("先开始一场后端知识模拟面试");
        assertThat(result.getNextActionReason()).contains("闭环还没有起点", "生成报告");
        assertThat(result.getNextActionPriority()).isEqualTo("HIGH");
        assertThat(result.getClosureSummary()).contains("暂无模拟面试记录");
        assertThat(result.getReviewPathSummary()).contains("暂无模拟面试记录");
        assertThat(result.getNextTargetHref()).isEqualTo("/mock-interview");
        assertThat(result.getNextTargetLabel()).isEqualTo("开始面试");
        assertThat(result.getReportReviewHref()).isNull();
        assertThat(result.getReportReviewLabel()).isNull();
    }

    @Test
    void getMockInterviewTraceMarksReportUnlinkedWhenNoTrainingItemsExist() {
        MockInterviewSession latest = mockSession(30L, "REPORTED");
        when(mockInterviewSessionMapper.selectList(any())).thenReturn(List.of(latest));
        MockInterviewReport report = new MockInterviewReport();
        report.setId(70L);
        report.setSessionId(30L);
        report.setUserId(1L);
        report.setAverageScore(new BigDecimal("72.00"));
        report.setRecommendedCardIds("11");
        report.setCreatedAt(LocalDateTime.of(2026, 5, 24, 21, 5));
        when(mockInterviewReportMapper.selectList(any())).thenReturn(List.of(report));
        when(mockInterviewTurnMapper.selectList(any())).thenReturn(List.of());
        when(userWeaknessEventMapper.selectCount(any())).thenReturn(0L);
        when(trainingPlanMapper.selectList(any())).thenReturn(List.of(trainingPlan()));
        when(trainingPlanItemMapper.selectCount(any())).thenReturn(0L);

        MockInterviewTraceVO result = userLearningService.getMockInterviewTrace(1L);

        assertThat(result.getTrainingPlanItemCount()).isZero();
        assertThat(result.getReportTrainingPlanLinked()).isFalse();
        assertThat(result.getClosureStatus()).isEqualTo("REVIEW_CARD_REQUIRED");
        assertThat(result.getClosureStatusLabel()).isEqualTo("复盘推荐卡");
        assertThat(result.getNextAction()).contains("复盘最近面试推荐知识卡 #11");
        assertThat(result.getNextActionReason()).contains("训练计划尚未沉淀", "复盘推荐卡");
        assertThat(result.getNextActionPriority()).isEqualTo("HIGH");
        assertThat(result.getClosureSummary()).contains("报告暂未接入训练计划");
        assertThat(result.getReviewPathSummary())
                .contains("报告 #70", "推荐知识卡 1 张", "训练计划 0 项", "待检查训练计划生成");
        assertThat(result.getNextTargetHref()).isEqualTo("/knowledge?cardId=11");
        assertThat(result.getNextTargetLabel()).isEqualTo("去复盘");
    }

    @Test
    void getMockInterviewTraceSuggestsContinuingActiveSession() {
        MockInterviewSession active = mockSession(31L, "ASKING_FOLLOW_UP");
        when(mockInterviewSessionMapper.selectList(any())).thenReturn(List.of(active));
        when(mockInterviewReportMapper.selectList(any())).thenReturn(List.of());
        when(mockInterviewTurnMapper.selectList(any())).thenReturn(List.of());
        when(userWeaknessEventMapper.selectCount(any())).thenReturn(0L);

        MockInterviewTraceVO result = userLearningService.getMockInterviewTrace(1L);

        assertThat(result.getLatestSessionId()).isEqualTo(31L);
        assertThat(result.getClosureStatus()).isEqualTo("IN_PROGRESS");
        assertThat(result.getClosureStatusLabel()).isEqualTo("继续面试");
        assertThat(result.getNextAction()).contains("继续完成最近模拟面试").contains("#31");
        assertThat(result.getNextActionReason()).contains("尚未生成报告", "主问题和追问");
        assertThat(result.getNextActionPriority()).isEqualTo("HIGH");
        assertThat(result.getClosureSummary()).contains("最近会话仍在进行中");
        assertThat(result.getReviewPathSummary()).contains("会话 #31", "尚未生成报告");
        assertThat(result.getNextTargetHref()).isEqualTo("/mock-interview?sessionId=31");
        assertThat(result.getNextTargetLabel()).isEqualTo("继续面试");
    }

    @Test
    void getMockInterviewTraceSuggestsReviewingReportWhenNoRecommendedCardsButLowScoreExists() {
        MockInterviewSession latest = mockSession(30L, "REPORTED");
        when(mockInterviewSessionMapper.selectList(any())).thenReturn(List.of(latest));
        MockInterviewReport report = new MockInterviewReport();
        report.setId(70L);
        report.setSessionId(30L);
        report.setUserId(1L);
        report.setAverageScore(new BigDecimal("55.00"));
        report.setWeaknessTags("表达结构");
        report.setRecommendedCardIds("");
        report.setCreatedAt(LocalDateTime.of(2026, 5, 24, 21, 5));
        when(mockInterviewReportMapper.selectList(any())).thenReturn(List.of(report));
        MockInterviewTurn lowScore = mockTurn(1L, 30L, 11L, 45,
                "缺少事务传播边界", LocalDateTime.of(2026, 5, 24, 20, 30));
        when(mockInterviewTurnMapper.selectList(any())).thenReturn(List.of(lowScore));
        when(userWeaknessEventMapper.selectCount(any())).thenReturn(1L);

        MockInterviewTraceVO result = userLearningService.getMockInterviewTrace(1L);

        assertThat(result.getRecommendedCardIds()).isEmpty();
        assertThat(result.getLowScoreTurnCount()).isEqualTo(1);
        assertThat(result.getClosureStatus()).isEqualTo("REPORT_REVIEW_REQUIRED");
        assertThat(result.getClosureStatusLabel()).isEqualTo("复盘报告");
        assertThat(result.getNextAction()).contains("复盘最近模拟面试报告", "低分回答");
        assertThat(result.getNextActionReason()).contains("低分回答", "薄弱标签", "缺失要点");
        assertThat(result.getNextActionPriority()).isEqualTo("MEDIUM");
        assertThat(result.getNextTargetHref()).isEqualTo("/mock-interview?sessionId=30");
        assertThat(result.getNextTargetLabel()).isEqualTo("查看报告");
    }

    @Test
    void getMockInterviewTraceSuggestsSameCategoryRetestWhenReportHasNoPendingReviewWork() {
        MockInterviewSession latest = mockSession(30L, "REPORTED");
        latest.setCategory("MYSQL");
        when(mockInterviewSessionMapper.selectList(any())).thenReturn(List.of(latest));
        MockInterviewReport report = new MockInterviewReport();
        report.setId(70L);
        report.setSessionId(30L);
        report.setUserId(1L);
        report.setAverageScore(new BigDecimal("88.00"));
        report.setRecommendedCardIds("");
        report.setCreatedAt(LocalDateTime.of(2026, 5, 24, 21, 5));
        when(mockInterviewReportMapper.selectList(any())).thenReturn(List.of(report));
        MockInterviewTurn strongTurn = mockTurn(1L, 30L, 11L, 88,
                "", LocalDateTime.of(2026, 5, 24, 20, 30));
        when(mockInterviewTurnMapper.selectList(any())).thenReturn(List.of(strongTurn));
        when(userWeaknessEventMapper.selectCount(any())).thenReturn(0L);

        MockInterviewTraceVO result = userLearningService.getMockInterviewTrace(1L);

        assertThat(result.getLatestCategory()).isEqualTo("MYSQL");
        assertThat(result.getLowScoreTurnCount()).isZero();
        assertThat(result.getRecommendedCardIds()).isEmpty();
        assertThat(result.getClosureStatus()).isEqualTo("RETEST_READY");
        assertThat(result.getClosureStatusLabel()).isEqualTo("同类复测");
        assertThat(result.getNextAction()).contains("同类知识点再做一次复测");
        assertThat(result.getNextTargetHref()).isEqualTo("/mock-interview?category=MYSQL");
        assertThat(result.getNextTargetLabel()).isEqualTo("同类复测");
        assertThat(result.getClosureSummary()).contains("无需生成复盘训练计划");
        assertThat(result.getReviewPathSummary())
                .contains("无待复盘推荐卡", "下一步 /mock-interview?category=MYSQL")
                .doesNotContain("待检查训练计划生成");
    }

    @Test
    void getMockInterviewTraceIgnoresOlderLowScoreTurnsWhenLatestReportHasNoPendingReviewWork() {
        MockInterviewSession latest = mockSession(30L, "REPORTED");
        latest.setCategory("REDIS");
        MockInterviewSession older = mockSession(29L, "REPORTED");
        older.setCategory("SPRING");
        when(mockInterviewSessionMapper.selectList(any())).thenReturn(List.of(latest, older));
        MockInterviewReport latestReport = new MockInterviewReport();
        latestReport.setId(70L);
        latestReport.setSessionId(30L);
        latestReport.setUserId(1L);
        latestReport.setAverageScore(new BigDecimal("90.00"));
        latestReport.setRecommendedCardIds("");
        latestReport.setCreatedAt(LocalDateTime.of(2026, 5, 24, 21, 5));
        when(mockInterviewReportMapper.selectList(any())).thenReturn(List.of(latestReport));
        MockInterviewTurn latestStrongTurn = mockTurn(1L, 30L, 11L, 90,
                "", LocalDateTime.of(2026, 5, 24, 20, 30));
        MockInterviewTurn olderLowScoreTurn = mockTurn(2L, 29L, 12L, 40,
                "历史会话低分，不应污染最近报告下一步", LocalDateTime.of(2026, 5, 23, 20, 30));
        when(mockInterviewTurnMapper.selectList(any())).thenReturn(List.of(latestStrongTurn, olderLowScoreTurn));
        when(userWeaknessEventMapper.selectCount(any())).thenReturn(1L);

        MockInterviewTraceVO result = userLearningService.getMockInterviewTrace(1L);

        assertThat(result.getAnsweredTurnCount()).isEqualTo(1);
        assertThat(result.getLowScoreTurnCount()).isZero();
        assertThat(result.getNextAction()).contains("同类知识点再做一次复测");
        assertThat(result.getNextTargetHref()).isEqualTo("/mock-interview?category=REDIS");
        assertThat(result.getClosureSummary()).contains("低分回答 0", "无需生成复盘训练计划");
    }

    @Test
    void getMockInterviewTrendsShowsScoreDeltaByKnowledgeCard() {
        MockInterviewSession latest = mockSession(40L, "REPORTED");
        latest.setFinishedAt(LocalDateTime.of(2026, 5, 24, 20, 0));
        latest.setCreatedAt(LocalDateTime.of(2026, 5, 24, 19, 45));
        MockInterviewSession previous = mockSession(39L, "REPORTED");
        previous.setFinishedAt(LocalDateTime.of(2026, 5, 23, 20, 0));
        previous.setCreatedAt(LocalDateTime.of(2026, 5, 23, 19, 45));
        when(mockInterviewSessionMapper.selectList(any())).thenReturn(List.of(latest, previous));
        when(mockInterviewTurnMapper.selectList(any())).thenReturn(List.of(
                mockTurn(1L, 40L, 7L, 80, "Bean 生命周期缺少销毁阶段",
                        LocalDateTime.of(2026, 5, 24, 19, 50)),
                mockTurn(2L, 40L, 7L, 70, "追问里没有说清楚 BeanPostProcessor",
                        LocalDateTime.of(2026, 5, 24, 19, 55)),
                mockTurn(3L, 39L, 7L, 60, "初始化流程顺序不清楚",
                        LocalDateTime.of(2026, 5, 23, 19, 50))));
        KnowledgeCard card = new KnowledgeCard();
        card.setId(7L);
        card.setCategory("SPRING");
        card.setTitle("Spring Bean 生命周期");
        when(knowledgeCardMapper.selectBatchIds(any())).thenReturn(List.of(card));

        List<MockInterviewTrendVO> result = userLearningService.getMockInterviewTrends(1L, 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKnowledgeCardId()).isEqualTo(7L);
        assertThat(result.get(0).getKnowledgePoint()).isEqualTo("Spring Bean 生命周期");
        assertThat(result.get(0).getLatestSessionId()).isEqualTo(40L);
        assertThat(result.get(0).getLatestScore()).isEqualByComparingTo("75.0");
        assertThat(result.get(0).getPreviousScore()).isEqualByComparingTo("60.0");
        assertThat(result.get(0).getDeltaScore()).isEqualByComparingTo("15.0");
        assertThat(result.get(0).getTrendLabel()).contains("提升");
        assertThat(result.get(0).getInterviewCount()).isEqualTo(2);
        assertThat(result.get(0).getLatestIssue()).contains("Bean 生命周期");
    }

    @Test
    void getMockInterviewTrendsDistinguishesScoreDropAndFlatTrend() {
        MockInterviewSession latest = mockSession(40L, "REPORTED");
        latest.setFinishedAt(LocalDateTime.of(2026, 5, 24, 20, 0));
        latest.setCreatedAt(LocalDateTime.of(2026, 5, 24, 19, 45));
        MockInterviewSession previous = mockSession(39L, "REPORTED");
        previous.setFinishedAt(LocalDateTime.of(2026, 5, 23, 20, 0));
        previous.setCreatedAt(LocalDateTime.of(2026, 5, 23, 19, 45));
        when(mockInterviewSessionMapper.selectList(any())).thenReturn(List.of(latest, previous));
        when(mockInterviewTurnMapper.selectList(any())).thenReturn(List.of(
                mockTurn(1L, 40L, 7L, 70, "事务隔离级别解释不完整",
                        LocalDateTime.of(2026, 5, 24, 19, 50)),
                mockTurn(2L, 39L, 7L, 85, "MVCC 和锁边界不清楚",
                        LocalDateTime.of(2026, 5, 23, 19, 50)),
                mockTurn(3L, 40L, 8L, 75, "表达结构基本稳定",
                        LocalDateTime.of(2026, 5, 24, 19, 55)),
                mockTurn(4L, 39L, 8L, 75, "表达结构基本稳定",
                        LocalDateTime.of(2026, 5, 23, 19, 55))));
        KnowledgeCard txCard = new KnowledgeCard();
        txCard.setId(7L);
        txCard.setCategory("MYSQL");
        txCard.setTitle("MySQL 事务隔离");
        KnowledgeCard hashCard = new KnowledgeCard();
        hashCard.setId(8L);
        hashCard.setCategory("JAVA");
        hashCard.setTitle("HashMap 扩容机制");
        when(knowledgeCardMapper.selectBatchIds(any())).thenReturn(List.of(txCard, hashCard));

        List<MockInterviewTrendVO> result = userLearningService.getMockInterviewTrends(1L, 5);

        assertThat(result).hasSize(2);
        MockInterviewTrendVO dropped = result.stream()
                .filter(trend -> trend.getKnowledgeCardId().equals(7L))
                .findFirst()
                .orElseThrow();
        assertThat(dropped.getLatestScore()).isEqualByComparingTo("70.0");
        assertThat(dropped.getPreviousScore()).isEqualByComparingTo("85.0");
        assertThat(dropped.getDeltaScore()).isEqualByComparingTo("-15.0");
        assertThat(dropped.getTrendLabel()).contains("下降", "15");

        MockInterviewTrendVO flat = result.stream()
                .filter(trend -> trend.getKnowledgeCardId().equals(8L))
                .findFirst()
                .orElseThrow();
        assertThat(flat.getLatestScore()).isEqualByComparingTo("75.0");
        assertThat(flat.getPreviousScore()).isEqualByComparingTo("75.0");
        assertThat(flat.getDeltaScore()).isEqualByComparingTo("0.0");
        assertThat(flat.getTrendLabel()).contains("持平");
    }

    @Test
    void getMockInterviewTrendsDistinguishesKnowledgeGapAndExpressionGap() {
        MockInterviewSession latest = mockSession(40L, "REPORTED");
        latest.setFinishedAt(LocalDateTime.of(2026, 5, 24, 20, 0));
        latest.setCreatedAt(LocalDateTime.of(2026, 5, 24, 19, 45));
        when(mockInterviewSessionMapper.selectList(any())).thenReturn(List.of(latest));
        MockInterviewTurn knowledgeGapTurn = mockTurn(1L, 40L, 7L, 52, "没有说出 BeanPostProcessor 扩展点",
                LocalDateTime.of(2026, 5, 24, 19, 50));
        MockInterviewTurn expressionGapTurn = mockTurn(2L, 40L, 8L, 72, "",
                LocalDateTime.of(2026, 5, 24, 19, 55));
        expressionGapTurn.setExpressionIssue("表达偏罗列，缺少先后顺序。");
        when(mockInterviewTurnMapper.selectList(any())).thenReturn(List.of(knowledgeGapTurn, expressionGapTurn));
        KnowledgeCard beanCard = new KnowledgeCard();
        beanCard.setId(7L);
        beanCard.setCategory("SPRING");
        beanCard.setTitle("Spring Bean 生命周期");
        KnowledgeCard hashmapCard = new KnowledgeCard();
        hashmapCard.setId(8L);
        hashmapCard.setCategory("JAVA");
        hashmapCard.setTitle("HashMap 扩容机制");
        when(knowledgeCardMapper.selectBatchIds(any())).thenReturn(List.of(beanCard, hashmapCard));

        List<MockInterviewTrendVO> result = userLearningService.getMockInterviewTrends(1L, 5);

        MockInterviewTrendVO knowledgeGap = result.stream()
                .filter(trend -> trend.getKnowledgeCardId().equals(7L))
                .findFirst()
                .orElseThrow();
        assertThat(knowledgeGap)
                .extracting("latestIssueType", "latestIssueTypeLabel")
                .containsExactly("KNOWLEDGE_GAP", "知识点不会");

        MockInterviewTrendVO expressionGap = result.stream()
                .filter(trend -> trend.getKnowledgeCardId().equals(8L))
                .findFirst()
                .orElseThrow();
        assertThat(expressionGap)
                .extracting("latestIssueType", "latestIssueTypeLabel")
                .containsExactly("EXPRESSION_GAP", "表达不完整");
    }

    private Submission submission(Long problemId, String status) {
        Submission submission = new Submission();
        submission.setUserId(1L);
        submission.setProblemId(problemId);
        submission.setStatus(status);
        return submission;
    }

    private Problem problem(Long id, String title) {
        Problem problem = new Problem();
        problem.setId(id);
        problem.setTitle(title);
        return problem;
    }

    private TrainingPlan trainingPlan() {
        TrainingPlan plan = new TrainingPlan();
        plan.setId(100L);
        plan.setUserId(1L);
        plan.setTitle("3 天专项训练");
        plan.setSummary("算法训练和复习。");
        plan.setStartDate(LocalDate.now());
        plan.setEndDate(LocalDate.now().plusDays(2));
        plan.setCreatedAt(LocalDateTime.now());
        return plan;
    }

    private MockInterviewSession mockSession(Long id, String status) {
        MockInterviewSession session = new MockInterviewSession();
        session.setId(id);
        session.setUserId(1L);
        session.setCategory("SPRING");
        session.setStatus(status);
        session.setInterviewerStyle("BIG_TECH");
        session.setQuestionCount(3);
        session.setAnsweredMainCount(1);
        session.setStartedAt(LocalDateTime.now().minusMinutes(15));
        session.setFinishedAt("REPORTED".equals(status) ? LocalDateTime.now().minusMinutes(5) : null);
        session.setCreatedAt(LocalDateTime.now().minusMinutes(15));
        session.setUpdatedAt(LocalDateTime.now().minusMinutes(5));
        return session;
    }

    private MockInterviewTurn mockTurn(Long id, Long sessionId, Long knowledgeCardId, Integer score,
            String missingKeyPoints, LocalDateTime createdAt) {
        MockInterviewTurn turn = new MockInterviewTurn();
        turn.setId(id);
        turn.setSessionId(sessionId);
        turn.setKnowledgeCardId(knowledgeCardId);
        turn.setTurnOrder(id.intValue());
        turn.setTurnType("MAIN");
        turn.setScore(score);
        turn.setMissingKeyPoints(missingKeyPoints);
        turn.setCreatedAt(createdAt);
        return turn;
    }

    private Object mockInterviewTraceValue(MockInterviewTraceVO trace, String fieldName) {
        try {
            java.lang.reflect.Field field = MockInterviewTraceVO.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(trace);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Missing mock interview trace field: " + fieldName, ex);
        }
    }

    private Object trainingPlanTraceValue(TrainingPlanTraceVO trace, String fieldName) {
        try {
            java.lang.reflect.Field field = TrainingPlanTraceVO.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(trace);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Missing training plan trace field: " + fieldName, ex);
        }
    }

    private Object trainingPlanValue(TrainingPlanVO plan, String fieldName) {
        try {
            java.lang.reflect.Field field = TrainingPlanVO.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(plan);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Missing training plan field: " + fieldName, ex);
        }
    }

    private Object trainingPlanHistoryValue(TrainingPlanHistoryVO plan, String fieldName) {
        try {
            java.lang.reflect.Field field = TrainingPlanHistoryVO.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(plan);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Missing training plan history field: " + fieldName, ex);
        }
    }

    private Object trainingPlanActivityValue(TrainingPlanActivityVO activity, String fieldName) {
        try {
            java.lang.reflect.Field field = TrainingPlanActivityVO.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(activity);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Missing training plan activity field: " + fieldName, ex);
        }
    }

    private UserWeakness weakness(Long id, String knowledgePoint, String errorType,
            Integer wrongCount, String weaknessScore) {
        UserWeakness weakness = new UserWeakness();
        weakness.setId(id);
        weakness.setUserId(1L);
        weakness.setKnowledgePoint(knowledgePoint);
        weakness.setErrorType(errorType);
        weakness.setWrongCount(wrongCount);
        weakness.setSubmitCount(wrongCount);
        weakness.setWeaknessScore(new BigDecimal(weaknessScore));
        return weakness;
    }
}
