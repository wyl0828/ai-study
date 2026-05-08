package com.interview.coach.controller;

import com.interview.coach.service.UserLearningService;
import com.interview.coach.vo.ApiResponse;
import com.interview.coach.vo.DashboardStatsVO;
import com.interview.coach.vo.MistakeCardVO;
import com.interview.coach.vo.SubmissionHistoryVO;
import com.interview.coach.vo.TrainingPlanVO;
import com.interview.coach.vo.UserWeaknessVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserLearningService userLearningService;

    @GetMapping("/{userId}/dashboard/stats")
    public ApiResponse<DashboardStatsVO> getDashboardStats(@PathVariable Long userId) {
        return ApiResponse.success(userLearningService.getDashboardStats(userId));
    }

    @GetMapping("/{userId}/weaknesses")
    public ApiResponse<List<UserWeaknessVO>> getWeaknesses(@PathVariable Long userId) {
        return ApiResponse.success(userLearningService.getWeaknesses(userId));
    }

    @GetMapping("/{userId}/mistakes")
    public ApiResponse<List<MistakeCardVO>> getMistakes(@PathVariable Long userId) {
        return ApiResponse.success(userLearningService.getMistakes(userId));
    }

    @GetMapping("/{userId}/training-plans/latest")
    public ApiResponse<TrainingPlanVO> getLatestTrainingPlan(@PathVariable Long userId) {
        return ApiResponse.success(userLearningService.getLatestTrainingPlan(userId));
    }

    @GetMapping("/{userId}/submissions/recent")
    public ApiResponse<List<SubmissionHistoryVO>> getRecentSubmissions(@PathVariable Long userId) {
        return ApiResponse.success(userLearningService.getRecentSubmissions(userId));
    }
}
