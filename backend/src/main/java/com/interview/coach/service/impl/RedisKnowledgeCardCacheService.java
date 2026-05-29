package com.interview.coach.service.impl;

import com.interview.coach.config.KnowledgeCacheProperties;
import com.interview.coach.service.KnowledgeCardCacheService;
import com.interview.coach.vo.KnowledgeCacheStatusVO;
import com.interview.coach.vo.KnowledgeCardVO;
import com.interview.coach.vo.KnowledgeCategoryVO;
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
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisKnowledgeCardCacheService implements KnowledgeCardCacheService {

    private static final String CATEGORY_KEY = "coach:knowledge:categories:v1";

    private static final String LIST_KEY_PREFIX = "coach:knowledge:cards:v1:";

    private static final String DETAIL_KEY_PREFIX = "coach:knowledge:card:v1:";

    private final RedisTemplate<String, Object> redisTemplate;

    private final KnowledgeCacheProperties properties;

    private final AtomicLong hitCount = new AtomicLong();

    private final AtomicLong missCount = new AtomicLong();

    private final AtomicLong fallbackCount = new AtomicLong();

    private final AtomicReference<String> lastFallbackReason = new AtomicReference<>();

    @Override
    public KnowledgeCacheStatusVO status() {
        KnowledgeCacheStatusVO status = new KnowledgeCacheStatusVO();
        status.setCheckedAt(LocalDateTime.now());
        status.setEnabled(properties.isEnabled());
        status.setProvider("Redis");
        boolean available = properties.isEnabled() && redisAvailable();
        status.setRedisAvailable(available);
        status.setCategoryKey(CATEGORY_KEY);
        status.setListKeyPattern(LIST_KEY_PREFIX + "{category|ALL}");
        status.setDetailKeyPattern(DETAIL_KEY_PREFIX + "{cardId}");
        status.setCategoryTtlSeconds(ttlSeconds(properties.getCategoryTtl()));
        status.setListTtlSeconds(ttlSeconds(properties.getListTtl()));
        status.setDetailTtlSeconds(ttlSeconds(properties.getDetailTtl()));
        fillRuntimeCounters(status);
        if (available) {
            fillCachedKeyCounts(status);
        }
        status.setFallback("Redis 读写失败时降级 MySQL；知识卡自测、掌握度、训练计划、RAG 用户记忆和模拟面试不以 Redis 为事实源。");
        fillStatusSummary(status);
        return status;
    }

    private void fillStatusSummary(KnowledgeCacheStatusVO status) {
        if (!Boolean.TRUE.equals(status.getEnabled())) {
            status.setStatusLabel("DISABLED");
            status.setSummary("Knowledge cache disabled; MySQL remains the source of truth.");
            status.setMaintenanceAction("Enable KNOWLEDGE_CACHE_ENABLED when read-mostly knowledge-card cache is needed.");
            return;
        }
        if (!Boolean.TRUE.equals(status.getRedisAvailable())) {
            status.setStatusLabel("REDIS_UNAVAILABLE");
            status.setSummary("Redis unavailable; knowledge-card APIs will downgrade to MySQL.");
            status.setMaintenanceAction("Check Redis connection, then call POST /api/knowledge/cache/refresh.");
            return;
        }
        if (status.getProbeWarning() != null && !status.getProbeWarning().isBlank()) {
            status.setStatusLabel("PARTIAL_DEGRADED");
            status.setSummary("Knowledge cache status probe degraded: " + status.getProbeWarning()
                    + ". Redis is reachable and knowledge-card APIs still downgrade to MySQL on cache read/write failure.");
            status.setMaintenanceAction("Check Redis key scan permissions, then retry GET /api/knowledge/cache/status.");
            return;
        }
        status.setStatusLabel("READY");
        status.setSummary("Knowledge cache ready: cached keys=" + status.getCachedKeyCount()
                + ", hits=" + status.getHitCount()
                + ", misses=" + status.getMissCount()
                + ", fallbacks=" + status.getFallbackCount()
                + ", hitRate=" + status.getHitRate()
                + "%"
                + lastFallbackSummary(status)
                + ", category TTL=" + status.getCategoryTtlSeconds()
                + "s, list TTL=" + status.getListTtlSeconds()
                + "s, detail TTL=" + status.getDetailTtlSeconds() + "s.");
        status.setMaintenanceAction("POST /api/knowledge/cache/refresh warms category, list, and detail keys from MySQL.");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<List<KnowledgeCategoryVO>> getCategories() {
        if (!properties.isEnabled()) {
            log.debug("Knowledge cache disabled: key={}", CATEGORY_KEY);
            recordMiss();
            return Optional.empty();
        }
        Object value = getValue(CATEGORY_KEY);
        if (value instanceof List<?> list) {
            log.debug("Knowledge cache hit: key={}", CATEGORY_KEY);
            recordHit();
            return Optional.of((List<KnowledgeCategoryVO>) list);
        }
        log.debug("Knowledge cache miss: key={}", CATEGORY_KEY);
        recordMiss();
        return Optional.empty();
    }

    @Override
    public void putCategories(List<KnowledgeCategoryVO> categories) {
        if (!properties.isEnabled() || categories == null) {
            return;
        }
        put(CATEGORY_KEY, categories, properties.getCategoryTtl());
        log.debug("Knowledge cache write: key={}, ttl={}", CATEGORY_KEY, properties.getCategoryTtl());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<List<KnowledgeCardVO>> getCards(String category) {
        if (!properties.isEnabled()) {
            recordMiss();
            return Optional.empty();
        }
        String key = listKey(category);
        Object value = getValue(key);
        if (value instanceof List<?> list) {
            log.debug("Knowledge cache hit: key={}", key);
            recordHit();
            return Optional.of((List<KnowledgeCardVO>) list);
        }
        log.debug("Knowledge cache miss: key={}", key);
        recordMiss();
        return Optional.empty();
    }

    @Override
    public void putCards(String category, List<KnowledgeCardVO> cards) {
        if (!properties.isEnabled() || cards == null) {
            return;
        }
        String key = listKey(category);
        put(key, cards, properties.getListTtl());
        log.debug("Knowledge cache write: key={}, ttl={}", key, properties.getListTtl());
    }

    @Override
    public Optional<KnowledgeCardVO> getCardDetail(Long cardId) {
        if (!properties.isEnabled() || cardId == null) {
            recordMiss();
            return Optional.empty();
        }
        String key = detailKey(cardId);
        Object value = getValue(key);
        if (value instanceof KnowledgeCardVO detail) {
            log.debug("Knowledge cache hit: key={}", key);
            recordHit();
            return Optional.of(detail);
        }
        log.debug("Knowledge cache miss: key={}", key);
        recordMiss();
        return Optional.empty();
    }

    @Override
    public void putCardDetail(Long cardId, KnowledgeCardVO detail) {
        if (!properties.isEnabled() || cardId == null || detail == null) {
            return;
        }
        String key = detailKey(cardId);
        put(key, detail, properties.getDetailTtl());
        log.debug("Knowledge cache write: key={}, ttl={}", key, properties.getDetailTtl());
    }

    @Override
    public void evictAll() {
        if (!properties.isEnabled()) {
            return;
        }
        Set<String> keys = new LinkedHashSet<>();
        keys.add(CATEGORY_KEY);
        keys.addAll(keys(LIST_KEY_PREFIX + "*"));
        keys.addAll(keys(DETAIL_KEY_PREFIX + "*"));
        redisTemplate.delete(keys);
        log.debug("Knowledge cache evict all: keys={}", keys);
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
            log.debug("Knowledge cache Redis ping failed: {}", ex.getMessage());
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

    private void fillCachedKeyCounts(KnowledgeCacheStatusVO status) {
        try {
            boolean categoryCached = Boolean.TRUE.equals(redisTemplate.hasKey(CATEGORY_KEY));
            int listCount = keys(LIST_KEY_PREFIX + "*").size();
            int detailCount = keys(DETAIL_KEY_PREFIX + "*").size();
            status.setCategoryCached(categoryCached);
            status.setListCachedKeyCount(listCount);
            status.setDetailCachedKeyCount(detailCount);
            status.setCachedKeyCount((categoryCached ? 1 : 0) + listCount + detailCount);
        } catch (Exception ex) {
            status.setProbeWarning("key count failed: " + ex.getMessage());
            log.debug("Knowledge cache key count failed: {}", ex.getMessage());
        }
    }

    private void fillRuntimeCounters(KnowledgeCacheStatusVO status) {
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
            log.warn("Knowledge cache read failed; downgrade to MySQL: key={}, error={}", key, ex.getMessage());
            return null;
        }
    }

    private void recordFallbackReason(String reason) {
        lastFallbackReason.set(reason);
    }

    private void clearFallbackReason() {
        lastFallbackReason.set(null);
    }

    private String lastFallbackSummary(KnowledgeCacheStatusVO status) {
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

    private String listKey(String category) {
        String normalized = StringUtils.hasText(category) && !"ALL".equalsIgnoreCase(category)
                ? category.trim().toUpperCase()
                : "ALL";
        return LIST_KEY_PREFIX + normalized;
    }

    private String detailKey(Long cardId) {
        return DETAIL_KEY_PREFIX + cardId;
    }
}
