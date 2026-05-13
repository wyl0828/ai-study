package com.interview.coach.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.dto.JudgeCase;
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
    void submitStoresOriginalSolutionButJudgesWrappedCodeForSolutionProblem() {
        String userCode = """
                class Solution {
                    public int[] twoSum(int[] nums, int target) {
                        return new int[] {0, 1};
                    }
                }
                """;
        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setUserId(1L);
        request.setProblemId(1L);
        request.setLanguage("java");
        request.setCode(userCode);
        when(problemService.getEnabledProblem(1L)).thenReturn(problem(1L, "solution"));
        when(testCaseMapper.selectList(any())).thenReturn(List.of(testCase()));
        when(judgeService.judgeJava(any(), any())).thenReturn(acceptedResult());

        submissionService.submit(request);

        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionMapper).insert(submissionCaptor.capture());
        assertThat(submissionCaptor.getValue().getCode()).isEqualTo(userCode);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(judgeService).judgeJava(codeCaptor.capture(), any());
        assertThat(codeCaptor.getValue()).contains("public class Main");
        assertThat(codeCaptor.getValue()).contains("new Solution().twoSum(nums, target)");
        assertThat(codeCaptor.getValue()).contains("class Solution");
        assertThat(codeCaptor.getValue()).contains("public int[] twoSum(int[] nums, int target)");
    }

    @Test
    void rejudgeUsesWrappedCodeForSolutionProblem() {
        String userCode = """
                class Solution {
                    public ListNode mergeTwoLists(ListNode list1, ListNode list2) {
                        return list1;
                    }
                }
                """;
        Submission submission = new Submission();
        submission.setId(7L);
        submission.setProblemId(21L);
        submission.setLanguage("java");
        submission.setCode(userCode);
        when(submissionMapper.selectById(7L)).thenReturn(submission);
        when(problemService.getEnabledProblem(21L)).thenReturn(problem(21L, "solution"));
        when(testCaseMapper.selectList(any())).thenReturn(List.of(testCase()));
        when(judgeService.judgeJava(any(), any())).thenReturn(acceptedResult());

        submissionService.rejudge(7L);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(judgeService).judgeJava(codeCaptor.capture(), any());
        assertThat(codeCaptor.getValue()).contains("public class Main");
        assertThat(codeCaptor.getValue()).contains("class ListNode");
        assertThat(codeCaptor.getValue()).contains("new Solution().mergeTwoLists(list1, list2)");
        assertThat(codeCaptor.getValue()).contains("class Solution");
        assertThat(codeCaptor.getValue()).contains("public ListNode mergeTwoLists(ListNode list1, ListNode list2)");
    }

    @Test
    void submitLeavesAcmProblemCodeUnchanged() {
        String acmCode = "public class Main { public static void main(String[] args) {} }";
        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setUserId(1L);
        request.setProblemId(101L);
        request.setLanguage("java");
        request.setCode(acmCode);
        when(problemService.getEnabledProblem(101L)).thenReturn(problem(101L, "acm"));
        when(testCaseMapper.selectList(any())).thenReturn(List.of(testCase()));
        when(judgeService.judgeJava(eq(acmCode), any())).thenReturn(acceptedResult());

        submissionService.submit(request);

        verify(judgeService).judgeJava(eq(acmCode), any());
    }

    @Test
    void submitDoesNotWrapKnownSolutionIdWhenCodeModeIsAcm() {
        String solutionCode = """
                class Solution {
                    public int[] twoSum(int[] nums, int target) {
                        return new int[] {0, 1};
                    }
                }
                """;
        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setUserId(1L);
        request.setProblemId(1L);
        request.setLanguage("java");
        request.setCode(solutionCode);
        when(problemService.getEnabledProblem(1L)).thenReturn(problem(1L, "acm"));
        when(testCaseMapper.selectList(any())).thenReturn(List.of(testCase()));
        when(judgeService.judgeJava(eq(solutionCode), any())).thenReturn(acceptedResult());

        submissionService.submit(request);

        verify(judgeService).judgeJava(eq(solutionCode), any());
    }

    private Problem problem(Long id, String codeMode) {
        Problem problem = new Problem();
        problem.setId(id);
        problem.setCodeMode(codeMode);
        return problem;
    }

    private TestCase testCase() {
        TestCase testCase = new TestCase();
        testCase.setId(1L);
        testCase.setProblemId(102L);
        testCase.setInputData("a\nb\n");
        testCase.setExpectedOutput("false");
        return testCase;
    }

    private JudgeResult acceptedResult() {
        JudgeResult result = new JudgeResult();
        result.setStatus(SubmissionStatusEnum.ACCEPTED);
        result.setPassedCount(1);
        result.setTotalCount(1);
        result.setFailedCases(List.of());
        return result;
    }
}
