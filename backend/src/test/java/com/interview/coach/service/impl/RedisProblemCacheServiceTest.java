package com.interview.coach.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.config.ProblemCacheProperties;
import com.interview.coach.vo.ProblemCacheStatusVO;
import com.interview.coach.vo.ProblemDetailVO;
import com.interview.coach.vo.ProblemListItemVO;
import com.interview.coach.vo.ProblemTemplateVO;
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
class RedisProblemCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisConnection connection;

    private ProblemCacheProperties properties;

    private RedisProblemCacheService cacheService;

    @BeforeEach
    void setUp() {
        properties = new ProblemCacheProperties();
        cacheService = new RedisProblemCacheService(redisTemplate, properties);
    }

    @Test
    void putProblemListUsesStableKeyAndTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ProblemListItemVO item = new ProblemListItemVO();
        item.setId(1L);

        cacheService.putProblemList(List.of(item));

        verify(valueOperations).set(eq("coach:problem:list:v1"), eq(List.of(item)), eq(Duration.ofMinutes(10)));
    }

    @Test
    void statusReturnsCacheBoundaryAndRedisAvailability() {
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        when(redisTemplate.hasKey("coach:problem:list:v1")).thenReturn(true);
        when(redisTemplate.keys("coach:problem:detail:v1:*"))
                .thenReturn(Set.of("coach:problem:detail:v1:1", "coach:problem:detail:v1:2"));
        when(redisTemplate.keys("coach:problem:template:v1:*"))
                .thenReturn(Set.of("coach:problem:template:v1:1"));

        ProblemCacheStatusVO status = cacheService.status();

        assertTrue(status.getEnabled());
        assertTrue(status.getRedisAvailable());
        assertEquals("Redis", status.getProvider());
        assertEquals("coach:problem:list:v1", status.getListKey());
        assertEquals("coach:problem:detail:v1:{problemId}", status.getDetailKeyPattern());
        assertEquals("coach:problem:template:v1:{problemId}", status.getTemplateKeyPattern());
        assertEquals(600L, status.getListTtlSeconds());
        assertEquals(1800L, status.getDetailTtlSeconds());
        assertEquals(1800L, status.getTemplateTtlSeconds());
        assertEquals(4, status.getCachedKeyCount());
        assertTrue(status.getListCached());
        assertEquals(2, status.getDetailCachedKeyCount());
        assertEquals(1, status.getTemplateCachedKeyCount());
        assertTrue(status.getFallback().contains("降级 MySQL"));
        assertEquals("READY", status.getStatusLabel());
        assertNotNull(status.getCheckedAt());
        assertTrue(status.getSummary().contains("Problem cache ready"));
        assertTrue(status.getSummary().contains("cached keys=4"));
        assertTrue(status.getMaintenanceAction().contains("POST /api/problems/cache/refresh"));
    }

    @Test
    void statusDoesNotFailWhenRedisPingFails() {
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenThrow(new IllegalStateException("redis down"));

        ProblemCacheStatusVO status = cacheService.status();

        assertTrue(status.getEnabled());
        assertTrue(!status.getRedisAvailable());
        assertEquals(0, status.getCachedKeyCount());
        assertTrue(!status.getListCached());
        assertEquals(0, status.getDetailCachedKeyCount());
        assertEquals(0, status.getTemplateCachedKeyCount());
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
        when(redisTemplate.hasKey("coach:problem:list:v1")).thenThrow(new IllegalStateException("keys disabled"));

        ProblemCacheStatusVO status = cacheService.status();

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
    void getProblemDetailUsesProblemIdKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ProblemDetailVO detail = new ProblemDetailVO();
        detail.setId(1L);
        when(valueOperations.get("coach:problem:detail:v1:1")).thenReturn(detail);

        Optional<ProblemDetailVO> result = cacheService.getProblemDetail(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void statusExposesReadPathHitMissAndFallbackCounters() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("coach:problem:list:v1")).thenReturn(List.of(new ProblemListItemVO()));
        when(valueOperations.get("coach:problem:detail:v1:1")).thenReturn(null);
        when(valueOperations.get("coach:problem:template:v1:1")).thenReturn(null);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        when(redisTemplate.keys("coach:problem:detail:v1:*")).thenReturn(Set.of());
        when(redisTemplate.keys("coach:problem:template:v1:*")).thenReturn(Set.of());

        cacheService.getProblemList();
        cacheService.getProblemDetail(1L);
        cacheService.getProblemTemplate(1L);
        ProblemCacheStatusVO status = cacheService.status();

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
        when(valueOperations.get("coach:problem:list:v1")).thenThrow(new IllegalStateException("redis down"));
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        when(redisTemplate.keys("coach:problem:detail:v1:*")).thenReturn(Set.of());
        when(redisTemplate.keys("coach:problem:template:v1:*")).thenReturn(Set.of());

        Optional<List<ProblemListItemVO>> result = cacheService.getProblemList();
        ProblemCacheStatusVO status = cacheService.status();

        assertTrue(result.isEmpty());
        assertEquals(0L, status.getHitCount());
        assertEquals(1L, status.getMissCount());
        assertEquals(1L, status.getFallbackCount());
        assertTrue(status.getLastFallbackReason().contains("coach:problem:list:v1"));
        assertTrue(status.getLastFallbackReason().contains("redis down"));
        assertTrue(status.getSummary().contains("lastFallback="));
    }

    @Test
    void successfulWarmupClearsStaleFallbackReason() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("coach:problem:list:v1")).thenThrow(new IllegalStateException("old format"));
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        when(redisTemplate.hasKey("coach:problem:list:v1")).thenReturn(true);
        when(redisTemplate.keys("coach:problem:detail:v1:*")).thenReturn(Set.of());
        when(redisTemplate.keys("coach:problem:template:v1:*")).thenReturn(Set.of());

        cacheService.getProblemList();
        ProblemListItemVO item = new ProblemListItemVO();
        item.setId(1L);
        cacheService.putProblemList(List.of(item));
        ProblemCacheStatusVO status = cacheService.status();

        assertEquals(1L, status.getFallbackCount());
        assertEquals(null, status.getLastFallbackReason());
        assertTrue(!status.getSummary().contains("lastFallback="));
    }

    @Test
    void putProblemTemplateUsesProblemIdKeyAndTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ProblemTemplateVO template = new ProblemTemplateVO();
        template.setProblemId(1L);
        template.setTemplateCode("class Solution {}");

        cacheService.putProblemTemplate(1L, template);

        verify(valueOperations).set(
                eq("coach:problem:template:v1:1"),
                eq(template),
                eq(Duration.ofMinutes(30)));
    }

    @Test
    void disabledCacheDoesNotTouchRedis() {
        properties.setEnabled(false);

        assertTrue(cacheService.getProblemList().isEmpty());
        cacheService.putProblemList(List.of(new ProblemListItemVO()));

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void evictProblemDeletesListDetailAndTemplateKeys() {
        cacheService.evictProblem(1L);

        verify(redisTemplate).delete(List.of("coach:problem:detail:v1:1", "coach:problem:template:v1:1"));
        verify(redisTemplate).delete("coach:problem:list:v1");
    }

    @Test
    void evictAllDeletesListDetailAndTemplateKeys() {
        when(redisTemplate.keys("coach:problem:detail:v1:*"))
                .thenReturn(Set.of("coach:problem:detail:v1:1", "coach:problem:detail:v1:2"));
        when(redisTemplate.keys("coach:problem:template:v1:*"))
                .thenReturn(Set.of("coach:problem:template:v1:1"));

        cacheService.evictAll();

        verify(redisTemplate).delete(Set.of(
                "coach:problem:list:v1",
                "coach:problem:detail:v1:1",
                "coach:problem:detail:v1:2",
                "coach:problem:template:v1:1"));
    }
}
