package com.interview.coach.controller;

import com.interview.coach.auth.CurrentUserContext;
import com.interview.coach.dto.SubmitCodeRequest;
import com.interview.coach.service.SubmissionService;
import com.interview.coach.vo.ApiResponse;
import com.interview.coach.vo.SubmissionResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    private final CurrentUserContext currentUserContext;

    @PostMapping
    public ApiResponse<SubmissionResultVO> submit(@Valid @RequestBody SubmitCodeRequest request) {
        request.setUserId(currentUserContext.requireUserId());
        return ApiResponse.success(submissionService.submit(request));
    }
}
