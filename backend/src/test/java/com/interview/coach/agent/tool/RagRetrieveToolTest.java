package com.interview.coach.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.RagRetrieveResult;
import com.interview.coach.service.RagService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RagRetrieveToolTest {

    @Mock
    private RagService ragService;

    @InjectMocks
    private RagRetrieveTool tool;

    @Test
    void executeRetrievesEvidenceAndStoresItOnContext() {
        AgentContext context = new AgentContext();
        RagRetrieveResult expected = new RagRetrieveResult();
        when(ragService.retrieveForDiagnosis(context, 5)).thenReturn(expected);

        RagRetrieveResult result = tool.execute(context, context);

        assertThat(result).isSameAs(expected);
        assertThat(context.getRagRetrieveResult()).isSameAs(expected);
        verify(ragService).retrieveForDiagnosis(context, 5);
    }
}
