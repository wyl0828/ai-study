package com.interview.coach.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.interview.coach.entity.Problem;
import com.interview.coach.handler.BusinessException;
import com.interview.coach.mapper.KnowledgePointMapper;
import com.interview.coach.mapper.ProblemKnowledgePointMapper;
import com.interview.coach.mapper.ProblemMapper;
import com.interview.coach.mapper.TestCaseMapper;
import com.interview.coach.service.ProblemCacheService;
import com.interview.coach.vo.ProblemDetailVO;
import com.interview.coach.vo.ProblemListItemVO;
import com.interview.coach.vo.ProblemTemplateVO;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProblemServiceImplTest {

    @Mock
    private ProblemMapper problemMapper;

    @Mock
    private TestCaseMapper testCaseMapper;

    @Mock
    private ProblemKnowledgePointMapper problemKnowledgePointMapper;

    @Mock
    private KnowledgePointMapper knowledgePointMapper;

    @Mock
    private ProblemCacheService problemCacheService;

    private ProblemServiceImpl problemService;

    @BeforeEach
    void setUp() {
        problemService = new ProblemServiceImpl(
                problemMapper,
                testCaseMapper,
                problemKnowledgePointMapper,
                knowledgePointMapper,
                problemCacheService);
    }

    @Test
    void listProblemsReturnsCachedValueWithoutQueryingMysql() {
        ProblemListItemVO cached = new ProblemListItemVO();
        cached.setId(1L);
        cached.setTitle("两数之和");
        when(problemCacheService.getProblemList()).thenReturn(Optional.of(List.of(cached)));

        List<ProblemListItemVO> result = problemService.listProblems();

        assertEquals(1, result.size());
        assertEquals("两数之和", result.get(0).getTitle());
        verify(problemMapper, never()).selectList(any());
    }

    @Test
    void listProblemsQueriesMysqlAndBackfillsCacheWhenMissing() {
        when(problemCacheService.getProblemList()).thenReturn(Optional.empty());
        when(problemMapper.selectList(any())).thenReturn(List.of(problem(1L, "两数之和")));

        List<ProblemListItemVO> result = problemService.listProblems();

        assertEquals(1, result.size());
        verify(problemCacheService).putProblemList(result);
    }

    @Test
    void listProblemsDowngradesToMysqlWhenCacheReadFails() {
        when(problemCacheService.getProblemList()).thenThrow(new IllegalStateException("redis down"));
        when(problemMapper.selectList(any())).thenReturn(List.of(problem(1L, "两数之和")));

        List<ProblemListItemVO> result = problemService.listProblems();

        assertEquals(1, result.size());
        assertEquals("两数之和", result.get(0).getTitle());
    }

    @Test
    void listProblemsIgnoresCacheWriteFailure() {
        when(problemCacheService.getProblemList()).thenReturn(Optional.empty());
        when(problemMapper.selectList(any())).thenReturn(List.of(problem(1L, "两数之和")));
        org.mockito.Mockito.doThrow(new IllegalStateException("redis down"))
                .when(problemCacheService).putProblemList(any());

        List<ProblemListItemVO> result = problemService.listProblems();

        assertEquals(1, result.size());
    }

    @Test
    void getProblemDetailReturnsCachedValueWithoutQueryingMysql() {
        ProblemDetailVO cached = new ProblemDetailVO();
        cached.setId(1L);
        cached.setTitle("两数之和");
        when(problemCacheService.getProblemDetail(1L)).thenReturn(Optional.of(cached));

        ProblemDetailVO result = problemService.getProblemDetail(1L);

        assertEquals("两数之和", result.getTitle());
        verify(problemMapper, never()).selectOne(any());
    }

    @Test
    void getProblemDetailBackfillsCacheWhenMissing() {
        when(problemCacheService.getProblemDetail(1L)).thenReturn(Optional.empty());
        when(problemMapper.selectOne(any())).thenReturn(problem(1L, "两数之和"));
        when(problemKnowledgePointMapper.selectList(any())).thenReturn(List.of());
        when(testCaseMapper.selectList(any())).thenReturn(List.of());

        ProblemDetailVO result = problemService.getProblemDetail(1L);

        assertEquals("两数之和", result.getTitle());
        verify(problemCacheService).putProblemDetail(eq(1L), eq(result));
    }

    @Test
    void getProblemTemplateReturnsCachedValueWithoutQueryingMysql() {
        ProblemTemplateVO cached = new ProblemTemplateVO();
        cached.setProblemId(1L);
        cached.setLanguage("java");
        cached.setTemplateCode("class Solution {}");
        when(problemCacheService.getProblemTemplate(1L)).thenReturn(Optional.of(cached));

        ProblemTemplateVO result = problemService.getProblemTemplate(1L);

        assertEquals("class Solution {}", result.getTemplateCode());
        verify(problemMapper, never()).selectOne(any());
    }

    @Test
    void getEnabledProblemStillUsesMysqlAsFactSource() {
        when(problemMapper.selectOne(any())).thenReturn(problem(1L, "两数之和"));

        Problem result = problemService.getEnabledProblem(1L);

        assertEquals(1L, result.getId());
        verify(problemCacheService, never()).getProblemDetail(any());
    }

    @Test
    void getEnabledProblemThrowsWhenMysqlHasNoEnabledProblem() {
        when(problemMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> problemService.getEnabledProblem(404L));

        assertEquals(404, ex.getCode());
        assertTrue(ex.getMessage().contains("problem not found"));
        assertFalse(ex.getMessage().isBlank());
    }

    private Problem problem(Long id, String title) {
        Problem problem = new Problem();
        problem.setId(id);
        problem.setTitle(title);
        problem.setDifficulty("EASY");
        problem.setCategory("HashMap");
        problem.setDescription("description");
        problem.setInputFormat("input");
        problem.setOutputFormat("output");
        problem.setTemplateCode("class Solution {}");
        problem.setEnabled(true);
        return problem;
    }
}
