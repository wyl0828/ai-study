package com.interview.coach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.interview.coach.entity.MistakeCard;
import com.interview.coach.entity.Problem;
import com.interview.coach.entity.Submission;
import com.interview.coach.mapper.MistakeCardMapper;
import com.interview.coach.mapper.ProblemMapper;
import com.interview.coach.mapper.SubmissionMapper;
import com.interview.coach.mapper.TrainingPlanItemMapper;
import com.interview.coach.mapper.TrainingPlanMapper;
import com.interview.coach.mapper.UserWeaknessMapper;
import com.interview.coach.service.impl.UserLearningServiceImpl;
import com.interview.coach.vo.DashboardStatsVO;
import com.interview.coach.vo.MistakeCardVO;
import com.interview.coach.vo.SubmissionHistoryVO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserLearningServiceImplTest {

    @Mock
    private SubmissionMapper submissionMapper;

    @Mock
    private UserWeaknessMapper userWeaknessMapper;

    @Mock
    private MistakeCardMapper mistakeCardMapper;

    @Mock
    private TrainingPlanMapper trainingPlanMapper;

    @Mock
    private TrainingPlanItemMapper trainingPlanItemMapper;

    @Mock
    private ProblemMapper problemMapper;

    @InjectMocks
    private UserLearningServiceImpl userLearningService;

    @Test
    void getDashboardStatsCountsAcceptedDistinctProblems() {
        when(submissionMapper.selectCount(any())).thenReturn(4L);
        when(userWeaknessMapper.selectCount(any())).thenReturn(3L);
        when(mistakeCardMapper.selectCount(any())).thenReturn(2L);

        Submission firstAccepted = submission(101L, "ACCEPTED");
        Submission duplicateAccepted = submission(101L, "ACCEPTED");
        Submission secondAccepted = submission(102L, "ACCEPTED");
        when(submissionMapper.selectList(any()))
                .thenReturn(List.of(firstAccepted, duplicateAccepted, secondAccepted));

        DashboardStatsVO stats = userLearningService.getDashboardStats(1L);

        assertThat(stats.getTotalSubmissions()).isEqualTo(4);
        assertThat(stats.getPassedProblems()).isEqualTo(2);
        assertThat(stats.getWeakPointCount()).isEqualTo(3);
        assertThat(stats.getMistakeCount()).isEqualTo(2);
    }

    @Test
    void getLatestTrainingPlanReturnsNullWhenUserHasNoPlan() {
        when(trainingPlanMapper.selectOne(any())).thenReturn(null);

        assertThat(userLearningService.getLatestTrainingPlan(1L)).isNull();
    }

    @Test
    void getMistakesAddsProblemTitleFromBatchProblemLookup() {
        MistakeCard mistake = new MistakeCard();
        mistake.setId(9L);
        mistake.setProblemId(101L);
        mistake.setErrorType("LOGIC_ERROR");
        mistake.setKnowledgePoint("HashMap");
        mistake.setMistakeSummary("Checked complement after insert.");
        mistake.setCorrectIdea("Check complement before insert.");
        when(mistakeCardMapper.selectList(any())).thenReturn(List.of(mistake));
        when(problemMapper.selectBatchIds(any())).thenReturn(List.of(problem(101L, "Two Sum")));

        List<MistakeCardVO> mistakes = userLearningService.getMistakes(1L);

        assertThat(mistakes).hasSize(1);
        assertThat(mistakes.get(0).getProblemTitle()).isEqualTo("Two Sum");
        assertThat(mistakes.get(0).getMistakeSummary()).isEqualTo("Checked complement after insert.");
        assertThat(mistakes.get(0).getCorrectIdea()).isEqualTo("Check complement before insert.");
    }

    @Test
    void getRecentSubmissionsAddsProblemTitleFromBatchProblemLookup() {
        Submission submission = submission(102L, "WRONG_ANSWER");
        submission.setPassedCount(1);
        submission.setTotalCount(3);
        when(submissionMapper.selectList(any())).thenReturn(List.of(submission));
        when(problemMapper.selectBatchIds(any())).thenReturn(List.of(problem(102L, "Reverse Linked List")));

        List<SubmissionHistoryVO> submissions = userLearningService.getRecentSubmissions(1L);

        assertThat(submissions).hasSize(1);
        assertThat(submissions.get(0).getProblemTitle()).isEqualTo("Reverse Linked List");
        assertThat(submissions.get(0).getStatus()).isEqualTo("WRONG_ANSWER");
        assertThat(submissions.get(0).getPassedCount()).isEqualTo(1);
        assertThat(submissions.get(0).getTotalCount()).isEqualTo(3);
    }

    private Submission submission(Long problemId, String status) {
        Submission submission = new Submission();
        submission.setUserId(1L);
        submission.setProblemId(problemId);
        submission.setStatus(status);
        return submission;
    }

    private Problem problem(Long id, String title) {
        Problem problem = new Problem();
        problem.setId(id);
        problem.setTitle(title);
        return problem;
    }
}
