package com.interview.coach.controller;

import com.interview.coach.dto.MockInterviewAnswerRequest;
import com.interview.coach.dto.MockInterviewCreateRequest;
import com.interview.coach.service.MockInterviewService;
import com.interview.coach.vo.ApiResponse;
import com.interview.coach.vo.MockInterviewSessionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mock-interviews")
public class MockInterviewController {

    private final MockInterviewService mockInterviewService;

    @PostMapping
    public ApiResponse<MockInterviewSessionVO> create(@Valid @RequestBody MockInterviewCreateRequest request) {
        return ApiResponse.success(mockInterviewService.create(request));
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<MockInterviewSessionVO> getSession(@PathVariable Long sessionId) {
        return ApiResponse.success(mockInterviewService.getSession(sessionId));
    }

    @PostMapping("/{sessionId}/answers")
    public ApiResponse<MockInterviewSessionVO> answer(@PathVariable Long sessionId,
            @Valid @RequestBody MockInterviewAnswerRequest request) {
        return ApiResponse.success(mockInterviewService.answer(sessionId, request));
    }

    @PostMapping("/{sessionId}/finish")
    public ApiResponse<MockInterviewSessionVO> finish(@PathVariable Long sessionId) {
        return ApiResponse.success(mockInterviewService.finish(sessionId));
    }
}
