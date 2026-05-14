package com.interview.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.AiDiagnosisResult;
import com.interview.coach.dto.HintGenerationResult;
import com.interview.coach.entity.AiDiagnosis;
import com.interview.coach.entity.HintRecord;
import com.interview.coach.entity.MistakeCard;
import com.interview.coach.entity.UserWeakness;
import com.interview.coach.entity.UserWeaknessEvent;
import com.interview.coach.enums.ErrorTypeEnum;
import com.interview.coach.enums.HintLevelEnum;
import com.interview.coach.mapper.AiDiagnosisMapper;
import com.interview.coach.mapper.HintRecordMapper;
import com.interview.coach.mapper.MistakeCardMapper;
import com.interview.coach.mapper.UserWeaknessEventMapper;
import com.interview.coach.mapper.UserWeaknessMapper;
import com.interview.coach.service.LearningTracker;
import com.interview.coach.service.RagService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j

@Service
@RequiredArgsConstructor
public class LearningTrackerImpl implements LearningTracker {

    private static final BigDecimal DEFAULT_SCORE_DELTA = new BigDecimal("5");

    private static final BigDecimal MAX_SCORE = new BigDecimal("100");

    private final AiDiagnosisMapper aiDiagnosisMapper;

    private final HintRecordMapper hintRecordMapper;

    private final UserWeaknessMapper userWeaknessMapper;

    private final UserWeaknessEventMapper userWeaknessEventMapper;

    private final MistakeCardMapper mistakeCardMapper;

    private final RagService ragService;

    @Override
    @Transactional
    public void recordDiagnosis(AgentContext context) {
        LocalDateTime now = LocalDateTime.now();
        AiDiagnosis diagnosis = insertDiagnosis(context, now);
        insertHints(context, now);
        upsertWeakness(context, now);
        MistakeCard mistakeCard = insertMistakeCard(context, now);
        indexLearningMemory(context, diagnosis, mistakeCard);
    }

    private AiDiagnosis insertDiagnosis(AgentContext context, LocalDateTime now) {
        AiDiagnosisResult result = context.getDiagnosis();
        AiDiagnosis diagnosis = new AiDiagnosis();
        diagnosis.setAgentRunId(context.getAgentRunId());
        diagnosis.setSubmissionId(context.getSubmissionId());
        diagnosis.setUserId(context.getUserId());
        diagnosis.setProblemId(context.getProblemId());
        diagnosis.setErrorType(result.getErrorType());
        diagnosis.setKnowledgePoint(result.getKnowledgePoint());
        diagnosis.setSpecificError(result.getSpecificError());
        diagnosis.setDiagnosis(result.getDiagnosis());
        diagnosis.setSuggestion(result.getSuggestion());
        diagnosis.setConfidence(result.getConfidence() == null ? BigDecimal.ZERO : result.getConfidence());
        diagnosis.setCreatedAt(now);
        aiDiagnosisMapper.insert(diagnosis);
        return diagnosis;
    }

    private void insertHints(AgentContext context, LocalDateTime now) {
        HintGenerationResult hints = context.getHints();
        if (hints == null) {
            log.info("Skip insert hints because hint generation is disabled");
            return;
        }
        insertHint(context, HintLevelEnum.LEVEL_1, hints.getHintLevel1(), now);
        insertHint(context, HintLevelEnum.LEVEL_2, hints.getHintLevel2(), now);
        insertHint(context, HintLevelEnum.LEVEL_3, hints.getHintLevel3(), now);
    }

    private void insertHint(AgentContext context, HintLevelEnum level, String content, LocalDateTime now) {
        HintRecord record = new HintRecord();
        record.setAgentRunId(context.getAgentRunId());
        record.setSubmissionId(context.getSubmissionId());
        record.setUserId(context.getUserId());
        record.setProblemId(context.getProblemId());
        record.setHintLevel(level.getValue());
        record.setHintContent(content);
        record.setCreatedAt(now);
        hintRecordMapper.insert(record);
    }

    private void upsertWeakness(AgentContext context, LocalDateTime now) {
        AiDiagnosisResult diagnosis = context.getDiagnosis();
        if (isAcceptedReview(diagnosis)) {
            return;
        }
        UserWeakness weakness = userWeaknessMapper.selectOne(new LambdaQueryWrapper<UserWeakness>()
                .eq(UserWeakness::getUserId, context.getUserId())
                .eq(UserWeakness::getKnowledgePoint, diagnosis.getKnowledgePoint())
                .eq(UserWeakness::getErrorType, diagnosis.getErrorType()));
        if (weakness == null) {
            weakness = new UserWeakness();
            weakness.setUserId(context.getUserId());
            weakness.setKnowledgePoint(diagnosis.getKnowledgePoint());
            weakness.setErrorType(diagnosis.getErrorType());
            weakness.setWrongCount(0);
            weakness.setSubmitCount(0);
            weakness.setWeaknessScore(BigDecimal.ZERO);
            weakness.setCreatedAt(now);
        }

        BigDecimal beforeScore = weakness.getWeaknessScore();
        BigDecimal deltaScore = scoreDelta(diagnosis);
        weakness.setWrongCount(weakness.getWrongCount() + 1);
        weakness.setLastWrongAt(now);
        weakness.setSubmitCount(weakness.getSubmitCount() + 1);
        weakness.setWeaknessScore(capScore(weakness.getWeaknessScore().add(deltaScore)));
        weakness.setUpdatedAt(now);

        if (weakness.getId() == null) {
            userWeaknessMapper.insert(weakness);
        } else {
            userWeaknessMapper.updateById(weakness);
        }
        insertWeaknessEvent(context, diagnosis, beforeScore, weakness.getWeaknessScore(), deltaScore, now);
    }

