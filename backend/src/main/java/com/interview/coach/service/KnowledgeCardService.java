package com.interview.coach.service;

import com.interview.coach.vo.KnowledgeCardVO;
import com.interview.coach.vo.KnowledgeCategoryVO;
import java.util.List;

public interface KnowledgeCardService {

    List<KnowledgeCategoryVO> listCategories();

    List<KnowledgeCardVO> listCards(String category);

    KnowledgeCardVO getCardDetail(Long id);

    List<KnowledgeCardVO> listReviewCards(int limit);
}
