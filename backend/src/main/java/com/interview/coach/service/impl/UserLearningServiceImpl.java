package com.interview.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.coach.entity.MistakeCard;
import com.interview.coach.entity.Problem;
import com.interview.coach.entity.Submission;
import com.interview.coach.entity.TrainingPlan;
import com.interview.coach.entity.TrainingPlanItem;
import com.interview.coach.entity.UserWeakness;
import com.interview.coach.enums.SubmissionStatusEnum;
import com.interview.coach.mapper.MistakeCardMapper;
import com.interview.coach.mapper.ProblemMapper;
import com.interview.coach.mapper.SubmissionMapper;
import com.interview.coach.mapper.TrainingPlanItemMapper;
import com.interview.coach.mapper.TrainingPlanMapper;
import com.interview.coach.mapper.UserWeaknessMapper;
import com.interview.coach.service.UserLearningService;
import com.interview.coach.vo.DashboardStatsVO;
import com.interview.coach.vo.ErrorStatsVO;
import com.interview.coach.vo.MistakeCardVO;
import com.interview.coach.vo.SubmissionHistoryVO;
import com.interview.coach.vo.TrainingPlanItemVO;
import com.interview.coach.vo.TrainingPlanVO;
import com.interview.coach.vo.UserWeaknessVO;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserLearningServiceImpl implements UserLearningService {

    private final SubmissionMapper submissionMapper;

    private final UserWeaknessMapper userWeaknessMapper;

    private final MistakeCardMapper mistakeCardMapper;

    private final TrainingPlanMapper trainingPlanMapper;

    private final TrainingPlanItemMapper trainingPlanItemMapper;

    private final ProblemMapper problemMapper;

    @Override
    public DashboardStatsVO getDashboardStats(Long userId) {
        DashboardStatsVO vo = new DashboardStatsVO();
        vo.setTotalSubmissions(toInt(submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, userId))));
        List<Submission> acceptedSubmissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, userId)
                .eq(Submission::getStatus, SubmissionStatusEnum.ACCEPTED.name()));
        vo.setPassedProblems((int) acceptedSubmissions.stream()
                .map(Submission::getProblemId)
                .distinct()
                .count());
        vo.setWeakPointCount(toInt(userWeaknessMapper.selectCount(new LambdaQueryWrapper<UserWeakness>()
                .eq(UserWeakness::getUserId, userId))));
        vo.setMistakeCount(toInt(mistakeCardMapper.selectCount(new LambdaQueryWrapper<MistakeCard>()
                .eq(MistakeCard::getUserId, userId))));
        return vo;
    }

    @Override
    public List<UserWeaknessVO> getWeaknesses(Long userId) {
        List<UserWeakness> weaknesses = userWeaknessMapper.selectList(new LambdaQueryWrapper<UserWeakness>()
                .eq(UserWeakness::getUserId, userId)
                .orderByDesc(UserWeakness::getWeaknessScore));
        return weaknesses.stream().map(this::toUserWeaknessVO).toList();
    }

    @Override
    public List<MistakeCardVO> getMistakes(Long userId) {
        List<MistakeCard> mistakes = mistakeCardMapper.selectList(new LambdaQueryWrapper<MistakeCard>()
                .eq(MistakeCard::getUserId, userId)
                .orderByDesc(MistakeCard::getCreatedAt));
        Map<Long, Problem> problems = problemsById(mistakes.stream()
                .map(MistakeCard::getProblemId)
                .toList());
        return mistakes.stream()
                .map(mistake -> toMistakeCardVO(mistake, problems))
                .toList();
    }

    @Override
    public TrainingPlanVO getLatestTrainingPlan(Long userId) {
        TrainingPlan plan = trainingPlanMapper.selectOne(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getUserId, userId)
                .orderByDesc(TrainingPlan::getCreatedAt)
                .last("LIMIT 1"));
        if (plan == null) {
            return null;
        }
        List<TrainingPlanItem> items = trainingPlanItemMapper.selectList(new LambdaQueryWrapper<TrainingPlanItem>()
                .eq(TrainingPlanItem::getPlanId, plan.getId())
                .orderByAsc(TrainingPlanItem::getDayIndex)
                .orderByAsc(TrainingPlanItem::getId));
        TrainingPlanVO vo = new TrainingPlanVO();
        vo.setTitle(plan.getTitle());
        vo.setSummary(plan.getSummary());
        vo.setItems(items.stream().map(this::toTrainingPlanItemVO).toList());
        return vo;
    }

    @Override
    public List<SubmissionHistoryVO> getRecentSubmissions(Long userId) {
        List<Submission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, userId)
                .orderByDesc(Submission::getCreatedAt)
                .last("LIMIT 10"));
        Map<Long, Problem> problems = problemsById(submissions.stream()
                .map(Submission::getProblemId)
                .toList());
        return submissions.stream()
                .map(submission -> toSubmissionHistoryVO(submission, problems))
                .toList();
    }

    @Override
    public ErrorStatsVO getErrorStats(Long userId) {
        List<UserWeakness> weaknesses = userWeaknessMapper.selectList(new LambdaQueryWrapper<UserWeakness>()
                .eq(UserWeakness::getUserId, userId));

        // 错误类型分布：按 errorType 分组统计 wrongCount 总和
        Map<String, Integer> errorTypeMap = weaknesses.stream()
                .collect(Collectors.groupingBy(
                        UserWeakness::getErrorType,
                        Collectors.summingInt(UserWeakness::getWrongCount)));
        List<ErrorStatsVO.ErrorTypeCount> errorTypeDistribution = errorTypeMap.entrySet().stream()
                .map(entry -> {
                    ErrorStatsVO.ErrorTypeCount count = new ErrorStatsVO.ErrorTypeCount();
                    count.setErrorType(entry.getKey());
                    count.setCount(entry.getValue());
                    return count;
                })
                .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
                .toList();

        // Top 5 薄弱点：按 weaknessScore 降序
        List<ErrorStatsVO.KnowledgeWeakness> topWeakPoints = weaknesses.stream()
                .sorted((a, b) -> b.getWeaknessScore().compareTo(a.getWeaknessScore()))
                .limit(5)
                .map(w -> {
                    ErrorStatsVO.KnowledgeWeakness kw = new ErrorStatsVO.KnowledgeWeakness();
                    kw.setKnowledgePoint(w.getKnowledgePoint());
                    kw.setErrorType(w.getErrorType());
                    kw.setWrongCount(w.getWrongCount());
                    kw.setWeaknessScore(w.getWeaknessScore().doubleValue());
                    return kw;
                })
                .toList();

        ErrorStatsVO vo = new ErrorStatsVO();
        vo.setErrorTypeDistribution(errorTypeDistribution);
        vo.setTopWeakPoints(topWeakPoints);
        return vo;
    }

    private UserWeaknessVO toUserWeaknessVO(UserWeakness weakness) {
        UserWeaknessVO vo = new UserWeaknessVO();
        vo.setId(weakness.getId());
        vo.setKnowledgePoint(weakness.getKnowledgePoint());
        vo.setErrorType(weakness.getErrorType());
        vo.setWrongCount(weakness.getWrongCount());
        vo.setWeaknessScore(weakness.getWeaknessScore());
        return vo;
    }

    private MistakeCardVO toMistakeCardVO(MistakeCard mistake, Map<Long, Problem> problems) {
        MistakeCardVO vo = new MistakeCardVO();
        vo.setId(mistake.getId());
        vo.setProblemId(mistake.getProblemId());
        vo.setProblemTitle(problemTitle(mistake.getProblemId(), problems));
        vo.setErrorType(mistake.getErrorType());
        vo.setKnowledgePoint(mistake.getKnowledgePoint());
        vo.setMistakeSummary(mistake.getMistakeSummary());
        vo.setCorrectIdea(mistake.getCorrectIdea());
        return vo;
    }

    private TrainingPlanItemVO toTrainingPlanItemVO(TrainingPlanItem item) {
        TrainingPlanItemVO vo = new TrainingPlanItemVO();
        vo.setItemType(item.getItemType() == null ? "PROBLEM" : item.getItemType());
        vo.setKnowledgeCardId(item.getKnowledgeCardId());
        vo.setDayIndex(item.getDayIndex());
        vo.setKnowledgePoint(item.getKnowledgePoint());
        vo.setProblemTitle(item.getProblemTitle());
        vo.setKnowledgeCardTitle(item.getKnowledgeCardTitle());
        vo.setReason(item.getReason());
        vo.setReviewFocus(item.getReviewFocus());
        vo.setStatus(item.getStatus());
        return vo;
    }

    private SubmissionHistoryVO toSubmissionHistoryVO(Submission submission, Map<Long, Problem> problems) {
        SubmissionHistoryVO vo = new SubmissionHistoryVO();
        vo.setProblemId(submission.getProblemId());
        vo.setProblemTitle(problemTitle(submission.getProblemId(), problems));
        vo.setStatus(submission.getStatus());
        vo.setPassedCount(submission.getPassedCount());
        vo.setTotalCount(submission.getTotalCount());
        vo.setCreatedAt(submission.getCreatedAt());
        return vo;
    }

    private Map<Long, Problem> problemsById(List<Long> problemIds) {
        List<Long> ids = problemIds.stream().distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return problemMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Problem::getId, Function.identity()));
    }

    private String problemTitle(Long problemId, Map<Long, Problem> problems) {
        Problem problem = problems.get(problemId);
        return problem == null ? "Unknown Problem" : problem.getTitle();
    }

    private Integer toInt(Long value) {
        return Math.toIntExact(value == null ? 0L : value);
    }
}
