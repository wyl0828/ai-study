package com.interview.coach.service;

import com.interview.coach.dto.MockInterviewAnswerRequest;
import com.interview.coach.dto.MockInterviewCreateRequest;
import com.interview.coach.vo.MockInterviewSessionVO;

public interface MockInterviewService {

    MockInterviewSessionVO create(MockInterviewCreateRequest request);

    MockInterviewSessionVO getSession(Long sessionId);

    MockInterviewSessionVO answer(Long sessionId, MockInterviewAnswerRequest request);

    MockInterviewSessionVO finish(Long sessionId);
}
