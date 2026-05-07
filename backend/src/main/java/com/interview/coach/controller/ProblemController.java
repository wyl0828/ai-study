package com.interview.coach.controller;

import com.interview.coach.service.ProblemService;
import com.interview.coach.vo.ApiResponse;
import com.interview.coach.vo.ProblemDetailVO;
import com.interview.coach.vo.ProblemListItemVO;
import com.interview.coach.vo.ProblemTemplateVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;

    @GetMapping
    public ApiResponse<List<ProblemListItemVO>> listProblems() {
        return ApiResponse.success(problemService.listProblems());
    }

    @GetMapping("/{id}")
    public ApiResponse<ProblemDetailVO> getProblemDetail(@PathVariable Long id) {
        return ApiResponse.success(problemService.getProblemDetail(id));
    }

    @GetMapping("/{id}/template")
    public ApiResponse<ProblemTemplateVO> getProblemTemplate(@PathVariable Long id) {
        return ApiResponse.success(problemService.getProblemTemplate(id));
    }
}
