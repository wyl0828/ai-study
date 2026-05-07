package com.interview.coach.service;

import com.interview.coach.vo.AgentAnalyzeVO;
import java.util.function.Consumer;

public interface AgentService {

    AgentAnalyzeVO analyze(Long submissionId);

    AgentAnalyzeVO analyze(Long submissionId, Consumer<String> eventSink);
}
