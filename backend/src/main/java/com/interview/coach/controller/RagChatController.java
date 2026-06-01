package com.interview.coach.controller;

import com.interview.coach.auth.CurrentUserContext;
import com.interview.coach.dto.RagChatRequest;
import com.interview.coach.service.RagChatService;
import com.interview.coach.service.RagService;
import com.interview.coach.vo.ApiResponse;
import com.interview.coach.vo.RagChatResponseVO;
import com.interview.coach.vo.RagHealthVO;
import com.interview.coach.vo.RagSystemRebuildVO;
import com.interview.coach.vo.RagVectorRetryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rag")
public class RagChatController {

    private final RagChatService ragChatService;

    private final RagService ragService;

    private final CurrentUserContext currentUserContext;

    @GetMapping("/health")
    public ApiResponse<RagHealthVO> health() {
        return ApiResponse.success(ragService.checkHealth());
    }

    @PostMapping("/vector/retry-failed")
    public ApiResponse<RagVectorRetryVO> retryFailedVectors(
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(ragService.retryFailedVectors(limit));
    }

    @PostMapping("/system-index/rebuild")
    public ApiResponse<RagSystemRebuildVO> rebuildSystemIndex() {
        return ApiResponse.success(ragService.rebuildSystemIndexForMaintenance());
    }

    @PostMapping("/chat")
    public ApiResponse<RagChatResponseVO> chat(@Valid @RequestBody RagChatRequest request) {
        return ApiResponse.success(ragChatService.ask(currentUserContext.requireUserId(), request.getQuestion()));
    }
}
