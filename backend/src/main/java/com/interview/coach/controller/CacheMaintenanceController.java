package com.interview.coach.controller;

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
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cache")
public class CacheMaintenanceController {

    private final ProblemCacheService problemCacheService;

    private final ProblemService problemService;

    private final KnowledgeCardCacheService knowledgeCardCacheService;

    private final KnowledgeCardService knowledgeCardService;

    @GetMapping("/status")
    public ApiResponse<CacheMaintenanceStatusVO> status() {
        ProblemCacheStatusVO problem = problemStatus();
        KnowledgeCacheStatusVO knowledge = knowledgeStatus();

        CacheMaintenanceStatusVO status = new CacheMaintenanceStatusVO();
        status.setCheckedAt(LocalDateTime.now());
        status.setProblem(problem);
        status.setKnowledge(knowledge);
        status.setAllEnabled(Boolean.TRUE.equals(problem.getEnabled())
                && Boolean.TRUE.equals(knowledge.getEnabled()));
        status.setAllRedisAvailable(Boolean.TRUE.equals(problem.getRedisAvailable())
                && Boolean.TRUE.equals(knowledge.getRedisAvailable()));
        status.setCachedKeyCount(safeCount(problem.getCachedKeyCount()) + safeCount(knowledge.getCachedKeyCount()));
        long hits = safeLong(problem.getHitCount()) + safeLong(knowledge.getHitCount());
        long misses = safeLong(problem.getMissCount()) + safeLong(knowledge.getMissCount());
        status.setHitCount(hits);
        status.setMissCount(misses);
        status.setFallbackCount(safeLong(problem.getFallbackCount()) + safeLong(knowledge.getFallbackCount()));
        status.setHitRate(hitRate(hits, misses));
        status.setLastFallbackReason(combineFallbackReason(problem, knowledge));
        status.setProbeWarning(combineProbeWarning(problem, knowledge));
        fillStatusSummary(status);
        fillExplainableCacheSummaries(status);
        return ApiResponse.success(status);
    }

    private void fillExplainableCacheSummaries(CacheMaintenanceStatusVO status) {
        status.setCacheBenefitSummary(status.getCachedKeyCount()
                + " read-mostly keys are observable in Redis with "
                + status.getHitRate()
                + "% hit rate across problem and knowledge-card reads.");
        status.setFallbackRiskSummary(status.getFallbackCount()
                + " Redis fallbacks have used MySQL as the source of truth"
                + fallbackRiskReason(status)
                + ".");
        status.setProtectedDataSummary("Protected durable learning data stays out of Redis: submissions, diagnoses, "
                + "weakness memory, training plans, RAG user memory, and mock interviews remain MySQL-backed.");
    }

    private String fallbackRiskReason(CacheMaintenanceStatusVO status) {
        if (status.getLastFallbackReason() == null || status.getLastFallbackReason().isBlank()) {
            return "";
        }
        return "; latest fallback=" + status.getLastFallbackReason();
    }

