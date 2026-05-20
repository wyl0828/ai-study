package com.interview.coach.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.coach.agent.AgentContext;
import com.interview.coach.agent.InterviewCoachAgent;
import com.interview.coach.dto.AgentExecutionObservation;
import com.interview.coach.dto.AiDiagnosisResult;
import com.interview.coach.dto.FailedCaseResult;
import com.interview.coach.mapper.AgentRunMapper;
import com.interview.coach.service.ProblemService;
import com.interview.coach.service.SubmissionService;
import com.interview.coach.vo.AgentAnalyzeVO;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentServiceImplTest {

    @Test
    void analyzeVoUsesFailedCaseAsFailurePhenomenonBeforeAiText() throws Exception {
        AgentContext context = new AgentContext();
        context.setAgentRunId(1L);
        context.setSubmissionId(10L);

        AgentExecutionObservation observation = new AgentExecutionObservation();
        observation.setErrorMessage("wrong answer");
        observation.setFailedCases(List.of(new FailedCaseResult(1L, "[3,3]\n6", "[0,1]", "[0,0]")));
        context.setObservation(observation);

        AiDiagnosisResult diagnosis = new AiDiagnosisResult();
        diagnosis.setErrorType("LOGIC_ERROR");
        diagnosis.setKnowledgePoint("HashMap 查询与写入顺序");
        diagnosis.setSpecificError("当前元素先写入 map，导致自己匹配自己。");
        diagnosis.setDiagnosis("HashMap 写入时机不对。");
        diagnosis.setSuggestion("每轮先查询 complement，再写入当前元素。");
        diagnosis.setFailurePhenomenon("AI 生成的失败现象不应优先展示。");
        diagnosis.setRootCause("当前元素先 put 进 map，重复元素场景会自匹配。");
        diagnosis.setRepairDirection("先查 complement，再 put 当前元素。");
        diagnosis.setInterviewReminder("重复元素场景要主动说明哈希表写入时机。");
        context.setDiagnosis(diagnosis);

        AgentAnalyzeVO vo = toAnalyzeVO(context);

        assertThat(vo.getFailurePhenomenon())
                .contains("[3,3]")
                .contains("期望输出 [0,1]")
                .contains("实际输出 [0,0]")
                .doesNotContain("AI 生成");
        assertThat(vo.getRootCause()).isEqualTo("当前元素先 put 进 map，重复元素场景会自匹配。");
        assertThat(vo.getRepairDirection()).isEqualTo("先查 complement，再 put 当前元素。");
        assertThat(vo.getInterviewReminder()).isEqualTo("重复元素场景要主动说明哈希表写入时机。");
        assertThat(vo.getSuggestion()).isEqualTo("每轮先查询 complement，再写入当前元素。");
    }

    @Test
    void analyzeVoFallsBackToLegacyDiagnosisFields() throws Exception {
        AgentContext context = new AgentContext();
        context.setAgentRunId(2L);
        context.setSubmissionId(11L);

        AgentExecutionObservation observation = new AgentExecutionObservation();
        observation.setErrorMessage("compile error: missing return");
        context.setObservation(observation);

        AiDiagnosisResult diagnosis = new AiDiagnosisResult();
        diagnosis.setKnowledgePoint("链表指针边界");
        diagnosis.setSpecificError("没有处理空链表。");
        diagnosis.setDiagnosis("递归出口缺失导致运行失败。");
        context.setDiagnosis(diagnosis);

        AgentAnalyzeVO vo = toAnalyzeVO(context);

        assertThat(vo.getFailurePhenomenon()).isEqualTo("compile error: missing return");
        assertThat(vo.getRootCause()).isEqualTo("递归出口缺失导致运行失败。");
        assertThat(vo.getRepairDirection()).isEqualTo("递归出口缺失导致运行失败。");
        assertThat(vo.getInterviewReminder()).contains("链表指针边界");
    }

    @Test
    void analyzeVoSummarizesRuntimeStackTraceAsReadableFailurePhenomenon() throws Exception {
        AgentContext context = new AgentContext();
        context.setAgentRunId(3L);
        context.setSubmissionId(12L);

        AgentExecutionObservation observation = new AgentExecutionObservation();
        observation.setErrorMessage("""
                Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
                    at java.base/java.util.Arrays.copyOf(Arrays.java:3536)
                    at java.base/java.lang.AbstractStringBuilder.ensureCapacityInternal(AbstractStringBuilder.java:228)
                    at Main.printList(Main.java:27)
                    at Main.main(Main.java:8)
                """);
        context.setObservation(observation);

        AiDiagnosisResult diagnosis = new AiDiagnosisResult();
        diagnosis.setKnowledgePoint("链表指针更新顺序");
        diagnosis.setSpecificError("指针更新顺序错误，可能形成环状链表。");
        diagnosis.setDiagnosis("链表反转时先移动 prev，导致 cur.next 指回自身。");
        context.setDiagnosis(diagnosis);

        AgentAnalyzeVO vo = toAnalyzeVO(context);

        assertThat(vo.getFailurePhenomenon())
                .contains("运行时异常")
                .contains("OutOfMemoryError")
                .contains("Java heap space")
                .doesNotContain("Arrays.copyOf")
                .doesNotContain("Main.printList")
                .doesNotContain("at java.base");
    }

    private AgentAnalyzeVO toAnalyzeVO(AgentContext context) throws Exception {
        AgentServiceImpl service = new AgentServiceImpl(
                nullSubmissionService(),
                nullProblemService(),
                nullAgent(),
                nullAgentRunMapper(),
                new ObjectMapper());
        Method method = AgentServiceImpl.class.getDeclaredMethod("toAnalyzeVO", AgentContext.class);
        method.setAccessible(true);
        return (AgentAnalyzeVO) method.invoke(service, context);
    }

    private SubmissionService nullSubmissionService() {
        return null;
    }

    private ProblemService nullProblemService() {
        return null;
    }

    private InterviewCoachAgent nullAgent() {
        return null;
    }

    private AgentRunMapper nullAgentRunMapper() {
        return null;
    }
}
