package com.interview.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.coach.dto.FailedCaseResult;
import com.interview.coach.dto.JudgeCase;
import com.interview.coach.dto.JudgeResult;
import com.interview.coach.dto.SubmitCodeRequest;
import com.interview.coach.entity.Problem;
import com.interview.coach.entity.Submission;
import com.interview.coach.entity.TestCase;
import com.interview.coach.enums.LanguageEnum;
import com.interview.coach.enums.SubmissionStatusEnum;
import com.interview.coach.handler.BusinessException;
import com.interview.coach.mapper.SubmissionMapper;
import com.interview.coach.mapper.TestCaseMapper;
import com.interview.coach.service.JudgeService;
import com.interview.coach.service.ProblemService;
import com.interview.coach.service.SubmissionService;
import com.interview.coach.vo.FailedCaseVO;
import com.interview.coach.vo.SubmissionResultVO;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final ProblemService problemService;

    private final TestCaseMapper testCaseMapper;

    private final SubmissionMapper submissionMapper;

    private final JudgeService judgeService;

    @Override
    @Transactional
    public SubmissionResultVO submit(SubmitCodeRequest request) {
        if (!LanguageEnum.isJava(request.getLanguage())) {
            return unsupportedLanguageResult(request);
        }

        Problem problem = problemService.getEnabledProblem(request.getProblemId());
        List<TestCase> testCases = listTestCases(problem.getId());
        if (testCases.isEmpty()) {
            throw new BusinessException(500, "problem has no test cases");
        }

        Submission submission = createRunningSubmission(request);
        JudgeResult judgeResult = judgeService.judgeJava(request.getCode(), toJudgeCases(testCases));
        updateSubmission(submission, judgeResult);
        return toSubmissionResultVO(submission, judgeResult);
    }

    @Override
    public Submission getSubmissionOrThrow(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(404, "submission not found");
        }
        return submission;
    }

    @Override
    @Transactional
    public JudgeResult rejudge(Long submissionId) {
        Submission submission = getSubmissionOrThrow(submissionId);
        if (!LanguageEnum.isJava(submission.getLanguage())) {
            JudgeResult result = new JudgeResult();
            result.setStatus(SubmissionStatusEnum.UNSUPPORTED_LANGUAGE);
            result.setErrorMessage("Phase 1 supports Java only");
            return result;
        }
        Problem problem = problemService.getEnabledProblem(submission.getProblemId());
        List<TestCase> testCases = listTestCases(problem.getId());
        if (testCases.isEmpty()) {
            throw new BusinessException(500, "problem has no test cases");
        }
        JudgeResult judgeResult = judgeService.judgeJava(submission.getCode(), toJudgeCases(testCases));
        updateSubmission(submission, judgeResult);
        return judgeResult;
    }

    private SubmissionResultVO unsupportedLanguageResult(SubmitCodeRequest request) {
        Submission submission = new Submission();
        LocalDateTime now = LocalDateTime.now();
        submission.setUserId(request.getUserId());
        submission.setProblemId(request.getProblemId());
        submission.setLanguage(request.getLanguage());
        submission.setCode(request.getCode());
        submission.setStatus(SubmissionStatusEnum.UNSUPPORTED_LANGUAGE.name());
        submission.setPassedCount(0);
        submission.setTotalCount(0);
        submission.setErrorMessage("Phase 1 supports Java only");
        submission.setCreatedAt(now);
        submission.setUpdatedAt(now);
        submissionMapper.insert(submission);

        JudgeResult result = new JudgeResult();
        result.setStatus(SubmissionStatusEnum.UNSUPPORTED_LANGUAGE);
        result.setErrorMessage(submission.getErrorMessage());
        return toSubmissionResultVO(submission, result);
    }

    private List<TestCase> listTestCases(Long problemId) {
        return testCaseMapper.selectList(new LambdaQueryWrapper<TestCase>()
                .eq(TestCase::getProblemId, problemId)
                .orderByAsc(TestCase::getId));
    }

    private Submission createRunningSubmission(SubmitCodeRequest request) {
        Submission submission = new Submission();
        LocalDateTime now = LocalDateTime.now();
        submission.setUserId(request.getUserId());
        submission.setProblemId(request.getProblemId());
        submission.setLanguage("java");
        submission.setCode(request.getCode());
        submission.setStatus(SubmissionStatusEnum.RUNNING.name());
        submission.setPassedCount(0);
        submission.setTotalCount(0);
        submission.setCreatedAt(now);
        submission.setUpdatedAt(now);
        submissionMapper.insert(submission);
        return submission;
    }

    private List<JudgeCase> toJudgeCases(List<TestCase> testCases) {
        return testCases.stream()
                .map(testCase -> new JudgeCase(
                        testCase.getId(),
                        testCase.getInputData(),
                        testCase.getExpectedOutput()))
                .toList();
    }

    private void updateSubmission(Submission submission, JudgeResult judgeResult) {
        submission.setStatus(judgeResult.getStatus().name());
        submission.setPassedCount(judgeResult.getPassedCount());
        submission.setTotalCount(judgeResult.getTotalCount());
        submission.setExecutionTime(judgeResult.getRuntime());
        submission.setMemoryUsage(judgeResult.getMemory());
        submission.setErrorMessage(judgeResult.getErrorMessage());
        submission.setUpdatedAt(LocalDateTime.now());
        submissionMapper.updateById(submission);
    }

    private SubmissionResultVO toSubmissionResultVO(Submission submission, JudgeResult judgeResult) {
        SubmissionResultVO vo = new SubmissionResultVO();
        vo.setSubmissionId(submission.getId());
        vo.setStatus(judgeResult.getStatus().name());
        vo.setPassedCount(judgeResult.getPassedCount());
        vo.setTotalCount(judgeResult.getTotalCount());
        vo.setRuntime(judgeResult.getRuntime());
        vo.setMemory(judgeResult.getMemory());
        vo.setErrorMessage(judgeResult.getErrorMessage());
        vo.setFailedCases(judgeResult.getFailedCases().stream().map(this::toFailedCaseVO).toList());
        return vo;
    }

    private FailedCaseVO toFailedCaseVO(FailedCaseResult result) {
        FailedCaseVO vo = new FailedCaseVO();
        vo.setCaseId(result.getCaseId());
        vo.setInput(result.getInput());
        vo.setExpectedOutput(result.getExpectedOutput());
        vo.setActualOutput(result.getActualOutput());
        return vo;
    }
}
