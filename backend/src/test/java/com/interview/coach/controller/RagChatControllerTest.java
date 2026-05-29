package com.interview.coach.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.dto.RagChatRequest;
import com.interview.coach.service.RagChatService;
import com.interview.coach.service.RagService;
import com.interview.coach.vo.ApiResponse;
import com.interview.coach.vo.RagChatResponseVO;
import com.interview.coach.vo.RagHealthVO;
import com.interview.coach.vo.RagSystemRebuildVO;
import com.interview.coach.vo.RagVectorRetryVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RagChatControllerTest {

    @Mock
    private RagChatService ragChatService;

    @Mock
    private RagService ragService;

    @Test
    void healthReturnsRagHealthWithoutRawRetrieval() {
        RagHealthVO health = new RagHealthVO();
        health.setHealthy(true);
        health.setSystemChunkCount(12);
        health.setUserMemoryChunkCount(3);
        health.setWarnings(List.of());
        when(ragService.checkHealth()).thenReturn(health);
        RagChatController controller = new RagChatController(ragChatService, ragService);

        ApiResponse<RagHealthVO> response = controller.health();

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).isSameAs(health);
        assertThat(response.getData().getSystemChunkCount()).isEqualTo(12);
        verify(ragService).checkHealth();
    }

    @Test
    void chatStillUsesControlledRagChatService() {
        RagChatResponseVO chatResponse = new RagChatResponseVO();
        chatResponse.setAnswer("HashMap 要先查 complement。");
        when(ragChatService.ask(1L, "Two Sum 为什么错？")).thenReturn(chatResponse);
        RagChatRequest request = new RagChatRequest();
        request.setUserId(1L);
        request.setQuestion("Two Sum 为什么错？");
        RagChatController controller = new RagChatController(ragChatService, ragService);

        ApiResponse<RagChatResponseVO> response = controller.chat(request);

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).isSameAs(chatResponse);
        verify(ragChatService).ask(1L, "Two Sum 为什么错？");
    }

    @Test
    void retryFailedVectorsUsesMaintenanceServiceWithoutRawRetrieval() {
        RagVectorRetryVO retry = new RagVectorRetryVO();
        retry.setEnabled(true);
        retry.setAttemptedCount(2);
        retry.setIndexedCount(1);
        retry.setFailedCount(1);
        retry.setRequestedLimit(20);
        retry.setEffectiveLimit(20);
        retry.setRetriedAt(LocalDateTime.of(2026, 5, 28, 11, 30));
        retry.setStatusLabel("RETRY_PARTIAL_FAILED");
        retry.setMaintenanceAction("Inspect embedding/Qdrant connectivity, then rerun POST /api/rag/vector/retry-failed?limit=20.");
        retry.setSummary("Vector retry summary: matched=2, attempted=2, indexed=1, failed=1, skipped=0.");
        when(ragService.retryFailedVectors(20)).thenReturn(retry);
        RagChatController controller = new RagChatController(ragChatService, ragService);

        ApiResponse<RagVectorRetryVO> response = controller.retryFailedVectors(20);

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).isSameAs(retry);
        assertThat(response.getData().getIndexedCount()).isEqualTo(1);
        assertThat(response.getData().getRequestedLimit()).isEqualTo(20);
        assertThat(response.getData().getEffectiveLimit()).isEqualTo(20);
        assertThat(response.getData().getRetriedAt()).isEqualTo(LocalDateTime.of(2026, 5, 28, 11, 30));
        assertThat(response.getData().getStatusLabel()).isEqualTo("RETRY_PARTIAL_FAILED");
        assertThat(response.getData().getMaintenanceAction()).contains("/api/rag/vector/retry-failed");
        assertThat(response.getData().getSummary()).contains("Vector retry summary", "attempted=2");
        verify(ragService).retryFailedVectors(20);
    }

    @Test
    void rebuildSystemIndexUsesMaintenanceServiceWithoutRawRetrieval() {
        RagSystemRebuildVO rebuild = new RagSystemRebuildVO();
        rebuild.setAttempted(true);
        rebuild.setSuccess(true);
        rebuild.setIndexedProblemCount(20);
        rebuild.setIndexedKnowledgeCardCount(120);
        rebuild.setRebuiltAt(LocalDateTime.of(2026, 5, 28, 11, 35));
        rebuild.setStatusLabel("REBUILT");
        rebuild.setMaintenanceAction("No RAG rebuild follow-up required; system problem and knowledge-card indexes were rebuilt.");
        rebuild.setSummary("System RAG rebuild summary: system documents 140 -> 140, user memory documents 8 -> 8.");
        when(ragService.rebuildSystemIndexForMaintenance()).thenReturn(rebuild);
        RagChatController controller = new RagChatController(ragChatService, ragService);

        ApiResponse<RagSystemRebuildVO> response = controller.rebuildSystemIndex();

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).isSameAs(rebuild);
        assertThat(response.getData().getIndexedKnowledgeCardCount()).isEqualTo(120);
        assertThat(response.getData().getRebuiltAt()).isEqualTo(LocalDateTime.of(2026, 5, 28, 11, 35));
        assertThat(response.getData().getStatusLabel()).isEqualTo("REBUILT");
        assertThat(response.getData().getMaintenanceAction()).contains("No RAG rebuild follow-up required");
        assertThat(response.getData().getSummary()).contains("System RAG rebuild summary", "user memory");
        verify(ragService).rebuildSystemIndexForMaintenance();
    }
}
