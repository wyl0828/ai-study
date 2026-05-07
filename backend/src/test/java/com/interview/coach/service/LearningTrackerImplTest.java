package com.interview.coach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.AiDiagnosisResult;
import com.interview.coach.dto.HintGenerationResult;
import com.interview.coach.entity.UserWeakness;
import com.interview.coach.mapper.AiDiagnosisMapper;
import com.interview.coach.mapper.HintRecordMapper;
import com.interview.coach.mapper.MistakeCardMapper;
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
}
