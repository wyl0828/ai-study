package com.interview.coach.service.impl;

import com.interview.coach.config.ProblemCacheProperties;
import com.interview.coach.service.ProblemCacheService;
import com.interview.coach.vo.ProblemCacheStatusVO;
import com.interview.coach.vo.ProblemDetailVO;
import com.interview.coach.vo.ProblemListItemVO;
import com.interview.coach.vo.ProblemTemplateVO;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisProblemCacheService implements ProblemCacheService {

    private static final String PROBLEM_LIST_KEY = "coach:problem:list:v1";

    private static final String PROBLEM_DETAIL_KEY_PREFIX = "coach:problem:detail:v1:";

    private static final String PROBLEM_TEMPLATE_KEY_PREFIX = "coach:problem:template:v1:";

    private final RedisTemplate<String, Object> redisTemplate;

    private final ProblemCacheProperties properties;

    private final AtomicLong hitCount = new AtomicLong();

    private final AtomicLong missCount = new AtomicLong();

    private final AtomicLong fallbackCount = new AtomicLong();

    private final AtomicReference<String> lastFallbackReason = new AtomicReference<>();

    @Override
    public ProblemCacheStatusVO status() {
        ProblemCacheStatusVO status = new ProblemCacheStatusVO();
        status.setCheckedAt(LocalDateTime.now());
        status.setEnabled(properties.isEnabled());
        status.setProvider("Redis");
        boolean available = properties.isEnabled() && redisAvailable();
        status.setRedisAvailable(available);
        status.setListKey(PROBLEM_LIST_KEY);
        status.setDetailKeyPattern(PROBLEM_DETAIL_KEY_PREFIX + "{problemId}");
        status.setTemplateKeyPattern(PROBLEM_TEMPLATE_KEY_PREFIX + "{problemId}");
        status.setListTtlSeconds(ttlSeconds(properties.getListTtl()));
        status.setDetailTtlSeconds(ttlSeconds(properties.getDetailTtl()));
        status.setTemplateTtlSeconds(ttlSeconds(properties.getTemplateTtl()));
        fillRuntimeCounters(status);
        if (available) {
            fillCachedKeyCounts(status);
        }
        status.setFallback("Redis 读写失败时降级 MySQL；提交、诊断、训练计划、RAG 用户记忆和模拟面试不以 Redis 为事实源。");
        fillStatusSummary(status);
        return status;
    }

    private void fillStatusSummary(ProblemCacheStatusVO status) {
        if (!Boolean.TRUE.equals(status.getEnabled())) {
            status.setStatusLabel("DISABLED");
            status.setSummary("Problem cache disabled; MySQL remains the source of truth.");
            status.setMaintenanceAction("Enable PROBLEM_CACHE_ENABLED when read-mostly problem cache is needed.");
            return;
        }
        if (!Boolean.TRUE.equals(status.getRedisAvailable())) {
            status.setStatusLabel("REDIS_UNAVAILABLE");
            status.setSummary("Redis unavailable; problem APIs will downgrade to MySQL.");
            status.setMaintenanceAction("Check Redis connection, then call POST /api/problems/cache/refresh.");
            return;
        }
        if (status.getProbeWarning() != null && !status.getProbeWarning().isBlank()) {
            status.setStatusLabel("PARTIAL_DEGRADED");
            status.setSummary("Problem cache status probe degraded: " + status.getProbeWarning()
                    + ". Redis is reachable and problem APIs still downgrade to MySQL on cache read/write failure.");
            status.setMaintenanceAction("Check Redis key scan permissions, then retry GET /api/problems/cache/status.");
            return;
        }
        status.setStatusLabel("READY");
        status.setSummary("Problem cache ready: cached keys=" + status.getCachedKeyCount()
                + ", hits=" + status.getHitCount()
                + ", misses=" + status.getMissCount()
                + ", fallbacks=" + status.getFallbackCount()
                + ", hitRate=" + status.getHitRate()
                + "%"
                + lastFallbackSummary(status)
                + ", list TTL=" + status.getListTtlSeconds()
                + "s, detail TTL=" + status.getDetailTtlSeconds()
                + "s, template TTL=" + status.getTemplateTtlSeconds() + "s.");
        status.setMaintenanceAction("POST /api/problems/cache/refresh warms problem list, detail, and template keys from MySQL.");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<List<ProblemListItemVO>> getProblemList() {
        if (!properties.isEnabled()) {
            log.debug("Problem cache disabled: key={}", PROBLEM_LIST_KEY);
            recordMiss();
            return Optional.empty();
        }
        Object value = getValue(PROBLEM_LIST_KEY);
        if (value instanceof List<?> list) {
            log.debug("Problem cache hit: key={}", PROBLEM_LIST_KEY);
            recordHit();
            return Optional.of((List<ProblemListItemVO>) list);
        }
        log.debug("Problem cache miss: key={}", PROBLEM_LIST_KEY);
        recordMiss();
        return Optional.empty();
    }

    @Override
    public void putProblemList(List<ProblemListItemVO> problems) {
        if (!properties.isEnabled() || problems == null) {
            return;
        }
        put(PROBLEM_LIST_KEY, problems, properties.getListTtl());
        log.debug("Problem cache write: key={}, ttl={}", PROBLEM_LIST_KEY, properties.getListTtl());
    }

    @Override
    public Optional<ProblemDetailVO> getProblemDetail(Long problemId) {
        if (!properties.isEnabled() || problemId == null) {
            log.debug("Problem cache disabled or invalid detail id: problemId={}", problemId);
            recordMiss();
            return Optional.empty();
        }
        String key = detailKey(problemId);
        Object value = getValue(key);
        if (value instanceof ProblemDetailVO detail) {
            log.debug("Problem cache hit: key={}", key);
            recordHit();
            return Optional.of(detail);
        }
        log.debug("Problem cache miss: key={}", key);
        recordMiss();
        return Optional.empty();
    }

    @Override
    public void putProblemDetail(Long problemId, ProblemDetailVO detail) {
        if (!properties.isEnabled() || problemId == null || detail == null) {
            return;
        }
        String key = detailKey(problemId);
        put(key, detail, properties.getDetailTtl());
        log.debug("Problem cache write: key={}, ttl={}", key, properties.getDetailTtl());
    }

    @Override
    public Optional<ProblemTemplateVO> getProblemTemplate(Long problemId) {
        if (!properties.isEnabled() || problemId == null) {
            log.debug("Problem cache disabled or invalid template id: problemId={}", problemId);
            recordMiss();
            return Optional.empty();
        }
        String key = templateKey(problemId);
        Object value = getValue(key);
        if (value instanceof ProblemTemplateVO template) {
            log.debug("Problem cache hit: key={}", key);
            recordHit();
            return Optional.of(template);
        }
        log.debug("Problem cache miss: key={}", key);
        recordMiss();
        return Optional.empty();
    }

    @Override
    public void putProblemTemplate(Long problemId, ProblemTemplateVO template) {
        if (!properties.isEnabled() || problemId == null || template == null) {
            return;
        }
        String key = templateKey(problemId);
        put(key, template, properties.getTemplateTtl());
        log.debug("Problem cache write: key={}, ttl={}", key, properties.getTemplateTtl());
    }

    @Override
    public void evictProblem(Long problemId) {
        if (!properties.isEnabled() || problemId == null) {
            return;
        }
        List<String> problemKeys = List.of(detailKey(problemId), templateKey(problemId));
        redisTemplate.delete(problemKeys);
        redisTemplate.delete(PROBLEM_LIST_KEY);
        log.debug("Problem cache evict: keys={}, listKey={}", problemKeys, PROBLEM_LIST_KEY);
    }

    @Override
    public void evictAll() {
        if (!properties.isEnabled()) {
            return;
        }
        Set<String> keys = new LinkedHashSet<>();
        keys.add(PROBLEM_LIST_KEY);
        keys.addAll(keys(PROBLEM_DETAIL_KEY_PREFIX + "*"));
        keys.addAll(keys(PROBLEM_TEMPLATE_KEY_PREFIX + "*"));
        redisTemplate.delete(keys);
        log.debug("Problem cache evict all: keys={}", keys);
    }

    private void put(String key, Object value, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            redisTemplate.opsForValue().set(key, value);
            clearFallbackReason();
            return;
        }
        redisTemplate.opsForValue().set(key, value, ttl);
        clearFallbackReason();
    }

    private boolean redisAvailable() {
        try {
            if (redisTemplate.getConnectionFactory() == null) {
                return false;
            }
            var connection = redisTemplate.getConnectionFactory().getConnection();
            try {
                String ping = connection.ping();
                return "PONG".equalsIgnoreCase(ping);
            } finally {
                connection.close();
            }
        } catch (Exception ex) {
            log.debug("Problem cache Redis ping failed: {}", ex.getMessage());
            return false;
        }
    }

    private Long ttlSeconds(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return null;
        }
        return ttl.toSeconds();
    }

    private Set<String> keys(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        return keys == null ? Set.of() : keys;
    }

    private void fillCachedKeyCounts(ProblemCacheStatusVO status) {
        try {
            boolean listCached = Boolean.TRUE.equals(redisTemplate.hasKey(PROBLEM_LIST_KEY));
            int detailCount = keys(PROBLEM_DETAIL_KEY_PREFIX + "*").size();
            int templateCount = keys(PROBLEM_TEMPLATE_KEY_PREFIX + "*").size();
            status.setListCached(listCached);
            status.setDetailCachedKeyCount(detailCount);
            status.setTemplateCachedKeyCount(templateCount);
            status.setCachedKeyCount((listCached ? 1 : 0) + detailCount + templateCount);
        } catch (Exception ex) {
            status.setProbeWarning("key count failed: " + ex.getMessage());
            log.debug("Problem cache key count failed: {}", ex.getMessage());
        }
    }

    private void fillRuntimeCounters(ProblemCacheStatusVO status) {
        long hits = hitCount.get();
        long misses = missCount.get();
        long fallbacks = fallbackCount.get();
        status.setHitCount(hits);
        status.setMissCount(misses);
        status.setFallbackCount(fallbacks);
        status.setHitRate(hitRate(hits, misses));
        status.setLastFallbackReason(lastFallbackReason.get());
    }

    private void recordHit() {
        hitCount.incrementAndGet();
        clearFallbackReason();
    }

    private void recordMiss() {
        missCount.incrementAndGet();
        fallbackCount.incrementAndGet();
    }

    private Object getValue(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (RuntimeException ex) {
            recordFallbackReason(key + ": " + ex.getMessage());
            log.warn("Problem cache read failed; downgrade to MySQL: key={}, error={}", key, ex.getMessage());
            return null;
        }
    }

    private void recordFallbackReason(String reason) {
        lastFallbackReason.set(reason);
    }

    private void clearFallbackReason() {
        lastFallbackReason.set(null);
    }

    private String lastFallbackSummary(ProblemCacheStatusVO status) {
        if (status.getLastFallbackReason() == null || status.getLastFallbackReason().isBlank()) {
            return "";
        }
        return ", lastFallback=" + status.getLastFallbackReason();
    }

    private int hitRate(long hits, long misses) {
        long total = hits + misses;
        if (total <= 0) {
            return 0;
        }
        return Math.toIntExact(Math.round(hits * 100.0 / total));
    }

    private String detailKey(Long problemId) {
        return PROBLEM_DETAIL_KEY_PREFIX + problemId;
    }

    private String templateKey(Long problemId) {
        return PROBLEM_TEMPLATE_KEY_PREFIX + problemId;
    }
}
