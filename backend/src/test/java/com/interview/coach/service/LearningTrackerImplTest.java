package com.interview.coach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.AiDiagnosisResult;
import com.interview.coach.dto.HintGenerationResult;
import com.interview.coach.entity.MistakeCard;
import com.interview.coach.entity.UserWeaknessEvent;
import com.interview.coach.entity.UserWeakness;
import com.interview.coach.mapper.AiDiagnosisMapper;
import com.interview.coach.mapper.HintRecordMapper;
import com.interview.coach.mapper.MistakeCardMapper;
import com.interview.coach.mapper.UserWeaknessEventMapper;
import com.interview.coach.mapper.UserWeaknessMapper;
import com.interview.coach.service.impl.LearningTrackerImpl;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LearningTrackerImplTest {

    @Mock
    private AiDiagnosisMapper aiDiagnosisMapper;

    @Mock
    private HintRecordMapper hintRecordMapper;

    @Mock
    private UserWeaknessMapper userWeaknessMapper;

    @Mock
    private UserWeaknessEventMapper userWeaknessEventMapper;

    @Mock
    private MistakeCardMapper mistakeCardMapper;

    @InjectMocks
    private LearningTrackerImpl learningTracker;

    @Test
    void recordDiagnosisCapsWeaknessScoreAtOneHundred() {
        UserWeakness existing = new UserWeakness();
        existing.setId(5L);
        existing.setUserId(1L);
        existing.setKnowledgePoint("HashMap");
        existing.setErrorType("BOUNDARY_ERROR");
        existing.setWrongCount(3);
        existing.setSubmitCount(4);
        existing.setWeaknessScore(new BigDecimal("98"));
        when(userWeaknessMapper.selectOne(any())).thenReturn(existing);

        AgentContext context = new AgentContext();
        context.setAgentRunId(10L);
        context.setSubmissionId(11L);
        context.setUserId(1L);
        context.setProblemId(2L);
        AiDiagnosisResult diagnosis = new AiDiagnosisResult();
        diagnosis.setErrorType("BOUNDARY_ERROR");
        diagnosis.setKnowledgePoint("HashMap");
        diagnosis.setSpecificError("duplicate value case is missed");
        diagnosis.setDiagnosis("The implementation overwrites indexes too early.");
        diagnosis.setSuggestion("Check before storing the current value.");
        diagnosis.setConfidence(new BigDecimal("0.80"));
        diagnosis.setWeaknessScoreDelta(new BigDecimal("8"));
        context.setDiagnosis(diagnosis);
        HintGenerationResult hints = new HintGenerationResult();
        hints.setHintLevel1("Think about duplicate values.");
        hints.setHintLevel2("HashMap lookup order matters.");
        hints.setHintLevel3("Check complement, then store current value.");
        context.setHints(hints);

        learningTracker.recordDiagnosis(context);

        ArgumentCaptor<UserWeakness> captor = ArgumentCaptor.forClass(UserWeakness.class);
        verify(userWeaknessMapper).updateById(captor.capture());
        assertThat(captor.getValue().getWeaknessScore()).isEqualByComparingTo("100");
        assertThat(captor.getValue().getWrongCount()).isEqualTo(4);
        assertThat(captor.getValue().getSubmitCount()).isEqualTo(5);
    }

    @Test
    void recordDiagnosisUsesDefaultScoreDeltaWhenModelReturnsNegativeDelta() {
        UserWeakness existing = new UserWeakness();
        existing.setId(6L);
        existing.setUserId(1L);
        existing.setKnowledgePoint("HashMap");
        existing.setErrorType("LOGIC_ERROR");
        existing.setWrongCount(0);
        existing.setSubmitCount(0);
        existing.setWeaknessScore(BigDecimal.ZERO);
        when(userWeaknessMapper.selectOne(any())).thenReturn(existing);

        AgentContext context = new AgentContext();
        context.setAgentRunId(12L);
        context.setSubmissionId(13L);
        context.setUserId(1L);
        context.setProblemId(2L);
        AiDiagnosisResult diagnosis = new AiDiagnosisResult();
        diagnosis.setErrorType("LOGIC_ERROR");
        diagnosis.setKnowledgePoint("HashMap");
        diagnosis.setSpecificError("self-pairing");
        diagnosis.setDiagnosis("Current index is reused.");
        diagnosis.setSuggestion("Check complement before inserting.");
        diagnosis.setConfidence(new BigDecimal("0.90"));
        diagnosis.setWeaknessScoreDelta(new BigDecimal("-1"));
        context.setDiagnosis(diagnosis);
        HintGenerationResult hints = new HintGenerationResult();
        hints.setHintLevel1("Check loop order.");
        hints.setHintLevel2("HashMap insertion order matters.");
        hints.setHintLevel3("Check complement before insert.");
        context.setHints(hints);

        learningTracker.recordDiagnosis(context);

        ArgumentCaptor<UserWeakness> captor = ArgumentCaptor.forClass(UserWeakness.class);
        verify(userWeaknessMapper).updateById(captor.capture());
        assertThat(captor.getValue().getWeaknessScore()).isEqualByComparingTo("5");
    }

    @Test
    void recordDiagnosisWritesWeaknessEventWithScoreChange() {
        UserWeakness existing = new UserWeakness();
        existing.setId(7L);
        existing.setUserId(1L);
        existing.setKnowledgePoint("HashMap");
        existing.setErrorType("LOGIC_ERROR");
        existing.setWrongCount(1);
        existing.setSubmitCount(1);
        existing.setWeaknessScore(new BigDecimal("10"));
        when(userWeaknessMapper.selectOne(any())).thenReturn(existing);

        AgentContext context = context("LOGIC_ERROR", "HashMap", "self-pairing");

        learningTracker.recordDiagnosis(context);

        ArgumentCaptor<UserWeaknessEvent> captor = ArgumentCaptor.forClass(UserWeaknessEvent.class);
        verify(userWeaknessEventMapper).insert(captor.capture());
        assertThat(captor.getValue().getSourceType()).isEqualTo("SUBMISSION_FAILED");
        assertThat(captor.getValue().getSourceId()).isEqualTo(11L);
        assertThat(captor.getValue().getBeforeScore()).isEqualByComparingTo("10");
        assertThat(captor.getValue().getAfterScore()).isEqualByComparingTo("15");
        assertThat(captor.getValue().getDeltaScore()).isEqualByComparingTo("5");
    }

    @Test
    void recordDiagnosisUpdatesExistingOpenMistakeCardWhenFingerprintRepeats() {
        UserWeakness existing = new UserWeakness();
        existing.setId(8L);
        existing.setUserId(1L);
        existing.setKnowledgePoint("HashMap");
        existing.setErrorType("LOGIC_ERROR");
        existing.setWrongCount(1);
        existing.setSubmitCount(1);
        existing.setWeaknessScore(BigDecimal.ZERO);
        MistakeCard existingCard = new MistakeCard();
        existingCard.setId(99L);
        existingCard.setUserId(1L);
        existingCard.setProblemId(2L);
        existingCard.setSubmissionId(10L);
        existingCard.setAgentRunId(9L);
        existingCard.setErrorType("LOGIC_ERROR");
        existingCard.setKnowledgePoint("HashMap");
        existingCard.setMistakeSummary("self pairing");
        existingCard.setCorrectIdea("Check complement before insert.");
        existingCard.setFingerprint("1|hashmap|logic_error|self pairing");
        existingCard.setRepeatCount(2);
        existingCard.setStatus("OPEN");
        when(userWeaknessMapper.selectOne(any())).thenReturn(existing);
        when(mistakeCardMapper.selectOne(any())).thenReturn(existingCard);

        AgentContext context = context("LOGIC_ERROR", "HashMap", "self pairing");

        learningTracker.recordDiagnosis(context);

        ArgumentCaptor<MistakeCard> captor = ArgumentCaptor.forClass(MistakeCard.class);
        verify(mistakeCardMapper).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(99L);
        assertThat(captor.getValue().getRepeatCount()).isEqualTo(3);
        assertThat(captor.getValue().getSubmissionId()).isEqualTo(11L);
        assertThat(captor.getValue().getAgentRunId()).isEqualTo(10L);
    }

    private AgentContext context(String errorType, String knowledgePoint, String specificError) {
        AgentContext context = new AgentContext();
        context.setAgentRunId(10L);
        context.setSubmissionId(11L);
        context.setUserId(1L);
        context.setProblemId(2L);
        AiDiagnosisResult diagnosis = new AiDiagnosisResult();
        diagnosis.setErrorType(errorType);
        diagnosis.setKnowledgePoint(knowledgePoint);
        diagnosis.setSpecificError(specificError);
        diagnosis.setDiagnosis("Current index is reused.");
        diagnosis.setSuggestion("Check complement before inserting.");
        diagnosis.setConfidence(new BigDecimal("0.90"));
        context.setDiagnosis(diagnosis);
        return context;
    }
}
