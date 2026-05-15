package com.interview.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.coach.entity.KnowledgeCard;
import com.interview.coach.handler.BusinessException;
import com.interview.coach.mapper.KnowledgeCardMapper;
import com.interview.coach.service.KnowledgeCardService;
import com.interview.coach.vo.KnowledgeCardVO;
import com.interview.coach.vo.KnowledgeCategoryVO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    @Override
    public List<KnowledgeCategoryVO> listCategories() {
        Map<String, Long> counts = knowledgeCardMapper.selectList(enabledQuery())
                .stream()
                .collect(Collectors.groupingBy(KnowledgeCard::getCategory, Collectors.counting()));
        return CATEGORIES.stream()
                .map(meta -> toCategoryVO(meta, counts.getOrDefault(meta.category(), 0L)))
                .toList();
    }

    @Override
    public List<KnowledgeCardVO> listCards(String category) {
        LambdaQueryWrapper<KnowledgeCard> query = enabledQuery()
                .orderByAsc(KnowledgeCard::getSortOrder)
                .orderByAsc(KnowledgeCard::getId);
        String normalizedCategory = normalizeCategory(category);
        if (StringUtils.hasText(normalizedCategory)) {
            query.eq(KnowledgeCard::getCategory, normalizedCategory);
        }
        return knowledgeCardMapper.selectList(query).stream()
                .map(card -> toVO(card, false))
                .toList();
    }

    @Override
    public KnowledgeCardVO getCardDetail(Long id) {
        KnowledgeCard card = knowledgeCardMapper.selectOne(enabledQuery()
                .eq(KnowledgeCard::getId, id));
        if (card == null) {
            throw new BusinessException(404, "knowledge card not found");
        }
        return toVO(card, true);
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
