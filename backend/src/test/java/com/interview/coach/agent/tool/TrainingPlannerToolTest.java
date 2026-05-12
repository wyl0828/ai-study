package com.interview.coach.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.AiDiagnosisResult;
import com.interview.coach.dto.TrainingPlanResult;
import com.interview.coach.entity.Problem;
import com.interview.coach.service.KnowledgeCardService;
import com.interview.coach.service.TrainingPlanService;
import com.interview.coach.vo.KnowledgeCardVO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Test
    void fallbackPlanKeepsAlgorithmItemsAndAddsAtMostTwoKnowledgeCards() {
        when(knowledgeCardService.listReviewCards(2)).thenReturn(List.of(
                knowledgeCard(1L, "HashMap 底层结构"),
                knowledgeCard(2L, "MySQL 索引为什么能加速查询")));
        AgentContext context = context();

        TrainingPlanResult result = tool.execute(context, context);

        assertThat(result.getItems())
                .filteredOn(item -> "PROBLEM".equals(item.getItemType()))
                .hasSize(3);
        assertThat(result.getItems())
                .filteredOn(item -> "KNOWLEDGE_CARD".equals(item.getItemType()))
                .hasSize(2);
        assertThat(result.getItems())
                .filteredOn(item -> "KNOWLEDGE_CARD".equals(item.getItemType()))
                .extracting(TrainingPlanResult.TrainingPlanItemResult::getKnowledgeCardTitle)
                .containsExactly("HashMap 底层结构", "MySQL 索引为什么能加速查询");
        verify(trainingPlanService).savePlan(any(), any());
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
