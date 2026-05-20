package com.interview.coach.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.TrainingPlanResult;
import com.interview.coach.dto.TrainingPlanResult.TrainingPlanItemResult;
import com.interview.coach.entity.TrainingPlan;
import com.interview.coach.entity.TrainingPlanItem;
import com.interview.coach.entity.UserWeakness;
import com.interview.coach.handler.BusinessException;
import com.interview.coach.mapper.TrainingPlanItemMapper;
import com.interview.coach.mapper.TrainingPlanMapper;
import com.interview.coach.mapper.UserWeaknessMapper;
import com.interview.coach.service.KnowledgeCardService;
import com.interview.coach.vo.KnowledgeCardVO;
import java.math.BigDecimal;
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

    @Mock
    private UserWeaknessMapper userWeaknessMapper;

    @Mock
    private KnowledgeCardService knowledgeCardService;

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

    @Test
    void savePlanMarksExistingActivePlanAsRegeneratedBeforeCreatingNewPlan() {
        TrainingPlan existing = new TrainingPlan();
        existing.setId(100L);
        existing.setUserId(1L);
        existing.setStatus("ACTIVE");
        when(trainingPlanMapper.selectList(any())).thenReturn(List.of(existing));
        AgentContext context = new AgentContext();
        context.setUserId(1L);
        context.setAgentRunId(9L);
        TrainingPlanResult result = new TrainingPlanResult();
        result.setTitle("3 天专项训练");
        result.setSummary("算法训练。");
        result.setItems(List.of(problemItem()));

        trainingPlanService.savePlan(context, result);

        ArgumentCaptor<TrainingPlan> planCaptor = ArgumentCaptor.forClass(TrainingPlan.class);
        verify(trainingPlanMapper).updateById(planCaptor.capture());
        assertThat(planCaptor.getValue().getId()).isEqualTo(100L);
        assertThat(planCaptor.getValue().getStatus()).isEqualTo("REGENERATED");
    }

    @Test
    void updateItemStatusCompletesPlanWhenAllItemsAreTerminal() {
        TrainingPlanItem item = new TrainingPlanItem();
        item.setId(5L);
        item.setPlanId(100L);
        item.setStatus("PENDING");
        TrainingPlan plan = new TrainingPlan();
        plan.setId(100L);
        plan.setUserId(1L);
        plan.setStatus("ACTIVE");
        TrainingPlanItem completed = new TrainingPlanItem();
        completed.setId(5L);
        completed.setPlanId(100L);
        completed.setStatus("COMPLETED");
        TrainingPlanItem skipped = new TrainingPlanItem();
        skipped.setId(6L);
        skipped.setPlanId(100L);
        skipped.setStatus("SKIPPED");
        when(trainingPlanItemMapper.selectById(5L)).thenReturn(item);
        when(trainingPlanMapper.selectById(100L)).thenReturn(plan);
        when(trainingPlanItemMapper.selectList(any())).thenReturn(List.of(completed, skipped));

        trainingPlanService.updateItemStatus(1L, 5L, "COMPLETED");

        ArgumentCaptor<TrainingPlanItem> itemCaptor = ArgumentCaptor.forClass(TrainingPlanItem.class);
        verify(trainingPlanItemMapper).updateById(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getStatus()).isEqualTo("COMPLETED");
        ArgumentCaptor<TrainingPlan> planCaptor = ArgumentCaptor.forClass(TrainingPlan.class);
        verify(trainingPlanMapper).updateById(planCaptor.capture());
        assertThat(planCaptor.getValue().getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void updateItemStatusRejectsUnknownStatus() {
        assertThatThrownBy(() -> trainingPlanService.updateItemStatus(1L, 5L, "DONE"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void regenerateCreatesThreeDayPlanFromTopWeaknesses() {
        when(trainingPlanMapper.selectList(any())).thenReturn(List.of());
        when(userWeaknessMapper.selectList(any())).thenReturn(List.of(
                weakness("贪心维护最优值", "ALGORITHM_ERROR", "17"),
                weakness("二叉树层序遍历", "LOGIC_ERROR", "12"),
                weakness("链表反转指针操作", "POINTER_ERROR", "9")));
        when(knowledgeCardService.listReviewCards(3)).thenReturn(List.of(
                knowledgeCard(1L, "HashMap 底层结构", "Java"),
                knowledgeCard(2L, "MySQL 索引为什么能加速查询", "MySQL"),
                knowledgeCard(3L, "Spring Bean 生命周期", "Spring")));

        trainingPlanService.regenerate(1L, true, "USER_REQUEST");

        ArgumentCaptor<TrainingPlan> planCaptor = ArgumentCaptor.forClass(TrainingPlan.class);
        verify(trainingPlanMapper).insert(planCaptor.capture());
        assertThat(planCaptor.getValue().getTitle()).contains("3 天");
        assertThat(planCaptor.getValue().getSummary()).doesNotContain("USER_REQUEST");

        ArgumentCaptor<TrainingPlanItem> itemCaptor = ArgumentCaptor.forClass(TrainingPlanItem.class);
        verify(trainingPlanItemMapper, org.mockito.Mockito.times(6)).insert(itemCaptor.capture());
        assertThat(itemCaptor.getAllValues())
                .extracting(TrainingPlanItem::getDayIndex)
                .containsExactly(1, 1, 2, 2, 3, 3);
        assertThat(itemCaptor.getAllValues())
                .extracting(TrainingPlanItem::getKnowledgePoint)
                .containsExactly(
                        "贪心维护最优值",
                        "Java",
                        "二叉树层序遍历",
                        "MySQL",
                        "链表反转指针操作",
                        "Spring");
        assertThat(itemCaptor.getAllValues())
                .extracting(TrainingPlanItem::getItemType)
                .containsExactly(
                        "PROBLEM",
                        "KNOWLEDGE_CARD",
                        "PROBLEM",
                        "KNOWLEDGE_CARD",
                        "PROBLEM",
                        "KNOWLEDGE_CARD");
        assertThat(itemCaptor.getAllValues())
                .extracting(TrainingPlanItem::getProblemTitle)
                .contains("贪心维护最优值专项复盘");
        assertThat(itemCaptor.getAllValues())
                .extracting(TrainingPlanItem::getKnowledgeCardTitle)
                .contains("HashMap 底层结构", "MySQL 索引为什么能加速查询", "Spring Bean 生命周期");
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

    private UserWeakness weakness(String knowledgePoint, String errorType, String score) {
        UserWeakness weakness = new UserWeakness();
        weakness.setUserId(1L);
        weakness.setKnowledgePoint(knowledgePoint);
        weakness.setErrorType(errorType);
        weakness.setWrongCount(1);
        weakness.setSubmitCount(1);
        weakness.setWeaknessScore(new BigDecimal(score));
        return weakness;
    }

    private KnowledgeCardVO knowledgeCard(Long id, String title, String label) {
        KnowledgeCardVO card = new KnowledgeCardVO();
        card.setId(id);
        card.setTitle(title);
        card.setLabel(label);
        card.setTags(List.of("核心机制", "面试表达"));
        return card;
    }
}
