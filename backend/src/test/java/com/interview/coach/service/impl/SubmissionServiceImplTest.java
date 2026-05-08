package com.interview.coach.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.dto.JudgeResult;
import com.interview.coach.dto.SubmitCodeRequest;
import com.interview.coach.entity.Problem;
import com.interview.coach.entity.Submission;
import com.interview.coach.entity.TestCase;
import com.interview.coach.enums.SubmissionStatusEnum;
import com.interview.coach.mapper.SubmissionMapper;
import com.interview.coach.mapper.TestCaseMapper;
import com.interview.coach.service.JudgeService;
import com.interview.coach.service.ProblemService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceImplTest {

    @Mock
    private ProblemService problemService;

    @Mock
    private TestCaseMapper testCaseMapper;

    @Mock
    private SubmissionMapper submissionMapper;

    @Mock
    private JudgeService judgeService;

    @InjectMocks
    private SubmissionServiceImpl submissionService;

    @Test
    void submitSavesOriginalReverseListSolutionAndJudgesWrappedCode() {
        String solutionCode = """
                class Solution {
                    public ListNode reverseList(ListNode head) {
                        return head;
                    }
                }
                """;
        SubmitCodeRequest request = submitRequest(103L, solutionCode);
        when(problemService.getEnabledProblem(103L)).thenReturn(problem(103L));
        when(testCaseMapper.selectList(any())).thenReturn(List.of(testCase(7L, 103L)));
        doAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            submission.setId(55L);
            return 1;
        }).when(submissionMapper).insert(any(Submission.class));
        when(judgeService.judgeJava(any(), any())).thenReturn(acceptedResult());

        submissionService.submit(request);

        ArgumentCaptor<Submission> insertedSubmission = ArgumentCaptor.forClass(Submission.class);
        verify(submissionMapper).insert(insertedSubmission.capture());
        assertThat(insertedSubmission.getValue().getCode()).isEqualTo(solutionCode);

        ArgumentCaptor<String> judgedCode = ArgumentCaptor.forClass(String.class);
        verify(judgeService).judgeJava(judgedCode.capture(), any());
        assertThat(judgedCode.getValue()).contains("public class Main");
        assertThat(judgedCode.getValue()).contains("class ListNode");
        assertThat(judgedCode.getValue()).contains(solutionCode);
    }

    @Test
    void rejudgeWrapsPersistedReverseListSolutionBeforeJudging() {
        String solutionCode = "class Solution { public ListNode reverseList(ListNode head) { return head; } }";
        Submission submission = new Submission();
        submission.setId(77L);
        submission.setProblemId(103L);
        submission.setLanguage("java");
        submission.setCode(solutionCode);
        when(submissionMapper.selectById(77L)).thenReturn(submission);
        when(problemService.getEnabledProblem(103L)).thenReturn(problem(103L));
        when(testCaseMapper.selectList(any())).thenReturn(List.of(testCase(8L, 103L)));
        when(judgeService.judgeJava(any(), any())).thenReturn(acceptedResult());

        submissionService.rejudge(77L);

        ArgumentCaptor<String> judgedCode = ArgumentCaptor.forClass(String.class);
        verify(judgeService).judgeJava(judgedCode.capture(), any());
        assertThat(judgedCode.getValue()).contains("public class Main");
        assertThat(judgedCode.getValue()).contains(solutionCode);
    }

    @Test
    void submitKeepsMainModeCodeUnchangedForOtherProblems() {
        String mainCode = "public class Main {}";
        SubmitCodeRequest request = submitRequest(101L, mainCode);
        when(problemService.getEnabledProblem(101L)).thenReturn(problem(101L));
        when(testCaseMapper.selectList(any())).thenReturn(List.of(testCase(1L, 101L)));
        when(judgeService.judgeJava(any(), any())).thenReturn(acceptedResult());

        submissionService.submit(request);

        ArgumentCaptor<String> judgedCode = ArgumentCaptor.forClass(String.class);
        verify(judgeService).judgeJava(judgedCode.capture(), any());
        assertThat(judgedCode.getValue()).isSameAs(mainCode);
    }

    private SubmitCodeRequest submitRequest(Long problemId, String code) {
        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setUserId(1L);
        request.setProblemId(problemId);
        request.setLanguage("java");
        request.setCode(code);
        return request;
    }

    private Problem problem(Long id) {
        Problem problem = new Problem();
        problem.setId(id);
        problem.setEnabled(true);
        return problem;
    }

    private TestCase testCase(Long id, Long problemId) {
        TestCase testCase = new TestCase();
        testCase.setId(id);
        testCase.setProblemId(problemId);
        testCase.setInputData("0\n");
        testCase.setExpectedOutput("");
        return testCase;
    }

    private JudgeResult acceptedResult() {
        JudgeResult result = new JudgeResult();
        result.setStatus(SubmissionStatusEnum.ACCEPTED);
        result.setPassedCount(1);
        result.setTotalCount(1);
        return result;
    }
}
