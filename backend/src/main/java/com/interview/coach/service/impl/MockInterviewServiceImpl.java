package com.interview.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.MockInterviewAiEvaluationResponse;
import com.interview.coach.dto.MockInterviewAnswerRequest;
import com.interview.coach.dto.MockInterviewCreateRequest;
import com.interview.coach.dto.TrainingPlanResult;
import com.interview.coach.dto.TrainingPlanResult.TrainingPlanItemResult;
import com.interview.coach.entity.KnowledgeCard;
import com.interview.coach.entity.MockInterviewReport;
import com.interview.coach.entity.MockInterviewSession;
import com.interview.coach.entity.MockInterviewTurn;
import com.interview.coach.entity.UserWeaknessEvent;
import com.interview.coach.enums.MockInterviewSessionStatusEnum;
import com.interview.coach.enums.MockInterviewTurnTypeEnum;
import com.interview.coach.handler.BusinessException;
import com.interview.coach.integration.ai.AnthropicCompatibleClient;
import com.interview.coach.mapper.KnowledgeCardMapper;
import com.interview.coach.mapper.MockInterviewReportMapper;
import com.interview.coach.mapper.MockInterviewSessionMapper;
import com.interview.coach.mapper.MockInterviewTurnMapper;
import com.interview.coach.mapper.UserWeaknessEventMapper;
import com.interview.coach.service.MockInterviewService;
import com.interview.coach.service.TrainingPlanService;
import com.interview.coach.vo.MockInterviewReportVO;
import com.interview.coach.vo.MockInterviewSessionVO;
import com.interview.coach.vo.MockInterviewTurnVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MockInterviewServiceImpl implements MockInterviewService {

    private static final int DEFAULT_QUESTION_COUNT = 3;

    private static final int MAX_QUESTION_COUNT = 5;

    private static final int LOW_SCORE_THRESHOLD = 60;

    private static final int MAX_TURN_SCORE = 95;

    private static final int MAX_FOLLOW_UP_PER_MAIN = 2;

    private static final String DEFAULT_INTERVIEWER_STYLE = "BIG_TECH";

    private final KnowledgeCardMapper knowledgeCardMapper;

    private final MockInterviewSessionMapper sessionMapper;

    private final MockInterviewTurnMapper turnMapper;

    private final MockInterviewReportMapper reportMapper;

    private final UserWeaknessEventMapper weaknessEventMapper;

    private final AnthropicCompatibleClient aiClient;

    private final TrainingPlanService trainingPlanService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public MockInterviewSessionVO create(MockInterviewCreateRequest request) {
        Long userId = request.getUserId();
        if (userId == null) {
            throw new BusinessException("userId is required");
        }
        String category = normalizeCategory(request.getCategory());
        KnowledgeCard firstCard = firstCard(category);
        LocalDateTime now = LocalDateTime.now();

        MockInterviewSession session = new MockInterviewSession();
        session.setUserId(userId);
        session.setCategory(category);
        session.setStatus(MockInterviewSessionStatusEnum.ASKING_MAIN.name());
        session.setInterviewerStyle(normalizeInterviewerStyle(request.getInterviewerStyle()));
        session.setQuestionCount(safeQuestionCount(request.getQuestionCount()));
        session.setAnsweredMainCount(0);
        session.setCurrentKnowledgeCardId(firstCard.getId());
        session.setStartedAt(now);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        sessionMapper.insert(session);

        return buildSessionVO(session, List.of(), null, firstCard);
    }

    @Override
    public MockInterviewSessionVO getSession(Long sessionId) {
        MockInterviewSession session = requireSession(sessionId);
        KnowledgeCard currentCard = currentCard(session);
        List<MockInterviewTurn> turns = turns(sessionId);
        MockInterviewReport report = report(sessionId);
        return buildSessionVO(session, turns, report, currentCard);
    }

    @Override
    @Transactional
    public MockInterviewSessionVO answer(Long sessionId, MockInterviewAnswerRequest request) {
        if (request == null || !StringUtils.hasText(request.getUserAnswer())) {
            throw new BusinessException("userAnswer is required");
        }
        MockInterviewSession session = requireSession(sessionId);
        MockInterviewSessionStatusEnum status = status(session);
        if (status != MockInterviewSessionStatusEnum.ASKING_MAIN
                && status != MockInterviewSessionStatusEnum.ASKING_FOLLOW_UP) {
            throw new BusinessException("cannot answer in status " + session.getStatus());
        }

        KnowledgeCard card = currentCard(session);
        List<MockInterviewTurn> existingTurns = turns(sessionId);
        MockInterviewTurnTypeEnum turnType = status == MockInterviewSessionStatusEnum.ASKING_MAIN
                ? MockInterviewTurnTypeEnum.MAIN
                : MockInterviewTurnTypeEnum.FOLLOW_UP;
        MockInterviewTurn parentTurn = turnType == MockInterviewTurnTypeEnum.FOLLOW_UP
                ? latestMainTurn(existingTurns)
                : null;
        String question = questionFor(session, card, parentTurn);
        MockInterviewAiEvaluationResponse evaluation = evaluate(session, card, question, request.getUserAnswer());
        DisplayFeedback displayFeedback = displayFeedback(session, card, turnType, evaluation, request.getUserAnswer());
        LocalDateTime now = LocalDateTime.now();

        MockInterviewTurn turn = new MockInterviewTurn();
        turn.setSessionId(sessionId);
        turn.setKnowledgeCardId(card.getId());
        turn.setTurnOrder(existingTurns.size() + 1);
        turn.setTurnType(turnType.name());
        turn.setParentTurnId(parentTurn == null ? null : parentTurn.getId());
        turn.setQuestion(question);
        turn.setUserAnswer(request.getUserAnswer());
        turn.setScore(safeScore(evaluation.getScore()));
        turn.setFeedback(displayFeedback.feedback());
        turn.setPerformanceLevel(displayFeedback.performanceLevel());
        turn.setStrengthSummary(displayFeedback.strengthSummary());
        turn.setGapSummary(displayFeedback.gapSummary());
        turn.setExpressionFeedback(displayFeedback.expressionFeedback());
        turn.setInterviewerObservation(displayFeedback.interviewerObservation());
        turn.setFollowUpReason(displayFeedback.followUpReason());
        turn.setHitKeyPoints(join(evaluation.getHitKeyPoints()));
        turn.setMissingKeyPoints(join(evaluation.getMissingKeyPoints()));
        turn.setExpressionIssue(defaultText(evaluation.getExpressionIssue(), "表达可以继续补充结构和边界。"));
        turn.setAiRawJson(toJson(evaluation));
        turn.setCreatedAt(now);
        turn.setUpdatedAt(now);
        turnMapper.insert(turn);

        if (shouldWriteWeakness(turn, evaluation)) {
            insertWeaknessEvent(session, card, turn, evaluation, now);
        }

        updateSessionAfterAnswer(session, card, turnType, evaluation, existingTurns, now);
        sessionMapper.updateById(session);

        List<MockInterviewTurn> resultTurns = turns(sessionId);
        return buildSessionVO(session, resultTurns, report(sessionId), currentCard(session));
    }

    @Override
    @Transactional
    public MockInterviewSessionVO finish(Long sessionId) {
        MockInterviewSession session = requireSession(sessionId);
        MockInterviewSessionStatusEnum status = status(session);
        if (status == MockInterviewSessionStatusEnum.ASKING_MAIN
                || status == MockInterviewSessionStatusEnum.ASKING_FOLLOW_UP) {
            session.setStatus(MockInterviewSessionStatusEnum.FINISHED.name());
            session.setFinishedAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(session);
        } else if (status != MockInterviewSessionStatusEnum.FINISHED && status != MockInterviewSessionStatusEnum.REPORTED) {
            throw new BusinessException("cannot finish in status " + session.getStatus());
        }
        MockInterviewReport existing = report(sessionId);
        if (existing != null) {
            session.setStatus(MockInterviewSessionStatusEnum.REPORTED.name());
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(session);
            return buildSessionVO(session, turns(sessionId), existing, currentCard(session));
        }

        List<MockInterviewTurn> turns = turns(sessionId);
        MockInterviewReport report = buildReport(session, turns);
        reportMapper.insert(report);
        saveTrainingPlan(session, report, turns);
        session.setStatus(MockInterviewSessionStatusEnum.REPORTED.name());
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
        return buildSessionVO(session, turns, report, currentCard(session));
    }

    private void saveTrainingPlan(MockInterviewSession session, MockInterviewReport report,
            List<MockInterviewTurn> turns) {
        TrainingPlanResult result = buildTrainingPlanResult(report, turns);
        if (result.getItems().isEmpty()) {
            return;
        }
        AgentContext context = new AgentContext();
        context.setUserId(session.getUserId());
        try {
            trainingPlanService.savePlan(context, result);
        } catch (RuntimeException ignored) {
            // Training-plan persistence should not block report generation.
        }
    }

    private TrainingPlanResult buildTrainingPlanResult(MockInterviewReport report, List<MockInterviewTurn> turns) {
        TrainingPlanResult result = new TrainingPlanResult();
        result.setTitle("模拟面试复盘训练");
        result.setSummary("本次模拟面试平均分 " + report.getAverageScore() + "，优先复盘面试中暴露的知识卡和表达缺口。");

        List<Long> recommendedIds = splitLongs(report.getRecommendedCardIds());
        Set<String> weaknessTags = new LinkedHashSet<>(splitComma(report.getWeaknessTags()));
        for (int index = 0; index < recommendedIds.size(); index++) {
            Long cardId = recommendedIds.get(index);
            TrainingPlanItemResult item = new TrainingPlanItemResult();
            item.setItemType("KNOWLEDGE_CARD");
            item.setKnowledgeCardId(cardId);
            item.setDayIndex(index + 1);
            item.setKnowledgePoint(firstWeaknessTag(weaknessTags));
            item.setKnowledgeCardTitle(cardTitle(cardId, turns));
            item.setReason("来自模拟面试报告的推荐复盘项。");
            item.setReviewFocus(reviewFocus(weaknessTags));
            item.setSourceType("MOCK_INTERVIEW_REPORT");
            item.setSourceId(report.getId());
            item.setSourceSummary("来自模拟面试报告 #" + report.getId() + " 的推荐复盘项。");
            result.getItems().add(item);
        }
        return result;
    }

    private String cardTitle(Long cardId, List<MockInterviewTurn> turns) {
        return turns.stream()
                .filter(turn -> Objects.equals(turn.getKnowledgeCardId(), cardId))
                .map(MockInterviewTurn::getQuestion)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("模拟面试知识卡片复盘");
    }

    private String firstWeaknessTag(Set<String> weaknessTags) {
        return weaknessTags.stream().findFirst().orElse("模拟面试表达复盘");
    }

    private String reviewFocus(Set<String> weaknessTags) {
        if (weaknessTags.isEmpty()) {
            return "复盘本次面试问答，整理成“定义 -> 流程 -> 边界 -> 场景”的 1 分钟回答。";
        }
        return "重点补齐：" + joinNatural(weaknessTags.stream().toList(), 3);
    }

    private void updateSessionAfterAnswer(MockInterviewSession session, KnowledgeCard card,
            MockInterviewTurnTypeEnum turnType, MockInterviewAiEvaluationResponse evaluation,
            List<MockInterviewTurn> existingTurns, LocalDateTime now) {
        if (turnType == MockInterviewTurnTypeEnum.MAIN) {
            session.setAnsweredMainCount(nullToZero(session.getAnsweredMainCount()) + 1);
            session.setStatus(MockInterviewSessionStatusEnum.ASKING_FOLLOW_UP.name());
        } else {
            int followUpCountAfterAnswer = followUpCount(existingTurns, card.getId()) + 1;
            if (followUpCountAfterAnswer < MAX_FOLLOW_UP_PER_MAIN && shouldContinueFollowUp(evaluation)) {
                session.setStatus(MockInterviewSessionStatusEnum.ASKING_FOLLOW_UP.name());
            } else if (nullToZero(session.getAnsweredMainCount()) >= nullToZero(session.getQuestionCount())) {
                session.setStatus(MockInterviewSessionStatusEnum.FINISHED.name());
                session.setFinishedAt(now);
            } else {
                KnowledgeCard next = nextCard(session.getCategory(), session.getAnsweredMainCount(), card.getId());
                if (next == null) {
                    session.setStatus(MockInterviewSessionStatusEnum.FINISHED.name());
                    session.setFinishedAt(now);
                } else {
                    session.setStatus(MockInterviewSessionStatusEnum.ASKING_MAIN.name());
                    session.setCurrentKnowledgeCardId(next.getId());
                }
            }
        }
        session.setUpdatedAt(now);
    }

    private boolean shouldContinueFollowUp(MockInterviewAiEvaluationResponse evaluation) {
        return evaluation.getMissingKeyPoints() != null
                && !evaluation.getMissingKeyPoints().isEmpty()
                && StringUtils.hasText(evaluation.getFollowUpQuestion());
    }

    private MockInterviewAiEvaluationResponse evaluate(MockInterviewSession session, KnowledgeCard card, String question,
            String userAnswer) {
        if (isForgetfulAnswer(userAnswer)) {
            return forgetfulEvaluation(card);
        }
        try {
            MockInterviewAiEvaluationResponse response = aiClient.askJson(
                    systemPrompt(),
                    userPrompt(session, card, question, userAnswer),
                    MockInterviewAiEvaluationResponse.class);
            normalizeEvaluation(response, card);
            calibrateEvaluation(response, card, userAnswer);
            return response;
        } catch (RuntimeException ex) {
            return fallbackEvaluation(card, userAnswer);
        }
    }

    private MockInterviewAiEvaluationResponse forgetfulEvaluation(KnowledgeCard card) {
        List<String> missing = forgetfulMissingPoints(card);
        MockInterviewAiEvaluationResponse response = new MockInterviewAiEvaluationResponse();
        response.setScore(6);
        response.setHitKeyPoints(List.of());
        response.setMissingKeyPoints(missing);
        response.setFeedback("你目前只表达了“不会/忘了”，还没有说出当前问题的核心阶段。");
        response.setExpressionIssue("先不要空着，可以先复述一个最小可回答点。");
        response.setFollowUpQuestion(forgetfulFollowUpQuestion(card));
        response.setWeaknessTags(missing);
        response.setRecommendedCardIds(List.of(card.getId()));
        return response;
    }

    private List<String> forgetfulMissingPoints(KnowledgeCard card) {
        if (isSpringBeanLifecycle(card)) {
            return List.of(
                    "实例化和属性填充的区别",
                    "BeanPostProcessor 的前后置扩展点",
                    "AOP 代理通常发生在初始化后置处理阶段",
                    "singleton 和 prototype 销毁管理不同");
        }
        List<String> keyPoints = splitLines(card.getKeyPoints());
        if (!keyPoints.isEmpty()) {
            return keyPoints;
        }
        return List.of(defaultText(card.getTitle(), "当前知识点"));
    }

    private String forgetfulFollowUpQuestion(KnowledgeCard card) {
        if (isSpringBeanLifecycle(card)) {
            return "没关系，我们先缩小范围。你只说实例化和初始化的区别就行。实例化是创建 Bean 对象，初始化是对象创建后执行扩展回调和 init 方法。你可以试着用自己的话复述一下吗？";
        }
        return "没关系，我们先缩小范围。你先用自己的话说出这个知识点的定义，或者说一个你最确定的使用场景。";
    }

    private void normalizeEvaluation(MockInterviewAiEvaluationResponse response, KnowledgeCard card) {
        if (response.getScore() == null) {
            response.setScore(0);
        }
        response.setScore(safeScore(response.getScore()));
        if (response.getHitKeyPoints() == null) {
            response.setHitKeyPoints(List.of());
        }
        if (response.getMissingKeyPoints() == null) {
            response.setMissingKeyPoints(List.of());
        }
        if (response.getWeaknessTags() == null) {
            response.setWeaknessTags(response.getMissingKeyPoints());
        }
        if (response.getRecommendedCardIds() == null || response.getRecommendedCardIds().isEmpty()) {
            response.setRecommendedCardIds(List.of(card.getId()));
        }
        if (!StringUtils.hasText(response.getFollowUpQuestion())) {
            response.setFollowUpQuestion(firstLine(card.getFollowUp()));
        }
    }

    private MockInterviewAiEvaluationResponse fallbackEvaluation(KnowledgeCard card, String userAnswer) {
        List<String> keyPoints = splitLines(card.getKeyPoints());
        List<String> hit = keyPoints.stream()
                .filter(point -> pointMatched(point, userAnswer))
                .toList();
        List<String> missing = keyPoints.stream()
                .filter(point -> !hit.contains(point))
                .toList();
        int score = keyPoints.isEmpty()
                ? 50
                : capScoreForVisibleGaps(card, userAnswer, missing, Math.round(hit.size() * 100.0f / keyPoints.size()));

        MockInterviewAiEvaluationResponse response = new MockInterviewAiEvaluationResponse();
        response.setScore(score);
        response.setHitKeyPoints(hit);
        response.setMissingKeyPoints(missing);
        response.setFeedback("你的回答已经能覆盖一部分思路，但还需要把关键阶段和扩展机制说得更完整。");
        response.setExpressionIssue(score >= LOW_SCORE_THRESHOLD ? "表达基本可理解，建议补充层次和边界。"
                : "表达覆盖点偏少，需要先按阶段或因果关系组织答案。");
        response.setFollowUpQuestion(firstLine(card.getFollowUp()));
        response.setWeaknessTags(missing.isEmpty() ? List.of(card.getTitle()) : missing);
        response.setRecommendedCardIds(List.of(card.getId()));
        return response;
    }

    private void calibrateEvaluation(MockInterviewAiEvaluationResponse response, KnowledgeCard card, String userAnswer) {
        List<String> keyPoints = splitLines(card.getKeyPoints());
        if (keyPoints.isEmpty()) {
            response.setScore(capScoreForVisibleGaps(card, userAnswer, response.getMissingKeyPoints(), safeScore(response.getScore())));
            return;
        }
        List<String> strictHit = keyPoints.stream()
                .filter(point -> pointMatched(point, userAnswer))
                .toList();
        List<String> strictMissing = keyPoints.stream()
                .filter(point -> !strictHit.contains(point))
                .toList();
        response.setHitKeyPoints(strictHit);
        response.setMissingKeyPoints(strictMissing);
        if (response.getWeaknessTags() == null || response.getWeaknessTags().isEmpty()
                || !strictMissing.isEmpty()) {
            response.setWeaknessTags(strictMissing.isEmpty() ? List.of(card.getTitle()) : strictMissing);
        }
        int calibrated = Math.min(safeScore(response.getScore()), calibratedScore(card, userAnswer, keyPoints, strictHit, strictMissing));
        response.setScore(capScoreForVisibleGaps(card, userAnswer, strictMissing, calibrated));
    }

    private int capScoreForVisibleGaps(KnowledgeCard card, String userAnswer, List<String> missing, int score) {
        if (isSpringBeanLifecycle(card)
                && mentionsLifecycleSkeleton(userAnswer)
                && missing != null
                && missing.stream().anyMatch(this::isDeepSpringLifecyclePoint)) {
            return Math.min(score, 78);
        }
        return score;
    }

    private int calibratedScore(KnowledgeCard card, String userAnswer, List<String> keyPoints,
            List<String> hit, List<String> missing) {
        int coverageScore = Math.round(hit.size() * 100.0f / keyPoints.size());
        int score = Math.max(35, coverageScore);
        if (isSpringBeanLifecycle(card) && mentionsLifecycleSkeleton(userAnswer)) {
            score = Math.max(score, 78);
        }
        if (missing.stream().anyMatch(this::isDeepSpringLifecyclePoint)) {
            score = Math.min(score, 78);
        }
        return Math.min(score, MAX_TURN_SCORE);
    }

    private boolean shouldWriteWeakness(MockInterviewTurn turn, MockInterviewAiEvaluationResponse evaluation) {
        return safeScore(turn.getScore()) < LOW_SCORE_THRESHOLD
                || (evaluation.getMissingKeyPoints() != null && !evaluation.getMissingKeyPoints().isEmpty());
    }

    private void insertWeaknessEvent(MockInterviewSession session, KnowledgeCard card, MockInterviewTurn turn,
            MockInterviewAiEvaluationResponse evaluation, LocalDateTime now) {
        UserWeaknessEvent event = new UserWeaknessEvent();
        event.setUserId(session.getUserId());
        event.setKnowledgePoint(StringUtils.hasText(card.getTitle()) ? card.getTitle() : card.getCategory());
        event.setErrorType("MOCK_INTERVIEW_LOW_SCORE");
        event.setSourceType("MOCK_INTERVIEW");
        event.setSourceId(turn.getId());
        event.setDeltaScore(new BigDecimal("5"));
        event.setBeforeScore(BigDecimal.ZERO);
        event.setAfterScore(new BigDecimal("5"));
        event.setReason(defaultText(turn.getFeedback(), "模拟面试回答存在缺失点。")
                + " 缺失：" + joinComma(evaluation.getMissingKeyPoints()));
        event.setCreatedAt(now);
        weaknessEventMapper.insert(event);
    }

    private MockInterviewReport buildReport(MockInterviewSession session, List<MockInterviewTurn> turns) {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal average = turns.isEmpty()
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(turns.stream()
                                .map(MockInterviewTurn::getScore)
                                .filter(Objects::nonNull)
                                .mapToInt(Integer::intValue)
                                .average()
                                .orElse(0))
                        .setScale(2, RoundingMode.HALF_UP);
        Set<Long> recommendedIds = new LinkedHashSet<>();
        Set<String> weaknessTags = new LinkedHashSet<>();
        Set<String> expressionIssues = new LinkedHashSet<>();
        for (MockInterviewTurn turn : turns) {
            if (turn.getKnowledgeCardId() != null) {
                recommendedIds.add(turn.getKnowledgeCardId());
            }
            weaknessTags.addAll(splitLines(turn.getMissingKeyPoints()));
            if (StringUtils.hasText(turn.getExpressionIssue())) {
                expressionIssues.add(turn.getExpressionIssue());
            }
        }

        MockInterviewReport report = new MockInterviewReport();
        report.setSessionId(session.getId());
        report.setUserId(session.getUserId());
        report.setAverageScore(average);
        report.setSummary("本次模拟面试平均分 " + average + "，建议围绕缺失要点继续复盘。");
        report.setStrengths(average.compareTo(new BigDecimal("70")) >= 0
                ? "能覆盖部分主流程，具备基础表达能力。"
                : "已经完成面试表达练习，后续重点补齐关键要点。");
        report.setWeaknesses(join(weaknessTags.stream().toList()));
        report.setExpressionAdvice(expressionIssues.isEmpty()
                ? "回答时建议使用“定义 -> 流程 -> 边界 -> 扩展点”的结构。"
                : join(expressionIssues.stream().toList()));
        report.setRecommendedCardIds(joinComma(recommendedIds.stream().toList()));
        report.setWeaknessTags(joinComma(weaknessTags.stream().toList()));
        report.setCreatedAt(now);
        report.setUpdatedAt(now);
        return report;
    }

    private MockInterviewSessionVO buildSessionVO(MockInterviewSession session, List<MockInterviewTurn> turns,
            MockInterviewReport report, KnowledgeCard currentCard) {
        MockInterviewSessionVO vo = new MockInterviewSessionVO();
        vo.setSessionId(session.getId());
        vo.setStatus(session.getStatus());
        vo.setCategory(session.getCategory());
        vo.setInterviewerStyle(defaultText(session.getInterviewerStyle(), DEFAULT_INTERVIEWER_STYLE));
        vo.setQuestionCount(session.getQuestionCount());
        vo.setAnsweredMainCount(session.getAnsweredMainCount());
        vo.setCurrentKnowledgeCardId(session.getCurrentKnowledgeCardId());
        vo.setCurrentQuestion(currentQuestion(session, turns, currentCard));
        vo.setCurrentTurnType(currentTurnType(session));
        vo.setTurns(turns.stream().map(this::toTurnVO).toList());
        vo.setReport(report == null ? null : toReportVO(report));
        vo.setStartedAt(session.getStartedAt());
        vo.setFinishedAt(session.getFinishedAt());
        return vo;
    }

    private MockInterviewTurnVO toTurnVO(MockInterviewTurn turn) {
        MockInterviewTurnVO vo = new MockInterviewTurnVO();
        vo.setId(turn.getId());
        vo.setKnowledgeCardId(turn.getKnowledgeCardId());
        vo.setTurnOrder(turn.getTurnOrder());
        vo.setTurnType(turn.getTurnType());
        vo.setParentTurnId(turn.getParentTurnId());
        vo.setQuestion(turn.getQuestion());
        vo.setUserAnswer(turn.getUserAnswer());
        vo.setScore(turn.getScore());
        vo.setFeedback(turn.getFeedback());
        vo.setPerformanceLevel(turn.getPerformanceLevel());
        vo.setStrengthSummary(turn.getStrengthSummary());
        vo.setGapSummary(turn.getGapSummary());
        vo.setExpressionFeedback(turn.getExpressionFeedback());
        vo.setInterviewerObservation(turn.getInterviewerObservation());
        vo.setFollowUpReason(turn.getFollowUpReason());
        vo.setHitKeyPoints(splitLines(turn.getHitKeyPoints()));
        vo.setMissingKeyPoints(splitLines(turn.getMissingKeyPoints()));
        vo.setExpressionIssue(turn.getExpressionIssue());
        vo.setCreatedAt(turn.getCreatedAt());
        return vo;
    }

    private MockInterviewReportVO toReportVO(MockInterviewReport report) {
        MockInterviewReportVO vo = new MockInterviewReportVO();
        vo.setId(report.getId());
        vo.setAverageScore(report.getAverageScore());
        vo.setSummary(report.getSummary());
        vo.setStrengths(report.getStrengths());
        vo.setWeaknesses(report.getWeaknesses());
        vo.setExpressionAdvice(report.getExpressionAdvice());
        vo.setRecommendedCardIds(splitLongs(report.getRecommendedCardIds()));
        vo.setWeaknessTags(splitComma(report.getWeaknessTags()));
        vo.setCreatedAt(report.getCreatedAt());
        return vo;
    }

    private String currentQuestion(MockInterviewSession session, List<MockInterviewTurn> turns, KnowledgeCard currentCard) {
        if (currentCard == null) {
            return null;
        }
        MockInterviewSessionStatusEnum status = status(session);
        if (status == MockInterviewSessionStatusEnum.ASKING_MAIN) {
            return currentCard.getQuestion();
        }
        if (status == MockInterviewSessionStatusEnum.ASKING_FOLLOW_UP) {
            return questionFor(session, currentCard, latestQuestionSource(turns));
        }
        return null;
    }

    private String currentTurnType(MockInterviewSession session) {
        MockInterviewSessionStatusEnum status = status(session);
        if (status == MockInterviewSessionStatusEnum.ASKING_MAIN) {
            return MockInterviewTurnTypeEnum.MAIN.name();
        }
        if (status == MockInterviewSessionStatusEnum.ASKING_FOLLOW_UP) {
            return MockInterviewTurnTypeEnum.FOLLOW_UP.name();
        }
        return null;
    }

    private String questionFor(MockInterviewSession session, KnowledgeCard card, MockInterviewTurn promptSourceTurn) {
        if (status(session) == MockInterviewSessionStatusEnum.ASKING_MAIN) {
            return card.getQuestion();
        }
        if (promptSourceTurn != null && StringUtils.hasText(promptSourceTurn.getAiRawJson())) {
            try {
                MockInterviewAiEvaluationResponse response =
                        objectMapper.readValue(promptSourceTurn.getAiRawJson(), MockInterviewAiEvaluationResponse.class);
                if (StringUtils.hasText(response.getFollowUpQuestion())) {
                    return response.getFollowUpQuestion();
                }
            } catch (JsonProcessingException ignored) {
                // fall back to card follow-up
            }
        }
        return defaultText(firstLine(card.getFollowUp()), "请结合刚才答案补充一个关键边界或扩展点。");
    }

    private MockInterviewTurn latestMainTurn(List<MockInterviewTurn> turns) {
        return turns.stream()
                .filter(turn -> MockInterviewTurnTypeEnum.MAIN.name().equals(turn.getTurnType()))
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private MockInterviewTurn latestQuestionSource(List<MockInterviewTurn> turns) {
        return turns.stream().reduce((first, second) -> second).orElse(null);
    }

    private int followUpCount(List<MockInterviewTurn> turns, Long knowledgeCardId) {
        return Math.toIntExact(turns.stream()
                .filter(turn -> MockInterviewTurnTypeEnum.FOLLOW_UP.name().equals(turn.getTurnType()))
                .filter(turn -> Objects.equals(turn.getKnowledgeCardId(), knowledgeCardId))
                .count());
    }

    private List<MockInterviewTurn> turns(Long sessionId) {
        return turnMapper.selectList(new LambdaQueryWrapper<MockInterviewTurn>()
                .eq(MockInterviewTurn::getSessionId, sessionId)
                .orderByAsc(MockInterviewTurn::getTurnOrder)
                .orderByAsc(MockInterviewTurn::getId));
    }

    private MockInterviewReport report(Long sessionId) {
        return reportMapper.selectOne(new LambdaQueryWrapper<MockInterviewReport>()
                .eq(MockInterviewReport::getSessionId, sessionId));
    }

    private MockInterviewSession requireSession(Long sessionId) {
        MockInterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(404, "mock interview session not found");
        }
        return session;
    }

    private KnowledgeCard currentCard(MockInterviewSession session) {
        if (session.getCurrentKnowledgeCardId() == null) {
            return null;
        }
        KnowledgeCard card = knowledgeCardMapper.selectById(session.getCurrentKnowledgeCardId());
        if (card == null) {
            MockInterviewSessionStatusEnum status = status(session);
            if (status == MockInterviewSessionStatusEnum.FINISHED || status == MockInterviewSessionStatusEnum.REPORTED) {
                return null;
            }
            throw new BusinessException(404, "knowledge card not found");
        }
        return card;
    }

    private KnowledgeCard firstCard(String category) {
        List<KnowledgeCard> cards = categoryCards(category);
        if (cards.isEmpty()) {
            throw new BusinessException(404, "no knowledge cards for mock interview category");
        }
        return cards.get(0);
    }

    private KnowledgeCard nextCard(String category, int answeredMainCount, Long currentCardId) {
        List<KnowledgeCard> cards = categoryCards(category);
        if (answeredMainCount >= cards.size()) {
            return null;
        }
        KnowledgeCard next = cards.get(answeredMainCount);
        if (Objects.equals(next.getId(), currentCardId) && answeredMainCount + 1 < cards.size()) {
            return cards.get(answeredMainCount + 1);
        }
        return next;
    }

    private List<KnowledgeCard> categoryCards(String category) {
        return knowledgeCardMapper.selectList(new LambdaQueryWrapper<KnowledgeCard>()
                .eq(KnowledgeCard::getEnabled, true)
                .eq(KnowledgeCard::getCategory, category)
                .orderByAsc(KnowledgeCard::getSortOrder)
                .orderByAsc(KnowledgeCard::getId));
    }

    private MockInterviewSessionStatusEnum status(MockInterviewSession session) {
        return MockInterviewSessionStatusEnum.valueOf(session.getStatus());
    }

    private int safeQuestionCount(Integer questionCount) {
        if (questionCount == null) {
            return DEFAULT_QUESTION_COUNT;
        }
        return Math.max(1, Math.min(questionCount, MAX_QUESTION_COUNT));
    }

    private String normalizeCategory(String category) {
        if (!StringUtils.hasText(category)) {
            throw new BusinessException("category is required");
        }
        if ("PROJECT".equalsIgnoreCase(category.trim())) {
            return "AI";
        }
        return category.trim().toUpperCase();
    }

    private String normalizeInterviewerStyle(String style) {
        if (!StringUtils.hasText(style)) {
            return DEFAULT_INTERVIEWER_STYLE;
        }
        String normalized = style.trim().toUpperCase();
        return switch (normalized) {
            case "GUIDED", "FAST_SCREEN", "BIG_TECH" -> normalized;
            default -> DEFAULT_INTERVIEWER_STYLE;
        };
    }

    private String systemPrompt() {
        return """
                You are an AI interview coach for Java backend interview training.
                Evaluate the answer as an interviewer. Return strict JSON only.
                Do not provide complete Java AC code or turn this into generic chat.
                Score like a real interviewer: avoid 100, be strict about depth and boundaries.
                If the user says they forgot, do not escalate to a harder question; narrow the scope and ask for one minimal recoverable point.
                Required fields: score, hitKeyPoints, missingKeyPoints, feedback, expressionIssue,
                followUpQuestion, weaknessTags, recommendedCardIds.

                Text quality rules:
                - feedback and missingKeyPoints must be standalone Chinese sentences, not dependent clauses.
                - Never start a sentence with: 但、但是、不过、然而、同时、而且.
                - feedback should directly state what is missing, not restate strengths then pivot.
                - Each missingKeyPoints item should be a complete, self-contained statement.
                """;
    }

    private String userPrompt(MockInterviewSession session, KnowledgeCard card, String question, String userAnswer) {
        return """
                Knowledge card title: %s
                Category: %s
                Main question: %s
                Current question: %s
                Interviewer style: %s
                Reference answer: %s
                Key points:
                %s

                User answer:
                %s
                """.formatted(
                card.getTitle(),
                card.getCategory(),
                card.getQuestion(),
                question,
                defaultText(session.getInterviewerStyle(), DEFAULT_INTERVIEWER_STYLE),
                card.getAnswer(),
                card.getKeyPoints(),
                userAnswer);
    }

    private String toJson(MockInterviewAiEvaluationResponse evaluation) {
        try {
            return objectMapper.writeValueAsString(evaluation);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private String joinComma(List<?> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(StringUtils::hasText)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private List<String> splitLines(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private List<String> splitComma(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private List<Long> splitLongs(String value) {
        return splitComma(value).stream()
                .map(Long::valueOf)
                .toList();
    }

    private int safeScore(Integer score) {
        if (score == null) {
            return 0;
        }
        return Math.max(0, Math.min(score, MAX_TURN_SCORE));
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String firstLine(String value) {
        return splitLines(value).stream().findFirst().orElse(null);
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private DisplayFeedback displayFeedback(MockInterviewSession session, KnowledgeCard card,
            MockInterviewTurnTypeEnum turnType, MockInterviewAiEvaluationResponse evaluation, String userAnswer) {
        String level = performanceLevel(safeScore(evaluation.getScore()));
        List<String> hit = evaluation.getHitKeyPoints() == null ? List.of() : evaluation.getHitKeyPoints();
        List<String> missing = evaluation.getMissingKeyPoints() == null ? List.of() : evaluation.getMissingKeyPoints();
        if (isForgetfulAnswer(userAnswer)) {
            String strength = "你这轮主要表达了不会或忘了，说明当前题目还没有形成可复述的知识框架。";
            String gap = isSpringBeanLifecycle(card)
                    ? "还没有说出 Bean 生命周期的核心阶段。"
                    : "还没有说出当前问题的核心概念和主流程。";
            String expression = "真实面试里可以先说“我先按我记得的部分回答”，再补一个最小可回答点。";
            String reason = isSpringBeanLifecycle(card)
                    ? "我会先缩小范围，让你只补实例化和初始化的区别。"
                    : "我会先缩小范围，让你补一个最小可回答点。";
            return new DisplayFeedback(level, strength + gap, strength, gap, expression, reason, reason);
        }
        String focus = missing.stream().filter(StringUtils::hasText).findFirst()
                .map(this::compactPoint)
                .orElseGet(() -> StringUtils.hasText(card.getTitle()) ? card.getTitle() : card.getCategory());
        String strength = strengthSummary(card, userAnswer, hit);
        String gap = gapSummary(card, missing);
        String expression = defaultText(evaluation.getExpressionIssue(), "建议按“定义、流程、边界、扩展点”的顺序组织答案。");
        String observation = observation(session, card, focus, turnType, missing);
        String reason = followUpReason(session, focus, turnType, missing);
        String feedback = turnType == MockInterviewTurnTypeEnum.MAIN
                ? strength + gap
                : shortFollowUpFeedback(safeScore(evaluation.getScore()), focus);
        return new DisplayFeedback(level, feedback, strength, gap, expression, observation, reason);
    }

    private String strengthSummary(KnowledgeCard card, String userAnswer, List<String> hit) {
        if (isSpringBeanLifecycle(card) && mentionsLifecycleSkeleton(userAnswer)) {
            return "你能说出 Bean 生命周期的大致阶段，主流程方向是对的。";
        }
        if (hit.isEmpty()) {
            return "你已经给出了基础回答，但结构还可以再收紧。";
        }
        return "你已经覆盖了" + joinNatural(hit.stream().map(this::compactPoint).toList(), 2) + "这些主线。";
    }

    private String gapSummary(KnowledgeCard card, List<String> missing) {
        if (isSpringBeanLifecycle(card) && missing.stream().anyMatch(this::isBeanPostProcessorPoint)) {
            return "初始化前置/后置处理阶段没有明确说出 BeanPostProcessor，也没有解释它和 AOP 代理创建的关系。";
        }
        if (missing.isEmpty()) {
            return "这一轮核心内容覆盖比较完整，后面可以继续补充边界和工程场景。";
        }
        List<String> compacted = missing.stream().map(this::compactPoint).toList();
        if (compacted.size() == 1) {
            return "还需要补充" + compacted.get(0) + "这一点。";
        }
        if (compacted.size() == 2) {
            return "还需要补充" + compacted.get(0) + "和" + compacted.get(1) + "。";
        }
        return "还需要补充" + compacted.get(0) + "、" + compacted.get(1) + "等" + compacted.size() + "个关键点。";
    }

    private String performanceLevel(int score) {
        if (score < 50) {
            return "不够深入";
        }
        if (score < 70) {
            return "不够深入";
        }
        if (score < 80) {
            return "基础合格";
        }
        if (score < 89) {
            return "表达较完整";
        }
        return "回答比较扎实";
    }

    private String observation(MockInterviewSession session, KnowledgeCard card, String focus,
            MockInterviewTurnTypeEnum turnType, List<String> missing) {
        String style = defaultText(session.getInterviewerStyle(), DEFAULT_INTERVIEWER_STYLE);
        if (turnType == MockInterviewTurnTypeEnum.FOLLOW_UP) {
            return "这个回答比上一轮更接近面试要求，但我还会继续追一下" + focus + "，确认你的理解边界。";
        }
        if (isSpringBeanLifecycle(card) && missing.stream().anyMatch(this::isBeanPostProcessorPoint)) {
            return "你能说出生命周期主流程，但“初始化前置/后置处理”没有明确说出 BeanPostProcessor，也没有解释它和 AOP 代理创建的关系。";
        }
        if ("GUIDED".equals(style)) {
            return "你已经能搭出回答框架，我会顺着面试里的高频延伸点继续帮你把" + focus + "说完整。";
        }
        if ("FAST_SCREEN".equals(style)) {
            return "主流程有了，但筛选面试会很快追到" + focus + "，这里要回答得更直接。";
        }
        return "你已经能说出" + card.getTitle() + "的主线，但 Spring 面试里通常还会继续追初始化扩展机制，尤其是" + focus + "的关系。";
    }

    private String followUpReason(MockInterviewSession session, String focus, MockInterviewTurnTypeEnum turnType,
            List<String> missing) {
        String style = defaultText(session.getInterviewerStyle(), DEFAULT_INTERVIEWER_STYLE);
        if (turnType == MockInterviewTurnTypeEnum.FOLLOW_UP) {
            return "那我继续往下压一下：" + focus + "在真实项目里容易怎么出问题？";
        }
        if (missing.stream().anyMatch(this::isBeanPostProcessorPoint)) {
            return "所以我会继续追问 BeanPostProcessor 的执行时机。";
        }
        if ("FAST_SCREEN".equals(style)) {
            return "我会直接追问" + focus + "，看你能不能快速说到关键边界。";
        }
        if ("GUIDED".equals(style)) {
            return "我会顺着你刚才的回答追一下" + focus + "，你可以用例子把它讲清楚。";
        }
        return "我会继续追一下" + focus + "，看你能不能把原理和边界说到面试深度。";
    }

    private String shortFollowUpFeedback(int score, String focus) {
        if (score >= 70) {
            return "这个回答比刚才完整一些，可以继续把" + focus + "和实际场景连起来。";
        }
        return "这轮补充还不够深入，" + focus + "仍然需要再往原理和边界上展开。";
    }

    private String joinNatural(List<String> values, int limit) {
        return values.stream()
                .filter(StringUtils::hasText)
                .limit(limit)
                .reduce((left, right) -> left + "、" + right)
                .orElse("部分要点");
    }

    private boolean pointMatched(String point, String answer) {
        if (!StringUtils.hasText(point) || !StringUtils.hasText(answer)) {
            return false;
        }
        if (answer.contains(point)) {
            return true;
        }
        if (point.contains("BeanPostProcessor")) {
            return answer.contains("BeanPostProcessor") || answer.contains("Bean 后置处理器")
                    || answer.contains("bean 后置处理器");
        }
        return keyTerms(point).stream().anyMatch(answer::contains);
    }

    private boolean isSpringBeanLifecycle(KnowledgeCard card) {
        return card != null && StringUtils.hasText(card.getTitle()) && card.getTitle().contains("Bean 生命周期");
    }

    private boolean mentionsLifecycleSkeleton(String answer) {
        if (!StringUtils.hasText(answer)) {
            return false;
        }
        int count = 0;
        for (String term : List.of("实例化", "属性填充", "依赖注入", "初始化", "使用", "销毁")) {
            if (answer.contains(term)) {
                count++;
            }
        }
        return count >= 3;
    }

    private boolean isForgetfulAnswer(String answer) {
        if (!StringUtils.hasText(answer)) {
            return false;
        }
        return List.of("我忘了", "不知道", "不太清楚", "没思路", "忘记了", "不会").stream()
                .anyMatch(answer::contains);
    }

    private boolean isBeanPostProcessorPoint(String point) {
        return StringUtils.hasText(point) && point.contains("BeanPostProcessor");
    }

    private boolean isDeepSpringLifecyclePoint(String point) {
        return StringUtils.hasText(point)
                && (point.contains("BeanPostProcessor")
                || point.contains("AOP")
                || point.contains("@PostConstruct")
                || point.contains("afterPropertiesSet")
                || point.contains("InitializingBean")
                || point.contains("prototype"));
    }

    private List<String> keyTerms(String point) {
        List<String> candidates = List.of(
                // Spring
                "BeanPostProcessor", "AOP", "@PostConstruct", "afterPropertiesSet",
                "InitializingBean", "prototype", "Aware", "实例化", "依赖注入",
                "属性填充", "初始化", "销毁", "后置处理器", "扩展机制",
                // Redis
                "内存", "I/O多路复用", "IO多路复用", "epoll", "select", "单线程",
                "命令执行", "串行", "锁竞争", "上下文切换", "数据结构",
                "跳表", "哈希表", "压缩列表", "持久化", "RDB", "AOF",
                "主从", "哨兵", "集群", "过期", "淘汰", "大key",
                // MySQL
                "索引", "B+树", "事务", "ACID", "MVCC", "幻读", "脏读",
                "可重复读", "行锁", "表锁", "间隙锁", "慢查询", "回表",
                "覆盖索引", "联合索引", "最左前缀", "分库分表", "主从复制",
                // JVM
                "堆", "栈", "方法区", "GC", "垃圾回收", "可达性分析",
                "标记清除", "复制算法", "分代", "类加载", "双亲委派",
                "Full GC", "Minor GC", "内存溢出", "内存泄漏",
                // Java 并发
                "线程池", "CAS", "synchronized", "ReentrantLock", "volatile",
                "ThreadLocal", "CountDownLatch", "CompletableFuture",
                // 通用
                "高并发", "缓存", "一致性", "可用性", "扩展性",
                "负载均衡", "限流", "熔断", "降级", "幂等");
        return candidates.stream()
                .filter(point::contains)
                .toList();
    }

    private String compactPoint(String point) {
        if (!StringUtils.hasText(point)) {
            return "关键扩展点";
        }
        // Spring
        if (point.contains("BeanPostProcessor") && point.contains("AOP")) {
            return "BeanPostProcessor 与 AOP 的关系";
        }
        if (point.contains("BeanPostProcessor")) {
            return "BeanPostProcessor";
        }
        if (point.contains("AOP")) {
            return "AOP 创建机制";
        }
        if (point.contains("@PostConstruct") || point.contains("afterPropertiesSet")
                || point.contains("InitializingBean")) {
            return "初始化回调顺序";
        }
        if (point.contains("prototype")) {
            return "prototype 销毁边界";
        }
        if (point.contains("初始化") || point.contains("后置处理器") || point.contains("扩展机制")) {
            return "初始化扩展机制";
        }
        if (point.contains("依赖注入") || point.contains("属性填充")) {
            return "属性填充和依赖注入";
        }
        if (point.contains("实例化")) {
            return "实例化阶段";
        }
        if (point.contains("销毁")) {
            return "销毁阶段";
        }
        // Redis
        if (point.contains("内存") && point.contains("单线程")) {
            return "内存访问与单线程模型";
        }
        if (point.contains("内存")) {
            return "内存访问";
        }
        if (point.contains("I/O多路复用") || point.contains("IO多路复用") || point.contains("epoll")) {
            return "I/O 多路复用";
        }
        if (point.contains("单线程") && point.contains("锁")) {
            return "单线程减少锁竞争";
        }
        if (point.contains("单线程")) {
            return "单线程模型";
        }
        if (point.contains("数据结构")) {
            return "高效数据结构";
        }
        if (point.contains("持久化") || point.contains("RDB") || point.contains("AOF")) {
            return "持久化机制";
        }
        if (point.contains("大key") || point.contains("大 key")) {
            return "大 key 问题";
        }
        // MySQL
        if (point.contains("索引") && point.contains("B+")) {
            return "B+ 树索引";
        }
        if (point.contains("索引")) {
            return "索引设计";
        }
        if (point.contains("MVCC")) {
            return "MVCC 机制";
        }
        if (point.contains("事务")) {
            return "事务隔离";
        }
        if (point.contains("慢查询")) {
            return "慢查询优化";
        }
        // Generic fallback: extract first meaningful segment
        String[] segments = point.split("[，。；]");
        if (segments.length > 0 && segments[0].length() > 2) {
            String first = segments[0].trim();
            return first.length() > 20 ? first.substring(0, 20) : first;
        }
        return point.length() > 20 ? point.substring(0, 20) : point;
    }

    private record DisplayFeedback(
            String performanceLevel,
            String feedback,
            String strengthSummary,
            String gapSummary,
            String expressionFeedback,
            String interviewerObservation,
            String followUpReason) {
    }
}