    private boolean isAcceptedReview(AiDiagnosisResult diagnosis) {
        return ErrorTypeEnum.ACCEPTED_REVIEW.name().equals(diagnosis.getErrorType());
    }

    private void insertWeaknessEvent(AgentContext context, AiDiagnosisResult diagnosis,
            BigDecimal beforeScore, BigDecimal afterScore, BigDecimal deltaScore, LocalDateTime now) {
        UserWeaknessEvent event = new UserWeaknessEvent();
        event.setUserId(context.getUserId());
        event.setKnowledgePoint(diagnosis.getKnowledgePoint());
        event.setErrorType(diagnosis.getErrorType());
        event.setSourceType("SUBMISSION_FAILED");
        event.setSourceId(context.getSubmissionId());
        event.setDeltaScore(deltaScore);
        event.setBeforeScore(beforeScore);
        event.setAfterScore(afterScore);
        event.setReason(diagnosis.getSpecificError());
        event.setCreatedAt(now);
        userWeaknessEventMapper.insert(event);
    }

    private BigDecimal scoreDelta(AiDiagnosisResult diagnosis) {
        if (diagnosis.getWeaknessScoreDelta() == null
                || diagnosis.getWeaknessScoreDelta().compareTo(BigDecimal.ZERO) <= 0) {
            return DEFAULT_SCORE_DELTA;
        }
        return diagnosis.getWeaknessScoreDelta();
    }

    private BigDecimal capScore(BigDecimal score) {
        return score.compareTo(MAX_SCORE) > 0 ? MAX_SCORE : score;
    }

    private MistakeCard insertMistakeCard(AgentContext context, LocalDateTime now) {
        AiDiagnosisResult diagnosis = context.getDiagnosis();
        if (isAcceptedReview(diagnosis)) {
            return null;
        }
        String fingerprint = fingerprint(context.getUserId(), diagnosis);
        MistakeCard existing = mistakeCardMapper.selectOne(new LambdaQueryWrapper<MistakeCard>()
                .eq(MistakeCard::getUserId, context.getUserId())
                .eq(MistakeCard::getFingerprint, fingerprint)
                .eq(MistakeCard::getStatus, "OPEN")
                .last("LIMIT 1"));
        if (existing != null) {
            existing.setProblemId(context.getProblemId());
            existing.setSubmissionId(context.getSubmissionId());
            existing.setAgentRunId(context.getAgentRunId());
            existing.setMistakeSummary(diagnosis.getSpecificError());
            existing.setCorrectIdea(diagnosis.getSuggestion());
            existing.setRepeatCount((existing.getRepeatCount() == null ? 1 : existing.getRepeatCount()) + 1);
            existing.setLastSeenAt(now);
            mistakeCardMapper.updateById(existing);
            return existing;
        }
        MistakeCard card = new MistakeCard();
        card.setUserId(context.getUserId());
        card.setProblemId(context.getProblemId());
        card.setSubmissionId(context.getSubmissionId());
        card.setAgentRunId(context.getAgentRunId());
        card.setErrorType(diagnosis.getErrorType());
        card.setKnowledgePoint(diagnosis.getKnowledgePoint());
        card.setMistakeSummary(diagnosis.getSpecificError());
        card.setCorrectIdea(diagnosis.getSuggestion());
        card.setFingerprint(fingerprint);
        card.setRepeatCount(1);
        card.setLastSeenAt(now);
        card.setStatus("OPEN");
        card.setCreatedAt(now);
        mistakeCardMapper.insert(card);
        return card;
    }

    private void indexLearningMemory(AgentContext context, AiDiagnosis diagnosis, MistakeCard mistakeCard) {
        try {
            ragService.indexLearningMemory(context, diagnosis, mistakeCard);
        } catch (Exception ex) {
            log.warn("Skip RAG learning memory index because indexing failed: {}", ex.getMessage());
        }
    }

    private String fingerprint(Long userId, AiDiagnosisResult diagnosis) {
        return "%s|%s|%s|%s".formatted(
                userId,
                normalize(diagnosis.getKnowledgePoint()),
                normalize(diagnosis.getErrorType()),
                normalize(diagnosis.getSpecificError()));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
