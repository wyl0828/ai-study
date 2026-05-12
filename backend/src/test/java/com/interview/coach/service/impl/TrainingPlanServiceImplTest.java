package com.interview.coach.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.TrainingPlanResult;
import com.interview.coach.dto.TrainingPlanResult.TrainingPlanItemResult;
import com.interview.coach.entity.TrainingPlan;
import com.interview.coach.entity.TrainingPlanItem;
import com.interview.coach.mapper.TrainingPlanItemMapper;
import com.interview.coach.mapper.TrainingPlanMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrainingPlanServiceImplTest {

    @Mock
    private TrainingPlanMapper trainingPlanMapper;

    @Mock
    private TrainingPlanItemMapper trainingPlanItemMapper;

    @InjectMocks
    private TrainingPlanServiceImpl trainingPlanService;

    @Test
    void savePlanPersistsProblemAndKnowledgeCardItems() {
        AgentContext context = new AgentContext();
        context.setUserId(1L);
        context.setAgentRunId(9L);
        TrainingPlanResult result = new TrainingPlanResult();
        result.setTitle("3 天专项训练");
        result.setSummary("算法训练和后端知识复习。");
        result.setItems(List.of(
                problemItem(),
                knowledgeCardItem()));

        trainingPlanService.savePlan(context, result);

        verify(trainingPlanMapper).insert(any(TrainingPlan.class));
        ArgumentCaptor<TrainingPlanItem> itemCaptor = ArgumentCaptor.forClass(TrainingPlanItem.class);
        verify(trainingPlanItemMapper, org.mockito.Mockito.times(2)).insert(itemCaptor.capture());
        assertThat(itemCaptor.getAllValues())
                .extracting(TrainingPlanItem::getItemType)
                .containsExactly("PROBLEM", "KNOWLEDGE_CARD");
        assertThat(itemCaptor.getAllValues().get(0).getProblemTitle()).isEqualTo("两数之和");
        assertThat(itemCaptor.getAllValues().get(1).getKnowledgeCardId()).isEqualTo(1L);
        assertThat(itemCaptor.getAllValues().get(1).getKnowledgeCardTitle()).isEqualTo("HashMap 底层结构");
    }

    private TrainingPlanItemResult problemItem() {
        TrainingPlanItemResult item = new TrainingPlanItemResult();
        item.setItemType("PROBLEM");
        item.setDayIndex(1);
        item.setKnowledgePoint("HashMap Lookup");
        item.setProblemTitle("两数之和");
        item.setReason("复盘本次失败的知识点。");
        item.setReviewFocus("说明失败用例为什么会击穿当前思路。");
        return item;
    }

    private TrainingPlanItemResult knowledgeCardItem() {
        TrainingPlanItemResult item = new TrainingPlanItemResult();
        item.setItemType("KNOWLEDGE_CARD");
        item.setKnowledgeCardId(1L);
        item.setKnowledgeCardTitle("HashMap 底层结构");
        item.setDayIndex(1);
        item.setKnowledgePoint("Java 集合");
        item.setReason("穿插一个 Java 后端高频知识点，保持面试表达训练。");
        item.setReviewFocus("数组、链表、红黑树、扩容。");
        return item;
    }
}
