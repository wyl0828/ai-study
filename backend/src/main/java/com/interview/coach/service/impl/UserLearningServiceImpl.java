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
import com.interview.coach.vo.MockInterviewTraceVO;
import com.interview.coach.vo.MockInterviewTrendVO;
import com.interview.coach.vo.SubmissionHistoryVO;
import com.interview.coach.vo.TrainingPlanActivityVO;
import com.interview.coach.vo.TrainingPlanHistoryVO;
import com.interview.coach.vo.TrainingPlanItemVO;
import com.interview.coach.vo.TrainingPlanTraceVO;
import com.interview.coach.vo.TrainingPlanVO;
import com.interview.coach.vo.UserWeaknessEventVO;
import com.interview.coach.vo.UserWeaknessVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
        vo.setStatusLabel(trainingPlanStatusLabel(vo.getStatus()));
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
    public TrainingPlanTraceVO getTrainingPlanTrace(Long userId) {
        TrainingPlan plan = trainingPlanMapper.selectOne(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getUserId, userId)
                .orderByDesc(TrainingPlan::getCreatedAt)
                .last("LIMIT 1"));
        if (plan == null) {
            TrainingPlanTraceVO empty = new TrainingPlanTraceVO();
            empty.setNextAction("暂无训练计划，先完成一次代码提交诊断或手动生成训练计划。");
            empty.setNextActionReason("训练闭环还没有起点，先通过一次提交诊断生成薄弱点、错题卡和训练计划。");
            empty.setNextActionPriority("HIGH");
            empty.setNextTargetHref("/problem/1");
            empty.setNextTargetLabel("去做题");
            empty.setProgressSummary("暂无训练计划，完成一次提交诊断后会生成可追踪训练项。");
            empty.setSourceTypeSummary("暂无推荐来源");
            return empty;
        }

        List<TrainingPlanItem> items = trainingPlanItemMapper.selectList(new LambdaQueryWrapper<TrainingPlanItem>()
                .eq(TrainingPlanItem::getPlanId, plan.getId())
                .orderByAsc(TrainingPlanItem::getDayIndex)
                .orderByAsc(TrainingPlanItem::getId));
        TrainingPlanTraceVO trace = new TrainingPlanTraceVO();
        trace.setPlanId(plan.getId());
        trace.setTitle(plan.getTitle());
        trace.setSummary(plan.getSummary());
        trace.setStatus(plan.getStatus() == null ? "ACTIVE" : plan.getStatus());
        trace.setStatusLabel(trainingPlanStatusLabel(trace.getStatus()));
        trace.setStartDate(plan.getStartDate());
        trace.setEndDate(plan.getEndDate());
        trace.setPlanCreatedAt(plan.getCreatedAt());
        fillTrainingPlanTimeline(trace);
        trace.setItemCount(items.size());
        trace.setPendingCount(countByStatus(items, "PENDING"));
        trace.setCompletedCount(countByStatus(items, "COMPLETED"));
        trace.setSkippedCount(countByStatus(items, "SKIPPED"));
        trace.setHandledCount(safeInt(trace.getCompletedCount()) + safeInt(trace.getSkippedCount()));
        trace.setCompletionRate(completionRate(trace.getCompletedCount(), trace.getItemCount()));
        trace.setHandledRate(completionRate(trace.getHandledCount(), trace.getItemCount()));
        trace.setSourceTypeCounts(sourceTypeCounts(items));
        trace.setSourceTypeSummary(sourceTypeSummary(trace.getSourceTypeCounts()));
        TrainingPlanItemVO nextItem = items.stream()
                .filter(item -> item.getStatus() == null || "PENDING".equalsIgnoreCase(item.getStatus()))
                .findFirst()
                .map(this::toTrainingPlanItemVO)
                .orElse(null);
        trace.setNextItem(nextItem);
        trace.setNextAction(trainingPlanNextAction(trace, nextItem));
        trace.setNextActionReason(trainingPlanNextActionReason(trace, nextItem));
        trace.setNextActionPriority(trainingPlanNextActionPriority(trace, nextItem));
        trace.setNextTargetHref(trainingPlanNextTargetHref(nextItem));
        trace.setNextTargetLabel(trainingPlanNextTargetLabel(nextItem));
        trace.setProgressSummary(trainingPlanProgressSummary(trace));
        List<TrainingPlanActivityVO> recentActivities = items.stream()
                .filter(item -> "COMPLETED".equalsIgnoreCase(item.getStatus())
                        || "SKIPPED".equalsIgnoreCase(item.getStatus()))
                .sorted(Comparator.comparing(
                        TrainingPlanItem::getStatusUpdatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(item -> toTrainingPlanActivityVO(item, plan))
                .toList();
        trace.setRecentActivities(recentActivities);
        trace.setLatestActivitySummary(latestTrainingActivitySummary(recentActivities));
        trace.setLatestActivityAt(recentActivities.stream()
                .map(TrainingPlanActivityVO::getStatusUpdatedAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null));
        return trace;
    }

    private void fillTrainingPlanTimeline(TrainingPlanTraceVO trace) {
        LocalDate today = LocalDate.now();
        if (trace.getPlanCreatedAt() != null) {
            long days = ChronoUnit.DAYS.between(trace.getPlanCreatedAt().toLocalDate(), today);
            trace.setDaysSinceCreated((int) Math.max(0, days));
        }
        if (trace.getEndDate() != null) {
            trace.setDaysRemaining((int) ChronoUnit.DAYS.between(today, trace.getEndDate()));
        }
    }

    private String trainingPlanStatusLabel(String status) {
        if ("ACTIVE".equalsIgnoreCase(status)) {
            return "进行中";
        }
        if ("COMPLETED".equalsIgnoreCase(status)) {
            return "已完成";
        }
        if ("REGENERATED".equalsIgnoreCase(status)) {
            return "已重新生成";
        }
        return status == null || status.isBlank() ? "未知状态" : status;
    }

    private String trainingPlanItemStatusLabel(String status) {
        if ("COMPLETED".equalsIgnoreCase(status)) {
            return "已完成";
        }
        if ("SKIPPED".equalsIgnoreCase(status)) {
            return "已跳过";
        }
        if ("PENDING".equalsIgnoreCase(status)) {
            return "待完成";
        }
        return status == null || status.isBlank() ? "未知状态" : status;
    }

    private String trainingPlanProgressSummary(TrainingPlanTraceVO trace) {
        if (trace == null || trace.getItemCount() == null || trace.getItemCount() == 0) {
            return "当前训练计划暂无条目。";
        }
        String timeline = trainingPlanTimelineSummary(trace);
        return "已完成 " + trace.getCompletedCount() + "/" + trace.getItemCount()
                + "，已处理 " + trace.getHandledCount() + "/" + trace.getItemCount()
                + timeline
                + "，推荐来源：" + trace.getSourceTypeSummary() + "。";
    }

    private String trainingPlanTimelineSummary(TrainingPlanTraceVO trace) {
        Integer remaining = trace.getDaysRemaining();
        boolean overdue = remaining != null && remaining < 0
                && trace.getPendingCount() != null && trace.getPendingCount() > 0;
        trace.setOverdue(overdue);
        if (remaining == null) {
            return "";
        }
        if (overdue) {
            return "，已逾期 " + Math.abs(remaining) + " 天";
        }
        if (remaining < 0) {
            return "，计划已结束";
        }
        return "，剩余 " + remaining + " 天";
    }

    private String trainingPlanNextAction(TrainingPlanTraceVO trace, TrainingPlanItemVO nextItem) {
        if (nextItem != null) {
            String target = nextItem.getKnowledgeCardTitle() != null && !nextItem.getKnowledgeCardTitle().isBlank()
                    ? nextItem.getKnowledgeCardTitle()
                    : nextItem.getProblemTitle();
            if (target == null || target.isBlank()) {
                target = nextItem.getKnowledgePoint();
            }
            if ("KNOWLEDGE_CARD".equalsIgnoreCase(nextItem.getItemType())) {
                return "下一步复习知识卡：" + target;
            }
            return "下一步完成算法复盘：" + target;
        }
        if (trace.getItemCount() != null && trace.getItemCount() > 0 && trace.getPendingCount() == 0) {
            return "当前计划暂无待完成项，建议复盘最近错题或重新生成下一轮训练计划。";
        }
        return "暂无待练任务，建议先完成一次代码提交诊断或手动生成训练计划。";
    }

    private String trainingPlanNextActionReason(TrainingPlanTraceVO trace, TrainingPlanItemVO nextItem) {
        if (nextItem != null) {
            String source = sourceTypeLabel(nextItem.getSourceType());
            String focus = nextItem.getReviewFocus();
            if (focus == null || focus.isBlank()) {
                focus = nextItem.getReason();
            }
            if (focus == null || focus.isBlank()) {
                focus = nextItem.getKnowledgePoint();
            }
            return "优先处理该项，因为它来自" + source + "；本轮重点是" + safeText(focus, "把当前薄弱点复盘到可复述") + "。";
        }
        if (trace.getItemCount() != null && trace.getItemCount() > 0 && trace.getPendingCount() == 0) {
            return "当前计划已经没有待完成项，需要复盘最近错题或重新生成下一轮计划，避免训练链路停在已完成状态。";
        }
        return "当前没有待练任务，需要先通过提交诊断或手动生成计划建立下一步训练入口。";
    }

    private String trainingPlanNextActionPriority(TrainingPlanTraceVO trace, TrainingPlanItemVO nextItem) {
        if (nextItem != null) {
            if (nextItem.getStatus() == null || nextItem.getStatus().isBlank()
                    || "PENDING".equalsIgnoreCase(nextItem.getStatus())) {
                return "HIGH";
            }
            return "MEDIUM";
        }
        if (trace.getItemCount() != null && trace.getItemCount() > 0 && trace.getPendingCount() == 0) {
            return "MEDIUM";
        }
        return "HIGH";
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String trainingPlanNextTargetHref(TrainingPlanItemVO nextItem) {
        if (nextItem == null) {
            return "/dashboard";
        }
        if ("KNOWLEDGE_CARD".equalsIgnoreCase(nextItem.getItemType())) {
            return nextItem.getKnowledgeCardId() == null
                    ? "/knowledge"
                    : "/knowledge?cardId=" + nextItem.getKnowledgeCardId();
        }
        Long problemId = nextItem.getProblemId() == null ? 1L : nextItem.getProblemId();
        return "/problem/" + problemId;
    }

    private String trainingPlanNextTargetLabel(TrainingPlanItemVO nextItem) {
        if (nextItem == null) {
            return "查看计划";
        }
        return "KNOWLEDGE_CARD".equalsIgnoreCase(nextItem.getItemType()) ? "去复习" : "去做题";
    }

    private String latestTrainingActivitySummary(List<TrainingPlanActivityVO> recentActivities) {
        if (recentActivities == null || recentActivities.isEmpty()) {
            return "暂无完成或跳过记录。";
        }
        TrainingPlanActivityVO latest = recentActivities.get(0);
        String action = "SKIPPED".equalsIgnoreCase(latest.getStatus()) ? "跳过" : "完成";
        return "最近" + action + "：" + latest.getTaskTitle();
    }

    private int countByStatus(List<TrainingPlanItem> items, String status) {
        return (int) items.stream()
                .filter(item -> status.equalsIgnoreCase(normalizedTrainingPlanItemStatus(item)))
                .count();
    }

    private String normalizedTrainingPlanItemStatus(TrainingPlanItem item) {
        if (item == null || item.getStatus() == null || item.getStatus().isBlank()) {
            return "PENDING";
        }
        return item.getStatus();
    }

    private BigDecimal completionRate(Integer completedCount, Integer itemCount) {
        if (itemCount == null || itemCount == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(completedCount == null ? 0 : completedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(itemCount), 1, RoundingMode.HALF_UP);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private Map<String, Integer> sourceTypeCounts(List<TrainingPlanItem> items) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (TrainingPlanItem item : items) {
            String sourceType = item.getSourceType() == null || item.getSourceType().isBlank()
                    ? "UNKNOWN"
                    : item.getSourceType();
            counts.merge(sourceType, 1, Integer::sum);
        }
        return counts;
    }

    private String sourceTypeSummary(Map<String, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            return "暂无推荐来源";
        }
        return counts.entrySet().stream()
                .map(entry -> sourceTypeLabel(entry.getKey()) + " " + entry.getValue())
                .collect(Collectors.joining("、"));
    }

    private String sourceTypeLabel(String sourceType) {
        if ("SUBMISSION_FAILED".equals(sourceType)) {
            return "失败提交";
        }
        if ("RAG_KNOWLEDGE_CARD".equals(sourceType)) {
            return "RAG 知识卡";
        }
        if ("USER_WEAKNESS".equals(sourceType)) {
            return "薄弱点";
        }
        if ("KNOWLEDGE_CARD_REVIEW".equals(sourceType)) {
            return "知识卡复习";
        }
        if ("MOCK_INTERVIEW_REPORT".equals(sourceType)) {
            return "模拟面试报告";
        }
        if ("SELF_TEST".equals(sourceType)) {
            return "知识自测";
        }
        if ("GENERAL_REVIEW".equals(sourceType)) {
            return "通用复盘";
        }
        if ("LEGACY_TRAINING_PLAN".equals(sourceType)) {
            return "学习记录";
        }
        return sourceType == null || sourceType.isBlank() ? "学习记录" : sourceType;
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
    public MockInterviewTraceVO getMockInterviewTrace(Long userId) {
        List<MockInterviewSession> sessions = mockInterviewSessionMapper.selectList(
                new LambdaQueryWrapper<MockInterviewSession>()
                        .eq(MockInterviewSession::getUserId, userId)
                        .orderByDesc(MockInterviewSession::getCreatedAt)
                        .last("LIMIT 20"));
        MockInterviewTraceVO trace = new MockInterviewTraceVO();
        if (sessions.isEmpty()) {
            trace.setNextAction("暂无模拟面试记录，建议先开始一场后端知识模拟面试。");
            trace.setNextActionReason(resolveMockInterviewNextActionReason(trace));
            trace.setNextActionPriority(resolveMockInterviewNextActionPriority(trace));
            fillMockInterviewClosureStatus(trace);
            fillMockInterviewTraceNextTarget(trace);
            trace.setClosureSummary(mockInterviewClosureSummary(trace));
            trace.setReviewPathSummary(mockInterviewReviewPathSummary(trace));
            return trace;
        }

        MockInterviewSession latestSession = sessions.get(0);
        List<Long> sessionIds = sessions.stream()
                .map(MockInterviewSession::getId)
                .toList();
        List<MockInterviewReport> reports = mockInterviewReportMapper.selectList(
                new LambdaQueryWrapper<MockInterviewReport>()
                        .eq(MockInterviewReport::getUserId, userId)
                        .in(MockInterviewReport::getSessionId, sessionIds)
                        .orderByDesc(MockInterviewReport::getCreatedAt));
        MockInterviewReport latestReport = reports.isEmpty() ? null : reports.get(0);
        List<MockInterviewTurn> turns = mockInterviewTurnMapper.selectList(
                new LambdaQueryWrapper<MockInterviewTurn>()
                        .in(MockInterviewTurn::getSessionId, sessionIds)
                        .isNotNull(MockInterviewTurn::getScore));
        Long traceTurnSessionId = latestReport != null && latestReport.getSessionId() != null
                ? latestReport.getSessionId()
                : latestSession.getId();
        List<MockInterviewTurn> traceTurns = turns.stream()
                .filter(turn -> java.util.Objects.equals(turn.getSessionId(), traceTurnSessionId))
                .toList();

        trace.setSessionCount(sessions.size());
        trace.setReportedSessionCount((int) sessions.stream()
                .filter(session -> "REPORTED".equalsIgnoreCase(session.getStatus()))
                .count());
        trace.setLatestSessionId(latestSession.getId());
        trace.setLatestSessionStatus(latestSession.getStatus());
        trace.setLatestSessionStatusLabel(mockInterviewSessionStatusLabel(latestSession.getStatus()));
        trace.setLatestCategory(latestSession.getCategory());
        trace.setLatestInterviewAt(latestSession.getFinishedAt() == null
                ? latestSession.getUpdatedAt()
                : latestSession.getFinishedAt());
        trace.setAnsweredTurnCount(traceTurns.size());
        trace.setLowScoreTurnCount((int) traceTurns.stream()
                .filter(turn -> turn.getScore() != null && turn.getScore() < 60)
                .count());
        trace.setWeaknessEventCount(mockInterviewWeaknessEventCount(userId));
        if (latestReport != null) {
            trace.setLatestReportId(latestReport.getId());
            trace.setLatestAverageScore(latestReport.getAverageScore());
            trace.setLatestWeaknessTags(splitComma(latestReport.getWeaknessTags()));
            trace.setRecommendedCardIds(splitLongs(latestReport.getRecommendedCardIds()));
            trace.setTrainingPlanItemCount(mockInterviewTrainingPlanItemCount(userId, latestReport.getId()));
            trace.setReportTrainingPlanLinked(trace.getTrainingPlanItemCount() != null
                    && trace.getTrainingPlanItemCount() > 0);
            if (latestReport.getSessionId() != null) {
                trace.setReportReviewHref("/mock-interview?sessionId=" + latestReport.getSessionId());
                trace.setReportReviewLabel("查看报告");
            }
        }
        trace.setNextAction(resolveMockInterviewNextAction(trace));
        trace.setNextActionReason(resolveMockInterviewNextActionReason(trace));
        trace.setNextActionPriority(resolveMockInterviewNextActionPriority(trace));
        fillMockInterviewClosureStatus(trace);
        fillMockInterviewTraceNextTarget(trace);
        trace.setClosureSummary(mockInterviewClosureSummary(trace));
        trace.setReviewPathSummary(mockInterviewReviewPathSummary(trace));
        return trace;
    }

    private void fillMockInterviewClosureStatus(MockInterviewTraceVO trace) {
        String status = resolveMockInterviewClosureStatus(trace);
        trace.setClosureStatus(status);
        trace.setClosureStatusLabel(mockInterviewClosureStatusLabel(status));
    }

    private String resolveMockInterviewClosureStatus(MockInterviewTraceVO trace) {
        if (trace.getLatestSessionId() == null) {
            return "NO_SESSION";
        }
        if (!"REPORTED".equalsIgnoreCase(trace.getLatestSessionStatus())) {
            return "IN_PROGRESS";
        }
        if (trace.getRecommendedCardIds() != null && !trace.getRecommendedCardIds().isEmpty()) {
            return "REVIEW_CARD_REQUIRED";
        }
        if (trace.getLowScoreTurnCount() != null && trace.getLowScoreTurnCount() > 0) {
            return "REPORT_REVIEW_REQUIRED";
        }
        return "RETEST_READY";
    }

    private String mockInterviewClosureStatusLabel(String status) {
        if ("NO_SESSION".equals(status)) {
            return "尚未开始";
        }
        if ("IN_PROGRESS".equals(status)) {
            return "继续面试";
        }
        if ("REVIEW_CARD_REQUIRED".equals(status)) {
            return "复盘推荐卡";
        }
        if ("REPORT_REVIEW_REQUIRED".equals(status)) {
            return "复盘报告";
        }
        if ("RETEST_READY".equals(status)) {
            return "同类复测";
        }
        return "待跟进";
    }

    private String mockInterviewSessionStatusLabel(String status) {
        if ("CREATED".equalsIgnoreCase(status)) {
            return "已创建";
        }
        if ("ASKING_MAIN".equalsIgnoreCase(status)) {
            return "主问题进行中";
        }
        if ("MAIN_ANSWERED".equalsIgnoreCase(status)) {
            return "主问题已答";
        }
        if ("ASKING_FOLLOW_UP".equalsIgnoreCase(status)) {
            return "追问进行中";
        }
        if ("FOLLOW_UP_ANSWERED".equalsIgnoreCase(status)) {
            return "追问已答";
        }
        if ("FINISHED".equalsIgnoreCase(status)) {
            return "待生成报告";
        }
        if ("REPORTED".equalsIgnoreCase(status)) {
            return "已生成报告";
        }
        return status == null || status.isBlank() ? "未知状态" : status;
    }

    private void fillMockInterviewTraceNextTarget(MockInterviewTraceVO trace) {
        if (trace.getLatestSessionId() == null) {
            trace.setNextTargetHref("/mock-interview");
            trace.setNextTargetLabel("开始面试");
            return;
        }
        if (!"REPORTED".equalsIgnoreCase(trace.getLatestSessionStatus())) {
            trace.setNextTargetHref("/mock-interview?sessionId=" + trace.getLatestSessionId());
            trace.setNextTargetLabel("继续面试");
            return;
        }
        if (!trace.getRecommendedCardIds().isEmpty()) {
            trace.setNextTargetHref("/knowledge?cardId=" + trace.getRecommendedCardIds().get(0));
            trace.setNextTargetLabel("去复盘");
            return;
        }
        if (trace.getLowScoreTurnCount() == null || trace.getLowScoreTurnCount() == 0) {
            String category = trace.getLatestCategory() == null || trace.getLatestCategory().isBlank()
                    ? "SPRING"
                    : trace.getLatestCategory();
            trace.setNextTargetHref("/mock-interview?category=" + category);
            trace.setNextTargetLabel("同类复测");
            return;
        }
        trace.setNextTargetHref("/mock-interview?sessionId=" + trace.getLatestSessionId());
        trace.setNextTargetLabel("查看报告");
    }

    private String mockInterviewClosureSummary(MockInterviewTraceVO trace) {
        if (trace.getSessionCount() == null || trace.getSessionCount() == 0) {
            return "暂无模拟面试记录，完成一场面试后会生成报告、弱点事件和训练计划推荐。";
        }
        if (!"REPORTED".equalsIgnoreCase(trace.getLatestSessionStatus())) {
            return "最近会话仍在进行中，先完成主问题和追问再生成报告。";
        }
        if (!hasMockInterviewPendingReviewWork(trace)) {
            return "已报告 " + trace.getReportedSessionCount() + "/" + trace.getSessionCount()
                    + "，低分回答 0，弱点事件 " + trace.getWeaknessEventCount()
                    + "，本次无待复盘推荐卡，无需生成复盘训练计划。";
        }
        String linked = Boolean.TRUE.equals(trace.getReportTrainingPlanLinked())
                ? "报告已接入训练计划"
                : "报告暂未接入训练计划";
        return "已报告 " + trace.getReportedSessionCount() + "/" + trace.getSessionCount()
                + "，低分回答 " + trace.getLowScoreTurnCount()
                + "，弱点事件 " + trace.getWeaknessEventCount()
                + "，" + linked + "。";
    }

    private String mockInterviewReviewPathSummary(MockInterviewTraceVO trace) {
        if (trace.getSessionCount() == null || trace.getSessionCount() == 0) {
            return "暂无模拟面试记录；路径从开始面试进入，完成后生成报告、推荐知识卡和训练计划项。";
        }
        if (!"REPORTED".equalsIgnoreCase(trace.getLatestSessionStatus())) {
            return "会话 #" + trace.getLatestSessionId() + " 尚未生成报告；先继续面试，报告生成后再进入知识卡复盘和训练计划。";
        }
        int cardCount = trace.getRecommendedCardIds() == null ? 0 : trace.getRecommendedCardIds().size();
        int planItemCount = trace.getTrainingPlanItemCount() == null ? 0 : trace.getTrainingPlanItemCount();
        String target = trace.getNextTargetHref() == null ? "/mock-interview?sessionId=" + trace.getLatestSessionId()
                : trace.getNextTargetHref();
        if (!hasMockInterviewPendingReviewWork(trace)) {
            return "报告 #" + trace.getLatestReportId()
                    + " -> 无待复盘推荐卡"
                    + " -> 下一步 " + target + "。";
        }
        String planStatus = planItemCount > 0
                ? "训练计划 " + planItemCount + " 项"
                : "训练计划 0 项，待检查训练计划生成";
        return "报告 #" + trace.getLatestReportId()
                + " -> 推荐知识卡 " + cardCount + " 张"
                + " -> " + planStatus
                + " -> 下一步 " + target + "。";
    }

    private boolean hasMockInterviewPendingReviewWork(MockInterviewTraceVO trace) {
        boolean hasRecommendedCards = trace.getRecommendedCardIds() != null
                && !trace.getRecommendedCardIds().isEmpty();
        boolean hasLowScoreTurns = trace.getLowScoreTurnCount() != null
                && trace.getLowScoreTurnCount() > 0;
        return hasRecommendedCards || hasLowScoreTurns;
    }

    private String resolveMockInterviewNextAction(MockInterviewTraceVO trace) {
        if (trace.getLatestSessionId() == null) {
            return "暂无模拟面试记录，建议先开始一场后端知识模拟面试。";
        }
        if (!"REPORTED".equalsIgnoreCase(trace.getLatestSessionStatus())) {
            return "继续完成最近模拟面试，会话 #" + trace.getLatestSessionId() + "。";
        }
        if (!trace.getRecommendedCardIds().isEmpty()) {
            return "复盘最近面试推荐知识卡 #" + trace.getRecommendedCardIds().get(0) + "，再回到模拟面试做同类追问。";
        }
        if (trace.getLowScoreTurnCount() != null && trace.getLowScoreTurnCount() > 0) {
            return "复盘最近模拟面试报告，优先补齐低分回答里的薄弱标签。";
        }
        return "最近模拟面试已完成，建议选择同类知识点再做一次复测。";
    }

    private String resolveMockInterviewNextActionReason(MockInterviewTraceVO trace) {
        if (trace.getLatestSessionId() == null) {
            return "模拟面试闭环还没有起点，先开始一场后端知识模拟面试，生成报告后才能沉淀弱点和训练计划。";
        }
        if (!"REPORTED".equalsIgnoreCase(trace.getLatestSessionStatus())) {
            return "最近会话尚未生成报告，先继续完成主问题和追问，才能进入知识卡复盘和训练计划。";
        }
        if (trace.getRecommendedCardIds() != null && !trace.getRecommendedCardIds().isEmpty()) {
            if (Boolean.TRUE.equals(trace.getReportTrainingPlanLinked())) {
                return "最近报告已推荐知识卡并接入训练计划，先复盘推荐卡，再回到 Dashboard 跟进训练项。";
            }
            return "最近报告已有推荐知识卡，但训练计划尚未沉淀对应任务，先复盘推荐卡并检查计划生成。";
        }
        if (trace.getLowScoreTurnCount() != null && trace.getLowScoreTurnCount() > 0) {
            return "最近报告存在低分回答，先回看报告里的薄弱标签和缺失要点。";
        }
        return "最近模拟面试已完成，做一次同类知识点复测，确认表达是否更稳定。";
    }

    private String resolveMockInterviewNextActionPriority(MockInterviewTraceVO trace) {
        if (trace.getLatestSessionId() == null) {
            return "HIGH";
        }
        if (!"REPORTED".equalsIgnoreCase(trace.getLatestSessionStatus())) {
            return "HIGH";
        }
        if (trace.getRecommendedCardIds() != null && !trace.getRecommendedCardIds().isEmpty()) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    private int mockInterviewWeaknessEventCount(Long userId) {
        if (userWeaknessEventMapper == null) {
            return 0;
        }
        Long count = userWeaknessEventMapper.selectCount(new LambdaQueryWrapper<UserWeaknessEvent>()
                .eq(UserWeaknessEvent::getUserId, userId)
                .eq(UserWeaknessEvent::getSourceType, "MOCK_INTERVIEW"));
        return toInt(count);
    }

    private int mockInterviewTrainingPlanItemCount(Long userId, Long reportId) {
        if (reportId == null) {
            return 0;
        }
        List<TrainingPlan> plans = trainingPlanMapper.selectList(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getUserId, userId)
                .orderByDesc(TrainingPlan::getCreatedAt)
                .last("LIMIT 20"));
        if (plans.isEmpty()) {
            return 0;
        }
        List<Long> planIds = plans.stream()
                .map(TrainingPlan::getId)
                .toList();
        Long count = trainingPlanItemMapper.selectCount(new LambdaQueryWrapper<TrainingPlanItem>()
                .in(TrainingPlanItem::getPlanId, planIds)
                .eq(TrainingPlanItem::getSourceType, "MOCK_INTERVIEW_REPORT")
                .eq(TrainingPlanItem::getSourceId, reportId));
        return toInt(count);
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
        if (event != null && event.getDeltaScore() != null) {
            int deltaCompare = event.getDeltaScore().compareTo(BigDecimal.ZERO);
            if (deltaCompare > 0) {
                return "最近加重";
            }
            if (deltaCompare < 0) {
                return "最近改善";
            }
        }
        if (weakness.getWeaknessScore() != null
                && weakness.getWeaknessScore().compareTo(new BigDecimal("40")) >= 0) {
            return "持续薄弱";
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
        vo.setLatestIssueType(latest.issueType());
        vo.setLatestIssueTypeLabel(mockInterviewIssueTypeLabel(latest.issueType()));
        vo.setLastInterviewAt(latest.attemptedAt());
        return vo;
    }

    private String mockInterviewIssueTypeLabel(String issueType) {
        if ("KNOWLEDGE_GAP".equals(issueType)) {
            return "知识点不会";
        }
        if ("EXPRESSION_GAP".equals(issueType)) {
            return "表达不完整";
        }
        return "待复盘";
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
        vo.setReason(trainingPlanItemReason(item));
        vo.setReviewFocus(trainingPlanItemReviewFocus(item));
        vo.setSourceType(trainingPlanItemSourceType(item));
        vo.setSourceId(item.getSourceId());
        vo.setSourceSummary(trainingPlanItemSourceSummary(item));
        vo.setTargetHref(trainingPlanNextTargetHref(vo));
        vo.setTargetLabel(trainingPlanNextTargetLabel(vo));
        vo.setStatus(item.getStatus());
        vo.setStatusUpdatedAt(item.getStatusUpdatedAt());
        return vo;
    }

    private String trainingPlanItemReason(TrainingPlanItem item) {
        if (item.getReason() != null && !item.getReason().isBlank()) {
            return item.getReason();
        }
        if (item.getSourceSummary() != null && !item.getSourceSummary().isBlank()) {
            return item.getSourceSummary();
        }
        if (item.getReviewFocus() != null && !item.getReviewFocus().isBlank()) {
            return item.getReviewFocus();
        }
        if (item.getKnowledgePoint() != null && !item.getKnowledgePoint().isBlank()) {
            return "围绕 " + item.getKnowledgePoint() + " 补齐当前训练闭环。";
        }
        return "根据当前学习记录安排的复盘任务。";
    }

    private String trainingPlanItemReviewFocus(TrainingPlanItem item) {
        if (item.getReviewFocus() != null && !item.getReviewFocus().isBlank()) {
            return item.getReviewFocus();
        }
        if (item.getKnowledgePoint() != null && !item.getKnowledgePoint().isBlank()) {
            return "复盘 " + item.getKnowledgePoint() + " 的核心思路和易错点。";
        }
        return "复盘当前训练项的关键思路和常见错误。";
    }

    private String trainingPlanItemSourceType(TrainingPlanItem item) {
        if (item.getSourceType() != null && !item.getSourceType().isBlank()) {
            return item.getSourceType();
        }
        return "LEGACY_TRAINING_PLAN";
    }

    private String trainingPlanItemSourceSummary(TrainingPlanItem item) {
        if (item.getSourceSummary() != null && !item.getSourceSummary().isBlank()) {
            return item.getSourceSummary();
        }
        if (item.getKnowledgePoint() != null && !item.getKnowledgePoint().isBlank()) {
            return "历史训练计划：" + item.getKnowledgePoint();
        }
        return "历史训练计划保留的复盘任务。";
    }

    private TrainingPlanHistoryVO toTrainingPlanHistoryVO(TrainingPlan plan, List<TrainingPlanItem> items) {
        TrainingPlanHistoryVO vo = new TrainingPlanHistoryVO();
        vo.setId(plan.getId());
        vo.setTitle(plan.getTitle());
        vo.setSummary(plan.getSummary());
        vo.setStatus(plan.getStatus() == null ? "ACTIVE" : plan.getStatus());
        vo.setStatusLabel(trainingPlanStatusLabel(vo.getStatus()));
        vo.setStartDate(plan.getStartDate());
        vo.setEndDate(plan.getEndDate());
        vo.setItemCount(items.size());
        int completedCount = (int) items.stream()
                .filter(item -> "COMPLETED".equalsIgnoreCase(item.getStatus()))
                .count();
        int skippedCount = (int) items.stream()
                .filter(item -> "SKIPPED".equalsIgnoreCase(item.getStatus()))
                .count();
        int pendingCount = (int) items.stream()
                .filter(item -> item.getStatus() == null || "PENDING".equalsIgnoreCase(item.getStatus()))
                .count();
        int handledCount = completedCount + skippedCount;
        vo.setCompletedCount(completedCount);
        vo.setSkippedCount(skippedCount);
        vo.setPendingCount(pendingCount);
        vo.setHandledCount(handledCount);
        vo.setCompletionRate(completionRate(completedCount, items.size()));
        vo.setHandledRate(completionRate(handledCount, items.size()));
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
        vo.setSourceType(trainingPlanItemSourceType(item));
        vo.setSourceSummary(trainingPlanItemSourceSummary(item));
        vo.setLearningImpactSummary(trainingActivityImpactSummary(item));
        vo.setStatus(item.getStatus());
        vo.setStatusLabel(trainingPlanItemStatusLabel(vo.getStatus()));
        vo.setStatusUpdatedAt(trainingActivityStatusUpdatedAt(item, plan));
        return vo;
    }

    private LocalDateTime trainingActivityStatusUpdatedAt(TrainingPlanItem item, TrainingPlan plan) {
        if (item.getStatusUpdatedAt() != null) {
            return item.getStatusUpdatedAt();
        }
        return plan == null ? null : plan.getCreatedAt();
    }

    private String trainingActivityImpactSummary(TrainingPlanItem item) {
        String knowledgePoint = item.getKnowledgePoint();
        if (knowledgePoint == null || knowledgePoint.isBlank()) {
            knowledgePoint = activityTaskTitle(item);
        }
        if ("SKIPPED".equalsIgnoreCase(item.getStatus())) {
            return "已跳过：" + knowledgePoint + "；仅记录训练节奏，不会降低薄弱点分数。";
        }
        return "已完成：" + knowledgePoint + "；对应薄弱点会记录为改善趋势。";
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

    private List<Long> splitLongs(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .map(text -> {
                    try {
                        return Long.valueOf(text);
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
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
            String issueType,
            LocalDateTime attemptedAt) {
    }

    private static class MockInterviewAttemptBuilder {

        private final Long knowledgeCardId;

        private final MockInterviewSession session;

        private int totalScore;

        private int scoreCount;

        private String issue;

        private String issueType;

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
                setIssue(turn.getMissingKeyPoints(), "KNOWLEDGE_GAP");
            }
            if (issue == null || issue.isBlank()) {
                setIssue(turn.getGapSummary(), "KNOWLEDGE_GAP");
            }
            if (issue == null || issue.isBlank()) {
                setIssue(turn.getExpressionIssue(), "EXPRESSION_GAP");
            }
            if (issue == null || issue.isBlank()) {
                setIssue(turn.getFeedback(), "REVIEW_REQUIRED");
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
                    issueType == null ? "REVIEW_REQUIRED" : issueType,
                    attemptedAt());
        }

        private void setIssue(String value, String type) {
            if (value == null || value.isBlank()) {
                return;
            }
            issue = value.trim();
            issueType = type;
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
