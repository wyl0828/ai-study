package com.interview.coach.integration.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnthropicCompatibleClientTest {

    @Test
    void extractJsonKeepsObjectFromModelExplanation() {
        String text = """
                Here is the result:
                {"errorType":"BOUNDARY_ERROR","knowledgePoint":"HashMap"}
                """;

        String json = AnthropicCompatibleClient.extractJson(text);

        assertThat(json).isEqualTo("{\"errorType\":\"BOUNDARY_ERROR\",\"knowledgePoint\":\"HashMap\"}");
    }
}
