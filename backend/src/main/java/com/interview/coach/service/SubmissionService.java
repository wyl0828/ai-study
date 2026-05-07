package com.interview.coach.service;

import com.interview.coach.dto.JudgeResult;
import com.interview.coach.dto.SubmitCodeRequest;
import com.interview.coach.entity.Submission;
import com.interview.coach.vo.SubmissionResultVO;

public interface SubmissionService {

    SubmissionResultVO submit(SubmitCodeRequest request);

    Submission getSubmissionOrThrow(Long submissionId);

    JudgeResult rejudge(Long submissionId);
}
