package com.interview.coach.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.TrainingPlanResult;
import com.interview.coach.dto.MockInterviewAiEvaluationResponse;
import com.interview.coach.dto.MockInterviewAnswerRequest;
import com.interview.coach.dto.MockInterviewCreateRequest;
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
import com.interview.coach.mapper.TrainingPlanItemMapper;
import com.interview.coach.mapper.UserWeaknessEventMapper;
import com.interview.coach.service.TrainingPlanService;
import com.interview.coach.vo.MockInterviewSessionVO;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MockInterviewServiceImplTest {

    @Mock
    private KnowledgeCardMapper knowledgeCardMapper;

    @Mock
    private MockInterviewSessionMapper sessionMapper;

    @Mock
    private MockInterviewTurnMapper turnMapper;

    @Mock
    private MockInterviewReportMapper reportMapper;

    @Mock
    private UserWeaknessEventMapper weaknessEventMapper;

    @Mock
    private AnthropicCompatibleClient aiClient;

    @Mock
    private TrainingPlanService trainingPlanService;

    @Mock
    private TrainingPlanItemMapper trainingPlanItemMapper;

    private MockInterviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MockInterviewServiceImpl(
                knowledgeCardMapper,
                sessionMapper,
                turnMapper,
                reportMapper,
                weaknessEventMapper,
                aiClient,
                trainingPlanService,
                trainingPlanItemMapper);
    }

    @Test
    void createSessionStartsAskingMainAndGetRestoresCurrentQuestion() {
        when(knowledgeCardMapper.selectList(any())).thenReturn(List.of(card(1831L)));
        doAnswer(invocation -> {
            MockInterviewSession session = invocation.getArgument(0);
            session.setId(9L);
            return 1;
        }).when(sessionMapper).insert(any(MockInterviewSession.class));
        MockInterviewCreateRequest request = createRequest();

        MockInterviewSessionVO created = service.create(request);

        assertThat(created.getSessionId()).isEqualTo(9L);
        assertThat(created.getStatus()).isEqualTo("ASKING_MAIN");
        assertThat(created.getCurrentKnowledgeCardId()).isEqualTo(1831L);
        assertThat(created.getCurrentQuestion()).isEqualTo("Spring Bean 生命周期是什么？");
        assertThat(created.getCurrentTurnType()).isEqualTo("MAIN");
        assertThat(created.getInterviewerStyle()).isEqualTo("BIG_TECH");

        when(sessionMapper.selectById(9L)).thenReturn(session(9L, "ASKING_MAIN", 1831L, 0));
        when(knowledgeCardMapper.selectById(1831L)).thenReturn(card(1831L));
        when(turnMapper.selectList(any())).thenReturn(List.of());
        when(reportMapper.selectOne(any())).thenReturn(null);

        MockInterviewSessionVO restored = service.getSession(9L);

        assertThat(restored.getCurrentQuestion()).isEqualTo("Spring Bean 生命周期是什么？");
        assertThat(restored.getTurns()).isEmpty();
        assertThat(restored.getReport()).isNull();
    }

    @Test
    void getSessionRestoresReportTrainingPlanLinkage() {
        when(sessionMapper.selectById(9L)).thenReturn(session(9L, "REPORTED", 1831L, 1));
        when(knowledgeCardMapper.selectById(1831L)).thenReturn(card(1831L));
        when(turnMapper.selectList(any())).thenReturn(List.of(mainTurn(21L), followUpTurn(22L)));
        when(reportMapper.selectOne(any())).thenReturn(report(31L));
        when(trainingPlanItemMapper.selectCount(any())).thenReturn(2L);

        MockInterviewSessionVO restored = service.getSession(9L);

        assertThat(restored.getReport()).isNotNull();
        assertThat(reportValue(restored, "trainingPlanLinked")).isEqualTo(true);
        assertThat(reportValue(restored, "trainingPlanItemCount")).isEqualTo(2);
        assertThat(reportValue(restored, "reviewPathSummary")).asString()
                .contains("已进入训练计划", "2 个复盘项");
    }

    @Test
    void submitMainAnswerPersistsMainTurnAndMovesToFollowUp() {
        when(sessionMapper.selectById(9L)).thenReturn(session(9L, "ASKING_MAIN", 1831L, 0));
        when(knowledgeCardMapper.selectById(1831L)).thenReturn(card(1831L));
        when(aiClient.askJson(anyString(), anyString(), eq(MockInterviewAiEvaluationResponse.class)))
                .thenReturn(aiResponse(72, "为什么 BeanPostProcessor 能影响初始化前后？"));
        doAnswer(invocation -> {
            MockInterviewTurn turn = invocation.getArgument(0);
            turn.setId(21L);
            return 1;
        }).when(turnMapper).insert(any(MockInterviewTurn.class));
        when(turnMapper.selectList(any())).thenReturn(List.of(mainTurn(21L)));

        MockInterviewSessionVO result = service.answer(9L, answer("提到了实例化、依赖注入和初始化。"));

        ArgumentCaptor<MockInterviewTurn> turnCaptor = ArgumentCaptor.forClass(MockInterviewTurn.class);
        verify(turnMapper).insert(turnCaptor.capture());
        assertThat(turnCaptor.getValue().getTurnType()).isEqualTo("MAIN");
        assertThat(turnCaptor.getValue().getScore()).isEqualTo(72);
        assertThat(turnCaptor.getValue().getPerformanceLevel()).isEqualTo("基础合格");
        assertThat(turnCaptor.getValue().getStrengthSummary()).contains("生命周期的大致阶段");
        assertThat(turnCaptor.getValue().getGapSummary()).contains("BeanPostProcessor");
        assertThat(turnCaptor.getValue().getInterviewerObservation()).contains("没有明确说出 BeanPostProcessor");
        assertThat(turnCaptor.getValue().getFollowUpReason()).doesNotContain("因为没提");
        assertThat(turnCaptor.getValue().getHitKeyPoints()).contains("实例化");
        assertThat(turnCaptor.getValue().getMissingKeyPoints()).contains("销毁");
        assertThat(turnCaptor.getValue().getAiRawJson()).contains("BeanPostProcessor");

        ArgumentCaptor<MockInterviewSession> sessionCaptor = ArgumentCaptor.forClass(MockInterviewSession.class);
        verify(sessionMapper).updateById(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getStatus()).isEqualTo("ASKING_FOLLOW_UP");
        assertThat(result.getStatus()).isEqualTo("ASKING_FOLLOW_UP");
        assertThat(result.getCurrentQuestion()).isEqualTo("为什么 BeanPostProcessor 能影响初始化前后？");
        assertThat(result.getCurrentTurnType()).isEqualTo("FOLLOW_UP");
    }

    @Test
    void submitMainAnswerWritesWeaknessEventWhenScorePassesButKeyPointsAreMissing() {
        when(sessionMapper.selectById(9L)).thenReturn(session(9L, "ASKING_MAIN", 1831L, 0));
        when(knowledgeCardMapper.selectById(1831L)).thenReturn(card(1831L));
        when(aiClient.askJson(anyString(), anyString(), eq(MockInterviewAiEvaluationResponse.class)))
                .thenReturn(aiResponse(72, "为什么 BeanPostProcessor 能影响初始化前后？"));
        doAnswer(invocation -> {
            MockInterviewTurn turn = invocation.getArgument(0);
            turn.setId(21L);
            return 1;
        }).when(turnMapper).insert(any(MockInterviewTurn.class));
        when(turnMapper.selectList(any())).thenReturn(List.of(mainTurn(21L)));

        service.answer(9L, answer("提到了实例化、依赖注入和初始化。"));

        ArgumentCaptor<UserWeaknessEvent> eventCaptor = ArgumentCaptor.forClass(UserWeaknessEvent.class);
        verify(weaknessEventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getSourceType()).isEqualTo("MOCK_INTERVIEW");
        assertThat(eventCaptor.getValue().getSourceId()).isEqualTo(21L);
        assertThat(eventCaptor.getValue().getKnowledgePoint()).isEqualTo("Spring Bean 生命周期");
        assertThat(eventCaptor.getValue().getReason()).contains("缺失", "销毁");
    }

    @Test
    void forgetfulMainAnswerUsesNarrowHintFollowUpInsteadOfHardPressure() {
        when(sessionMapper.selectById(9L)).thenReturn(session(9L, "ASKING_MAIN", 1831L, 0));
        when(knowledgeCardMapper.selectById(1831L)).thenReturn(card(1831L));
        AtomicReference<MockInterviewTurn> insertedTurn = new AtomicReference<>();
        doAnswer(invocation -> {
            MockInterviewTurn turn = invocation.getArgument(0);
            turn.setId(21L);
            insertedTurn.set(turn);
            return 1;
        }).when(turnMapper).insert(any(MockInterviewTurn.class));
        when(turnMapper.selectList(any())).thenAnswer(invocation ->
                insertedTurn.get() == null ? List.of() : List.of(insertedTurn.get()));

        MockInterviewSessionVO result = service.answer(9L, answer("我忘了，不太清楚。"));

        verify(aiClient, never()).askJson(anyString(), anyString(), eq(MockInterviewAiEvaluationResponse.class));
        ArgumentCaptor<MockInterviewTurn> turnCaptor = ArgumentCaptor.forClass(MockInterviewTurn.class);
        verify(turnMapper).insert(turnCaptor.capture());
        MockInterviewTurn turn = turnCaptor.getValue();
        assertThat(turn.getScore()).isLessThanOrEqualTo(10);
        assertThat(turn.getStrengthSummary()).contains("不会", "忘了");
        assertThat(turn.getGapSummary()).contains("还没有说出 Bean 生命周期的核心阶段");
        assertThat(turn.getFollowUpReason()).contains("缩小范围")
                .contains("实例化和初始化的区别");
        assertThat(turn.getAiRawJson()).contains("实例化和初始化的区别")
                .doesNotContain("AOP 为什么依赖 BeanPostProcessor");
        assertThat(result.getStatus()).isEqualTo("ASKING_FOLLOW_UP");
        assertThat(result.getCurrentQuestion()).contains("没关系")
                .contains("实例化和初始化的区别")
                .contains("复述");
    }

    @Test
    void submitFollowUpWritesWeaknessEventAndKeepsPressureWhenGapsRemain() {
        when(sessionMapper.selectById(9L)).thenReturn(session(9L, "ASKING_FOLLOW_UP", 1831L, 1));
        when(knowledgeCardMapper.selectById(1831L)).thenReturn(card(1831L));
        when(turnMapper.selectList(any())).thenReturn(List.of(mainTurn(21L)));
        when(aiClient.askJson(anyString(), anyString(), eq(MockInterviewAiEvaluationResponse.class)))
                .thenThrow(new RuntimeException("AI down"));
        doAnswer(invocation -> {
            MockInterviewTurn turn = invocation.getArgument(0);
            turn.setId(22L);
            return 1;
        }).when(turnMapper).insert(any(MockInterviewTurn.class));

        MockInterviewSessionVO result = service.answer(9L, answer("只答到初始化，没有说清楚扩展点。"));

        ArgumentCaptor<MockInterviewTurn> turnCaptor = ArgumentCaptor.forClass(MockInterviewTurn.class);
        verify(turnMapper).insert(turnCaptor.capture());
        assertThat(turnCaptor.getValue().getTurnType()).isEqualTo("FOLLOW_UP");
        assertThat(turnCaptor.getValue().getParentTurnId()).isEqualTo(21L);
        assertThat(turnCaptor.getValue().getScore()).isLessThan(60);
        assertThat(turnCaptor.getValue().getFeedback()).doesNotContain("fallback")
                .doesNotContain("keyPoints")
                .doesNotContain("命中率")
                .doesNotContain("AI 评分暂不可用");
        assertThat(turnCaptor.getValue().getInterviewerObservation()).contains("继续追");

        ArgumentCaptor<UserWeaknessEvent> eventCaptor = ArgumentCaptor.forClass(UserWeaknessEvent.class);
        verify(weaknessEventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().getSourceType()).isEqualTo("MOCK_INTERVIEW");
        assertThat(eventCaptor.getValue().getSourceId()).isEqualTo(22L);
        assertThat(eventCaptor.getValue().getKnowledgePoint()).isEqualTo("Spring Bean 生命周期");

        ArgumentCaptor<MockInterviewSession> sessionCaptor = ArgumentCaptor.forClass(MockInterviewSession.class);
        verify(sessionMapper).updateById(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getStatus()).isEqualTo("ASKING_FOLLOW_UP");
        assertThat(result.getStatus()).isEqualTo("ASKING_FOLLOW_UP");
    }

    @Test
    void aiPerfectScoreIsCappedAndTurnVoUsesInterviewDisplayFields() {
        when(sessionMapper.selectById(9L)).thenReturn(session(9L, "ASKING_MAIN", 1831L, 0));
        when(knowledgeCardMapper.selectById(1831L)).thenReturn(card(1831L));
        when(aiClient.askJson(anyString(), anyString(), eq(MockInterviewAiEvaluationResponse.class)))
                .thenReturn(aiResponse(100, "那 Spring AOP 为什么依赖 BeanPostProcessor？"));
        doAnswer(invocation -> {
            MockInterviewTurn turn = invocation.getArgument(0);
            turn.setId(21L);
            return 1;
        }).when(turnMapper).insert(any(MockInterviewTurn.class));
        when(turnMapper.selectList(any())).thenReturn(List.of(mainTurn(21L)));

        service.answer(9L, answer("Spring Bean 生命周期包括实例化、依赖注入、初始化、销毁，并且 BeanPostProcessor 会参与初始化前后处理。"));

        ArgumentCaptor<MockInterviewTurn> turnCaptor = ArgumentCaptor.forClass(MockInterviewTurn.class);
        verify(turnMapper).insert(turnCaptor.capture());
        assertThat(turnCaptor.getValue().getScore()).isEqualTo(95);
        assertThat(turnCaptor.getValue().getPerformanceLevel()).isEqualTo("回答比较扎实");
    }

    @Test
    void mainAnswerDoesNotPraiseBeanPostProcessorOrAopWhenUserOnlyMentionsLifecycleStages() {
        when(sessionMapper.selectById(9L)).thenReturn(session(9L, "ASKING_MAIN", 1831L, 0));
        when(knowledgeCardMapper.selectById(1831L)).thenReturn(card(1831L));
        MockInterviewAiEvaluationResponse overPraised = aiResponse(92, "BeanPostProcessor 在生命周期中什么时候执行？");
        overPraised.setHitKeyPoints(List.of("阶段流程", "BeanPostProcessor 与 AOP 的关系"));
        overPraised.setMissingKeyPoints(List.of());
        when(aiClient.askJson(anyString(), anyString(), eq(MockInterviewAiEvaluationResponse.class)))
                .thenReturn(overPraised);
        doAnswer(invocation -> {
            MockInterviewTurn turn = invocation.getArgument(0);
            turn.setId(21L);
            return 1;
        }).when(turnMapper).insert(any(MockInterviewTurn.class));
        when(turnMapper.selectList(any())).thenReturn(List.of(mainTurn(21L)));

        service.answer(9L, answer("Spring Bean 生命周期大致可以分成：实例化、属性填充、初始化前置处理、初始化、初始化后置处理、Bean 使用、Bean 销毁。"));

        ArgumentCaptor<MockInterviewTurn> turnCaptor = ArgumentCaptor.forClass(MockInterviewTurn.class);
        verify(turnMapper).insert(turnCaptor.capture());
        MockInterviewTurn turn = turnCaptor.getValue();
        assertThat(turn.getScore()).isLessThan(90);
        assertThat(turn.getPerformanceLevel()).isNotEqualTo("回答比较扎实");
        assertThat(turn.getStrengthSummary()).contains("生命周期的大致阶段")
                .doesNotContain("BeanPostProcessor 与 AOP 的关系");
        assertThat(turn.getGapSummary()).contains("BeanPostProcessor");
        assertThat(turn.getInterviewerObservation()).contains("没有明确说出 BeanPostProcessor")
                .contains("AOP 代理创建的关系");
        assertThat(turn.getFollowUpReason()).contains("BeanPostProcessor 的执行时机");
    }

    @Test
    void visibleBeanPostProcessorGapCapsScoreEvenWhenCardKeyPointsAreMissing() {
        KnowledgeCard card = card(1831L);
        card.setKeyPoints(null);
        when(sessionMapper.selectById(9L)).thenReturn(session(9L, "ASKING_MAIN", 1831L, 0));
        when(knowledgeCardMapper.selectById(1831L)).thenReturn(card);
        MockInterviewAiEvaluationResponse response = aiResponse(88, "BeanPostProcessor 在生命周期中什么时候执行？");
        response.setHitKeyPoints(List.of("生命周期阶段"));
        response.setMissingKeyPoints(List.of("BeanPostProcessor 与 AOP 的关系"));
        when(aiClient.askJson(anyString(), anyString(), eq(MockInterviewAiEvaluationResponse.class)))
                .thenReturn(response);
        doAnswer(invocation -> {
            MockInterviewTurn turn = invocation.getArgument(0);
            turn.setId(21L);
            return 1;
        }).when(turnMapper).insert(any(MockInterviewTurn.class));
        when(turnMapper.selectList(any())).thenReturn(List.of(mainTurn(21L)));

        service.answer(9L, answer("Spring Bean 生命周期大致可以分成：实例化、属性填充、初始化前置处理、初始化、初始化后置处理、Bean 使用、Bean 销毁。"));

        ArgumentCaptor<MockInterviewTurn> turnCaptor = ArgumentCaptor.forClass(MockInterviewTurn.class);
        verify(turnMapper).insert(turnCaptor.capture());
        assertThat(turnCaptor.getValue().getScore()).isEqualTo(78);
        assertThat(turnCaptor.getValue().getPerformanceLevel()).isEqualTo("基础合格");
    }

    @Test
    void visibleBeanPostProcessorGapCapsScoreForLongSentenceKeyPoints() {
        KnowledgeCard card = card(1831L);
        card.setKeyPoints("""
                Spring Bean 生命周期包括实例化、属性填充、Aware 回调、后置处理器、初始化、使用和销毁。
                InitializingBean、init-method 和 @PostConstruct 都属于初始化阶段相关扩展。
                容易踩坑的是把实例化和初始化混为一谈。
                单例 Bean 销毁由容器管理，prototype Bean 销毁通常不由容器完整托管。
                BeanPostProcessor 会在初始化前后插入扩展逻辑，AOP 代理也依赖它。
                """);
        when(sessionMapper.selectById(9L)).thenReturn(session(9L, "ASKING_MAIN", 1831L, 0));
        when(knowledgeCardMapper.selectById(1831L)).thenReturn(card);
        MockInterviewAiEvaluationResponse response = aiResponse(88, "BeanPostProcessor 在生命周期中什么时候执行？");
        response.setHitKeyPoints(List.of("生命周期阶段"));
        response.setMissingKeyPoints(List.of("BeanPostProcessor 与 AOP 的关系"));
        when(aiClient.askJson(anyString(), anyString(), eq(MockInterviewAiEvaluationResponse.class)))
                .thenReturn(response);
        doAnswer(invocation -> {
            MockInterviewTurn turn = invocation.getArgument(0);
            turn.setId(21L);
            return 1;
        }).when(turnMapper).insert(any(MockInterviewTurn.class));
        when(turnMapper.selectList(any())).thenReturn(List.of(mainTurn(21L)));

        service.answer(9L, answer("Spring Bean 生命周期大致可以分成：实例化、属性填充、初始化前置处理、初始化、初始化后置处理、Bean 使用、Bean 销毁。"));

        ArgumentCaptor<MockInterviewTurn> turnCaptor = ArgumentCaptor.forClass(MockInterviewTurn.class);
        verify(turnMapper).insert(turnCaptor.capture());
        assertThat(turnCaptor.getValue().getScore()).isEqualTo(78);
        assertThat(turnCaptor.getValue().getPerformanceLevel()).isEqualTo("基础合格");
        assertThat(turnCaptor.getValue().getHitKeyPoints()).doesNotContain("BeanPostProcessor 会在初始化前后插入扩展逻辑，AOP 代理也依赖它。");
    }

    @Test
    void fallbackAlsoCapsVisibleBeanPostProcessorGapForLongSentenceKeyPoints() {
        KnowledgeCard card = card(1831L);
        card.setKeyPoints("""
                Spring Bean 生命周期包括实例化、属性填充、Aware 回调、后置处理器、初始化、使用和销毁。
                InitializingBean、init-method 和 @PostConstruct 都属于初始化阶段相关扩展。
                容易踩坑的是把实例化和初始化混为一谈。
                单例 Bean 销毁由容器管理，prototype Bean 销毁通常不由容器完整托管。
                BeanPostProcessor 会在初始化前后插入扩展逻辑，AOP 代理也依赖它。
                """);
        when(sessionMapper.selectById(9L)).thenReturn(session(9L, "ASKING_MAIN", 1831L, 0));
        when(knowledgeCardMapper.selectById(1831L)).thenReturn(card);
        when(aiClient.askJson(anyString(), anyString(), eq(MockInterviewAiEvaluationResponse.class)))
                .thenThrow(new RuntimeException("AI unavailable"));
        doAnswer(invocation -> {
            MockInterviewTurn turn = invocation.getArgument(0);
            turn.setId(21L);
            return 1;
        }).when(turnMapper).insert(any(MockInterviewTurn.class));
        when(turnMapper.selectList(any())).thenReturn(List.of(mainTurn(21L)));

        service.answer(9L, answer("Spring Bean 生命周期大致可以分成：实例化、属性填充、初始化前置处理、初始化、初始化后置处理、Bean 使用、Bean 销毁。"));

        ArgumentCaptor<MockInterviewTurn> turnCaptor = ArgumentCaptor.forClass(MockInterviewTurn.class);
        verify(turnMapper).insert(turnCaptor.capture());
        assertThat(turnCaptor.getValue().getScore()).isEqualTo(78);
        assertThat(turnCaptor.getValue().getPerformanceLevel()).isEqualTo("基础合格");
    }

    @Test
    void followUpCanContinueOnceBeforeFinishing() {
        MockInterviewSession session = session(9L, "ASKING_FOLLOW_UP", 1831L, 1);
        session.setQuestionCount(1);
        when(sessionMapper.selectById(9L)).thenReturn(session);
        when(knowledgeCardMapper.selectById(1831L)).thenReturn(card(1831L));
        when(turnMapper.selectList(any())).thenReturn(List.of(mainTurn(21L), firstFollowUpTurn(22L)));
        when(aiClient.askJson(anyString(), anyString(), eq(MockInterviewAiEvaluationResponse.class)))
                .thenReturn(aiResponse(62, "那 Spring AOP 为什么依赖 BeanPostProcessor？"));
        doAnswer(invocation -> {
            MockInterviewTurn turn = invocation.getArgument(0);
            turn.setId(23L);
            return 1;
        }).when(turnMapper).insert(any(MockInterviewTurn.class));

        service.answer(9L, answer("补充了初始化前后的扩展点。"));

        ArgumentCaptor<MockInterviewSession> sessionCaptor = ArgumentCaptor.forClass(MockInterviewSession.class);
        verify(sessionMapper).updateById(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getStatus()).isEqualTo("FINISHED");
    }

    @Test
    void answerRejectsInvalidSessionState() {
        when(sessionMapper.selectById(9L)).thenReturn(session(9L, "FINISHED", 1831L, 1));

        assertThatThrownBy(() -> service.answer(9L, answer("继续回答")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot answer");

        verify(turnMapper, never()).insert(any(MockInterviewTurn.class));
    }

    @Test
    void finishGeneratesReportAndMarksSessionReported() {
        MockInterviewSession finished = session(9L, "FINISHED", 1831L, 1);
        when(sessionMapper.selectById(9L)).thenReturn(finished);
        when(turnMapper.selectList(any())).thenReturn(List.of(mainTurn(21L), followUpTurn(22L)));
        doAnswer(invocation -> {
            MockInterviewReport report = invocation.getArgument(0);
            report.setId(31L);
            return 1;
        }).when(reportMapper).insert(any(MockInterviewReport.class));

        MockInterviewSessionVO result = service.finish(9L);

        ArgumentCaptor<MockInterviewReport> reportCaptor = ArgumentCaptor.forClass(MockInterviewReport.class);
        verify(reportMapper).insert(reportCaptor.capture());
        assertThat(reportCaptor.getValue().getAverageScore()).isEqualByComparingTo("61.00");
        assertThat(reportCaptor.getValue().getRecommendedCardIds()).contains("1831");
        assertThat(reportCaptor.getValue().getWeaknessTags()).contains("销毁");
        ArgumentCaptor<AgentContext> contextCaptor = ArgumentCaptor.forClass(AgentContext.class);
        ArgumentCaptor<TrainingPlanResult> planCaptor = ArgumentCaptor.forClass(TrainingPlanResult.class);
        verify(trainingPlanService).savePlan(contextCaptor.capture(), planCaptor.capture());
        assertThat(contextCaptor.getValue().getUserId()).isEqualTo(1L);
        assertThat(planCaptor.getValue().getTitle()).contains("模拟面试");
        assertThat(planCaptor.getValue().getSummary()).contains("平均分 61.00");
        assertThat(planCaptor.getValue().getItems()).hasSize(1);
        assertThat(planCaptor.getValue().getItems().get(0).getItemType()).isEqualTo("KNOWLEDGE_CARD");
        assertThat(planCaptor.getValue().getItems().get(0).getKnowledgeCardId()).isEqualTo(1831L);
        assertThat(planCaptor.getValue().getItems().get(0).getReason()).contains("模拟面试");
        assertThat(planCaptor.getValue().getItems().get(0).getSourceType()).isEqualTo("MOCK_INTERVIEW_REPORT");
        assertThat(planCaptor.getValue().getItems().get(0).getSourceId()).isEqualTo(31L);
        assertThat(planCaptor.getValue().getItems().get(0).getSourceSummary()).contains("模拟面试报告 #31");

        ArgumentCaptor<MockInterviewSession> sessionCaptor = ArgumentCaptor.forClass(MockInterviewSession.class);
        verify(sessionMapper).updateById(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getStatus()).isEqualTo("REPORTED");
        assertThat(result.getStatus()).isEqualTo("REPORTED");
        assertThat(result.getReport().getAverageScore()).isEqualByComparingTo("61.00");
        assertThat(reportValue(result, "trainingPlanLinked")).isEqualTo(true);
        assertThat(reportValue(result, "trainingPlanItemCount")).isEqualTo(1);
        assertThat(reportValue(result, "reviewPathSummary")).asString()
                .contains("已进入训练计划", "1 个复盘项");
    }

    @Test
    void finishStillReturnsReportWhenTrainingPlanPersistenceFails() {
        MockInterviewSession finished = session(9L, "FINISHED", 1831L, 1);
        when(sessionMapper.selectById(9L)).thenReturn(finished);
        when(turnMapper.selectList(any())).thenReturn(List.of(mainTurn(21L), followUpTurn(22L)));
        doAnswer(invocation -> {
            MockInterviewReport report = invocation.getArgument(0);
            report.setId(31L);
            return 1;
        }).when(reportMapper).insert(any(MockInterviewReport.class));
        doThrow(new IllegalStateException("training plan database down"))
                .when(trainingPlanService).savePlan(any(AgentContext.class), any(TrainingPlanResult.class));

        MockInterviewSessionVO result = service.finish(9L);

        verify(trainingPlanService).savePlan(any(AgentContext.class), any(TrainingPlanResult.class));
        ArgumentCaptor<MockInterviewSession> sessionCaptor = ArgumentCaptor.forClass(MockInterviewSession.class);
        verify(sessionMapper).updateById(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getStatus()).isEqualTo("REPORTED");
        assertThat(result.getStatus()).isEqualTo("REPORTED");
        assertThat(result.getReport()).isNotNull();
        assertThat(result.getReport().getAverageScore()).isEqualByComparingTo("61.00");
    }

    @Test
    void finishDoesNotRecommendCardsOrTrainingPlanWhenAllAnswersAreStrongWithoutGaps() {
        MockInterviewSession finished = session(9L, "FINISHED", 1831L, 1);
        when(sessionMapper.selectById(9L)).thenReturn(finished);
        when(turnMapper.selectList(any())).thenReturn(List.of(strongTurn(21L)));
        doAnswer(invocation -> {
            MockInterviewReport report = invocation.getArgument(0);
            report.setId(31L);
            return 1;
        }).when(reportMapper).insert(any(MockInterviewReport.class));

        MockInterviewSessionVO result = service.finish(9L);

        ArgumentCaptor<MockInterviewReport> reportCaptor = ArgumentCaptor.forClass(MockInterviewReport.class);
        verify(reportMapper).insert(reportCaptor.capture());
        assertThat(reportCaptor.getValue().getAverageScore()).isEqualByComparingTo("92.00");
        assertThat(reportCaptor.getValue().getRecommendedCardIds()).isBlank();
        assertThat(reportCaptor.getValue().getWeaknessTags()).isBlank();
        assertThat(result.getReport().getRecommendedCardIds()).isEmpty();
        verify(trainingPlanService, never()).savePlan(any(AgentContext.class), any(TrainingPlanResult.class));
    }

    private MockInterviewCreateRequest createRequest() {
        MockInterviewCreateRequest request = new MockInterviewCreateRequest();
        request.setUserId(1L);
        request.setCategory("SPRING");
        request.setQuestionCount(1);
        request.setInterviewerStyle("BIG_TECH");
        return request;
    }

    private MockInterviewAnswerRequest answer(String text) {
        MockInterviewAnswerRequest request = new MockInterviewAnswerRequest();
        request.setUserAnswer(text);
        return request;
    }

    private KnowledgeCard card(Long id) {
        KnowledgeCard card = new KnowledgeCard();
        card.setId(id);
        card.setCategory("SPRING");
        card.setTitle("Spring Bean 生命周期");
        card.setQuestion("Spring Bean 生命周期是什么？");
        card.setAnswer("实例化、属性注入、初始化、使用、销毁，并可通过 BeanPostProcessor 扩展。");
        card.setFollowUp("树化阈值为什么不是 6？\n为什么 BeanPostProcessor 能影响初始化前后？");
        card.setKeyPoints("实例化\n依赖注入\n初始化\n销毁\nBeanPostProcessor");
        card.setEnabled(true);
        card.setSortOrder(1);
        return card;
    }

    private MockInterviewAiEvaluationResponse aiResponse(int score, String followUp) {
        MockInterviewAiEvaluationResponse response = new MockInterviewAiEvaluationResponse();
        response.setScore(score);
        response.setHitKeyPoints(List.of("实例化", "依赖注入", "初始化"));
        response.setMissingKeyPoints(List.of("销毁", "BeanPostProcessor"));
        response.setFeedback("回答覆盖主流程，但扩展点和销毁阶段不够完整。");
        response.setExpressionIssue("表达偏罗列，缺少阶段之间的顺序感。");
        response.setFollowUpQuestion(followUp);
        response.setWeaknessTags(List.of("销毁", "BeanPostProcessor"));
        response.setRecommendedCardIds(List.of(1831L));
        return response;
    }

    private MockInterviewSession session(Long id, String status, Long cardId, int answeredMainCount) {
        MockInterviewSession session = new MockInterviewSession();
        session.setId(id);
        session.setUserId(1L);
        session.setCategory("SPRING");
        session.setStatus(status);
        session.setQuestionCount(1);
        session.setAnsweredMainCount(answeredMainCount);
        session.setCurrentKnowledgeCardId(cardId);
        session.setInterviewerStyle("BIG_TECH");
        return session;
    }

    private MockInterviewTurn mainTurn(Long id) {
        MockInterviewTurn turn = new MockInterviewTurn();
        turn.setId(id);
        turn.setSessionId(9L);
        turn.setKnowledgeCardId(1831L);
        turn.setTurnOrder(1);
        turn.setTurnType(MockInterviewTurnTypeEnum.MAIN.name());
        turn.setQuestion("Spring Bean 生命周期是什么？");
        turn.setUserAnswer("提到了实例化、依赖注入和初始化。");
        turn.setScore(72);
        turn.setFeedback("回答覆盖主流程，但扩展点和销毁阶段不够完整。");
        turn.setHitKeyPoints("实例化\n依赖注入\n初始化");
        turn.setMissingKeyPoints("销毁\nBeanPostProcessor");
        turn.setExpressionIssue("表达偏罗列。");
        turn.setPerformanceLevel("基础合格");
        turn.setStrengthSummary("你已经能说出生命周期主流程。");
        turn.setGapSummary("初始化扩展机制还没有展开。");
        turn.setExpressionFeedback("建议按阶段和扩展点组织答案。");
        turn.setInterviewerObservation("你已经能说出生命周期主流程，但 Spring 面试里通常还会继续追初始化扩展机制。");
        turn.setFollowUpReason("我会继续追一下 BeanPostProcessor 与 AOP 的关系。");
        turn.setAiRawJson("{\"followUpQuestion\":\"为什么 BeanPostProcessor 能影响初始化前后？\"}");
        return turn;
    }

    private MockInterviewTurn followUpTurn(Long id) {
        MockInterviewTurn turn = mainTurn(id);
        turn.setTurnOrder(2);
        turn.setTurnType(MockInterviewTurnTypeEnum.FOLLOW_UP.name());
        turn.setParentTurnId(21L);
        turn.setQuestion("为什么 BeanPostProcessor 能影响初始化前后？");
        turn.setScore(50);
        turn.setMissingKeyPoints("销毁");
        return turn;
    }

    private MockInterviewTurn strongTurn(Long id) {
        MockInterviewTurn turn = mainTurn(id);
        turn.setScore(92);
        turn.setFeedback("回答结构清晰，核心流程和扩展点覆盖完整。");
        turn.setHitKeyPoints("实例化\n依赖注入\n初始化\n销毁\nBeanPostProcessor");
        turn.setMissingKeyPoints("");
        turn.setExpressionIssue("");
        turn.setGapSummary("");
        turn.setExpressionFeedback("表达结构清晰。");
        return turn;
    }

    private MockInterviewReport report(Long id) {
        MockInterviewReport report = new MockInterviewReport();
        report.setId(id);
        report.setSessionId(9L);
        report.setUserId(1L);
        report.setAverageScore(new java.math.BigDecimal("61.00"));
        report.setSummary("本次模拟面试平均分 61.00，建议围绕缺失要点继续复盘。");
        report.setStrengths("能覆盖部分主流程，具备基础表达能力。");
        report.setWeaknesses("销毁\nBeanPostProcessor");
        report.setExpressionAdvice("表达偏罗列。");
        report.setRecommendedCardIds("1831");
        report.setWeaknessTags("销毁,BeanPostProcessor");
        return report;
    }

    private MockInterviewTurn firstFollowUpTurn(Long id) {
        MockInterviewTurn turn = followUpTurn(id);
        turn.setScore(58);
        turn.setAiRawJson("{\"followUpQuestion\":\"那 Spring AOP 为什么依赖 BeanPostProcessor？\"}");
        return turn;
    }

    private Object reportValue(MockInterviewSessionVO session, String fieldName) {
        try {
            Object report = session.getReport();
            assertThat(report).isNotNull();
            java.lang.reflect.Field field = report.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(report);
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing mock interview report field: " + fieldName, e);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Cannot access mock interview report field: " + fieldName, e);
        }
    }
}
