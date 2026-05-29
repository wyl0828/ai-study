package com.interview.coach.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.service.KnowledgeCardCacheService;
import com.interview.coach.service.KnowledgeCardService;
import com.interview.coach.service.ProblemCacheService;
import com.interview.coach.service.ProblemService;
import com.interview.coach.vo.ApiResponse;
import com.interview.coach.vo.CacheMaintenanceRefreshVO;
import com.interview.coach.vo.CacheMaintenanceStatusVO;
import com.interview.coach.vo.KnowledgeCacheRefreshVO;
import com.interview.coach.vo.KnowledgeCacheStatusVO;
import com.interview.coach.vo.ProblemCacheRefreshVO;
import com.interview.coach.vo.ProblemCacheStatusVO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CacheMaintenanceControllerTest {

    @Test
    void statusAggregatesProblemAndKnowledgeCacheBoundaries() {
        ProblemCacheService problemCacheService = Mockito.mock(ProblemCacheService.class);
        ProblemService problemService = Mockito.mock(ProblemService.class);
        KnowledgeCardCacheService knowledgeCardCacheService = Mockito.mock(KnowledgeCardCacheService.class);
        KnowledgeCardService knowledgeCardService = Mockito.mock(KnowledgeCardService.class);
        ProblemCacheStatusVO problem = new ProblemCacheStatusVO();
        problem.setEnabled(true);
        problem.setRedisAvailable(true);
        problem.setCachedKeyCount(3);
        problem.setStatusLabel("READY");
        problem.setHitCount(2L);
        problem.setMissCount(1L);
        problem.setFallbackCount(1L);
        problem.setLastFallbackReason("coach:problem:list:v1: redis down");
        KnowledgeCacheStatusVO knowledge = new KnowledgeCacheStatusVO();
        knowledge.setEnabled(true);
        knowledge.setRedisAvailable(false);
        knowledge.setCachedKeyCount(4);
        knowledge.setStatusLabel("REDIS_UNAVAILABLE");
        knowledge.setHitCount(4L);
        knowledge.setMissCount(3L);
        knowledge.setFallbackCount(3L);
        knowledge.setLastFallbackReason("coach:knowledge:cards:v1:JAVA: timeout");
        when(problemCacheService.status()).thenReturn(problem);
        when(knowledgeCardCacheService.status()).thenReturn(knowledge);
        CacheMaintenanceController controller = new CacheMaintenanceController(
                problemCacheService,
                problemService,
                knowledgeCardCacheService,
                knowledgeCardService);

        ApiResponse<CacheMaintenanceStatusVO> response = controller.status();

        assertThat(response.getCode()).isZero();
        assertThat(response.getData().getProblem()).isSameAs(problem);
        assertThat(response.getData().getKnowledge()).isSameAs(knowledge);
        assertThat(response.getData().getAllEnabled()).isTrue();
        assertThat(response.getData().getAllRedisAvailable()).isFalse();
        assertThat(response.getData().getCachedKeyCount()).isEqualTo(7);
        assertThat(response.getData().getHitCount()).isEqualTo(6L);
        assertThat(response.getData().getMissCount()).isEqualTo(4L);
        assertThat(response.getData().getFallbackCount()).isEqualTo(4L);
        assertThat(response.getData().getHitRate()).isEqualTo(60);
        assertThat(response.getData().getLastFallbackReason())
                .contains("problem coach:problem:list:v1: redis down")
                .contains("knowledge coach:knowledge:cards:v1:JAVA: timeout");
        assertThat(response.getData().getStatusLabel()).isEqualTo("PARTIAL_DEGRADED");
        assertThat(response.getData().getCheckedAt()).isNotNull();
        assertThat(response.getData().getSummary())
                .contains("Redis partially degraded", "cached keys=7", "hits=6", "misses=4", "fallbacks=4",
                        "lastFallback=problem coach:problem:list:v1: redis down; knowledge coach:knowledge:cards:v1:JAVA: timeout",
                        "problem=READY", "knowledge=REDIS_UNAVAILABLE");
        assertThat(response.getData().getMaintenanceAction()).contains("POST /api/cache/refresh");
        assertThat(response.getData().getBoundary()).contains("learning state remains MySQL-backed");
        assertThat(cacheStatusValue(response.getData(), "cacheBenefitSummary"))
                .asString()
                .contains("7 read-mostly keys", "60% hit rate");
        assertThat(cacheStatusValue(response.getData(), "fallbackRiskSummary"))
                .asString()
                .contains("4 Redis fallbacks", "MySQL");
        assertThat(cacheStatusValue(response.getData(), "protectedDataSummary"))
                .asString()
                .contains("submissions", "diagnoses", "training plans", "mock interviews");
        verify(problemCacheService).status();
        verify(knowledgeCardCacheService).status();
    }

    @Test
    void statusStillReturnsProblemBoundaryWhenKnowledgeStatusFails() {
        ProblemCacheService problemCacheService = Mockito.mock(ProblemCacheService.class);
        ProblemService problemService = Mockito.mock(ProblemService.class);
        KnowledgeCardCacheService knowledgeCardCacheService = Mockito.mock(KnowledgeCardCacheService.class);
        KnowledgeCardService knowledgeCardService = Mockito.mock(KnowledgeCardService.class);
        ProblemCacheStatusVO problem = new ProblemCacheStatusVO();
        problem.setEnabled(true);
        problem.setRedisAvailable(true);
        problem.setCachedKeyCount(3);
        problem.setStatusLabel("READY");
        when(problemCacheService.status()).thenReturn(problem);
        when(knowledgeCardCacheService.status()).thenThrow(new IllegalStateException("knowledge redis probe failed"));
        CacheMaintenanceController controller = new CacheMaintenanceController(
                problemCacheService,
                problemService,
                knowledgeCardCacheService,
                knowledgeCardService);

        ApiResponse<CacheMaintenanceStatusVO> response = controller.status();

        assertThat(response.getCode()).isZero();
        assertThat(response.getData().getProblem()).isSameAs(problem);
        assertThat(response.getData().getKnowledge().getStatusLabel()).isEqualTo("STATUS_FAILED");
        assertThat(response.getData().getKnowledge().getSummary()).contains("knowledge redis probe failed");
        assertThat(response.getData().getAllEnabled()).isFalse();
        assertThat(response.getData().getAllRedisAvailable()).isFalse();
        assertThat(response.getData().getCachedKeyCount()).isEqualTo(3);
        assertThat(response.getData().getStatusLabel()).isEqualTo("PARTIAL_DEGRADED");
        assertThat(response.getData().getSummary())
                .contains("cache status probe failed", "problem=READY", "knowledge=STATUS_FAILED");
    }

    @Test
    void statusPromotesChildProbeWarningsToUnifiedMaintenanceStatus() {
        ProblemCacheService problemCacheService = Mockito.mock(ProblemCacheService.class);
        ProblemService problemService = Mockito.mock(ProblemService.class);
        KnowledgeCardCacheService knowledgeCardCacheService = Mockito.mock(KnowledgeCardCacheService.class);
        KnowledgeCardService knowledgeCardService = Mockito.mock(KnowledgeCardService.class);
        ProblemCacheStatusVO problem = new ProblemCacheStatusVO();
        problem.setEnabled(true);
        problem.setRedisAvailable(true);
        problem.setCachedKeyCount(3);
        problem.setStatusLabel("PARTIAL_DEGRADED");
        problem.setProbeWarning("key count failed: scan denied");
        KnowledgeCacheStatusVO knowledge = new KnowledgeCacheStatusVO();
        knowledge.setEnabled(true);
        knowledge.setRedisAvailable(true);
        knowledge.setCachedKeyCount(4);
        knowledge.setStatusLabel("READY");
        when(problemCacheService.status()).thenReturn(problem);
        when(knowledgeCardCacheService.status()).thenReturn(knowledge);
        CacheMaintenanceController controller = new CacheMaintenanceController(
                problemCacheService,
                problemService,
                knowledgeCardCacheService,
                knowledgeCardService);

        ApiResponse<CacheMaintenanceStatusVO> response = controller.status();

        assertThat(response.getCode()).isZero();
        assertThat(response.getData().getAllRedisAvailable()).isTrue();
        assertThat(response.getData().getStatusLabel()).isEqualTo("PARTIAL_DEGRADED");
        assertThat(response.getData().getProbeWarning()).contains("problem key count failed: scan denied");
        assertThat(response.getData().getSummary())
                .contains("status probe degraded", "problem key count failed: scan denied");
        assertThat(response.getData().getMaintenanceAction()).contains("key scan permissions");
    }

    @Test
    void refreshAggregatesProblemAndKnowledgeWarmupResults() {
        ProblemCacheService problemCacheService = Mockito.mock(ProblemCacheService.class);
        ProblemService problemService = Mockito.mock(ProblemService.class);
        KnowledgeCardCacheService knowledgeCardCacheService = Mockito.mock(KnowledgeCardCacheService.class);
        KnowledgeCardService knowledgeCardService = Mockito.mock(KnowledgeCardService.class);
        ProblemCacheRefreshVO problem = new ProblemCacheRefreshVO();
        problem.setFailedCount(1);
        problem.setTotalWarmAttemptedCount(3);
        KnowledgeCacheRefreshVO knowledge = new KnowledgeCacheRefreshVO();
        knowledge.setFailedCount(2);
        knowledge.setTotalWarmAttemptedCount(9);
        when(problemService.refreshProblemCache()).thenReturn(problem);
        when(knowledgeCardService.refreshKnowledgeCache()).thenReturn(knowledge);
        CacheMaintenanceController controller = new CacheMaintenanceController(
                problemCacheService,
                problemService,
                knowledgeCardCacheService,
                knowledgeCardService);

        ApiResponse<CacheMaintenanceRefreshVO> response = controller.refresh();

        assertThat(response.getCode()).isZero();
        assertThat(response.getData().getProblem()).isSameAs(problem);
        assertThat(response.getData().getKnowledge()).isSameAs(knowledge);
        assertThat(response.getData().getTotalWarmAttemptedCount()).isEqualTo(12);
        assertThat(response.getData().getFailedCount()).isEqualTo(3);
        assertThat(response.getData().getStatusLabel()).isEqualTo("PARTIAL_FAILED");
        assertThat(response.getData().getMaintenanceAction()).contains("Check failed cache warm-up items");
        assertThat(response.getData().getMessage()).contains("MySQL sources");
        assertThat(response.getData().getSummary()).isEqualTo("Cache warm-up attempted 12 keys, failed 3.");
        assertThat(response.getData().getRefreshedAt()).isNotNull();
        verify(problemService).refreshProblemCache();
        verify(knowledgeCardService).refreshKnowledgeCache();
    }

    @Test
    void refreshStillReturnsKnowledgeResultWhenProblemRefreshFails() {
        ProblemCacheService problemCacheService = Mockito.mock(ProblemCacheService.class);
        ProblemService problemService = Mockito.mock(ProblemService.class);
        KnowledgeCardCacheService knowledgeCardCacheService = Mockito.mock(KnowledgeCardCacheService.class);
        KnowledgeCardService knowledgeCardService = Mockito.mock(KnowledgeCardService.class);
        KnowledgeCacheRefreshVO knowledge = new KnowledgeCacheRefreshVO();
        knowledge.setTotalWarmAttemptedCount(5);
        knowledge.setFailedCount(1);
        knowledge.setSummary("Knowledge cache warm-up attempted 5 keys, failed 1.");
        when(problemService.refreshProblemCache()).thenThrow(new IllegalStateException("problem refresh failed"));
        when(knowledgeCardService.refreshKnowledgeCache()).thenReturn(knowledge);
        CacheMaintenanceController controller = new CacheMaintenanceController(
                problemCacheService,
                problemService,
                knowledgeCardCacheService,
                knowledgeCardService);

        ApiResponse<CacheMaintenanceRefreshVO> response = controller.refresh();

        assertThat(response.getCode()).isZero();
        assertThat(response.getData().getProblem().getFailedCount()).isEqualTo(1);
        assertThat(response.getData().getProblem().getSummary()).contains("problem refresh failed");
        assertThat(response.getData().getKnowledge()).isSameAs(knowledge);
        assertThat(response.getData().getTotalWarmAttemptedCount()).isEqualTo(5);
        assertThat(response.getData().getFailedCount()).isEqualTo(2);
        assertThat(response.getData().getStatusLabel()).isEqualTo("PARTIAL_FAILED");
        assertThat(response.getData().getMaintenanceAction()).contains("problem refresh failed");
        assertThat(response.getData().getSummary())
                .contains("problem: Problem cache refresh failed")
                .contains("knowledge: Knowledge cache warm-up attempted 5 keys, failed 1");
    }

    @Test
    void refreshSummaryIncludesChildSkipOrFailureReasons() {
        ProblemCacheService problemCacheService = Mockito.mock(ProblemCacheService.class);
        ProblemService problemService = Mockito.mock(ProblemService.class);
        KnowledgeCardCacheService knowledgeCardCacheService = Mockito.mock(KnowledgeCardCacheService.class);
        KnowledgeCardService knowledgeCardService = Mockito.mock(KnowledgeCardService.class);
        ProblemCacheRefreshVO problem = new ProblemCacheRefreshVO();
        problem.setEnabled(true);
        problem.setRedisAvailable(false);
        problem.setSummary("Problem cache refresh skipped because Redis is unavailable; MySQL remains source of truth.");
        KnowledgeCacheRefreshVO knowledge = new KnowledgeCacheRefreshVO();
        knowledge.setEnabled(true);
        knowledge.setRedisAvailable(true);
        knowledge.setTotalWarmAttemptedCount(5);
        knowledge.setFailedCount(1);
        knowledge.setSummary("Knowledge cache warm-up attempted 5 keys, failed 1.");
        when(problemService.refreshProblemCache()).thenReturn(problem);
        when(knowledgeCardService.refreshKnowledgeCache()).thenReturn(knowledge);
        CacheMaintenanceController controller = new CacheMaintenanceController(
                problemCacheService,
                problemService,
                knowledgeCardCacheService,
                knowledgeCardService);

        ApiResponse<CacheMaintenanceRefreshVO> response = controller.refresh();

        assertThat(response.getData().getSummary())
                .contains("Cache warm-up attempted 5 keys, failed 1")
                .contains("problem: Problem cache refresh skipped because Redis is unavailable")
                .contains("knowledge: Knowledge cache warm-up attempted 5 keys, failed 1");
        assertThat(response.getData().getStatusLabel()).isEqualTo("PARTIAL_FAILED");
        assertThat(response.getData().getMaintenanceAction()).contains("Redis unavailable");
    }

    @Test
    void refreshReportsReadyWhenBothCachesWarmSuccessfully() {
        ProblemCacheService problemCacheService = Mockito.mock(ProblemCacheService.class);
        ProblemService problemService = Mockito.mock(ProblemService.class);
        KnowledgeCardCacheService knowledgeCardCacheService = Mockito.mock(KnowledgeCardCacheService.class);
        KnowledgeCardService knowledgeCardService = Mockito.mock(KnowledgeCardService.class);
        ProblemCacheRefreshVO problem = new ProblemCacheRefreshVO();
        problem.setEnabled(true);
        problem.setRedisAvailable(true);
        problem.setTotalWarmAttemptedCount(3);
        KnowledgeCacheRefreshVO knowledge = new KnowledgeCacheRefreshVO();
        knowledge.setEnabled(true);
        knowledge.setRedisAvailable(true);
        knowledge.setTotalWarmAttemptedCount(9);
        when(problemService.refreshProblemCache()).thenReturn(problem);
        when(knowledgeCardService.refreshKnowledgeCache()).thenReturn(knowledge);
        CacheMaintenanceController controller = new CacheMaintenanceController(
                problemCacheService,
                problemService,
                knowledgeCardCacheService,
                knowledgeCardService);

        ApiResponse<CacheMaintenanceRefreshVO> response = controller.refresh();

        assertThat(response.getData().getStatusLabel()).isEqualTo("READY");
        assertThat(response.getData().getMaintenanceAction()).contains("No cache refresh follow-up required");
        assertThat(response.getData().getSummary()).contains("Cache warm-up attempted 12 keys, failed 0");
        assertThat(cacheRefreshValue(response.getData(), "boundary"))
                .asString()
                .contains("read-mostly problem and knowledge-card responses")
                .contains("learning state remains MySQL-backed");
        assertThat(cacheRefreshValue(response.getData(), "refreshScopeSummary"))
                .asString()
                .contains("problem", "knowledge-card", "read-mostly");
        assertThat(cacheRefreshValue(response.getData(), "warmupResultSummary"))
                .asString()
                .contains("12", "0");
        assertThat(cacheRefreshValue(response.getData(), "protectedDataSummary"))
                .asString()
                .contains("submissions", "diagnoses", "training plans", "RAG user memory", "mock interviews");
    }

    private Object cacheRefreshValue(CacheMaintenanceRefreshVO refresh, String fieldName) {
        try {
            java.lang.reflect.Field field = CacheMaintenanceRefreshVO.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(refresh);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Missing cache refresh field: " + fieldName, ex);
        }
    }

    private Object cacheStatusValue(CacheMaintenanceStatusVO status, String fieldName) {
        try {
            java.lang.reflect.Field field = CacheMaintenanceStatusVO.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(status);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Missing cache status field: " + fieldName, ex);
        }
    }
}
