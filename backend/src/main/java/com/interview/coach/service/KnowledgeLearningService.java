package com.interview.coach.service;

import com.interview.coach.dto.SelfTestSubmitRequest;
import com.interview.coach.vo.SelfTestRecordVO;
import java.util.List;

public interface KnowledgeLearningService {

    SelfTestRecordVO submitSelfTest(Long userId, Long cardId, SelfTestSubmitRequest request);

    List<SelfTestRecordVO> getRecentSelfTests(Long userId, Long cardId, int limit);
}
