package com.interview.coach.service.impl;

import com.interview.coach.dto.FailedCaseResult;
import com.interview.coach.dto.JudgeCase;
import com.interview.coach.dto.JudgeResult;
import com.interview.coach.enums.SubmissionStatusEnum;
import com.interview.coach.integration.piston.PistonClient;
import com.interview.coach.integration.piston.PistonExecuteResponse;
import com.interview.coach.integration.piston.PistonExecutionException;
import com.interview.coach.integration.piston.PistonRunResult;
import com.interview.coach.service.JudgeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class JudgeServiceImpl implements JudgeService {

    private static final int FAILED_CASE_LIMIT = 5;

    private final PistonClient pistonClient;

    @Override
    public JudgeResult judgeJava(String code, List<JudgeCase> cases) {
        JudgeResult result = new JudgeResult();
        result.setTotalCount(cases.size());

        for (JudgeCase testCase : cases) {
            PistonExecuteResponse response;
            try {
                response = pistonClient.executeJava(code, testCase.getInput());
            } catch (PistonExecutionException ex) {
                result.setStatus(SubmissionStatusEnum.SYSTEM_ERROR);
                result.setErrorMessage(ex.getMessage());
                return result;
            }

            if (response == null) {
                result.setStatus(SubmissionStatusEnum.SYSTEM_ERROR);
                result.setErrorMessage("empty Piston execution response");
                return result;
            }

            PistonRunResult compile = response.getCompile();
            if (compile != null && hasFailure(compile)) {
                result.setStatus(SubmissionStatusEnum.COMPILE_ERROR);
                result.setErrorMessage(readOutput(compile));
                return result;
            }

            PistonRunResult run = response.getRun();
            if (hasTimeout(run)) {
                result.setStatus(SubmissionStatusEnum.TIME_LIMIT_EXCEEDED);
                result.setErrorMessage(readOutput(run));
                return result;
            }
            if (hasFailure(run)) {
                result.setStatus(SubmissionStatusEnum.RUNTIME_ERROR);
                result.setErrorMessage(readOutput(run));
                return result;
            }

            String actualOutput = run == null ? "" : run.getStdout();
            if (normalizeOutput(testCase.getExpectedOutput()).equals(normalizeOutput(actualOutput))) {
                result.setPassedCount(result.getPassedCount() + 1);
            } else {
                if (result.getFailedCases().size() < FAILED_CASE_LIMIT) {
                    result.getFailedCases().add(new FailedCaseResult(
                            testCase.getCaseId(),
                            testCase.getInput(),
                            testCase.getExpectedOutput(),
                            actualOutput));
                }
            }
        }

        if (result.getPassedCount().equals(result.getTotalCount())) {
            result.setStatus(SubmissionStatusEnum.ACCEPTED);
        } else {
            result.setStatus(SubmissionStatusEnum.WRONG_ANSWER);
            result.setErrorMessage("failed " + (result.getTotalCount() - result.getPassedCount()) + " test case(s)");
        }
        return result;
    }

    private boolean hasFailure(PistonRunResult runResult) {
        if (runResult == null) {
            return true;
        }
        return (runResult.getCode() != null && runResult.getCode() != 0)
                || StringUtils.hasText(runResult.getSignal())
                || StringUtils.hasText(runResult.getStderr());
    }

    private boolean hasTimeout(PistonRunResult runResult) {
        if (runResult == null || !StringUtils.hasText(runResult.getSignal())) {
            return false;
        }
        String signal = runResult.getSignal().toUpperCase();
        return signal.contains("KILL") || signal.contains("TERM") || signal.contains("TIME");
    }

    private String readOutput(PistonRunResult runResult) {
        if (runResult == null) {
            return "empty Piston execution response";
        }
        if (StringUtils.hasText(runResult.getStderr())) {
            return runResult.getStderr();
        }
        if (StringUtils.hasText(runResult.getOutput())) {
            return runResult.getOutput();
        }
        if (StringUtils.hasText(runResult.getStdout())) {
            return runResult.getStdout();
        }
        if (StringUtils.hasText(runResult.getSignal())) {
            return "process terminated by signal: " + runResult.getSignal();
        }
        return "execution failed";
    }

    private String normalizeOutput(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (normalized.isEmpty()) {
            return "";
        }
        return String.join("\n", normalized.lines().map(String::trim).toList());
    }
}
