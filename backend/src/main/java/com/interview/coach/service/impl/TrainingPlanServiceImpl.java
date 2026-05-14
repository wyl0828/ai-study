package com.interview.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.TrainingPlanResult;
import com.interview.coach.dto.TrainingPlanResult.TrainingPlanItemResult;
import com.interview.coach.entity.UserWeakness;
import com.interview.coach.handler.BusinessException;
import com.interview.coach.entity.TrainingPlan;
import com.interview.coach.entity.TrainingPlanItem;
import com.interview.coach.mapper.TrainingPlanItemMapper;
import com.interview.coach.mapper.TrainingPlanMapper;
import com.interview.coach.mapper.UserWeaknessMapper;
import com.interview.coach.service.TrainingPlanService;
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

    private final TrainingPlanMapper trainingPlanMapper;

    private final TrainingPlanItemMapper trainingPlanItemMapper;

    private final UserWeaknessMapper userWeaknessMapper;

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
        item.setStatus(normalizedStatus);
        trainingPlanItemMapper.updateById(item);
        List<TrainingPlanItem> items = trainingPlanItemMapper.selectList(new LambdaQueryWrapper<TrainingPlanItem>()
                .eq(TrainingPlanItem::getPlanId, plan.getId()));
        if (!items.isEmpty() && items.stream().allMatch(this::isTerminalItem)) {
            plan.setStatus("COMPLETED");
            trainingPlanMapper.updateById(plan);
        }
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
                .last("LIMIT 3"));
        TrainingPlanResult result = new TrainingPlanResult();
        result.setTitle("当前弱点专项训练");
        result.setSummary((reason == null || reason.isBlank()) ? "根据当前薄弱点重新生成训练计划。" : reason);
        List<TrainingPlanItemResult> items = weaknesses.isEmpty()
                ? List.of(defaultItem())
                : weaknesses.stream().map(this::toPlanItem).toList();
        result.setItems(items);
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
        return Set.of("COMPLETED", "SKIPPED").contains(normalizeStatus(item.getStatus()));
    }

    private TrainingPlanItemResult toPlanItem(UserWeakness weakness) {
        TrainingPlanItemResult item = new TrainingPlanItemResult();
        item.setItemType("PROBLEM");
        item.setDayIndex(1);
        item.setKnowledgePoint(weakness.getKnowledgePoint());
        item.setProblemTitle(weakness.getKnowledgePoint() + "专项复盘");
        item.setReason("针对近期高分薄弱点安排复盘。");
        item.setReviewFocus("复盘失败提交中的错误原因，并重新说明修正思路。");
        return item;
    }

    private TrainingPlanItemResult defaultItem() {
        TrainingPlanItemResult item = new TrainingPlanItemResult();
        item.setItemType("PROBLEM");
        item.setDayIndex(1);
        item.setKnowledgePoint("基础编码习惯");
        item.setProblemTitle("近期错题复盘");
        item.setReason("当前薄弱点数据较少，先完成一次基础复盘。");
        item.setReviewFocus("检查边界条件、变量更新顺序和失败用例。");
        return item;
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
