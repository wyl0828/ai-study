package com.interview.coach.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.config.ProblemCacheProperties;
import com.interview.coach.vo.ProblemDetailVO;
import com.interview.coach.vo.ProblemListItemVO;
import com.interview.coach.vo.ProblemTemplateVO;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisProblemCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

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
}
