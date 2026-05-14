package com.interview.coach.controller;

import com.interview.coach.dto.SelfTestSubmitRequest;
import com.interview.coach.dto.TrainingPlanItemStatusRequest;
import com.interview.coach.dto.TrainingPlanRegenerateRequest;
import com.interview.coach.service.KnowledgeLearningService;
import com.interview.coach.service.TrainingPlanService;
import com.interview.coach.service.UserLearningService;
import com.interview.coach.vo.ApiResponse;
import com.interview.coach.vo.DashboardStatsVO;
import com.interview.coach.vo.ErrorStatsVO;
import com.interview.coach.vo.MistakeCardVO;
import com.interview.coach.vo.SelfTestRecordVO;
import com.interview.coach.vo.SubmissionHistoryVO;
import com.interview.coach.vo.TrainingPlanVO;
import com.interview.coach.vo.UserWeaknessEventVO;
import com.interview.coach.vo.UserWeaknessVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserLearningService userLearningService;

    private final TrainingPlanService trainingPlanService;

    private final KnowledgeLearningService knowledgeLearningService;

    @GetMapping("/{userId}/dashboard/stats")
    public ApiResponse<DashboardStatsVO> getDashboardStats(@PathVariable Long userId) {
        return ApiResponse.success(userLearningService.getDashboardStats(userId));
    }

    @GetMapping("/{userId}/weaknesses")
    public ApiResponse<List<UserWeaknessVO>> getWeaknesses(@PathVariable Long userId) {
        return ApiResponse.success(userLearningService.getWeaknesses(userId));
    }

    @GetMapping("/{userId}/weakness-events/recent")
    public ApiResponse<List<UserWeaknessEventVO>> getRecentWeaknessEvents(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(userLearningService.getRecentWeaknessEvents(userId, limit));
    }

    @GetMapping("/{userId}/mistakes")
    public ApiResponse<List<MistakeCardVO>> getMistakes(@PathVariable Long userId) {
        return ApiResponse.success(userLearningService.getMistakes(userId));
    }

    @GetMapping("/{userId}/training-plans/latest")
    public ApiResponse<TrainingPlanVO> getLatestTrainingPlan(@PathVariable Long userId) {
        return ApiResponse.success(userLearningService.getLatestTrainingPlan(userId));
    }

    @PatchMapping("/{userId}/training-plans/items/{itemId}/status")
    public ApiResponse<Void> updateTrainingPlanItemStatus(
            @PathVariable Long userId,
            @PathVariable Long itemId,
            @Valid @RequestBody TrainingPlanItemStatusRequest request) {
        trainingPlanService.updateItemStatus(userId, itemId, request.getStatus());
        return ApiResponse.success(null);
    }

    @PostMapping("/{userId}/training-plans/regenerate")
    public ApiResponse<Void> regenerateTrainingPlan(
            @PathVariable Long userId,
            @RequestBody(required = false) TrainingPlanRegenerateRequest request) {
        boolean replaceCurrentPlan = request == null || !Boolean.FALSE.equals(request.getReplaceCurrentPlan());
        String reason = request == null ? null : request.getReason();
        trainingPlanService.regenerate(userId, replaceCurrentPlan, reason);
        return ApiResponse.success(null);
    }

    @PostMapping("/{userId}/knowledge/cards/{cardId}/self-tests")
    public ApiResponse<SelfTestRecordVO> submitSelfTest(
            @PathVariable Long userId,
            @PathVariable Long cardId,
            @Valid @RequestBody SelfTestSubmitRequest request) {
        return ApiResponse.success(knowledgeLearningService.submitSelfTest(userId, cardId, request));
    }

    @GetMapping("/{userId}/knowledge/cards/{cardId}/self-tests/recent")
    public ApiResponse<List<SelfTestRecordVO>> getRecentSelfTests(
            @PathVariable Long userId,
            @PathVariable Long cardId,
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.success(knowledgeLearningService.getRecentSelfTests(userId, cardId, limit));
    }

    @GetMapping("/{userId}/submissions/recent")
    public ApiResponse<List<SubmissionHistoryVO>> getRecentSubmissions(@PathVariable Long userId) {
        return ApiResponse.success(userLearningService.getRecentSubmissions(userId));
    }

    @GetMapping("/{userId}/dashboard/error-stats")
    public ApiResponse<ErrorStatsVO> getErrorStats(@PathVariable Long userId) {
        return ApiResponse.success(userLearningService.getErrorStats(userId));
    }
}
