package com.interview.coach.service.impl;

import com.interview.coach.config.ProblemCacheProperties;
import com.interview.coach.service.ProblemCacheService;
import com.interview.coach.vo.ProblemDetailVO;
import com.interview.coach.vo.ProblemListItemVO;
import com.interview.coach.vo.ProblemTemplateVO;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisProblemCacheService implements ProblemCacheService {

    private static final String PROBLEM_LIST_KEY = "coach:problem:list:v1";

    private static final String PROBLEM_DETAIL_KEY_PREFIX = "coach:problem:detail:v1:";

    private static final String PROBLEM_TEMPLATE_KEY_PREFIX = "coach:problem:template:v1:";

    private final RedisTemplate<String, Object> redisTemplate;

    private final ProblemCacheProperties properties;

    @Override
    @SuppressWarnings("unchecked")
    public Optional<List<ProblemListItemVO>> getProblemList() {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        Object value = redisTemplate.opsForValue().get(PROBLEM_LIST_KEY);
        if (value instanceof List<?> list) {
            return Optional.of((List<ProblemListItemVO>) list);
        }
        return Optional.empty();
    }

    @Override
    public void putProblemList(List<ProblemListItemVO> problems) {
        if (!properties.isEnabled() || problems == null) {
            return;
        }
        put(PROBLEM_LIST_KEY, problems, properties.getListTtl());
    }

    @Override
    public Optional<ProblemDetailVO> getProblemDetail(Long problemId) {
        if (!properties.isEnabled() || problemId == null) {
            return Optional.empty();
        }
        Object value = redisTemplate.opsForValue().get(detailKey(problemId));
        if (value instanceof ProblemDetailVO detail) {
            return Optional.of(detail);
        }
        return Optional.empty();
    }

    @Override
    public void putProblemDetail(Long problemId, ProblemDetailVO detail) {
        if (!properties.isEnabled() || problemId == null || detail == null) {
            return;
        }
        put(detailKey(problemId), detail, properties.getDetailTtl());
    }

    @Override
    public Optional<ProblemTemplateVO> getProblemTemplate(Long problemId) {
        if (!properties.isEnabled() || problemId == null) {
            return Optional.empty();
        }
        Object value = redisTemplate.opsForValue().get(templateKey(problemId));
        if (value instanceof ProblemTemplateVO template) {
            return Optional.of(template);
        }
        return Optional.empty();
    }

    @Override
    public void putProblemTemplate(Long problemId, ProblemTemplateVO template) {
        if (!properties.isEnabled() || problemId == null || template == null) {
            return;
        }
        put(templateKey(problemId), template, properties.getTemplateTtl());
    }

    @Override
    public void evictProblem(Long problemId) {
        if (!properties.isEnabled() || problemId == null) {
            return;
        }
        redisTemplate.delete(List.of(detailKey(problemId), templateKey(problemId)));
        redisTemplate.delete(PROBLEM_LIST_KEY);
    }

    @Override
    public void evictAll() {
        if (!properties.isEnabled()) {
            return;
        }
        redisTemplate.delete(PROBLEM_LIST_KEY);
    }

    private void put(String key, Object value, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            redisTemplate.opsForValue().set(key, value);
            return;
        }
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    private String detailKey(Long problemId) {
        return PROBLEM_DETAIL_KEY_PREFIX + problemId;
    }

    private String templateKey(Long problemId) {
        return PROBLEM_TEMPLATE_KEY_PREFIX + problemId;
    }
}
