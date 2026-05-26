package com.interview.coach.agent.tool;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.RagChunkHit;
import com.interview.coach.dto.TrainingPlanResult;
import com.interview.coach.dto.TrainingPlanResult.TrainingPlanItemResult;
import com.interview.coach.service.KnowledgeCardService;
import com.interview.coach.service.TrainingPlanService;
import com.interview.coach.vo.KnowledgeCardVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrainingPlannerTool implements Tool<AgentContext, TrainingPlanResult> {

    private static final int KNOWLEDGE_CARD_LIMIT = 3;

    private final TrainingPlanService trainingPlanService;

    private final KnowledgeCardService knowledgeCardService;

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
        result.setSummary("围绕失败知识点、相邻题型、原题重做和后端知识卡片安排训练。");
        result.getItems().add(item(1, knowledgePoint, problemTitle,
                "趁错误记忆还清晰，先复盘本次失败的知识点。",
                "说明失败用例为什么会击穿当前思路。", context.getSubmissionId()));
        result.getItems().add(item(2, category, null,
                "练习同类题目中的相邻知识点。",
                "对比同类题型规律和原来的错误做法。", context.getSubmissionId()));
        result.getItems().add(item(3, knowledgePoint, problemTitle,
                "回顾错题卡后重新挑战原题。",
                "编码前先写出不变量或边界条件。", context.getSubmissionId()));
        addKnowledgeCards(context, result);
        return result;
    }

    private TrainingPlanItemResult item(Integer dayIndex, String knowledgePoint, String problemTitle,
            String reason, String reviewFocus, Long submissionId) {
        TrainingPlanItemResult item = new TrainingPlanItemResult();
        item.setItemType("PROBLEM");
        item.setDayIndex(dayIndex);
        item.setKnowledgePoint(knowledgePoint);
        item.setProblemTitle(problemTitle);
        item.setReason(reason);
        item.setReviewFocus(reviewFocus);
        item.setSourceType("SUBMISSION_FAILED");
        item.setSourceId(submissionId);
        item.setSourceSummary(submissionId == null
                ? "来自本次失败提交的 AI 诊断。"
                : "来自失败提交 #" + submissionId + " 的 AI 诊断。");
        return item;
    }

    private void addKnowledgeCards(AgentContext context, TrainingPlanResult result) {
        List<RagChunkHit> retrievedCards = retrievedKnowledgeCards(context);
        if (!retrievedCards.isEmpty()) {
            for (int i = 0; i < retrievedCards.size(); i++) {
                RagChunkHit hit = retrievedCards.get(i);
                TrainingPlanItemResult item = new TrainingPlanItemResult();
                item.setItemType("KNOWLEDGE_CARD");
                item.setKnowledgeCardId(hit.getSourceId());
                item.setKnowledgeCardTitle(title(hit));
                item.setDayIndex(i + 1);
                item.setKnowledgePoint(hit.getKnowledgePoint());
                item.setReason("结合本次错误知识点检索到的后端知识卡片，补充面试表达训练。");
                item.setReviewFocus(compact(hit.getChunkText()));
                item.setSourceType("RAG_KNOWLEDGE_CARD");
                item.setSourceId(hit.getSourceId());
                item.setSourceSummary("来自 RAG 命中的知识卡片：" + title(hit));
                result.getItems().add(item);
            }
            return;
        }
        try {
            List<KnowledgeCardVO> cards = knowledgeCardService.listReviewCards(KNOWLEDGE_CARD_LIMIT);
            for (int i = 0; i < cards.size() && i < KNOWLEDGE_CARD_LIMIT; i++) {
                KnowledgeCardVO card = cards.get(i);
                TrainingPlanItemResult item = new TrainingPlanItemResult();
                item.setItemType("KNOWLEDGE_CARD");
                item.setKnowledgeCardId(card.getId());
                item.setKnowledgeCardTitle(card.getTitle());
                item.setDayIndex(i + 1);
                item.setKnowledgePoint(card.getLabel());
                item.setReason("穿插一个 Java 后端高频知识点，保持面试表达训练。");
                item.setReviewFocus(String.join("、", card.getTags() == null ? List.of() : card.getTags()));
                item.setSourceType("KNOWLEDGE_CARD_REVIEW");
                item.setSourceId(card.getId());
                item.setSourceSummary("来自后端知识卡复习池：" + card.getTitle());
                result.getItems().add(item);
            }
        } catch (Exception ex) {
            log.warn("Skip knowledge cards in training plan because lookup or item build failed: {}", ex.getMessage());
        }
    }

    private List<RagChunkHit> retrievedKnowledgeCards(AgentContext context) {
        if (context.getRagRetrieveResult() == null || !context.getRagRetrieveResult().hasHits()) {
            return List.of();
        }
        return context.getRagRetrieveResult().getHits().stream()
                .filter(hit -> "KNOWLEDGE_CARD".equals(hit.getSourceType()))
                .filter(hit -> hit.getSourceId() != null)
                .limit(KNOWLEDGE_CARD_LIMIT)
                .toList();
    }

    private String title(RagChunkHit hit) {
        return hit.getTitle() == null ? "知识卡片#" + hit.getSourceId() : hit.getTitle();
    }

    private String compact(String value) {
        if (value == null || value.isBlank()) {
            return "围绕本次错误相关知识点复习定义、机制和面试表达。";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 80) + "...";
    }
}
