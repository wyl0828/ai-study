package com.interview.coach.agent.tool;

import com.interview.coach.agent.AgentContext;

public interface Tool<I, O> {

    String name();

    O execute(I input, AgentContext context);
}