    private void fillStatusSummary(CacheMaintenanceStatusVO status) {
        if (hasStatusFailure(status)) {
            status.setStatusLabel("PARTIAL_DEGRADED");
            status.setSummary("Redis cache status probe failed on one or more sides; available cache APIs should "
                    + "continue to downgrade to MySQL where needed, cached keys=" + status.getCachedKeyCount()
                    + ", hits=" + status.getHitCount()
                    + ", misses=" + status.getMissCount()
                    + ", fallbacks=" + status.getFallbackCount()
                    + fallbackSummary(status)
                    + ", " + cacheBoundarySummary(status) + ".");
            status.setMaintenanceAction("Check cache status failure details, then call POST /api/cache/refresh after Redis is healthy.");
            return;
        }
        if (!Boolean.TRUE.equals(status.getAllEnabled())) {
            status.setStatusLabel("DISABLED");
            status.setSummary("One or more read-mostly caches are disabled; MySQL remains the source of truth, "
                    + cacheBoundarySummary(status) + ".");
            status.setMaintenanceAction("Enable problem and knowledge cache switches, then call POST /api/cache/refresh.");
            return;
        }
        if (status.getProbeWarning() != null && !status.getProbeWarning().isBlank()) {
            status.setStatusLabel("PARTIAL_DEGRADED");
            status.setSummary("Redis cache status probe degraded: " + status.getProbeWarning()
                    + ". Redis is reachable and problem/knowledge APIs still downgrade to MySQL on cache read/write failure, cached keys="
                    + status.getCachedKeyCount()
                    + ", hits=" + status.getHitCount()
                    + ", misses=" + status.getMissCount()
                    + ", fallbacks=" + status.getFallbackCount()
                    + ", hitRate=" + status.getHitRate()
                    + "%"
                    + fallbackSummary(status)
                    + ", " + cacheBoundarySummary(status) + ".");
            status.setMaintenanceAction("Check Redis key scan permissions, then retry GET /api/cache/status.");
            return;
        }
        if (!Boolean.TRUE.equals(status.getAllRedisAvailable())) {
            status.setStatusLabel("PARTIAL_DEGRADED");
            status.setSummary("Redis partially degraded; problem/knowledge APIs downgrade to MySQL where needed, cached keys="
                    + status.getCachedKeyCount()
                    + ", hits=" + status.getHitCount()
                    + ", misses=" + status.getMissCount()
                    + ", fallbacks=" + status.getFallbackCount()
                    + ", hitRate=" + status.getHitRate()
                    + "%"
                    + fallbackSummary(status)
                    + ", " + cacheBoundarySummary(status) + ".");
            status.setMaintenanceAction("Check Redis availability, then call POST /api/cache/refresh to warm read-mostly keys.");
            return;
        }
        status.setStatusLabel("READY");
        status.setSummary("Read-mostly Redis cache ready: cached keys=" + status.getCachedKeyCount()
                + ", hits=" + status.getHitCount()
                + ", misses=" + status.getMissCount()
                + ", fallbacks=" + status.getFallbackCount()
                + ", hitRate=" + status.getHitRate()
                + "%"
                + fallbackSummary(status)
                + ", " + cacheBoundarySummary(status) + ". Learning state remains MySQL-backed.");
        status.setMaintenanceAction("POST /api/cache/refresh warms problem and knowledge-card read-mostly keys from MySQL.");
    }

    private boolean hasStatusFailure(CacheMaintenanceStatusVO status) {
        return "STATUS_FAILED".equals(statusLabel(status.getProblem()))
                || "STATUS_FAILED".equals(statusLabel(status.getKnowledge()));
    }

    private String cacheBoundarySummary(CacheMaintenanceStatusVO status) {
        return "problem=" + statusLabel(status.getProblem()) + ", knowledge=" + statusLabel(status.getKnowledge());
    }

    private String combineFallbackReason(ProblemCacheStatusVO problem, KnowledgeCacheStatusVO knowledge) {
        StringBuilder builder = new StringBuilder();
        if (problem != null && problem.getLastFallbackReason() != null && !problem.getLastFallbackReason().isBlank()) {
            builder.append("problem ").append(problem.getLastFallbackReason());
        }
        if (knowledge != null && knowledge.getLastFallbackReason() != null && !knowledge.getLastFallbackReason().isBlank()) {
            if (!builder.isEmpty()) {
                builder.append("; ");
            }
            builder.append("knowledge ").append(knowledge.getLastFallbackReason());
        }
        return builder.isEmpty() ? null : builder.toString();
    }

    private String combineProbeWarning(ProblemCacheStatusVO problem, KnowledgeCacheStatusVO knowledge) {
        StringBuilder builder = new StringBuilder();
        if (problem != null && problem.getProbeWarning() != null && !problem.getProbeWarning().isBlank()) {
            builder.append("problem ").append(problem.getProbeWarning());
        }
        if (knowledge != null && knowledge.getProbeWarning() != null && !knowledge.getProbeWarning().isBlank()) {
            if (!builder.isEmpty()) {
                builder.append("; ");
            }
            builder.append("knowledge ").append(knowledge.getProbeWarning());
        }
        return builder.isEmpty() ? null : builder.toString();
    }

    private String fallbackSummary(CacheMaintenanceStatusVO status) {
        if (status.getLastFallbackReason() == null || status.getLastFallbackReason().isBlank()) {
            return "";
        }
        return ", lastFallback=" + status.getLastFallbackReason();
    }

