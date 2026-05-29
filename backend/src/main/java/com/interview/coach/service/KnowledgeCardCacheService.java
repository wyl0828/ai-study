package com.interview.coach.service;

import com.interview.coach.vo.KnowledgeCacheStatusVO;
import com.interview.coach.vo.KnowledgeCardVO;
import com.interview.coach.vo.KnowledgeCategoryVO;
import java.util.List;
import java.util.Optional;

public interface KnowledgeCardCacheService {

    KnowledgeCacheStatusVO status();

    Optional<List<KnowledgeCategoryVO>> getCategories();

    void putCategories(List<KnowledgeCategoryVO> categories);

    Optional<List<KnowledgeCardVO>> getCards(String category);

    void putCards(String category, List<KnowledgeCardVO> cards);

    Optional<KnowledgeCardVO> getCardDetail(Long cardId);

    void putCardDetail(Long cardId, KnowledgeCardVO detail);

    void evictAll();
}
