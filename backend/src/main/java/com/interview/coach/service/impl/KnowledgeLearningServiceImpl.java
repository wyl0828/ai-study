package com.interview.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.coach.dto.SelfTestSubmitRequest;
import com.interview.coach.entity.KnowledgeCard;
import com.interview.coach.entity.SelfTestRecord;
import com.interview.coach.entity.UserKnowledgeCardMastery;
import com.interview.coach.entity.UserWeaknessEvent;
import com.interview.coach.handler.BusinessException;
import com.interview.coach.mapper.KnowledgeCardMapper;
import com.interview.coach.mapper.SelfTestRecordMapper;
import com.interview.coach.mapper.UserKnowledgeCardMasteryMapper;
import com.interview.coach.mapper.UserWeaknessEventMapper;
import com.interview.coach.service.KnowledgeLearningService;
import com.interview.coach.vo.SelfTestRecordVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class KnowledgeLearningServiceImpl implements KnowledgeLearningService {

    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;

    private static final BigDecimal MAX_SCORE = new BigDecimal("100");

    private final KnowledgeCardMapper knowledgeCardMapper;

    private final SelfTestRecordMapper selfTestRecordMapper;

    private final UserKnowledgeCardMasteryMapper masteryMapper;

    private final UserWeaknessEventMapper weaknessEventMapper;

    @Override
    @Transactional
    public SelfTestRecordVO submitSelfTest(Long userId, Long cardId, SelfTestSubmitRequest request) {
        KnowledgeCard card = knowledgeCardMapper.selectById(cardId);
        if (card == null || Boolean.FALSE.equals(card.getEnabled())) {
            throw new BusinessException(404, "knowledge card not found");
        }
        LocalDateTime now = LocalDateTime.now();
        SelfTestRecord record = new SelfTestRecord();
        record.setUserId(userId);
        record.setKnowledgeCardId(cardId);
        record.setQuestionSnapshot(card.getQuestion());
        record.setUserAnswer(request.getUserAnswer());
        record.setScore(request.getScore());
        record.setFeedback(request.getFeedback());
        record.setMissingKeyPoints(join(request.getMissingKeyPoints()));
        record.setCreatedAt(now);
        selfTestRecordMapper.insert(record);

        updateMastery(userId, cardId, request.getScore(), now);
        if (request.getScore() < 60) {
            insertLowScoreWeaknessEvent(userId, card, record.getId(), request, now);
        }
        return toVO(record);
    }

    @Override
    public List<SelfTestRecordVO> getRecentSelfTests(Long userId, Long cardId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        return selfTestRecordMapper.selectList(new LambdaQueryWrapper<SelfTestRecord>()
                .eq(SelfTestRecord::getUserId, userId)
                .eq(SelfTestRecord::getKnowledgeCardId, cardId)
                .orderByDesc(SelfTestRecord::getCreatedAt)
                .last("LIMIT " + safeLimit))
                .stream()
                .map(this::toVO)
                .toList();
    }

    private void updateMastery(Long userId, Long cardId, Integer score, LocalDateTime now) {
        UserKnowledgeCardMastery mastery = masteryMapper.selectOne(new LambdaQueryWrapper<UserKnowledgeCardMastery>()
                .eq(UserKnowledgeCardMastery::getUserId, userId)
                .eq(UserKnowledgeCardMastery::getKnowledgeCardId, cardId));
        if (mastery == null) {
            mastery = new UserKnowledgeCardMastery();
            mastery.setUserId(userId);
            mastery.setKnowledgeCardId(cardId);
            mastery.setMasteryScore(BigDecimal.ZERO);
            mastery.setSelfTestCount(0);
            mastery.setCreatedAt(now);
        }
        mastery.setMasteryScore(cap(mastery.getMasteryScore().add(masteryDelta(score))));
        mastery.setSelfTestCount((mastery.getSelfTestCount() == null ? 0 : mastery.getSelfTestCount()) + 1);
        mastery.setLastScore(score);
        mastery.setLastPracticedAt(now);
        mastery.setUpdatedAt(now);
        if (mastery.getId() == null) {
            masteryMapper.insert(mastery);
        } else {
            masteryMapper.updateById(mastery);
        }
    }

    private BigDecimal masteryDelta(Integer score) {
        if (score >= 80) {
            return new BigDecimal("10");
        }
        if (score >= 60) {
            return new BigDecimal("5");
        }
        return new BigDecimal("-5");
    }

    private BigDecimal cap(BigDecimal value) {
        if (value.compareTo(MIN_SCORE) < 0) {
            return MIN_SCORE;
        }
        if (value.compareTo(MAX_SCORE) > 0) {
            return MAX_SCORE;
        }
        return value;
    }

    private void insertLowScoreWeaknessEvent(Long userId, KnowledgeCard card, Long sourceId,
            SelfTestSubmitRequest request, LocalDateTime now) {
        UserWeaknessEvent event = new UserWeaknessEvent();
        event.setUserId(userId);
        event.setKnowledgePoint(knowledgePoint(card));
        event.setErrorType("SELF_TEST_LOW_SCORE");
        event.setSourceType("SELF_TEST");
        event.setSourceId(sourceId);
        event.setDeltaScore(new BigDecimal("5"));
        event.setBeforeScore(BigDecimal.ZERO);
        event.setAfterScore(new BigDecimal("5"));
        event.setReason(request.getFeedback());
        event.setCreatedAt(now);
        weaknessEventMapper.insert(event);
    }

    private String knowledgePoint(KnowledgeCard card) {
        if ("JAVA".equalsIgnoreCase(card.getCategory())) {
            return "Java 集合";
        }
        return StringUtils.hasText(card.getTitle()) ? card.getTitle() : card.getCategory();
    }

    private SelfTestRecordVO toVO(SelfTestRecord record) {
        SelfTestRecordVO vo = new SelfTestRecordVO();
        vo.setId(record.getId());
        vo.setKnowledgeCardId(record.getKnowledgeCardId());
        vo.setScore(record.getScore());
        vo.setFeedback(record.getFeedback());
        vo.setMissingKeyPoints(split(record.getMissingKeyPoints()));
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join("\n", values);
    }

    private List<String> split(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
