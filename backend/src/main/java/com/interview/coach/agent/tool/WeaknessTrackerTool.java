package com.interview.coach.agent.tool;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.service.LearningTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WeaknessTrackerTool implements Tool<AgentContext, AgentContext> {

    private final LearningTracker learningTracker;

    @Override
    public String name() {
        return "WeaknessTrackerTool";
    }

    @Override
    public AgentContext execute(AgentContext input, AgentContext context) {
        learningTracker.recordDiagnosis(context);
        return context;
    }
}
