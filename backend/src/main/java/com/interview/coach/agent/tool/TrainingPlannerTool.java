package com.interview.coach.agent.tool;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.TrainingPlanResult;
import com.interview.coach.dto.TrainingPlanResult.TrainingPlanItemResult;
import com.interview.coach.service.TrainingPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrainingPlannerTool implements Tool<AgentContext, TrainingPlanResult> {

    private final TrainingPlanService trainingPlanService;

    @Override
    public String name() {
        return "TrainingPlannerTool";
    }

    @Override
    public TrainingPlanResult execute(AgentContext input, AgentContext context) {
        TrainingPlanResult result = fallbackPlan(context);
        trainingPlanService.savePlan(context, result);
        context.setTrainingPlan(result);
        return result;
    }

    private TrainingPlanResult fallbackPlan(AgentContext context) {
        String knowledgePoint = context.getDiagnosis().getKnowledgePoint();
        String category = context.getProblem().getCategory();
        String problemTitle = context.getProblem().getTitle();

        TrainingPlanResult result = new TrainingPlanResult();
        result.setTitle("3-day recovery plan: " + knowledgePoint);
        result.setSummary("Focus on the failed knowledge point, one adjacent topic, and a retry of the original problem.");
        result.getItems().add(item(1, knowledgePoint, problemTitle,
                "Repeat the failed knowledge point while the mistake is fresh.",
                "Explain why the failed case breaks the submitted idea."));
        result.getItems().add(item(2, category, null,
                "Practice one adjacent topic from the same problem category.",
                "Compare the category pattern with the original failed approach."));
        result.getItems().add(item(3, knowledgePoint, problemTitle,
                "Review the mistake card and retry the original problem.",
                "Write the invariant or boundary condition before coding."));
        return result;
    }

    private TrainingPlanItemResult item(Integer dayIndex, String knowledgePoint, String problemTitle,
            String reason, String reviewFocus) {
        TrainingPlanItemResult item = new TrainingPlanItemResult();
        item.setDayIndex(dayIndex);
        item.setKnowledgePoint(knowledgePoint);
        item.setProblemTitle(problemTitle);
        item.setReason(reason);
        item.setReviewFocus(reviewFocus);
        return item;
    }
}
