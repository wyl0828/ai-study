package com.interview.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.TrainingPlanResult;
import com.interview.coach.dto.TrainingPlanResult.TrainingPlanItemResult;
import com.interview.coach.entity.UserWeakness;
import com.interview.coach.entity.UserWeaknessEvent;
import com.interview.coach.handler.BusinessException;
import com.interview.coach.entity.TrainingPlan;
import com.interview.coach.entity.TrainingPlanItem;
import com.interview.coach.mapper.TrainingPlanItemMapper;
import com.interview.coach.mapper.TrainingPlanMapper;
import com.interview.coach.mapper.UserWeaknessEventMapper;
import com.interview.coach.mapper.UserWeaknessMapper;
import com.interview.coach.service.KnowledgeCardService;
import com.interview.coach.service.TrainingPlanService;
import com.interview.coach.vo.KnowledgeCardVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrainingPlanServiceImpl implements TrainingPlanService {

    private static final int PLAN_DAYS = 3;

    private static final BigDecimal TRAINING_COMPLETION_DELTA = new BigDecimal("-2");

    private final TrainingPlanMapper trainingPlanMapper;

    private final TrainingPlanItemMapper trainingPlanItemMapper;

    private final UserWeaknessMapper userWeaknessMapper;

    private final UserWeaknessEventMapper userWeaknessEventMapper;

    private final KnowledgeCardService knowledgeCardService;

    @Override
    @Transactional
    public void savePlan(AgentContext context, TrainingPlanResult result) {
        savePlan(context, result, true);
    }

    private void savePlan(AgentContext context, TrainingPlanResult result, boolean replaceActivePlan) {
        if (replaceActivePlan) {
            markActivePlansRegenerated(context.getUserId());
        }
        LocalDate startDate = LocalDate.now();
        TrainingPlan plan = new TrainingPlan();
        plan.setUserId(context.getUserId());
        plan.setAgentRunId(context.getAgentRunId());
        plan.setTitle(result.getTitle());
        plan.setSummary(result.getSummary());
        plan.setStartDate(startDate);
        plan.setEndDate(startDate.plusDays(Math.max(maxDayIndex(result), 1) - 1L));
        plan.setStatus("ACTIVE");
        plan.setCreatedAt(LocalDateTime.now());
        trainingPlanMapper.insert(plan);

        for (TrainingPlanItemResult itemResult : result.getItems()) {
            TrainingPlanItem item = new TrainingPlanItem();
            item.setPlanId(plan.getId());
            item.setItemType(itemType(itemResult));
            item.setKnowledgeCardId(itemResult.getKnowledgeCardId());
            item.setDayIndex(itemResult.getDayIndex());
            item.setKnowledgePoint(itemResult.getKnowledgePoint());
            item.setProblemTitle(itemResult.getProblemTitle());
            item.setKnowledgeCardTitle(itemResult.getKnowledgeCardTitle());
            item.setReason(itemResult.getReason());
            item.setReviewFocus(itemResult.getReviewFocus());
            item.setSourceType(itemResult.getSourceType());
            item.setSourceId(itemResult.getSourceId());
            item.setSourceSummary(itemResult.getSourceSummary());
            item.setStatus("PENDING");
            trainingPlanItemMapper.insert(item);
        }
    }

    @Override
    @Transactional
    public void updateItemStatus(Long userId, Long itemId, String status) {
        String normalizedStatus = normalizeStatus(status);
        TrainingPlanItem item = trainingPlanItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(404, "training plan item not found");
        }
        TrainingPlan plan = trainingPlanMapper.selectById(item.getPlanId());
        if (plan == null || !userId.equals(plan.getUserId())) {
            throw new BusinessException(404, "training plan item not found");
        }
        String previousStatus = item.getStatus();
        item.setStatus(normalizedStatus);
        item.setStatusUpdatedAt(LocalDateTime.now());
        trainingPlanItemMapper.updateById(item);
        if (!"COMPLETED".equalsIgnoreCase(previousStatus)) {
            recordTrainingCompletion(userId, item, normalizedStatus, item.getStatusUpdatedAt());
        }
        List<TrainingPlanItem> items = trainingPlanItemMapper.selectList(new LambdaQueryWrapper<TrainingPlanItem>()
                .eq(TrainingPlanItem::getPlanId, plan.getId()));
        if (!items.isEmpty() && items.stream().allMatch(this::isTerminalItem)) {
            plan.setStatus("COMPLETED");
            trainingPlanMapper.updateById(plan);
        }
    }

    private void recordTrainingCompletion(Long userId, TrainingPlanItem item, String status, LocalDateTime now) {
        if (!"COMPLETED".equals(status)
                || item.getKnowledgePoint() == null
                || item.getKnowledgePoint().isBlank()) {
            return;
        }

        List<UserWeakness> weaknesses = userWeaknessMapper.selectList(new LambdaQueryWrapper<UserWeakness>()
                .eq(UserWeakness::getUserId, userId)
                .eq(UserWeakness::getKnowledgePoint, item.getKnowledgePoint())
                .orderByDesc(UserWeakness::getWeaknessScore)
                .last("LIMIT 1"));
        if (weaknesses == null || weaknesses.isEmpty()) {
            return;
        }

        UserWeakness weakness = weaknesses.get(0);
        BigDecimal beforeScore = weakness.getWeaknessScore() == null ? BigDecimal.ZERO : weakness.getWeaknessScore();
        BigDecimal afterScore = beforeScore.add(TRAINING_COMPLETION_DELTA).max(BigDecimal.ZERO);
        BigDecimal effectiveDelta = afterScore.subtract(beforeScore);
        if (effectiveDelta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        weakness.setWeaknessScore(afterScore);
        weakness.setUpdatedAt(now);
        userWeaknessMapper.updateById(weakness);

        UserWeaknessEvent event = new UserWeaknessEvent();
        event.setUserId(userId);
        event.setKnowledgePoint(weakness.getKnowledgePoint());
        event.setErrorType(weakness.getErrorType());
        event.setSourceType("TRAINING_PLAN_COMPLETED");
        event.setSourceId(item.getId());
        event.setDeltaScore(effectiveDelta);
        event.setBeforeScore(beforeScore);
        event.setAfterScore(afterScore);
        event.setReason("完成训练项：" + trainingItemTitle(item));
        event.setCreatedAt(now);
        userWeaknessEventMapper.insert(event);
    }

    private String trainingItemTitle(TrainingPlanItem item) {
        String title = "KNOWLEDGE_CARD".equalsIgnoreCase(item.getItemType())
                ? item.getKnowledgeCardTitle()
                : item.getProblemTitle();
        if (title != null && !title.isBlank()) {
            return title;
        }
        return item.getKnowledgePoint();
    }

    @Override
    @Transactional
    public TrainingPlanResult regenerate(Long userId, boolean replaceCurrentPlan, String reason) {
        if (replaceCurrentPlan) {
            markActivePlansRegenerated(userId);
        }
        List<UserWeakness> weaknesses = userWeaknessMapper.selectList(new LambdaQueryWrapper<UserWeakness>()
                .eq(UserWeakness::getUserId, userId)
                .orderByDesc(UserWeakness::getWeaknessScore)
                .last("LIMIT " + PLAN_DAYS));
        TrainingPlanResult result = new TrainingPlanResult();
        result.setTitle("3 天当前弱点专项训练");
        result.setSummary(regenerateSummary(reason));
        result.setItems(buildRegeneratedItems(weaknesses));
        AgentContext context = new AgentContext();
        context.setUserId(userId);
        savePlan(context, result, false);
        return result;
    }

    private void markActivePlansRegenerated(Long userId) {
        List<TrainingPlan> activePlans = trainingPlanMapper.selectList(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getUserId, userId)
                .eq(TrainingPlan::getStatus, "ACTIVE"));
        if (activePlans == null) {
            return;
        }
        for (TrainingPlan activePlan : activePlans) {
            activePlan.setStatus("REGENERATED");
            trainingPlanMapper.updateById(activePlan);
        }
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!Set.of("PENDING", "COMPLETED", "SKIPPED").contains(normalized)) {
            throw new BusinessException("unsupported training item status");
        }
        return normalized;
    }

    private boolean isTerminalItem(TrainingPlanItem item) {
        if (item == null || item.getStatus() == null || item.getStatus().isBlank()) {
            return false;
        }
        return Set.of("COMPLETED", "SKIPPED").contains(normalizeStatus(item.getStatus()));
    }

    private List<TrainingPlanItemResult> buildRegeneratedItems(List<UserWeakness> weaknesses) {
        List<KnowledgeCardVO> reviewCards = reviewCardsForPlan();
        if (weaknesses == null || weaknesses.isEmpty()) {
            return java.util.stream.IntStream.rangeClosed(1, PLAN_DAYS)
                    .boxed()
                    .flatMap(dayIndex -> defaultItems(dayIndex, reviewCards).stream())
                    .toList();
        }
        return java.util.stream.IntStream.rangeClosed(1, PLAN_DAYS)
                .boxed()
                .flatMap(dayIndex -> toPlanItems(
                        pickWeaknessForDay(weaknesses, dayIndex),
                        knowledgeCardForDay(reviewCards, dayIndex),
                        dayIndex).stream())
                .toList();
    }

    private UserWeakness pickWeaknessForDay(List<UserWeakness> weaknesses, int dayIndex) {
        if (weaknesses.size() >= dayIndex) {
            return weaknesses.get(dayIndex - 1);
        }
        return weaknesses.get((dayIndex - 1) % weaknesses.size());
    }

    private List<TrainingPlanItemResult> toPlanItems(
            UserWeakness weakness, KnowledgeCardVO knowledgeCard, int dayIndex) {
        return List.of(
                planItem(weakness.getKnowledgePoint(), weakness.getKnowledgePoint() + "专项复盘", dayIndex,
                        primaryReasonForDay(dayIndex), primaryReviewFocusForDay(dayIndex),
                        "USER_WEAKNESS", null, "来自当前薄弱点排行：" + weakness.getKnowledgePoint()),
                knowledgeCardItem(knowledgeCard, weakness.getKnowledgePoint(), dayIndex));
    }

    private TrainingPlanItemResult planItem(String knowledgePoint, String problemTitle, int dayIndex,
            String reason, String reviewFocus, String sourceType, Long sourceId, String sourceSummary) {
        TrainingPlanItemResult item = new TrainingPlanItemResult();
        item.setItemType("PROBLEM");
        item.setDayIndex(dayIndex);
        item.setKnowledgePoint(knowledgePoint);
        item.setProblemTitle(problemTitle);
        item.setReason(reason);
        item.setReviewFocus(reviewFocus);
        item.setSourceType(sourceType);
        item.setSourceId(sourceId);
        item.setSourceSummary(sourceSummary);
        return item;
    }

    private List<TrainingPlanItemResult> defaultItems(int dayIndex, List<KnowledgeCardVO> reviewCards) {
        return List.of(
                planItem("基础编码习惯", "近期错题复盘", dayIndex,
                        primaryReasonForDay(dayIndex), primaryReviewFocusForDay(dayIndex),
                        "GENERAL_REVIEW", null, "来自当前学习数据为空时的通用复盘安排。"),
                knowledgeCardItem(knowledgeCardForDay(reviewCards, dayIndex), "基础编码习惯", dayIndex));
    }

    private TrainingPlanItemResult knowledgeCardItem(KnowledgeCardVO card, String fallbackKnowledgePoint, int dayIndex) {
        TrainingPlanItemResult item = new TrainingPlanItemResult();
        item.setItemType("KNOWLEDGE_CARD");
        item.setKnowledgeCardId(card == null ? null : card.getId());
        item.setKnowledgeCardTitle(card == null ? "后端知识卡片复习" : card.getTitle());
        item.setDayIndex(dayIndex);
        item.setKnowledgePoint(card == null ? fallbackKnowledgePoint : card.getLabel());
        item.setReason(knowledgeReasonForDay(dayIndex));
        item.setReviewFocus(card == null ? knowledgeReviewFocusForDay(dayIndex) : cardReviewFocus(card));
        item.setSourceType("KNOWLEDGE_CARD_REVIEW");
        item.setSourceId(card == null ? null : card.getId());
        item.setSourceSummary(card == null
                ? "来自通用后端知识卡复习安排。"
                : "来自后端知识卡复习池：" + card.getTitle());
        return item;
    }

    private List<KnowledgeCardVO> reviewCardsForPlan() {
        try {
            return knowledgeCardService.listReviewCards(PLAN_DAYS);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private KnowledgeCardVO knowledgeCardForDay(List<KnowledgeCardVO> reviewCards, int dayIndex) {
        if (reviewCards == null || reviewCards.isEmpty()) {
            return null;
        }
        return reviewCards.get((dayIndex - 1) % reviewCards.size());
    }

    private String regenerateSummary(String reason) {
        if (reason == null || reason.isBlank() || "USER_REQUEST".equalsIgnoreCase(reason.trim())) {
            return "根据当前薄弱点重新生成 3 天训练计划。";
        }
        return reason;
    }

    private String primaryReasonForDay(int dayIndex) {
        return switch (dayIndex) {
            case 1 -> "优先复盘当前最高分薄弱点。";
            case 2 -> "练习相邻薄弱点，避免错误模式迁移。";
            default -> "回看错题卡后重新挑战相关题目。";
        };
    }

    private String primaryReviewFocusForDay(int dayIndex) {
        return switch (dayIndex) {
            case 1 -> "复盘失败提交中的错误原因，并重新说明修正思路。";
            case 2 -> "对比不同题型里的相同知识点，整理触发条件和边界。";
            default -> "编码前先写出关键不变量、边界条件和失败用例。";
        };
    }

    private String knowledgeReasonForDay(int dayIndex) {
        return switch (dayIndex) {
            case 1 -> "补一个后端知识卡片，把算法错误背后的基础表达说清楚。";
            case 2 -> "穿插系统知识复习，避免训练只停留在刷题。";
            default -> "用知识卡收尾，整理可以在面试里讲出来的复盘表达。";
        };
    }

    private String knowledgeReviewFocusForDay(int dayIndex) {
        return switch (dayIndex) {
            case 1 -> "先自测，再对照标杆回答补齐核心记忆点。";
            case 2 -> "重点补充定义、机制、使用场景和常见坑。";
            default -> "把本轮薄弱点总结成 1 分钟面试回答。";
        };
    }

    private String cardReviewFocus(KnowledgeCardVO card) {
        if (card.getTags() == null || card.getTags().isEmpty()) {
            return "先自测，再对照标杆回答补齐核心记忆点。";
        }
        return String.join("、", card.getTags());
    }

    private int maxDayIndex(TrainingPlanResult result) {
        return result.getItems().stream()
                .map(TrainingPlanItemResult::getDayIndex)
                .filter(dayIndex -> dayIndex != null && dayIndex > 0)
                .max(Integer::compareTo)
                .orElse(1);
    }

    private String itemType(TrainingPlanItemResult itemResult) {
        return itemResult.getItemType() == null ? "PROBLEM" : itemResult.getItemType();
    }
}
