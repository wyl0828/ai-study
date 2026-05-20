package com.interview.coach.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.AiDiagnosisResult;
import com.interview.coach.dto.RagChunkHit;
import com.interview.coach.dto.RagRetrieveResult;
import com.interview.coach.dto.TrainingPlanResult;
import com.interview.coach.entity.Problem;
import com.interview.coach.service.KnowledgeCardService;
import com.interview.coach.service.TrainingPlanService;
import com.interview.coach.vo.KnowledgeCardVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrainingPlannerToolTest {

    @Mock
    private TrainingPlanService trainingPlanService;

    @Mock
    private KnowledgeCardService knowledgeCardService;

    @InjectMocks
    private TrainingPlannerTool tool;

    private ArgumentCaptor<TrainingPlanResult> planCaptor;

    @BeforeEach
    void setUp() {
        planCaptor = ArgumentCaptor.forClass(TrainingPlanResult.class);
    }

    @Test
    void fallbackPlanBalancesAlgorithmItemsWithBackendKnowledgeCards() {
        when(knowledgeCardService.listReviewCards(3)).thenReturn(List.of(
                knowledgeCard(1L, "HashMap 底层结构"),
                knowledgeCard(2L, "MySQL 索引为什么能加速查询"),
                knowledgeCard(3L, "Spring Bean 生命周期")));
        AgentContext context = context();

        TrainingPlanResult result = tool.execute(context, context);

        assertThat(result.getItems())
                .filteredOn(item -> "PROBLEM".equals(item.getItemType()))
                .hasSize(3);
        assertThat(result.getItems())
                .filteredOn(item -> "KNOWLEDGE_CARD".equals(item.getItemType()))
                .hasSize(3);
        assertThat(result.getItems())
                .filteredOn(item -> "KNOWLEDGE_CARD".equals(item.getItemType()))
                .extracting(TrainingPlanResult.TrainingPlanItemResult::getKnowledgeCardTitle)
                .containsExactly("HashMap 底层结构", "MySQL 索引为什么能加速查询", "Spring Bean 生命周期");
        assertThat(result.getSummary()).contains("后端知识卡片");
        verify(trainingPlanService).savePlan(any(), any());
    }

    @Test
    void fallbackPlanStillSavesAlgorithmItemsWhenKnowledgeCardLookupFails() {
        when(knowledgeCardService.listReviewCards(3)).thenThrow(new RuntimeException("knowledge unavailable"));
        AgentContext context = context();

        TrainingPlanResult result = tool.execute(context, context);

        assertThat(result.getItems())
                .filteredOn(item -> "PROBLEM".equals(item.getItemType()))
                .hasSize(3);
        assertThat(result.getItems())
                .filteredOn(item -> "KNOWLEDGE_CARD".equals(item.getItemType()))
                .isEmpty();
        verify(trainingPlanService).savePlan(eq(context), planCaptor.capture());
        assertThat(planCaptor.getValue().getItems()).hasSize(3);
    }

    @Test
    void fallbackPlanUsesRetrievedKnowledgeCardsBeforeGenericReviewCards() {
        AgentContext context = context();
        RagChunkHit hit = new RagChunkHit();
        hit.setSourceType("KNOWLEDGE_CARD");
        hit.setSourceId(9L);
        hit.setTitle("HashMap 查找顺序");
        hit.setKnowledgePoint("HashMap 基础查找");
        RagRetrieveResult ragResult = new RagRetrieveResult();
        ragResult.setHits(List.of(hit));
        context.setRagRetrieveResult(ragResult);

        TrainingPlanResult result = tool.execute(context, context);

        assertThat(result.getItems())
                .filteredOn(item -> "KNOWLEDGE_CARD".equals(item.getItemType()))
                .extracting(TrainingPlanResult.TrainingPlanItemResult::getKnowledgeCardId)
                .containsExactly(9L);
        assertThat(result.getItems())
                .filteredOn(item -> "KNOWLEDGE_CARD".equals(item.getItemType()))
                .extracting(TrainingPlanResult.TrainingPlanItemResult::getReason)
                .containsExactly("结合本次错误知识点检索到的后端知识卡片，补充面试表达训练。");
        verify(trainingPlanService).savePlan(eq(context), any());
    }

    private AgentContext context() {
        AgentContext context = new AgentContext();
        Problem problem = new Problem();
        problem.setTitle("两数之和");
        problem.setCategory("HashMap");
        context.setProblem(problem);
        AiDiagnosisResult diagnosis = new AiDiagnosisResult();
        diagnosis.setKnowledgePoint("HashMap Lookup");
        context.setDiagnosis(diagnosis);
        return context;
    }

    private KnowledgeCardVO knowledgeCard(Long id, String title) {
        KnowledgeCardVO card = new KnowledgeCardVO();
        card.setId(id);
        card.setCategory("JAVA");
        card.setTitle(title);
        card.setQuestion(title + "是什么？");
        card.setDifficulty("MEDIUM");
        card.setTags(List.of("基础", "集合"));
        return card;
    }
}
