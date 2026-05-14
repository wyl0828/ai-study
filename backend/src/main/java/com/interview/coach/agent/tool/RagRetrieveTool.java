package com.interview.coach.agent.tool;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.RagRetrieveResult;
import com.interview.coach.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RagRetrieveTool implements Tool<AgentContext, RagRetrieveResult> {

    private static final int DEFAULT_LIMIT = 5;

    private final RagService ragService;

    @Override
    public String name() {
        return "RagRetrieveTool";
    }

    @Override
    public RagRetrieveResult execute(AgentContext input, AgentContext context) {
        RagRetrieveResult result = ragService.retrieveForDiagnosis(context, DEFAULT_LIMIT);
        context.setRagRetrieveResult(result);
        return result;
    }
}
