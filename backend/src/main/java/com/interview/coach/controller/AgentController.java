package com.interview.coach.controller;

import com.interview.coach.dto.AgentAnalyzeRequest;
import com.interview.coach.service.AgentService;
import com.interview.coach.vo.AgentAnalyzeVO;
import com.interview.coach.vo.ApiResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AgentController {

    private final AgentService agentService;

    @Qualifier("agentTaskExecutor")
    private final Executor agentTaskExecutor;

    @PostMapping("/agent/analyze")
    public ApiResponse<AgentAnalyzeVO> analyze(@Valid @RequestBody AgentAnalyzeRequest request) {
        return ApiResponse.success(agentService.analyze(request.getSubmissionId()));
    }

    @GetMapping("/submissions/{submissionId}/diagnosis/stream")
    public SseEmitter streamDiagnosis(@PathVariable Long submissionId) {
        SseEmitter emitter = new SseEmitter(0L);
        CompletableFuture.runAsync(() -> runDiagnosisStream(submissionId, emitter), agentTaskExecutor);
        return emitter;
    }

    private void runDiagnosisStream(Long submissionId, SseEmitter emitter) {
        log.info("SSE diagnosis start: submissionId={}", submissionId);
        try {
            AgentAnalyzeVO result = agentService.analyze(submissionId,
                    stepJson -> {
                        log.debug("SSE step event: {}", stepJson);
                        sendEvent(emitter, "agent_step", stepJson);
                    });
            log.info("SSE sending done event: submissionId={}", submissionId);
            if (sendEvent(emitter, "done", result)) {
                log.info("SSE emitter complete: submissionId={}", submissionId);
                emitter.complete();
            }
        } catch (Exception ex) {
            log.error("SSE error: submissionId={}, message={}", submissionId, ex.getMessage(), ex);
            sendEvent(emitter, "error", ApiResponse.fail(500, ex.getMessage()));
            safeComplete(emitter);
        }
    }

    private boolean sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
            return true;
        } catch (IOException | IllegalStateException ex) {
            log.warn("SSE client disconnected or send failed: event={}, error={}", name, ex.getMessage());
            safeComplete(emitter);
            return false;
        } catch (Exception ex) {
            log.warn("SSE send failed: event={}, error={}", name, ex.getMessage());
            safeComplete(emitter);
            return false;
        }
    }

    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
        }
    }
}
