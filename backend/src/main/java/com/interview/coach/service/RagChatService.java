package com.interview.coach.service;

import com.interview.coach.vo.RagChatResponseVO;

public interface RagChatService {

    RagChatResponseVO ask(Long userId, String question);
}
