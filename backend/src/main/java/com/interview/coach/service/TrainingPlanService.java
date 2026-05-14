package com.interview.coach.service;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.TrainingPlanResult;

public interface TrainingPlanService {

    void savePlan(AgentContext context, TrainingPlanResult result);

    void updateItemStatus(Long userId, Long itemId, String status);

    TrainingPlanResult regenerate(Long userId, boolean replaceCurrentPlan, String reason);
}