    private String statusLabel(ProblemCacheStatusVO status) {
        return status == null || status.getStatusLabel() == null ? "UNKNOWN" : status.getStatusLabel();
    }

    private String statusLabel(KnowledgeCacheStatusVO status) {
        return status == null || status.getStatusLabel() == null ? "UNKNOWN" : status.getStatusLabel();
    }

    @PostMapping("/refresh")
    public ApiResponse<CacheMaintenanceRefreshVO> refresh() {
        ProblemCacheRefreshVO problem = refreshProblem();
        KnowledgeCacheRefreshVO knowledge = refreshKnowledge();

        CacheMaintenanceRefreshVO refresh = new CacheMaintenanceRefreshVO();
        refresh.setProblem(problem);
        refresh.setKnowledge(knowledge);
        refresh.setTotalWarmAttemptedCount(safeCount(problem.getTotalWarmAttemptedCount())
                + safeCount(knowledge.getTotalWarmAttemptedCount()));
        refresh.setFailedCount(safeCount(problem.getFailedCount()) + safeCount(knowledge.getFailedCount()));
        refresh.setMessage("Read-mostly problem and knowledge-card cache refresh attempted from MySQL sources.");
        refresh.setSummary("Cache warm-up attempted " + refresh.getTotalWarmAttemptedCount()
                + " keys, failed " + refresh.getFailedCount() + "."
                + childRefreshSummary(problem.getSummary(), knowledge.getSummary()));
        refresh.setBoundary("Only read-mostly problem and knowledge-card responses are refreshed in Redis; learning state remains MySQL-backed.");
        fillRefreshExplainableSummaries(refresh);
        fillRefreshMaintenance(refresh);
        refresh.setRefreshedAt(LocalDateTime.now());
        return ApiResponse.success(refresh);
    }

    private void fillRefreshExplainableSummaries(CacheMaintenanceRefreshVO refresh) {
        refresh.setRefreshScopeSummary("Refresh scope is read-mostly problem and knowledge-card Redis keys only: "
                + "problem list/detail/template plus knowledge category/list/detail.");
        refresh.setWarmupResultSummary("Warm-up attempted " + safeCount(refresh.getTotalWarmAttemptedCount())
                + " read-mostly keys from MySQL; failed " + safeCount(refresh.getFailedCount()) + ".");
        refresh.setProtectedDataSummary("Protected durable learning data stays out of Redis refresh: submissions, "
                + "diagnoses, weakness memory, training plans, RAG user memory, and mock interviews remain MySQL-backed.");
    }

    private void fillRefreshMaintenance(CacheMaintenanceRefreshVO refresh) {
        if (safeCount(refresh.getFailedCount()) > 0) {
            refresh.setStatusLabel("PARTIAL_FAILED");
            refresh.setMaintenanceAction("Check failed cache warm-up items; "
                    + compactChildFailure(refresh.getProblem(), refresh.getKnowledge())
                    + skippedReason(refresh)
                    + " then retry POST /api/cache/refresh after Redis and MySQL are healthy.");
            return;
        }
        if (isRefreshSkipped(refresh.getProblem()) || isRefreshSkipped(refresh.getKnowledge())) {
            refresh.setStatusLabel("PARTIAL_SKIPPED");
            refresh.setMaintenanceAction("Redis unavailable or cache disabled on one side; restore it, then retry POST /api/cache/refresh.");
            return;
        }
        refresh.setStatusLabel("READY");
        refresh.setMaintenanceAction("No cache refresh follow-up required; read-mostly problem and knowledge-card keys were warmed from MySQL.");
    }

    private String skippedReason(CacheMaintenanceRefreshVO refresh) {
        if (isRefreshSkipped(refresh.getProblem()) || isRefreshSkipped(refresh.getKnowledge())) {
            return "Redis unavailable or cache disabled on one side; ";
        }
        return "";
    }

    private boolean isRefreshSkipped(ProblemCacheRefreshVO refresh) {
        return refresh == null || !Boolean.TRUE.equals(refresh.getEnabled())
                || !Boolean.TRUE.equals(refresh.getRedisAvailable());
    }

    private boolean isRefreshSkipped(KnowledgeCacheRefreshVO refresh) {
        return refresh == null || !Boolean.TRUE.equals(refresh.getEnabled())
                || !Boolean.TRUE.equals(refresh.getRedisAvailable());
    }

