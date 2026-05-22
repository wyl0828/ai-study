package com.interview.coach.controller;

import com.interview.coach.dto.RagChatRequest;
import com.interview.coach.service.RagChatService;
import com.interview.coach.vo.ApiResponse;
import com.interview.coach.vo.RagChatResponseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rag")
public class RagChatController {

    private final RagChatService ragChatService;

    @PostMapping("/chat")
    public ApiResponse<RagChatResponseVO> chat(@Valid @RequestBody RagChatRequest request) {
        return ApiResponse.success(ragChatService.ask(request.getUserId(), request.getQuestion()));
    }
}
