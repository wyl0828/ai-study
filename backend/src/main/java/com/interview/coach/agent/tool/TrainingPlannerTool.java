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
        result.setTitle("3 天专项训练：" + knowledgePoint);
        result.setSummary("围绕失败知识点、相邻题型和原题重做安排训练。");
        result.getItems().add(item(1, knowledgePoint, problemTitle,
                "趁错误记忆还清晰，先复盘本次失败的知识点。",
                "说明失败用例为什么会击穿当前思路。"));
        result.getItems().add(item(2, category, null,
                "练习同类题目中的相邻知识点。",
                "对比同类题型规律和原来的错误做法。"));
        result.getItems().add(item(3, knowledgePoint, problemTitle,
                "回顾错题卡后重新挑战原题。",
                "编码前先写出不变量或边界条件。"));
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
