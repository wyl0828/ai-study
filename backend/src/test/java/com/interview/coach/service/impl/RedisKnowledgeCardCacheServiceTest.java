package com.interview.coach.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.config.KnowledgeCacheProperties;
import com.interview.coach.vo.KnowledgeCacheStatusVO;
import com.interview.coach.vo.KnowledgeCardVO;
import com.interview.coach.vo.KnowledgeCategoryVO;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisKnowledgeCardCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisConnection connection;

    private KnowledgeCacheProperties properties;

    private RedisKnowledgeCardCacheService cacheService;

    @BeforeEach
    void setUp() {
        properties = new KnowledgeCacheProperties();
        cacheService = new RedisKnowledgeCardCacheService(redisTemplate, properties);
    }

    @Test
    void statusReturnsKnowledgeCacheBoundaryAndRedisAvailability() {
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        when(redisTemplate.hasKey("coach:knowledge:categories:v1")).thenReturn(true);
        when(redisTemplate.keys("coach:knowledge:cards:v1:*"))
                .thenReturn(Set.of("coach:knowledge:cards:v1:JAVA", "coach:knowledge:cards:v1:ALL"));
        when(redisTemplate.keys("coach:knowledge:card:v1:*"))
                .thenReturn(Set.of("coach:knowledge:card:v1:1"));

        KnowledgeCacheStatusVO status = cacheService.status();

        assertTrue(status.getEnabled());
        assertTrue(status.getRedisAvailable());
        assertEquals("Redis", status.getProvider());
        assertEquals("coach:knowledge:categories:v1", status.getCategoryKey());
        assertEquals("coach:knowledge:cards:v1:{category|ALL}", status.getListKeyPattern());
        assertEquals("coach:knowledge:card:v1:{cardId}", status.getDetailKeyPattern());
        assertEquals(1800L, status.getCategoryTtlSeconds());
        assertEquals(1800L, status.getListTtlSeconds());
        assertEquals(7200L, status.getDetailTtlSeconds());
        assertEquals(4, status.getCachedKeyCount());
        assertTrue(status.getCategoryCached());
        assertEquals(2, status.getListCachedKeyCount());
        assertEquals(1, status.getDetailCachedKeyCount());
        assertTrue(status.getFallback().contains("降级 MySQL"));
        assertEquals("READY", status.getStatusLabel());
        assertNotNull(status.getCheckedAt());
        assertTrue(status.getSummary().contains("Knowledge cache ready"));
        assertTrue(status.getSummary().contains("cached keys=4"));
        assertTrue(status.getMaintenanceAction().contains("POST /api/knowledge/cache/refresh"));
    }

    @Test
    void statusDoesNotFailWhenRedisPingFails() {
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenThrow(new IllegalStateException("redis down"));

        KnowledgeCacheStatusVO status = cacheService.status();

        assertTrue(status.getEnabled());
        assertTrue(!status.getRedisAvailable());
        assertEquals(0, status.getCachedKeyCount());
        assertTrue(!status.getCategoryCached());
        assertEquals(0, status.getListCachedKeyCount());
        assertEquals(0, status.getDetailCachedKeyCount());
        assertEquals("REDIS_UNAVAILABLE", status.getStatusLabel());
        assertNotNull(status.getCheckedAt());
        assertTrue(status.getSummary().contains("Redis unavailable"));
        assertTrue(status.getMaintenanceAction().contains("Check Redis"));
    }

    @Test
    void statusReportsPartialDegradedWhenKeyProbeFails() {
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        when(redisTemplate.hasKey("coach:knowledge:categories:v1")).thenThrow(new IllegalStateException("keys disabled"));

        KnowledgeCacheStatusVO status = cacheService.status();

        assertTrue(status.getEnabled());
        assertTrue(status.getRedisAvailable());
        assertEquals("PARTIAL_DEGRADED", status.getStatusLabel());
        assertEquals(0, status.getCachedKeyCount());
        assertTrue(status.getProbeWarning().contains("key count failed"));
        assertTrue(status.getProbeWarning().contains("keys disabled"));
        assertTrue(status.getSummary().contains("status probe degraded"));
        assertTrue(status.getMaintenanceAction().contains("Check Redis key scan permissions"));
    }

    @Test
    void putCategoriesUsesStableKeyAndTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        KnowledgeCategoryVO category = new KnowledgeCategoryVO();
        category.setCategory("JAVA");

        cacheService.putCategories(List.of(category));

        verify(valueOperations).set(
                eq("coach:knowledge:categories:v1"),
                eq(List.of(category)),
                eq(Duration.ofMinutes(30)));
    }

    @Test
    void getCardsNormalizesCategoryInKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        KnowledgeCardVO card = new KnowledgeCardVO();
        card.setId(1L);
        when(valueOperations.get("coach:knowledge:cards:v1:JAVA")).thenReturn(List.of(card));

        Optional<List<KnowledgeCardVO>> result = cacheService.getCards("java");

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().get(0).getId());
    }

    @Test
    void getCardDetailUsesCardIdKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        KnowledgeCardVO detail = new KnowledgeCardVO();
        detail.setId(1L);
        when(valueOperations.get("coach:knowledge:card:v1:1")).thenReturn(detail);

        Optional<KnowledgeCardVO> result = cacheService.getCardDetail(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void statusExposesReadPathHitMissAndFallbackCounters() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("coach:knowledge:categories:v1")).thenReturn(List.of(new KnowledgeCategoryVO()));
        when(valueOperations.get("coach:knowledge:cards:v1:JAVA")).thenReturn(null);
        when(valueOperations.get("coach:knowledge:card:v1:1")).thenReturn(null);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        when(redisTemplate.keys("coach:knowledge:cards:v1:*")).thenReturn(Set.of());
        when(redisTemplate.keys("coach:knowledge:card:v1:*")).thenReturn(Set.of());

        cacheService.getCategories();
        cacheService.getCards("java");
        cacheService.getCardDetail(1L);
        KnowledgeCacheStatusVO status = cacheService.status();

        assertEquals(1L, status.getHitCount());
        assertEquals(2L, status.getMissCount());
        assertEquals(2L, status.getFallbackCount());
        assertEquals(33, status.getHitRate());
        assertTrue(status.getSummary().contains("hits=1"));
        assertTrue(status.getSummary().contains("misses=2"));
        assertTrue(status.getSummary().contains("fallbacks=2"));
    }

    @Test
    void redisReadExceptionReturnsEmptyAndRecordsFallbackReason() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("coach:knowledge:cards:v1:JAVA")).thenThrow(new IllegalStateException("redis down"));
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        when(redisTemplate.keys("coach:knowledge:cards:v1:*")).thenReturn(Set.of());
        when(redisTemplate.keys("coach:knowledge:card:v1:*")).thenReturn(Set.of());

        Optional<List<KnowledgeCardVO>> result = cacheService.getCards("java");
        KnowledgeCacheStatusVO status = cacheService.status();

        assertTrue(result.isEmpty());
        assertEquals(0L, status.getHitCount());
        assertEquals(1L, status.getMissCount());
        assertEquals(1L, status.getFallbackCount());
        assertTrue(status.getLastFallbackReason().contains("coach:knowledge:cards:v1:JAVA"));
        assertTrue(status.getLastFallbackReason().contains("redis down"));
        assertTrue(status.getSummary().contains("lastFallback="));
    }

    @Test
    void disabledCacheDoesNotTouchRedis() {
        properties.setEnabled(false);

        assertTrue(cacheService.getCategories().isEmpty());
        cacheService.putCategories(List.of(new KnowledgeCategoryVO()));

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void evictAllDeletesCategoryListAndDetailKeys() {
        when(redisTemplate.keys("coach:knowledge:cards:v1:*"))
                .thenReturn(Set.of("coach:knowledge:cards:v1:JAVA", "coach:knowledge:cards:v1:ALL"));
        when(redisTemplate.keys("coach:knowledge:card:v1:*"))
                .thenReturn(Set.of("coach:knowledge:card:v1:1"));

        cacheService.evictAll();

        verify(redisTemplate).delete(Set.of(
                "coach:knowledge:categories:v1",
                "coach:knowledge:cards:v1:JAVA",
                "coach:knowledge:cards:v1:ALL",
                "coach:knowledge:card:v1:1"));
    }
}
