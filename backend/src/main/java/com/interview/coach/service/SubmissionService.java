package com.interview.coach.service;

import com.interview.coach.dto.SubmitCodeRequest;
import com.interview.coach.vo.SubmissionResultVO;

public interface SubmissionService {

    SubmissionResultVO submit(SubmitCodeRequest request);
}
