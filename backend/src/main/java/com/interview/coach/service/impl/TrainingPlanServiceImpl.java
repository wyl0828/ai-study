package com.interview.coach.service.impl;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.TrainingPlanResult;
import com.interview.coach.dto.TrainingPlanResult.TrainingPlanItemResult;
import com.interview.coach.entity.TrainingPlan;
import com.interview.coach.entity.TrainingPlanItem;
import com.interview.coach.mapper.TrainingPlanItemMapper;
import com.interview.coach.mapper.TrainingPlanMapper;
import com.interview.coach.service.TrainingPlanService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrainingPlanServiceImpl implements TrainingPlanService {

    private final TrainingPlanMapper trainingPlanMapper;

    private final TrainingPlanItemMapper trainingPlanItemMapper;

    @Override
    @Transactional
    public void savePlan(AgentContext context, TrainingPlanResult result) {
        LocalDate startDate = LocalDate.now();
        TrainingPlan plan = new TrainingPlan();
        plan.setUserId(context.getUserId());
        plan.setAgentRunId(context.getAgentRunId());
        plan.setTitle(result.getTitle());
        plan.setSummary(result.getSummary());
        plan.setStartDate(startDate);
        plan.setEndDate(startDate.plusDays(Math.max(maxDayIndex(result), 1) - 1L));
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