    private String compactChildFailure(ProblemCacheRefreshVO problem, KnowledgeCacheRefreshVO knowledge) {
        StringBuilder builder = new StringBuilder();
        if (problem != null && safeCount(problem.getFailedCount()) > 0 && problem.getSummary() != null) {
            builder.append("problem ").append(problem.getSummary()).append("; ");
        } else if (problem != null && isRefreshSkipped(problem) && problem.getSummary() != null) {
            builder.append("problem ").append(problem.getSummary()).append("; ");
        }
        if (knowledge != null && safeCount(knowledge.getFailedCount()) > 0 && knowledge.getSummary() != null) {
            builder.append("knowledge ").append(knowledge.getSummary()).append("; ");
        } else if (knowledge != null && isRefreshSkipped(knowledge) && knowledge.getSummary() != null) {
            builder.append("knowledge ").append(knowledge.getSummary()).append("; ");
        }
        return builder.toString();
    }

    private ProblemCacheStatusVO problemStatus() {
        try {
            return problemCacheService.status();
        } catch (RuntimeException ex) {
            ProblemCacheStatusVO status = new ProblemCacheStatusVO();
            status.setCheckedAt(LocalDateTime.now());
            status.setEnabled(false);
            status.setRedisAvailable(false);
            status.setStatusLabel("STATUS_FAILED");
            status.setSummary("Problem cache status failed: " + ex.getMessage());
            status.setMaintenanceAction("Check problem cache configuration and Redis connection.");
            status.setFallback("Problem APIs should continue to use MySQL fallback when cache status probing fails.");
            return status;
        }
    }

    private KnowledgeCacheStatusVO knowledgeStatus() {
        try {
            return knowledgeCardCacheService.status();
        } catch (RuntimeException ex) {
            KnowledgeCacheStatusVO status = new KnowledgeCacheStatusVO();
            status.setCheckedAt(LocalDateTime.now());
            status.setEnabled(false);
            status.setRedisAvailable(false);
            status.setStatusLabel("STATUS_FAILED");
            status.setSummary("Knowledge cache status failed: " + ex.getMessage());
            status.setMaintenanceAction("Check knowledge cache configuration and Redis connection.");
            status.setFallback("Knowledge-card APIs should continue to use MySQL fallback when cache status probing fails.");
            return status;
        }
    }

    private ProblemCacheRefreshVO refreshProblem() {
        try {
            return problemService.refreshProblemCache();
        } catch (RuntimeException ex) {
            ProblemCacheRefreshVO refresh = new ProblemCacheRefreshVO();
            refresh.setRefreshedAt(LocalDateTime.now());
            refresh.setEnabled(false);
            refresh.setRedisAvailable(false);
            refresh.setFailedCount(1);
            refresh.setMessage("Problem cache refresh failed: " + ex.getMessage());
            refresh.setSummary("Problem cache refresh failed: " + ex.getMessage());
            return refresh;
        }
    }

    private KnowledgeCacheRefreshVO refreshKnowledge() {
        try {
            return knowledgeCardService.refreshKnowledgeCache();
        } catch (RuntimeException ex) {
            KnowledgeCacheRefreshVO refresh = new KnowledgeCacheRefreshVO();
            refresh.setRefreshedAt(LocalDateTime.now());
            refresh.setEnabled(false);
            refresh.setRedisAvailable(false);
            refresh.setFailedCount(1);
            refresh.setMessage("Knowledge cache refresh failed: " + ex.getMessage());
            refresh.setSummary("Knowledge cache refresh failed: " + ex.getMessage());
            return refresh;
        }
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : value;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private int hitRate(long hits, long misses) {
        long total = hits + misses;
        if (total <= 0) {
            return 0;
        }
        return Math.toIntExact(Math.round(hits * 100.0 / total));
    }

    private String childRefreshSummary(String problemSummary, String knowledgeSummary) {
        StringBuilder builder = new StringBuilder();
        if (problemSummary != null && !problemSummary.isBlank()) {
            builder.append(" problem: ").append(problemSummary);
        }
        if (knowledgeSummary != null && !knowledgeSummary.isBlank()) {
            builder.append(" knowledge: ").append(knowledgeSummary);
        }
        return builder.toString();
    }
}
