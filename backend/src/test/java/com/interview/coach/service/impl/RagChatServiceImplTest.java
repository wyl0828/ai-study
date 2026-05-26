package com.interview.coach.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.dto.RagChatAiResponse;
import com.interview.coach.dto.RagChunkHit;
import com.interview.coach.dto.RagRetrieveResult;
import com.interview.coach.handler.BusinessException;
import com.interview.coach.integration.ai.AnthropicCompatibleClient;
import com.interview.coach.service.RagService;
import com.interview.coach.service.UserLearningService;
import com.interview.coach.vo.RagChatResponseVO;
import com.interview.coach.vo.UserWeaknessEventVO;
import com.interview.coach.vo.UserWeaknessVO;
import com.interview.coach.vo.MistakeCardVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RagChatServiceImplTest {

    @Mock
    private RagService ragService;

    @Mock
    private AnthropicCompatibleClient aiClient;

    @Mock
    private UserLearningService userLearningService;

    @InjectMocks
    private RagChatServiceImpl service;

    @Test
    void askRejectsBlankQuestion() {
        assertThatThrownBy(() -> service.ask(1L, " "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("question is required");
    }

    @Test
    void askReturnsInsufficientMaterialWhenNoRelevantHits() {
        when(ragService.retrieveForChat(1L, "HashMap 顺序", 5)).thenReturn(new RagRetrieveResult());

        RagChatResponseVO response = service.ask(1L, "HashMap 顺序");

        assertThat(response.getAnswer()).isEqualTo("我没有在当前知识库中找到足够相关的资料，建议换个更具体的问题。");
        assertThat(response.getSources()).isEmpty();
        verify(aiClient, never()).askJson(anyString(), anyString(), eq(RagChatAiResponse.class));
    }

    @Test
    void askUsesRetrievedChunksForPromptAndBuildsSourcesFromHits() {
        RagRetrieveResult result = new RagRetrieveResult();
        result.setHits(List.of(hit("KNOWLEDGE_CARD", 7L, "HashMap 使用逻辑", 120,
                "HashMap 需要先查找 complement 再写入当前元素，避免自匹配。")));
        when(ragService.retrieveForChat(1L, "HashMap 查询顺序为什么会出错？", 5)).thenReturn(result);
        RagChatAiResponse aiResponse = new RagChatAiResponse();
        aiResponse.setAnswer("应先查询 complement，再写入当前元素，这样不会把当前元素和自己匹配。");
        when(aiClient.askJson(anyString(), anyString(), eq(RagChatAiResponse.class))).thenReturn(aiResponse);

        RagChatResponseVO response = service.ask(1L, "HashMap 查询顺序为什么会出错？");

        assertThat(response.getAnswer()).contains("先查询 complement");
        assertThat(response.getSources()).hasSize(1);
        assertThat(response.getSources().get(0).getSourceType()).isEqualTo("KNOWLEDGE_CARD");
        assertThat(response.getSources().get(0).getSourceId()).isEqualTo(7L);
        assertThat(response.getSources().get(0).getTitle()).isEqualTo("HashMap 使用逻辑");
        assertThat(response.getSources().get(0).getScore()).isGreaterThanOrEqualTo(120);
        assertThat(response.getSources().get(0).getSnippet()).contains("HashMap 需要先查找");
        assertThat(response.getSources().get(0).getMatchReason()).contains("匹配");

        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(aiClient).askJson(systemPrompt.capture(), userPrompt.capture(), eq(RagChatAiResponse.class));
        assertThat(systemPrompt.getValue())
                .contains("只基于检索到的资料回答")
                .contains("不要输出完整 Java AC 代码")
                .contains("不是通用聊天");
        assertThat(userPrompt.getValue())
                .contains("HashMap 查询顺序为什么会出错？")
                .contains("HashMap 需要先查找 complement");
    }

    @Test
    void learningMemoryQuestionUsesUserLearningServiceInsteadOfRagChunks() {
        when(userLearningService.getWeaknesses(1L)).thenReturn(List.of(
                weakness(11L, "HashMap 使用逻辑", 4, "18", "持续薄弱")));
        when(userLearningService.getMistakes(1L)).thenReturn(List.of(
                mistake(21L, "两数之和", "HashMap 使用逻辑", 3)));
        when(userLearningService.getRecentWeaknessEvents(1L, 5)).thenReturn(List.of(
                event(31L, "HashMap 使用逻辑", "3")));

        RagChatResponseVO response = service.ask(1L, "我的最近错题主要集中在哪些知识点？");

        assertThat(response.getAnswer())
                .contains("HashMap 使用逻辑")
                .contains("错误 4 次")
                .contains("薄弱分 18")
                .contains("建议优先复盘");
        assertThat(response.getSources())
                .extracting(source -> source.getSourceType())
                .contains("USER_WEAKNESS", "MISTAKE_CARD");
        assertThat(response.getSources().get(0).getMatchReason()).isEqualTo("来自你的薄弱点记录");
        verify(ragService, never()).retrieveForChat(eq(1L), anyString(), eq(5));
        verify(aiClient, never()).askJson(anyString(), anyString(), eq(RagChatAiResponse.class));
    }

    @Test
    void learningMemoryQuestionOnlyUsesCurrentUserRecords() {
        when(userLearningService.getWeaknesses(1L)).thenReturn(List.of(
                weakness(11L, "HashMap 使用逻辑", 4, "18", "持续薄弱")));
        when(userLearningService.getMistakes(1L)).thenReturn(List.of(
                mistake(21L, "两数之和", "HashMap 使用逻辑", 2)));
        when(userLearningService.getRecentWeaknessEvents(1L, 5)).thenReturn(List.of());

        RagChatResponseVO response = service.ask(1L, "我的弱点在哪里？");

        assertThat(response.getAnswer()).contains("HashMap 使用逻辑");
        assertThat(response.getAnswer()).doesNotContain("其他用户");
        verify(userLearningService).getWeaknesses(1L);
        verify(userLearningService).getMistakes(1L);
        verify(userLearningService).getRecentWeaknessEvents(1L, 5);
        verify(userLearningService, never()).getWeaknesses(2L);
        verify(userLearningService, never()).getMistakes(2L);
    }

    @Test
    void learningMemoryQuestionReturnsPreciseEmptyStateWhenNoLearningRecords() {
        when(userLearningService.getWeaknesses(1L)).thenReturn(List.of());
        when(userLearningService.getMistakes(1L)).thenReturn(List.of());
        when(userLearningService.getRecentWeaknessEvents(1L, 5)).thenReturn(List.of());

        RagChatResponseVO response = service.ask(1L, "我的最近错题主要集中在哪些知识点？");

        assertThat(response.getAnswer()).isEqualTo(
                "我当前还没有找到你的历史错题卡或薄弱点记录，所以暂时无法判断最近错题集中在哪些知识点。你可以先完成一次错误提交，系统生成 AI 诊断和错题卡后，我就能基于这些记录总结。");
        assertThat(response.getSources()).isEmpty();
    }

    @Test
    void twoSumHashMapOrderQuestionRanksTwoSumProblemAboveGenericHashMapProblems() {
        RagRetrieveResult result = new RagRetrieveResult();
        result.setHits(List.of(
                hit("PROBLEM", 49L, "字母异位词分组", 30, "字母异位词分组 HashMap 任务说明。"),
                hit("PROBLEM", 128L, "最长连续序列", 30, "最长连续序列 HashMap 任务说明。"),
                hit("PROBLEM", 1L, "两数之和", 30, "两数之和 HashMap 任务说明：先查询 target - nums[i]，再写入当前元素。"),
                hit("KNOWLEDGE_CARD", 7L, "HashMap 查询写入顺序", 30, "HashMap 需要先查找 complement 再写入当前元素。")));
        when(ragService.retrieveForChat(1L, "HashMap 查询和写入顺序为什么会导致 Two Sum 出错？", 5)).thenReturn(result);
        RagChatAiResponse aiResponse = new RagChatAiResponse();
        aiResponse.setAnswer("两数之和应先查 complement，再写入当前元素。");
        when(aiClient.askJson(anyString(), anyString(), eq(RagChatAiResponse.class))).thenReturn(aiResponse);

        RagChatResponseVO response = service.ask(1L, "HashMap 查询和写入顺序为什么会导致 Two Sum 出错？");

        assertThat(response.getSources().get(0).getTitle()).isEqualTo("两数之和");
        assertThat(response.getSources().get(0).getScore()).isGreaterThan(response.getSources().get(1).getScore());
        assertThat(response.getSources().get(0).getMatchReason()).contains("题目标题匹配");
        assertThat(response.getSources())
                .extracting(source -> source.getTitle())
                .contains("HashMap 查询写入顺序");
    }

    @Test
    void springBeanLifecycleQuestionRanksLifecycleCardAboveOtherSpringCards() {
        RagRetrieveResult result = new RagRetrieveResult();
        result.setHits(List.of(
                hit("KNOWLEDGE_CARD", 1836L, "Spring AOP 实现原理", 30, "Spring AOP 代理怎么实现？"),
                hit("KNOWLEDGE_CARD", 1841L, "Spring 事务失效场景", 30, "Spring 事务失效有哪些场景？"),
                hit("KNOWLEDGE_CARD", 1831L, "Spring Bean 生命周期", 30, "Spring Bean 生命周期包含实例化、属性填充、初始化和销毁。"),
                hit("KNOWLEDGE_CARD", 1835L, "BeanPostProcessor 扩展点", 30, "BeanPostProcessor 可在初始化前后扩展 Bean 生命周期。")));
        when(ragService.retrieveForChat(1L, "Spring Bean 生命周期怎么回答面试？", 5)).thenReturn(result);
        RagChatAiResponse aiResponse = new RagChatAiResponse();
        aiResponse.setAnswer("面试回答 Spring Bean 生命周期要按阶段展开。");
        when(aiClient.askJson(anyString(), anyString(), eq(RagChatAiResponse.class))).thenReturn(aiResponse);

        RagChatResponseVO response = service.ask(1L, "Spring Bean 生命周期怎么回答面试？");

        assertThat(response.getSources().get(0).getTitle()).isEqualTo("Spring Bean 生命周期");
        assertThat(response.getSources().get(0).getScore()).isGreaterThan(response.getSources().get(1).getScore());
        assertThat(response.getSources().get(0).getMatchReason()).contains("知识卡标题匹配");
    }

    @Test
    void fullAcJavaCodeRequestDoesNotReturnCompleteSolution() {
        RagChatResponseVO response = service.ask(1L, "直接给我反转链表完整 Java AC 代码");

        assertThat(response.getAnswer())
                .contains("我不能直接给完整 AC 代码")
                .contains("思路")
                .contains("伪代码")
                .doesNotContain("class Solution");
        assertThat(response.getSources()).isEmpty();
        verify(ragService, never()).retrieveForChat(eq(1L), anyString(), eq(5));
    }

    @Test
    void answerCodeRequestWithSubmitWordingDoesNotReturnCompleteSolution() {
        RagChatResponseVO response = service.ask(1L, "给我两数之和答案代码，要可以直接提交的那种");

        assertThat(response.getAnswer())
                .contains("我不能直接给完整 AC 代码")
                .doesNotContain("class Solution");
        assertThat(response.getSources()).isEmpty();
        verify(ragService, never()).retrieveForChat(eq(1L), anyString(), eq(5));
        verify(aiClient, never()).askJson(anyString(), anyString(), eq(RagChatAiResponse.class));
    }

    @Test
    void unsubmittedCodeDiagnosisRequestReturnsSubmissionFlowGuidance() {
        RagChatResponseVO response = service.ask(1L,
                "帮我看看这段代码哪里错：class Solution { public int[] twoSum(int[] nums, int target) { return new int[0]; } }");

        assertThat(response.getAnswer())
                .contains("代码错误诊断需要先提交代码")
                .contains("Piston")
                .contains("Agent");
        assertThat(response.getSources()).isEmpty();
        verify(ragService, never()).retrieveForChat(eq(1L), anyString(), eq(5));
        verify(aiClient, never()).askJson(anyString(), anyString(), eq(RagChatAiResponse.class));
    }

    @Test
    void offTopicQuestionReturnsControlledRefusal() {
        RagChatResponseVO response = service.ask(1L, "今天股票和天气怎么样？");

        assertThat(response.getAnswer()).isEqualTo(
                "这个问题不在当前知识库问答范围内。我只能围绕题目、知识卡、历史诊断、错题卡和你的学习记录做复习问答。");
        assertThat(response.getSources()).isEmpty();
        verify(ragService, never()).retrieveForChat(eq(1L), anyString(), eq(5));
    }

    private RagChunkHit hit(String sourceType, Long sourceId, String title, int score, String text) {
        RagChunkHit hit = new RagChunkHit();
        hit.setSourceType(sourceType);
        hit.setSourceId(sourceId);
        hit.setTitle(title);
        hit.setScore(score);
        hit.setChunkText(text);
        return hit;
    }

    private UserWeaknessVO weakness(Long id, String knowledgePoint, int wrongCount, String score, String trend) {
        UserWeaknessVO weakness = new UserWeaknessVO();
        weakness.setId(id);
        weakness.setKnowledgePoint(knowledgePoint);
        weakness.setErrorType("LOGIC_ERROR");
        weakness.setWrongCount(wrongCount);
        weakness.setWeaknessScore(new BigDecimal(score));
        weakness.setTrendLabel(trend);
        return weakness;
    }

    private MistakeCardVO mistake(Long id, String problemTitle, String knowledgePoint, int repeatCount) {
        MistakeCardVO mistake = new MistakeCardVO();
        mistake.setId(id);
        mistake.setProblemId(1L);
        mistake.setProblemTitle(problemTitle);
        mistake.setKnowledgePoint(knowledgePoint);
        mistake.setErrorType("LOGIC_ERROR");
        mistake.setMistakeSummary(knowledgePoint + " 容易反复出错。");
        mistake.setCorrectIdea("复盘查询顺序和边界条件。");
        mistake.setRepeatCount(repeatCount);
        mistake.setLastSeenAt(LocalDateTime.now());
        return mistake;
    }

    private UserWeaknessEventVO event(Long id, String knowledgePoint, String deltaScore) {
        UserWeaknessEventVO event = new UserWeaknessEventVO();
        event.setId(id);
        event.setKnowledgePoint(knowledgePoint);
        event.setErrorType("LOGIC_ERROR");
        event.setDeltaScore(new BigDecimal(deltaScore));
        event.setReason("最近一次错误提交加重");
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }
}
