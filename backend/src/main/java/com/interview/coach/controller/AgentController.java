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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
        try {
            AgentAnalyzeVO result = agentService.analyze(submissionId,
                    stepJson -> sendEvent(emitter, "agent_step", stepJson));
            sendEvent(emitter, "done", result);
            emitter.complete();
        } catch (Exception ex) {
            sendEvent(emitter, "error", ApiResponse.fail(500, ex.getMessage()));
            emitter.complete();
        }
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }
}
