package com.interview.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.coach.entity.KnowledgeCard;
import com.interview.coach.handler.BusinessException;
import com.interview.coach.mapper.KnowledgeCardMapper;
import com.interview.coach.service.KnowledgeCardCacheService;
import com.interview.coach.service.KnowledgeCardService;
import com.interview.coach.vo.KnowledgeCacheRefreshVO;
import com.interview.coach.vo.KnowledgeCacheStatusVO;
import com.interview.coach.vo.KnowledgeCardVO;
import com.interview.coach.vo.KnowledgeCategoryVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeCardServiceImpl implements KnowledgeCardService {

    private static final List<CategoryMeta> CATEGORIES = List.of(
            new CategoryMeta("JAVA", "Java"),
            new CategoryMeta("JVM", "JVM"),
            new CategoryMeta("SPRING", "Spring"),
            new CategoryMeta("MYSQL", "MySQL"),
            new CategoryMeta("REDIS", "Redis"),
            new CategoryMeta("AI", "AI"));

    private final KnowledgeCardMapper knowledgeCardMapper;

    private final KnowledgeCardCacheService knowledgeCardCacheService;

    @Override
    public List<KnowledgeCategoryVO> listCategories() {
        try {
            var cached = knowledgeCardCacheService.getCategories();
            if (cached.isPresent()) {
                return cached.get();
            }
        } catch (RuntimeException ex) {
            log.warn("Knowledge category cache read failed; downgrade to MySQL: {}", ex.getMessage());
        }
        return listCategoriesFromMysqlAndCache();
    }

    private List<KnowledgeCategoryVO> listCategoriesFromMysqlAndCache() {
        Map<String, Long> counts = knowledgeCardMapper.selectList(enabledQuery())
                .stream()
                .collect(Collectors.groupingBy(KnowledgeCard::getCategory, Collectors.counting()));
        List<KnowledgeCategoryVO> result = CATEGORIES.stream()
                .map(meta -> toCategoryVO(meta, counts.getOrDefault(meta.category(), 0L)))
                .toList();
        try {
            knowledgeCardCacheService.putCategories(result);
        } catch (RuntimeException ex) {
            log.warn("Knowledge category cache write failed; keep MySQL result: {}", ex.getMessage());
        }
        return result;
    }

    @Override
    public List<KnowledgeCardVO> listCards(String category) {
        try {
            var cached = knowledgeCardCacheService.getCards(category);
            if (cached.isPresent()) {
                return cached.get();
            }
        } catch (RuntimeException ex) {
            log.warn("Knowledge card list cache read failed; downgrade to MySQL: category={}, error={}",
                    category, ex.getMessage());
        }
        return listCardsFromMysqlAndCache(category);
    }

    private List<KnowledgeCardVO> listCardsFromMysqlAndCache(String category) {
        LambdaQueryWrapper<KnowledgeCard> query = enabledQuery()
                .orderByAsc(KnowledgeCard::getSortOrder)
                .orderByAsc(KnowledgeCard::getId);
        String normalizedCategory = normalizeCategory(category);
        if (StringUtils.hasText(normalizedCategory)) {
            query.eq(KnowledgeCard::getCategory, normalizedCategory);
        }
        List<KnowledgeCardVO> result = knowledgeCardMapper.selectList(query).stream()
                .map(card -> toVO(card, true))
                .toList();
        try {
            knowledgeCardCacheService.putCards(category, result);
        } catch (RuntimeException ex) {
            log.warn("Knowledge card list cache write failed; keep MySQL result: category={}, error={}",
                    category, ex.getMessage());
        }
        return result;
    }

    @Override
    public KnowledgeCardVO getCardDetail(Long id) {
        try {
            var cached = knowledgeCardCacheService.getCardDetail(id);
            if (cached.isPresent()) {
                return cached.get();
            }
        } catch (RuntimeException ex) {
            log.warn("Knowledge card detail cache read failed; downgrade to MySQL: cardId={}, error={}",
                    id, ex.getMessage());
        }
        return getCardDetailFromMysqlAndCache(id);
    }

    private KnowledgeCardVO getCardDetailFromMysqlAndCache(Long id) {
        KnowledgeCard card = knowledgeCardMapper.selectOne(enabledQuery()
                .eq(KnowledgeCard::getId, id));
        if (card == null) {
            throw new BusinessException(404, "knowledge card not found");
        }
        KnowledgeCardVO result = toVO(card, true);
        try {
            knowledgeCardCacheService.putCardDetail(id, result);
        } catch (RuntimeException ex) {
            log.warn("Knowledge card detail cache write failed; keep MySQL result: cardId={}, error={}",
                    id, ex.getMessage());
        }
        return result;
    }

    @Override
    public List<KnowledgeCardVO> listReviewCards(int limit) {
        int safeLimit = Math.max(0, limit);
        if (safeLimit == 0) {
            return List.of();
        }
        List<KnowledgeCard> cards = knowledgeCardMapper.selectList(enabledQuery()
                .orderByAsc(KnowledgeCard::getSortOrder)
                .orderByAsc(KnowledgeCard::getId));
        return pickDiverseReviewCards(cards, safeLimit)
                .stream()
                .map(card -> toVO(card, false))
                .toList();
    }

    @Override
    public KnowledgeCacheRefreshVO refreshKnowledgeCache() {
        KnowledgeCacheRefreshVO result = new KnowledgeCacheRefreshVO();
        result.setRefreshedAt(LocalDateTime.now());
        KnowledgeCacheStatusVO status = knowledgeCardCacheService.status();
        result.setEnabled(status.getEnabled());
        result.setRedisAvailable(status.getRedisAvailable());
        if (!Boolean.TRUE.equals(status.getEnabled()) || !Boolean.TRUE.equals(status.getRedisAvailable())) {
            result.setMessage("Knowledge cache is disabled or Redis is unavailable; refresh skipped.");
            result.setSummary("Knowledge cache refresh skipped: Redis unavailable or disabled.");
            fillKnowledgeCacheRefreshMaintenance(result);
            return result;
        }

        try {
            knowledgeCardCacheService.evictAll();
        } catch (RuntimeException ex) {
            result.setFailedCount(result.getFailedCount() + 1);
            log.warn("Knowledge cache evict failed before refresh; continue warm-up from MySQL: {}",
                    ex.getMessage());
        }

        try {
            listCategoriesFromMysqlAndCache();
            result.setCategoryWarmAttempted(true);
        } catch (RuntimeException ex) {
            result.setFailedCount(result.getFailedCount() + 1);
            log.warn("Knowledge category cache warm-up failed: {}", ex.getMessage());
        }

        for (CategoryMeta category : CATEGORIES) {
            try {
                listCardsFromMysqlAndCache(category.category());
                result.setListWarmAttemptedCount(result.getListWarmAttemptedCount() + 1);
            } catch (RuntimeException ex) {
                result.setFailedCount(result.getFailedCount() + 1);
                log.warn("Knowledge card list cache warm-up failed: category={}, error={}",
                        category.category(), ex.getMessage());
            }
        }

        try {
            List<KnowledgeCard> cards = knowledgeCardMapper.selectList(enabledQuery()
                    .orderByAsc(KnowledgeCard::getSortOrder)
                    .orderByAsc(KnowledgeCard::getId));
            for (KnowledgeCard card : cards) {
                try {
                    knowledgeCardCacheService.putCardDetail(card.getId(), toVO(card, true));
                    result.setDetailWarmAttemptedCount(result.getDetailWarmAttemptedCount() + 1);
                } catch (RuntimeException ex) {
                    result.setFailedCount(result.getFailedCount() + 1);
                    log.warn("Knowledge card detail cache warm-up failed: cardId={}, error={}",
                            card.getId(), ex.getMessage());
                }
            }
        } catch (RuntimeException ex) {
            result.setFailedCount(result.getFailedCount() + 1);
            log.warn("Knowledge card detail warm-up source query failed: {}", ex.getMessage());
        }

        result.setTotalWarmAttemptedCount((Boolean.TRUE.equals(result.getCategoryWarmAttempted()) ? 1 : 0)
                + result.getListWarmAttemptedCount()
                + result.getDetailWarmAttemptedCount());
        result.setMessage("Knowledge cache refresh attempted from MySQL source.");
        result.setSummary("Knowledge cache warm-up attempted " + result.getTotalWarmAttemptedCount()
                + " keys, failed " + result.getFailedCount() + ".");
        fillKnowledgeCacheRefreshMaintenance(result);
        return result;
    }

    private void fillKnowledgeCacheRefreshMaintenance(KnowledgeCacheRefreshVO result) {
        if (!Boolean.TRUE.equals(result.getEnabled()) || !Boolean.TRUE.equals(result.getRedisAvailable())) {
            result.setStatusLabel("SKIPPED");
            result.setMaintenanceAction(
                    "Enable KNOWLEDGE_CACHE_ENABLED and Redis, then retry POST /api/knowledge/cache/refresh.");
            return;
        }
        if (result.getFailedCount() != null && result.getFailedCount() > 0) {
            result.setStatusLabel("PARTIAL_FAILED");
            result.setMaintenanceAction("Check cache warm-up warnings, then retry POST /api/knowledge/cache/refresh.");
            return;
        }
        result.setStatusLabel("READY");
        result.setMaintenanceAction(
                "Knowledge cache refreshed; use GET /api/knowledge/cache/status to confirm warmed keys.");
    }

    private LambdaQueryWrapper<KnowledgeCard> enabledQuery() {
        return new LambdaQueryWrapper<KnowledgeCard>()
                .eq(KnowledgeCard::getEnabled, true);
    }

    private List<KnowledgeCard> pickDiverseReviewCards(List<KnowledgeCard> cards, int limit) {
        List<KnowledgeCard> selected = new ArrayList<>();
        for (CategoryMeta category : CATEGORIES) {
            cards.stream()
                    .filter(card -> category.category().equals(card.getCategory()))
                    .findFirst()
                    .ifPresent(selected::add);
            if (selected.size() >= limit) {
                return selected;
            }
        }
        for (KnowledgeCard card : cards) {
            if (!selected.contains(card)) {
                selected.add(card);
            }
            if (selected.size() >= limit) {
                return selected;
            }
        }
        return selected;
    }

    private KnowledgeCategoryVO toCategoryVO(CategoryMeta meta, Long count) {
        KnowledgeCategoryVO vo = new KnowledgeCategoryVO();
        vo.setCategory(meta.category());
        vo.setLabel(meta.label());
        vo.setCount(Math.toIntExact(count));
        return vo;
    }

    private KnowledgeCardVO toVO(KnowledgeCard card, boolean includeAnswer) {
        KnowledgeCardVO vo = new KnowledgeCardVO();
        vo.setId(card.getId());
        vo.setCategory(card.getCategory());
        vo.setLabel(label(card.getCategory()));
        vo.setTitle(card.getTitle());
        vo.setQuestion(card.getQuestion());
        vo.setAnswer(includeAnswer ? card.getAnswer() : null);
        vo.setFollowUp(includeAnswer ? card.getFollowUp() : null);
        vo.setKeyPoints(splitLines(card.getKeyPoints()));
        vo.setDifficulty(card.getDifficulty());
        vo.setTags(splitComma(card.getTags()));
        vo.setSourceName(card.getSourceName());
        vo.setSourceUrl(card.getSourceUrl());
        return vo;
    }

    private String label(String category) {
        return CATEGORIES.stream()
                .filter(meta -> meta.category().equals(category))
                .map(CategoryMeta::label)
                .findFirst()
                .orElse(category);
    }

    private String normalizeCategory(String category) {
        if (!StringUtils.hasText(category) || "ALL".equalsIgnoreCase(category)) {
            return null;
        }
        return category.trim().toUpperCase();
    }

    private List<String> splitComma(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private List<String> splitLines(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private record CategoryMeta(String category, String label) {
    }
}
