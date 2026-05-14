package com.interview.coach.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.dto.SelfTestSubmitRequest;
import com.interview.coach.entity.KnowledgeCard;
import com.interview.coach.entity.SelfTestRecord;
import com.interview.coach.entity.UserKnowledgeCardMastery;
import com.interview.coach.entity.UserWeaknessEvent;
import com.interview.coach.mapper.KnowledgeCardMapper;
import com.interview.coach.mapper.SelfTestRecordMapper;
import com.interview.coach.mapper.UserKnowledgeCardMasteryMapper;
import com.interview.coach.mapper.UserWeaknessEventMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeLearningServiceImplTest {

    @Mock
    private KnowledgeCardMapper knowledgeCardMapper;

    @Mock
    private SelfTestRecordMapper selfTestRecordMapper;

    @Mock
    private UserKnowledgeCardMasteryMapper masteryMapper;

    @Mock
    private UserWeaknessEventMapper weaknessEventMapper;

    @InjectMocks
    private KnowledgeLearningServiceImpl knowledgeLearningService;

    @Test
    void submitSelfTestPersistsRecordAndRaisesMasteryForHighScore() {
        KnowledgeCard card = card();
        UserKnowledgeCardMastery mastery = new UserKnowledgeCardMastery();
        mastery.setId(8L);
        mastery.setUserId(1L);
        mastery.setKnowledgeCardId(2L);
        mastery.setMasteryScore(new BigDecimal("60"));
        mastery.setSelfTestCount(1);
        when(knowledgeCardMapper.selectById(2L)).thenReturn(card);
        when(masteryMapper.selectOne(any())).thenReturn(mastery);
        SelfTestSubmitRequest request = request(85);

        knowledgeLearningService.submitSelfTest(1L, 2L, request);

        ArgumentCaptor<SelfTestRecord> recordCaptor = ArgumentCaptor.forClass(SelfTestRecord.class);
        verify(selfTestRecordMapper).insert(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getScore()).isEqualTo(85);
        ArgumentCaptor<UserKnowledgeCardMastery> masteryCaptor =
                ArgumentCaptor.forClass(UserKnowledgeCardMastery.class);
        verify(masteryMapper).updateById(masteryCaptor.capture());
        assertThat(masteryCaptor.getValue().getMasteryScore()).isEqualByComparingTo("70");
        assertThat(masteryCaptor.getValue().getSelfTestCount()).isEqualTo(2);
    }

    @Test
    void submitSelfTestWritesWeaknessEventForLowScore() {
        when(knowledgeCardMapper.selectById(2L)).thenReturn(card());
        when(masteryMapper.selectOne(any())).thenReturn(null);
        SelfTestSubmitRequest request = request(40);

        knowledgeLearningService.submitSelfTest(1L, 2L, request);

        ArgumentCaptor<UserWeaknessEvent> eventCaptor = ArgumentCaptor.forClass(UserWeaknessEvent.class);
        verify(weaknessEventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getSourceType()).isEqualTo("SELF_TEST");
        assertThat(eventCaptor.getValue().getKnowledgePoint()).isEqualTo("Java 集合");
        assertThat(eventCaptor.getValue().getDeltaScore()).isEqualByComparingTo("5");
    }

    private KnowledgeCard card() {
        KnowledgeCard card = new KnowledgeCard();
        card.setId(2L);
        card.setCategory("JAVA");
        card.setTitle("HashMap 底层结构");
        card.setQuestion("HashMap 如何扩容？");
        card.setKeyPoints("数组\n链表\n红黑树\n扩容");
        return card;
    }

    private SelfTestSubmitRequest request(int score) {
        SelfTestSubmitRequest request = new SelfTestSubmitRequest();
        request.setUserAnswer("HashMap uses array and linked list.");
        request.setScore(score);
        request.setFeedback("回答覆盖了部分关键点。");
        request.setMissingKeyPoints(java.util.List.of("红黑树", "扩容"));
        return request;
    }
}
