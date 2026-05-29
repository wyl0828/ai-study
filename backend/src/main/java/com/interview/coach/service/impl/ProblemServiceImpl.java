package com.interview.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.coach.entity.KnowledgePoint;
import com.interview.coach.entity.Problem;
import com.interview.coach.entity.ProblemKnowledgePoint;
import com.interview.coach.entity.TestCase;
import com.interview.coach.handler.BusinessException;
import com.interview.coach.mapper.KnowledgePointMapper;
import com.interview.coach.mapper.ProblemKnowledgePointMapper;
import com.interview.coach.mapper.ProblemMapper;
import com.interview.coach.mapper.TestCaseMapper;
import com.interview.coach.service.ProblemCacheService;
import com.interview.coach.service.ProblemService;
import com.interview.coach.vo.ProblemCacheRefreshVO;
import com.interview.coach.vo.ProblemCacheStatusVO;
import com.interview.coach.vo.ProblemDetailVO;
import com.interview.coach.vo.ProblemListItemVO;
import com.interview.coach.vo.ProblemTemplateVO;
import com.interview.coach.vo.TestCaseVO;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemService {

    private final ProblemMapper problemMapper;

    private final TestCaseMapper testCaseMapper;

    private final ProblemKnowledgePointMapper problemKnowledgePointMapper;

    private final KnowledgePointMapper knowledgePointMapper;

    private final ProblemCacheService problemCacheService;

    @Override
    public List<ProblemListItemVO> listProblems() {
        try {
            return problemCacheService.getProblemList()
                    .orElseGet(this::listProblemsFromMysqlAndCache);
        } catch (Exception ex) {
            log.warn("Problem list cache read failed; downgrade to MySQL: {}", ex.getMessage());
            return listProblemsFromMysqlAndCache();
        }
    }

    @Override
    public ProblemCacheRefreshVO refreshProblemCache() {
        ProblemCacheRefreshVO result = new ProblemCacheRefreshVO();
        result.setRefreshedAt(LocalDateTime.now());
        ProblemCacheStatusVO status = problemCacheService.status();
        result.setEnabled(status.getEnabled());
        result.setRedisAvailable(status.getRedisAvailable());
        if (!Boolean.TRUE.equals(status.getEnabled()) || !Boolean.TRUE.equals(status.getRedisAvailable())) {
            result.setMessage("Problem cache is disabled or Redis is unavailable; refresh skipped.");
            result.setSummary("Problem cache refresh skipped: Redis unavailable or disabled.");
            fillProblemCacheRefreshMaintenance(result);
            return result;
        }

        try {
            problemCacheService.evictAll();
        } catch (Exception ex) {
            result.setFailedCount(result.getFailedCount() + 1);
            log.warn("Problem cache evict failed before refresh; continue warm-up from MySQL: {}", ex.getMessage());
        }

        List<ProblemListItemVO> problems = listProblemsFromMysqlAndCache();
        result.setListWarmAttempted(true);
        for (ProblemListItemVO problem : problems) {
            if (problem == null || problem.getId() == null) {
                continue;
            }
            try {
                getProblemDetailFromMysqlAndCache(problem.getId());
                result.setDetailWarmAttemptedCount(result.getDetailWarmAttemptedCount() + 1);
            } catch (Exception ex) {
                result.setFailedCount(result.getFailedCount() + 1);
                log.warn("Problem detail cache warm-up failed: problemId={}, error={}",
                        problem.getId(), ex.getMessage());
            }
            try {
                getProblemTemplateFromMysqlAndCache(problem.getId());
                result.setTemplateWarmAttemptedCount(result.getTemplateWarmAttemptedCount() + 1);
            } catch (Exception ex) {
                result.setFailedCount(result.getFailedCount() + 1);
                log.warn("Problem template cache warm-up failed: problemId={}, error={}",
                        problem.getId(), ex.getMessage());
            }
        }
        result.setTotalWarmAttemptedCount((Boolean.TRUE.equals(result.getListWarmAttempted()) ? 1 : 0)
                + result.getDetailWarmAttemptedCount()
                + result.getTemplateWarmAttemptedCount());
        result.setMessage("Problem cache refresh attempted from MySQL source.");
        result.setSummary("Problem cache warm-up attempted " + result.getTotalWarmAttemptedCount()
                + " keys, failed " + result.getFailedCount() + ".");
        fillProblemCacheRefreshMaintenance(result);
        return result;
    }

    private void fillProblemCacheRefreshMaintenance(ProblemCacheRefreshVO result) {
        if (!Boolean.TRUE.equals(result.getEnabled()) || !Boolean.TRUE.equals(result.getRedisAvailable())) {
            result.setStatusLabel("SKIPPED");
            result.setMaintenanceAction(
                    "Enable PROBLEM_CACHE_ENABLED and Redis, then retry POST /api/problems/cache/refresh.");
            return;
        }
        if (result.getFailedCount() != null && result.getFailedCount() > 0) {
            result.setStatusLabel("PARTIAL_FAILED");
            result.setMaintenanceAction("Check cache warm-up warnings, then retry POST /api/problems/cache/refresh.");
            return;
        }
        result.setStatusLabel("READY");
        result.setMaintenanceAction(
                "Problem cache refreshed; use GET /api/problems/cache/status to confirm warmed keys.");
    }

    private List<ProblemListItemVO> listProblemsFromMysqlAndCache() {
        List<Problem> problems = problemMapper.selectList(new LambdaQueryWrapper<Problem>()
                .eq(Problem::getEnabled, true)
                .orderByAsc(Problem::getId));
        List<ProblemListItemVO> result = problems.stream().map(this::toListItem).toList();
        try {
            problemCacheService.putProblemList(result);
        } catch (Exception ex) {
            log.warn("Problem list cache write failed; keep MySQL result: {}", ex.getMessage());
        }
        return result;
    }

    @Override
    public ProblemDetailVO getProblemDetail(Long id) {
        try {
            return problemCacheService.getProblemDetail(id)
                    .orElseGet(() -> getProblemDetailFromMysqlAndCache(id));
        } catch (Exception ex) {
            log.warn("Problem detail cache read failed; downgrade to MySQL: problemId={}, error={}",
                    id, ex.getMessage());
            return getProblemDetailFromMysqlAndCache(id);
        }
    }

    private ProblemDetailVO getProblemDetailFromMysqlAndCache(Long id) {
        Problem problem = getEnabledProblem(id);
        ProblemDetailVO vo = new ProblemDetailVO();
        vo.setId(problem.getId());
        vo.setTitle(problem.getTitle());
        vo.setDescription(problem.getDescription());
        vo.setDifficulty(problem.getDifficulty());
        vo.setCategory(problem.getCategory());
        vo.setInputFormat(problem.getInputFormat());
        vo.setOutputFormat(problem.getOutputFormat());
        vo.setKnowledgePoints(listKnowledgePointNames(problem.getId()));
        vo.setSampleCases(listSampleCases(problem.getId()));
        vo.setSolutionOutline(problem.getSolutionOutline());
        if (problem.getHintLevel1() != null) {
            ProblemDetailVO.PresetHintsVO hints = new ProblemDetailVO.PresetHintsVO();
            hints.setLevel1(problem.getHintLevel1());
            hints.setLevel2(problem.getHintLevel2());
            hints.setLevel3(problem.getHintLevel3());
            vo.setPresetHints(hints);
        }
        try {
            problemCacheService.putProblemDetail(id, vo);
        } catch (Exception ex) {
            log.warn("Problem detail cache write failed; keep MySQL result: problemId={}, error={}",
                    id, ex.getMessage());
        }
        return vo;
    }

    @Override
    public ProblemTemplateVO getProblemTemplate(Long id) {
        try {
            return problemCacheService.getProblemTemplate(id)
                    .orElseGet(() -> getProblemTemplateFromMysqlAndCache(id));
        } catch (Exception ex) {
            log.warn("Problem template cache read failed; downgrade to MySQL: problemId={}, error={}",
                    id, ex.getMessage());
            return getProblemTemplateFromMysqlAndCache(id);
        }
    }

    private ProblemTemplateVO getProblemTemplateFromMysqlAndCache(Long id) {
        Problem problem = getEnabledProblem(id);
        ProblemTemplateVO vo = new ProblemTemplateVO();
        vo.setProblemId(problem.getId());
        vo.setLanguage("java");
        vo.setTemplateCode(problem.getTemplateCode());
        try {
            problemCacheService.putProblemTemplate(id, vo);
        } catch (Exception ex) {
            log.warn("Problem template cache write failed; keep MySQL result: problemId={}, error={}",
                    id, ex.getMessage());
        }
        return vo;
    }

    @Override
    public Problem getEnabledProblem(Long id) {
        Problem problem = problemMapper.selectOne(new LambdaQueryWrapper<Problem>()
                .eq(Problem::getId, id)
                .eq(Problem::getEnabled, true));
        if (problem == null) {
            throw new BusinessException(404, "problem not found");
        }
        return problem;
    }

    private ProblemListItemVO toListItem(Problem problem) {
        ProblemListItemVO vo = new ProblemListItemVO();
        vo.setId(problem.getId());
        vo.setTitle(problem.getTitle());
        vo.setDifficulty(problem.getDifficulty());
        vo.setCategory(problem.getCategory());
        return vo;
    }

    @Override
    public List<String> listKnowledgePointNames(Long problemId) {
        List<ProblemKnowledgePoint> relations = problemKnowledgePointMapper.selectList(
                new LambdaQueryWrapper<ProblemKnowledgePoint>()
                        .eq(ProblemKnowledgePoint::getProblemId, problemId)
                        .orderByAsc(ProblemKnowledgePoint::getId));
        List<Long> ids = relations.stream().map(ProblemKnowledgePoint::getKnowledgePointId).toList();
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        return knowledgePointMapper.selectBatchIds(ids).stream()
                .map(KnowledgePoint::getName)
                .toList();
    }

    private List<TestCaseVO> listSampleCases(Long problemId) {
        List<TestCase> cases = testCaseMapper.selectList(new LambdaQueryWrapper<TestCase>()
                .eq(TestCase::getProblemId, problemId)
                .eq(TestCase::getSample, true)
                .orderByAsc(TestCase::getId));
        return cases.stream().map(this::toTestCaseVO).toList();
    }

    private TestCaseVO toTestCaseVO(TestCase testCase) {
        TestCaseVO vo = new TestCaseVO();
        vo.setId(testCase.getId());
        vo.setInput(testCase.getInputData());
        vo.setExpectedOutput(testCase.getExpectedOutput());
        vo.setSample(testCase.getSample());
        return vo;
    }
}
