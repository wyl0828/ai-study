package com.interview.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.coach.entity.KnowledgeCard;
import com.interview.coach.entity.MistakeCard;
import com.interview.coach.entity.MockInterviewReport;
import com.interview.coach.entity.MockInterviewSession;
import com.interview.coach.entity.MockInterviewTurn;
import com.interview.coach.entity.Problem;
import com.interview.coach.entity.Submission;
import com.interview.coach.entity.TrainingPlan;
import com.interview.coach.entity.TrainingPlanItem;
import com.interview.coach.entity.UserWeakness;
import com.interview.coach.entity.UserWeaknessEvent;
import com.interview.coach.enums.SubmissionStatusEnum;
import com.interview.coach.mapper.MistakeCardMapper;
import com.interview.coach.mapper.MockInterviewReportMapper;
import com.interview.coach.mapper.MockInterviewSessionMapper;
import com.interview.coach.mapper.MockInterviewTurnMapper;
import com.interview.coach.mapper.ProblemMapper;
import com.interview.coach.mapper.SubmissionMapper;
import com.interview.coach.mapper.TrainingPlanItemMapper;
import com.interview.coach.mapper.TrainingPlanMapper;
import com.interview.coach.mapper.UserWeaknessEventMapper;
import com.interview.coach.mapper.UserWeaknessMapper;
import com.interview.coach.mapper.KnowledgeCardMapper;
import com.interview.coach.service.UserLearningService;
import com.interview.coach.vo.DashboardStatsVO;
import com.interview.coach.vo.ErrorStatsVO;
import com.interview.coach.vo.MistakeCardVO;
import com.interview.coach.vo.MockInterviewRecentVO;
import com.interview.coach.vo.MockInterviewTrendVO;
import com.interview.coach.vo.SubmissionHistoryVO;
import com.interview.coach.vo.TrainingPlanActivityVO;
import com.interview.coach.vo.TrainingPlanHistoryVO;
import com.interview.coach.vo.TrainingPlanItemVO;
import com.interview.coach.vo.TrainingPlanVO;
import com.interview.coach.vo.UserWeaknessEventVO;
import com.interview.coach.vo.UserWeaknessVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserLearningServiceImpl implements UserLearningService {

    private final SubmissionMapper submissionMapper;

    private final UserWeaknessMapper userWeaknessMapper;

    private final UserWeaknessEventMapper userWeaknessEventMapper;

    private final MistakeCardMapper mistakeCardMapper;

    private final TrainingPlanMapper trainingPlanMapper;

    private final TrainingPlanItemMapper trainingPlanItemMapper;

    private final ProblemMapper problemMapper;

    private final MockInterviewSessionMapper mockInterviewSessionMapper;

    private final MockInterviewReportMapper mockInterviewReportMapper;

    private final MockInterviewTurnMapper mockInterviewTurnMapper;

    private final KnowledgeCardMapper knowledgeCardMapper;

    @Override
    public DashboardStatsVO getDashboardStats(Long userId) {
        DashboardStatsVO vo = new DashboardStatsVO();
        vo.setTotalSubmissions(toInt(submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, userId))));
        List<Submission> acceptedSubmissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, userId)
                .eq(Submission::getStatus, SubmissionStatusEnum.ACCEPTED.name()));
        vo.setPassedProblems((int) acceptedSubmissions.stream()
                .map(Submission::getProblemId)
                .distinct()
                .count());
        vo.setWeakPointCount(aggregateWeaknesses(loadWeaknesses(userId)).size());
        vo.setMistakeCount(toInt(mistakeCardMapper.selectCount(new LambdaQueryWrapper<MistakeCard>()
                .eq(MistakeCard::getUserId, userId))));
        return vo;
    }

    @Override
    public List<UserWeaknessVO> getWeaknesses(Long userId) {
        Map<String, UserWeaknessEvent> latestEvents = latestEventsByKnowledgePoint(userId);
        return aggregateWeaknesses(loadWeaknesses(userId)).stream()
                .map(weakness -> toUserWeaknessVO(weakness, latestEvents))
                .toList();
    }

    @Override
    public List<UserWeaknessEventVO> getRecentWeaknessEvents(Long userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        if (userWeaknessEventMapper == null) {
            return List.of();
        }
        return userWeaknessEventMapper.selectList(new LambdaQueryWrapper<UserWeaknessEvent>()
                .eq(UserWeaknessEvent::getUserId, userId)
                .orderByDesc(UserWeaknessEvent::getCreatedAt)
                .last("LIMIT " + safeLimit))
                .stream()
                .map(this::toUserWeaknessEventVO)
                .toList();
    }

    @Override
    public List<MistakeCardVO> getMistakes(Long userId) {
        List<MistakeCard> mistakes = mistakeCardMapper.selectList(new LambdaQueryWrapper<MistakeCard>()
                .eq(MistakeCard::getUserId, userId)
                .orderByDesc(MistakeCard::getCreatedAt));
        Map<Long, Problem> problems = problemsById(mistakes.stream()
                .map(MistakeCard::getProblemId)
                .toList());
        return mistakes.stream()
                .map(mistake -> toMistakeCardVO(mistake, problems))
                .toList();
    }

    @Override
    public TrainingPlanVO getLatestTrainingPlan(Long userId) {
        TrainingPlan plan = trainingPlanMapper.selectOne(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getUserId, userId)
                .orderByDesc(TrainingPlan::getCreatedAt)
                .last("LIMIT 1"));
        if (plan == null) {
            return null;
        }
        List<TrainingPlanItem> items = trainingPlanItemMapper.selectList(new LambdaQueryWrapper<TrainingPlanItem>()
                .eq(TrainingPlanItem::getPlanId, plan.getId())
                .orderByAsc(TrainingPlanItem::getDayIndex)
                .orderByAsc(TrainingPlanItem::getId));
        TrainingPlanVO vo = new TrainingPlanVO();
        vo.setId(plan.getId());
        vo.setTitle(plan.getTitle());
        vo.setSummary(plan.getSummary());
        vo.setStatus(plan.getStatus() == null ? "ACTIVE" : plan.getStatus());
        vo.setItems(items.stream().map(this::toTrainingPlanItemVO).toList());
        return vo;
    }

    @Override
    public List<TrainingPlanHistoryVO> getTrainingPlanHistory(Long userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        List<TrainingPlan> plans = trainingPlanMapper.selectList(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getUserId, userId)
                .orderByDesc(TrainingPlan::getCreatedAt)
                .last("LIMIT " + safeLimit));
        if (plans.isEmpty()) {
            return List.of();
        }

        List<Long> planIds = plans.stream()
                .map(TrainingPlan::getId)
                .toList();
        Map<Long, List<TrainingPlanItem>> itemsByPlanId = trainingPlanItemMapper.selectList(
                new LambdaQueryWrapper<TrainingPlanItem>()
                        .in(TrainingPlanItem::getPlanId, planIds))
                .stream()
                .collect(Collectors.groupingBy(TrainingPlanItem::getPlanId));

        return plans.stream()
                .map(plan -> toTrainingPlanHistoryVO(plan, itemsByPlanId.getOrDefault(plan.getId(), List.of())))
                .toList();
    }

    @Override
    public List<TrainingPlanActivityVO> getRecentTrainingActivities(Long userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        List<TrainingPlan> plans = trainingPlanMapper.selectList(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getUserId, userId)
                .orderByDesc(TrainingPlan::getCreatedAt)
                .last("LIMIT 20"));
        if (plans.isEmpty()) {
            return List.of();
        }

        Map<Long, TrainingPlan> plansById = plans.stream()
                .collect(Collectors.toMap(TrainingPlan::getId, Function.identity()));
        List<Long> planIds = plans.stream()
                .map(TrainingPlan::getId)
                .toList();
        return trainingPlanItemMapper.selectList(new LambdaQueryWrapper<TrainingPlanItem>()
                .in(TrainingPlanItem::getPlanId, planIds)
                .in(TrainingPlanItem::getStatus, List.of("COMPLETED", "SKIPPED"))
                .orderByDesc(TrainingPlanItem::getStatusUpdatedAt)
                .last("LIMIT " + safeLimit))
                .stream()
                .map(item -> toTrainingPlanActivityVO(item, plansById.get(item.getPlanId())))
                .toList();
    }

    @Override
    public List<SubmissionHistoryVO> getRecentSubmissions(Long userId) {
        List<Submission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, userId)
                .orderByDesc(Submission::getCreatedAt)
                .last("LIMIT 10"));
        Map<Long, Problem> problems = problemsById(submissions.stream()
                .map(Submission::getProblemId)
                .toList());
        return submissions.stream()
                .map(submission -> toSubmissionHistoryVO(submission, problems))
                .toList();
    }

    @Override
    public List<MockInterviewRecentVO> getRecentMockInterviews(Long userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        List<MockInterviewSession> sessions = mockInterviewSessionMapper.selectList(
                new LambdaQueryWrapper<MockInterviewSession>()
                        .eq(MockInterviewSession::getUserId, userId)
                        .orderByDesc(MockInterviewSession::getCreatedAt)
                        .last("LIMIT " + safeLimit));
        if (sessions.isEmpty()) {
            return List.of();
        }

        List<Long> sessionIds = sessions.stream()
                .map(MockInterviewSession::getId)
                .toList();
        Map<Long, MockInterviewReport> reportsBySessionId = mockInterviewReportMapper.selectList(
                new LambdaQueryWrapper<MockInterviewReport>()
                        .eq(MockInterviewReport::getUserId, userId)
                        .in(MockInterviewReport::getSessionId, sessionIds))
                .stream()
                .collect(Collectors.toMap(
                        MockInterviewReport::getSessionId,
                        Function.identity(),
                        (first, ignored) -> first));

        return sessions.stream()
                .map(session -> toMockInterviewRecentVO(session, reportsBySessionId.get(session.getId())))
                .toList();
    }

    @Override
    public List<MockInterviewTrendVO> getMockInterviewTrends(Long userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        List<MockInterviewSession> sessions = mockInterviewSessionMapper.selectList(
                new LambdaQueryWrapper<MockInterviewSession>()
                        .eq(MockInterviewSession::getUserId, userId)
                        .orderByDesc(MockInterviewSession::getCreatedAt)
                        .last("LIMIT 50"));
        if (sessions.isEmpty()) {
            return List.of();
        }

        Map<Long, MockInterviewSession> sessionsById = sessions.stream()
                .collect(Collectors.toMap(MockInterviewSession::getId, Function.identity()));
        List<Long> sessionIds = sessions.stream()
                .map(MockInterviewSession::getId)
                .toList();
        List<MockInterviewTurn> turns = mockInterviewTurnMapper.selectList(
                new LambdaQueryWrapper<MockInterviewTurn>()
                        .in(MockInterviewTurn::getSessionId, sessionIds)
                        .isNotNull(MockInterviewTurn::getScore)
                        .orderByDesc(MockInterviewTurn::getCreatedAt)
                        .orderByDesc(MockInterviewTurn::getId));
        if (turns.isEmpty()) {
            return List.of();
        }

        Map<Long, KnowledgeCard> cardsById = knowledgeCardsById(turns.stream()
                .map(MockInterviewTurn::getKnowledgeCardId)
                .filter(id -> id != null)
                .toList());
        Map<String, MockInterviewAttemptBuilder> builders = new LinkedHashMap<>();
        for (MockInterviewTurn turn : turns) {
            MockInterviewSession session = sessionsById.get(turn.getSessionId());
            if (session == null || turn.getKnowledgeCardId() == null || turn.getScore() == null) {
                continue;
            }
            String key = turn.getKnowledgeCardId() + "#" + turn.getSessionId();
            builders.computeIfAbsent(key,
                    ignored -> new MockInterviewAttemptBuilder(turn.getKnowledgeCardId(), session))
                    .add(turn);
        }

        Map<Long, List<MockInterviewAttempt>> attemptsByCard = builders.values().stream()
                .map(MockInterviewAttemptBuilder::build)
                .collect(Collectors.groupingBy(
                        MockInterviewAttempt::knowledgeCardId,
                        LinkedHashMap::new,
                        Collectors.toList()));

        return attemptsByCard.entrySet().stream()
                .map(entry -> toMockInterviewTrendVO(entry.getKey(), entry.getValue(), cardsById))
                .sorted(Comparator.comparing(MockInterviewTrendVO::getLastInterviewAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit)
                .toList();
    }

    @Override
    public ErrorStatsVO getErrorStats(Long userId) {
        List<UserWeakness> weaknesses = loadWeaknesses(userId);

        // 错误类型分布：按 errorType 分组统计 wrongCount 总和
        Map<String, Integer> errorTypeMap = weaknesses.stream()
                .collect(Collectors.groupingBy(
                        UserWeakness::getErrorType,
                        Collectors.summingInt(UserWeakness::getWrongCount)));
        List<ErrorStatsVO.ErrorTypeCount> errorTypeDistribution = errorTypeMap.entrySet().stream()
                .map(entry -> {
                    ErrorStatsVO.ErrorTypeCount count = new ErrorStatsVO.ErrorTypeCount();
                    count.setErrorType(entry.getKey());
                    count.setCount(entry.getValue());
                    return count;
                })
                .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
                .toList();

        List<ErrorStatsVO.KnowledgeWeakness> topWeakPoints = aggregateWeaknesses(weaknesses).stream()
                .limit(5)
                .map(w -> {
                    ErrorStatsVO.KnowledgeWeakness kw = new ErrorStatsVO.KnowledgeWeakness();
                    kw.setKnowledgePoint(w.getKnowledgePoint());
                    kw.setErrorType(w.getErrorType());
                    kw.setWrongCount(w.getWrongCount());
                    kw.setWeaknessScore(w.getWeaknessScore().doubleValue());
                    return kw;
                })
                .toList();

        ErrorStatsVO vo = new ErrorStatsVO();
        vo.setErrorTypeDistribution(errorTypeDistribution);
        vo.setTopWeakPoints(topWeakPoints);
        return vo;
    }

    private List<UserWeakness> loadWeaknesses(Long userId) {
        return userWeaknessMapper.selectList(new LambdaQueryWrapper<UserWeakness>()
                .eq(UserWeakness::getUserId, userId)
                .orderByDesc(UserWeakness::getWeaknessScore));
    }

    private List<UserWeakness> aggregateWeaknesses(List<UserWeakness> weaknesses) {
        Map<String, AggregatedWeakness> groups = new LinkedHashMap<>();
        for (UserWeakness weakness : weaknesses) {
            String groupName = normalizeKnowledgePoint(weakness.getKnowledgePoint());
            AggregatedWeakness group = groups.computeIfAbsent(groupName,
                    ignored -> new AggregatedWeakness(weakness, groupName));
            group.add(weakness);
        }
        return groups.values().stream()
                .map(AggregatedWeakness::toEntity)
                .sorted(Comparator.comparing(UserWeakness::getWeaknessScore).reversed())
                .toList();
    }

    private String normalizeKnowledgePoint(String value) {
        if (value == null || value.isBlank()) {
            return "未分类知识点";
        }
        String source = value.trim();
        String normalized = source.toLowerCase();
        if (containsAny(normalized, "two sum", "两数之和")) {
            return "HashMap 在两数之和中的应用";
        }
        if (containsAny(normalized, "duplicate", "重复", "冲突", "distinct indices", "self-match", "自匹配")) {
            return "HashMap 冲突处理";
        }
        if (containsAny(normalized, "traversal", "遍历")) {
            return "HashMap 遍历逻辑";
        }
        if (containsAny(normalized, "lookup", "查找", "hashmap")) {
            return "HashMap 基础查找";
        }
        return source;
    }

    private boolean containsAny(String source, String... candidates) {
        for (String candidate : candidates) {
            if (source.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, UserWeaknessEvent> latestEventsByKnowledgePoint(Long userId) {
        if (userWeaknessEventMapper == null) {
            return Collections.emptyMap();
        }
        List<UserWeaknessEvent> events = userWeaknessEventMapper.selectList(new LambdaQueryWrapper<UserWeaknessEvent>()
                .eq(UserWeaknessEvent::getUserId, userId)
                .orderByDesc(UserWeaknessEvent::getCreatedAt)
                .last("LIMIT 50"));
        if (events == null) {
            return Collections.emptyMap();
        }
        return events.stream()
                .collect(Collectors.toMap(
                        event -> normalizeKnowledgePoint(event.getKnowledgePoint()),
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new));
    }

    private UserWeaknessVO toUserWeaknessVO(UserWeakness weakness, Map<String, UserWeaknessEvent> latestEvents) {
        UserWeaknessVO vo = new UserWeaknessVO();
        vo.setId(weakness.getId());
        vo.setKnowledgePoint(weakness.getKnowledgePoint());
        vo.setErrorType(weakness.getErrorType());
        vo.setWrongCount(weakness.getWrongCount());
        vo.setWeaknessScore(weakness.getWeaknessScore());
        UserWeaknessEvent event = latestEvents.get(normalizeKnowledgePoint(weakness.getKnowledgePoint()));
        if (event != null) {
            vo.setLastDeltaScore(event.getDeltaScore());
            vo.setLastEventAt(event.getCreatedAt());
        }
        vo.setTrendLabel(trendLabel(weakness, event));
        return vo;
    }

    private String trendLabel(UserWeakness weakness, UserWeaknessEvent event) {
        if (weakness.getWrongCount() != null && weakness.getWrongCount() <= 1) {
            return "新暴露问题";
        }
        if (event != null && event.getDeltaScore() != null && event.getDeltaScore().compareTo(BigDecimal.ZERO) > 0) {
            return weakness.getWeaknessScore().compareTo(new BigDecimal("40")) >= 0 ? "持续薄弱" : "最近加重";
        }
        return "最近改善";
    }

    private UserWeaknessEventVO toUserWeaknessEventVO(UserWeaknessEvent event) {
        UserWeaknessEventVO vo = new UserWeaknessEventVO();
        vo.setId(event.getId());
        vo.setKnowledgePoint(event.getKnowledgePoint());
        vo.setErrorType(event.getErrorType());
        vo.setSourceType(event.getSourceType());
        vo.setSourceId(event.getSourceId());
        vo.setDeltaScore(event.getDeltaScore());
        vo.setBeforeScore(event.getBeforeScore());
        vo.setAfterScore(event.getAfterScore());
        vo.setReason(event.getReason());
        vo.setCreatedAt(event.getCreatedAt());
        return vo;
    }

    private MockInterviewRecentVO toMockInterviewRecentVO(MockInterviewSession session, MockInterviewReport report) {
        MockInterviewRecentVO vo = new MockInterviewRecentVO();
        vo.setSessionId(session.getId());
        vo.setCategory(session.getCategory());
        vo.setStatus(session.getStatus());
        vo.setInterviewerStyle(session.getInterviewerStyle());
        vo.setQuestionCount(session.getQuestionCount());
        vo.setAnsweredMainCount(session.getAnsweredMainCount());
        vo.setAverageScore(report == null ? null : report.getAverageScore());
        vo.setWeaknessTags(report == null ? List.of() : splitComma(report.getWeaknessTags()));
        vo.setStartedAt(session.getStartedAt());
        vo.setFinishedAt(session.getFinishedAt());
        vo.setCreatedAt(session.getCreatedAt());
        return vo;
    }

    private Map<Long, KnowledgeCard> knowledgeCardsById(List<Long> cardIds) {
        List<Long> ids = cardIds.stream().distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return knowledgeCardMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(KnowledgeCard::getId, Function.identity()));
    }

    private MockInterviewTrendVO toMockInterviewTrendVO(Long knowledgeCardId, List<MockInterviewAttempt> attempts,
            Map<Long, KnowledgeCard> cardsById) {
        List<MockInterviewAttempt> sortedAttempts = attempts.stream()
                .sorted(Comparator.comparing(MockInterviewAttempt::attemptedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        MockInterviewAttempt latest = sortedAttempts.get(0);
        MockInterviewAttempt previous = sortedAttempts.size() > 1 ? sortedAttempts.get(1) : null;
        KnowledgeCard card = cardsById.get(knowledgeCardId);
        BigDecimal delta = previous == null
                ? BigDecimal.ZERO
                : latest.averageScore().subtract(previous.averageScore());

        MockInterviewTrendVO vo = new MockInterviewTrendVO();
        vo.setKnowledgeCardId(knowledgeCardId);
        vo.setKnowledgePoint(card == null ? "知识卡 #" + knowledgeCardId : card.getTitle());
        vo.setCategory(card == null ? latest.category() : card.getCategory());
        vo.setLatestSessionId(latest.sessionId());
        vo.setLatestScore(latest.averageScore());
        vo.setPreviousScore(previous == null ? null : previous.averageScore());
        vo.setDeltaScore(delta);
        vo.setTrendLabel(trendDeltaLabel(delta, previous == null));
        vo.setInterviewCount(sortedAttempts.size());
        vo.setLatestIssue(latest.issue());
        vo.setLastInterviewAt(latest.attemptedAt());
        return vo;
    }

    private String trendDeltaLabel(BigDecimal delta, boolean firstAttempt) {
        if (firstAttempt) {
            return "首次记录";
        }
        int direction = delta.compareTo(BigDecimal.ZERO);
        if (direction > 0) {
            return "较上次提升 " + scoreText(delta) + " 分";
        }
        if (direction < 0) {
            return "较上次下降 " + scoreText(delta.abs()) + " 分";
        }
        return "与上次持平";
    }

    private String scoreText(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private MistakeCardVO toMistakeCardVO(MistakeCard mistake, Map<Long, Problem> problems) {
        MistakeCardVO vo = new MistakeCardVO();
        vo.setId(mistake.getId());
        vo.setProblemId(mistake.getProblemId());
        vo.setProblemTitle(problemTitle(mistake.getProblemId(), problems));
        vo.setErrorType(mistake.getErrorType());
        vo.setKnowledgePoint(mistake.getKnowledgePoint());
        vo.setMistakeSummary(mistake.getMistakeSummary());
        vo.setCorrectIdea(mistake.getCorrectIdea());
        vo.setRepeatCount(mistake.getRepeatCount() == null ? 1 : mistake.getRepeatCount());
        vo.setLastSeenAt(mistake.getLastSeenAt() == null ? mistake.getCreatedAt() : mistake.getLastSeenAt());
        vo.setStatus(mistake.getStatus() == null ? "OPEN" : mistake.getStatus());
        return vo;
    }

    private TrainingPlanItemVO toTrainingPlanItemVO(TrainingPlanItem item) {
        TrainingPlanItemVO vo = new TrainingPlanItemVO();
        vo.setId(item.getId());
        vo.setItemType(item.getItemType() == null ? "PROBLEM" : item.getItemType());
        vo.setProblemId(trainingProblemId(item));
        vo.setKnowledgeCardId(item.getKnowledgeCardId());
        vo.setDayIndex(item.getDayIndex());
        vo.setKnowledgePoint(item.getKnowledgePoint());
        vo.setProblemTitle(item.getProblemTitle());
        vo.setKnowledgeCardTitle(item.getKnowledgeCardTitle());
        vo.setReason(item.getReason());
        vo.setReviewFocus(item.getReviewFocus());
        vo.setSourceType(item.getSourceType());
        vo.setSourceId(item.getSourceId());
        vo.setSourceSummary(item.getSourceSummary());
        vo.setStatus(item.getStatus());
        vo.setStatusUpdatedAt(item.getStatusUpdatedAt());
        return vo;
    }

    private TrainingPlanHistoryVO toTrainingPlanHistoryVO(TrainingPlan plan, List<TrainingPlanItem> items) {
        TrainingPlanHistoryVO vo = new TrainingPlanHistoryVO();
        vo.setId(plan.getId());
        vo.setTitle(plan.getTitle());
        vo.setSummary(plan.getSummary());
        vo.setStatus(plan.getStatus() == null ? "ACTIVE" : plan.getStatus());
        vo.setStartDate(plan.getStartDate());
        vo.setEndDate(plan.getEndDate());
        vo.setItemCount(items.size());
        vo.setCompletedCount((int) items.stream()
                .filter(item -> "COMPLETED".equalsIgnoreCase(item.getStatus()))
                .count());
        vo.setSkippedCount((int) items.stream()
                .filter(item -> "SKIPPED".equalsIgnoreCase(item.getStatus()))
                .count());
        vo.setCreatedAt(plan.getCreatedAt());
        return vo;
    }

    private TrainingPlanActivityVO toTrainingPlanActivityVO(TrainingPlanItem item, TrainingPlan plan) {
        TrainingPlanActivityVO vo = new TrainingPlanActivityVO();
        vo.setItemId(item.getId());
        vo.setPlanId(item.getPlanId());
        vo.setPlanTitle(plan == null ? null : plan.getTitle());
        vo.setItemType(item.getItemType() == null ? "PROBLEM" : item.getItemType());
        vo.setTaskTitle(activityTaskTitle(item));
        vo.setKnowledgePoint(item.getKnowledgePoint());
        vo.setSourceType(item.getSourceType());
        vo.setSourceSummary(item.getSourceSummary());
        vo.setStatus(item.getStatus());
        vo.setStatusUpdatedAt(item.getStatusUpdatedAt());
        return vo;
    }

    private String activityTaskTitle(TrainingPlanItem item) {
        String itemType = item.getItemType() == null ? "PROBLEM" : item.getItemType();
        String title = "KNOWLEDGE_CARD".equalsIgnoreCase(itemType)
                ? item.getKnowledgeCardTitle()
                : item.getProblemTitle();
        if (title != null && !title.isBlank()) {
            return title;
        }
        if (item.getKnowledgePoint() != null && !item.getKnowledgePoint().isBlank()) {
            return item.getKnowledgePoint();
        }
        return "训练项";
    }

    private Long trainingProblemId(TrainingPlanItem item) {
        String itemType = item.getItemType() == null ? "PROBLEM" : item.getItemType();
        if (!"PROBLEM".equalsIgnoreCase(itemType)) {
            return null;
        }

        String text = String.join(" ",
                safeText(item.getProblemTitle()),
                safeText(item.getKnowledgePoint()),
                safeText(item.getReason()),
                safeText(item.getReviewFocus()))
                .toLowerCase();
        if (containsAny(text, "two sum", "两数之和", "hashmap", "哈希", "hash map")) {
            return 1L;
        }
        if (containsAny(text, "reverse", "linked", "链表", "反转链表")) {
            return 206L;
        }
        if (containsAny(text, "stock", "股票", "买卖股票", "贪心")) {
            return 121L;
        }
        return 1L;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private List<String> splitComma(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .toList();
    }

    private SubmissionHistoryVO toSubmissionHistoryVO(Submission submission, Map<Long, Problem> problems) {
        SubmissionHistoryVO vo = new SubmissionHistoryVO();
        vo.setProblemId(submission.getProblemId());
        vo.setProblemTitle(problemTitle(submission.getProblemId(), problems));
        vo.setStatus(submission.getStatus());
        vo.setPassedCount(submission.getPassedCount());
        vo.setTotalCount(submission.getTotalCount());
        vo.setCreatedAt(submission.getCreatedAt());
        return vo;
    }

    private Map<Long, Problem> problemsById(List<Long> problemIds) {
        List<Long> ids = problemIds.stream().distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return problemMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Problem::getId, Function.identity()));
    }

    private String problemTitle(Long problemId, Map<Long, Problem> problems) {
        Problem problem = problems.get(problemId);
        return problem == null ? "Unknown Problem" : problem.getTitle();
    }

    private Integer toInt(Long value) {
        return Math.toIntExact(value == null ? 0L : value);
    }

    private record MockInterviewAttempt(
            Long knowledgeCardId,
            Long sessionId,
            String category,
            BigDecimal averageScore,
            String issue,
            LocalDateTime attemptedAt) {
    }

    private static class MockInterviewAttemptBuilder {

        private final Long knowledgeCardId;

        private final MockInterviewSession session;

        private int totalScore;

        private int scoreCount;

        private String issue;

        private LocalDateTime latestTurnAt;

        MockInterviewAttemptBuilder(Long knowledgeCardId, MockInterviewSession session) {
            this.knowledgeCardId = knowledgeCardId;
            this.session = session;
        }

        void add(MockInterviewTurn turn) {
            totalScore += turn.getScore();
            scoreCount++;
            LocalDateTime turnTime = turn.getCreatedAt();
            if (turnTime != null && (latestTurnAt == null || turnTime.isAfter(latestTurnAt))) {
                latestTurnAt = turnTime;
            }
            if (issue == null || issue.isBlank()) {
                issue = firstNonBlank(
                        turn.getMissingKeyPoints(),
                        turn.getGapSummary(),
                        turn.getExpressionIssue(),
                        turn.getFeedback());
            }
        }

        MockInterviewAttempt build() {
            BigDecimal averageScore = scoreCount == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(totalScore).divide(BigDecimal.valueOf(scoreCount), 1, RoundingMode.HALF_UP);
            return new MockInterviewAttempt(
                    knowledgeCardId,
                    session.getId(),
                    session.getCategory(),
                    averageScore,
                    issue,
                    attemptedAt());
        }

        private LocalDateTime attemptedAt() {
            if (session.getFinishedAt() != null) {
                return session.getFinishedAt();
            }
            if (latestTurnAt != null) {
                return latestTurnAt;
            }
            if (session.getUpdatedAt() != null) {
                return session.getUpdatedAt();
            }
            return session.getCreatedAt();
        }

        private String firstNonBlank(String... values) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return value.trim();
                }
            }
            return null;
        }
    }

    private static class AggregatedWeakness {

        private final Long id;

        private final Long userId;

        private final String knowledgePoint;

        private String errorType;

        private Integer wrongCount = 0;

        private Integer submitCount = 0;

        private BigDecimal weaknessScore = BigDecimal.ZERO;

        private BigDecimal maxSingleScore = BigDecimal.ZERO;

        AggregatedWeakness(UserWeakness first, String knowledgePoint) {
            this.id = first.getId();
            this.userId = first.getUserId();
            this.knowledgePoint = knowledgePoint;
        }

        void add(UserWeakness weakness) {
            BigDecimal score = weakness.getWeaknessScore() == null ? BigDecimal.ZERO : weakness.getWeaknessScore();
            wrongCount += weakness.getWrongCount() == null ? 0 : weakness.getWrongCount();
            submitCount += weakness.getSubmitCount() == null ? 0 : weakness.getSubmitCount();
            weaknessScore = weaknessScore.add(score);
            if (errorType == null || score.compareTo(maxSingleScore) > 0) {
                errorType = weakness.getErrorType();
                maxSingleScore = score;
            }
        }

        UserWeakness toEntity() {
            UserWeakness weakness = new UserWeakness();
            weakness.setId(id);
            weakness.setUserId(userId);
            weakness.setKnowledgePoint(knowledgePoint);
            weakness.setErrorType(errorType);
            weakness.setWrongCount(wrongCount);
            weakness.setSubmitCount(submitCount);
            weakness.setWeaknessScore(weaknessScore);
            return weakness;
        }
    }
}
