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
import com.interview.coach.vo.MockInterviewTrendVO;
import com.interview.coach.vo.SubmissionHistoryVO;
import com.interview.coach.vo.TrainingPlanActivityVO;
import com.interview.coach.vo.TrainingPlanHistoryVO;
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
        item.setReviewFocus("Check complement before insert.");
        item.setStatus("PENDING");
        when(trainingPlanMapper.selectOne(any())).thenReturn(plan);
        when(trainingPlanItemMapper.selectList(any())).thenReturn(List.of(item));

        TrainingPlanVO result = userLearningService.getLatestTrainingPlan(1L);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getItemType()).isEqualTo("PROBLEM");
        assertThat(result.getItems().get(0).getProblemTitle()).isEqualTo("Two Sum");
        assertThat(result.getItems().get(0).getProblemId()).isEqualTo(1L);
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
        assertThat(result.get(0).getItemCount()).isEqualTo(2);
        assertThat(result.get(0).getCompletedCount()).isEqualTo(1);
        assertThat(result.get(0).getSkippedCount()).isEqualTo(0);
        assertThat(result.get(1).getId()).isEqualTo(99L);
        assertThat(result.get(1).getItemCount()).isEqualTo(1);
        assertThat(result.get(1).getCompletedCount()).isEqualTo(0);
        assertThat(result.get(1).getSkippedCount()).isEqualTo(1);
    }

    @Test
    void getRecentTrainingActivitiesReturnsCompletedAndSkippedItemsWithPlanContext() {
        TrainingPlan plan = trainingPlan();
        plan.setId(100L);
        plan.setTitle("3 天当前弱点专项训练");
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
        skipped.setStatusUpdatedAt(LocalDateTime.of(2026, 5, 24, 19, 0));
        when(trainingPlanItemMapper.selectList(any())).thenReturn(List.of(completed, skipped));

        List<TrainingPlanActivityVO> result = userLearningService.getRecentTrainingActivities(1L, 5);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getItemId()).isEqualTo(7L);
        assertThat(result.get(0).getPlanTitle()).isEqualTo("3 天当前弱点专项训练");
        assertThat(result.get(0).getTaskTitle()).isEqualTo("两数之和专项复盘");
        assertThat(result.get(0).getStatus()).isEqualTo("COMPLETED");
        assertThat(result.get(0).getSourceSummary()).contains("失败提交");
        assertThat(result.get(0).getStatusUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 24, 20, 0));
        assertThat(result.get(1).getTaskTitle()).isEqualTo("HashMap 底层结构");
        assertThat(result.get(1).getStatus()).isEqualTo("SKIPPED");
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
