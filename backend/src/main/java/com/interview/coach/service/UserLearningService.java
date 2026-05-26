package com.interview.coach.service;

import com.interview.coach.vo.DashboardStatsVO;
import com.interview.coach.vo.ErrorStatsVO;
import com.interview.coach.vo.MistakeCardVO;
import com.interview.coach.vo.MockInterviewRecentVO;
import com.interview.coach.vo.MockInterviewTrendVO;
import com.interview.coach.vo.SubmissionHistoryVO;
import com.interview.coach.vo.TrainingPlanActivityVO;
import com.interview.coach.vo.TrainingPlanHistoryVO;
import com.interview.coach.vo.TrainingPlanVO;
import com.interview.coach.vo.UserWeaknessEventVO;
import com.interview.coach.vo.UserWeaknessVO;
import java.util.List;

public interface UserLearningService {

    DashboardStatsVO getDashboardStats(Long userId);

    List<UserWeaknessVO> getWeaknesses(Long userId);

    List<UserWeaknessEventVO> getRecentWeaknessEvents(Long userId, int limit);

    List<MistakeCardVO> getMistakes(Long userId);

    TrainingPlanVO getLatestTrainingPlan(Long userId);

    List<TrainingPlanHistoryVO> getTrainingPlanHistory(Long userId, int limit);

    List<TrainingPlanActivityVO> getRecentTrainingActivities(Long userId, int limit);

    List<SubmissionHistoryVO> getRecentSubmissions(Long userId);

    List<MockInterviewRecentVO> getRecentMockInterviews(Long userId, int limit);

    List<MockInterviewTrendVO> getMockInterviewTrends(Long userId, int limit);

    ErrorStatsVO getErrorStats(Long userId);
}
