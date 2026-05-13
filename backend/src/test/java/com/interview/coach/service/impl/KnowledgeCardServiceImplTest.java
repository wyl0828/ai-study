package com.interview.coach.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.interview.coach.entity.KnowledgeCard;
import com.interview.coach.handler.BusinessException;
import com.interview.coach.mapper.KnowledgeCardMapper;
import com.interview.coach.vo.KnowledgeCardVO;
import com.interview.coach.vo.KnowledgeCategoryVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeCardServiceImplTest {

    @Mock
    private KnowledgeCardMapper knowledgeCardMapper;

    @InjectMocks
    private KnowledgeCardServiceImpl knowledgeCardService;

    @Test
    void listCardsReturnsEnabledCardsWithoutAnswers() {
        when(knowledgeCardMapper.selectList(any())).thenReturn(List.of(card(1L, "JAVA", true)));

        List<KnowledgeCardVO> cards = knowledgeCardService.listCards("JAVA");

        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).getCategory()).isEqualTo("JAVA");
        assertThat(cards.get(0).getTitle()).isEqualTo("HashMap 底层结构");
        assertThat(cards.get(0).getTags()).containsExactly("基础", "集合");
        assertThat(cards.get(0).getKeyPoints()).containsExactly("数组定位桶", "链表处理冲突");
        assertThat(cards.get(0).getAnswer()).isNull();
    }

    @Test
    void getCardDetailReturnsAnswerAndStructuredLists() {
        when(knowledgeCardMapper.selectOne(any())).thenReturn(card(1L, "JAVA", true));

        KnowledgeCardVO detail = knowledgeCardService.getCardDetail(1L);

        assertThat(detail.getAnswer()).contains("数组、链表和红黑树");
        assertThat(detail.getFollowUp()).contains("为什么链表");
        assertThat(detail.getKeyPoints()).containsExactly("数组定位桶", "链表处理冲突");
        assertThat(detail.getSourceUrl()).isEqualTo("https://xiaolincoding.com/interview/");
    }

    @Test
    void getCardDetailThrowsWhenCardDoesNotExist() {
        when(knowledgeCardMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> knowledgeCardService.getCardDetail(404L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("knowledge card not found");
    }

    @Test
    void listCategoriesUsesFixedOrderAndCountsEnabledCards() {
        KnowledgeCard javaCard = card(1L, "JAVA", true);
        KnowledgeCard springCard = card(2L, "SPRING", true);
        when(knowledgeCardMapper.selectList(any())).thenReturn(List.of(springCard, javaCard));

        List<KnowledgeCategoryVO> categories = knowledgeCardService.listCategories();

        assertThat(categories)
                .extracting(KnowledgeCategoryVO::getCategory)
                .containsExactly("JAVA", "JVM", "SPRING", "MYSQL", "REDIS");
        assertThat(categories.get(0).getCount()).isEqualTo(1);
        assertThat(categories.get(2).getCount()).isEqualTo(1);
        assertThat(categories.get(4).getCount()).isZero();
    }

    @Test
    void listReviewCardsPrefersCategoryDiversityForDashboardPlan() {
        when(knowledgeCardMapper.selectList(any())).thenReturn(List.of(
                card(1L, "JAVA", "HashMap 底层结构", true),
                card(2L, "JAVA", "ArrayList 和 LinkedList 的区别", true),
                card(3L, "MYSQL", "MySQL 索引失效场景", true),
                card(4L, "SPRING", "Spring 事务失效场景", true)));

        List<KnowledgeCardVO> cards = knowledgeCardService.listReviewCards(3);

        assertThat(cards)
                .extracting(KnowledgeCardVO::getCategory)
                .containsExactly("JAVA", "SPRING", "MYSQL");
        assertThat(cards)
                .extracting(KnowledgeCardVO::getTitle)
                .containsExactly("HashMap 底层结构", "Spring 事务失效场景", "MySQL 索引失效场景");
    }

    private KnowledgeCard card(Long id, String category, Boolean enabled) {
        return card(id, category, "HashMap 底层结构", enabled);
    }

    private KnowledgeCard card(Long id, String category, String title, Boolean enabled) {
        KnowledgeCard card = new KnowledgeCard();
        card.setId(id);
        card.setCategory(category);
        card.setTitle(title);
        card.setQuestion("HashMap 在 JDK 1.8 中的底层结构是什么？");
        card.setAnswer("HashMap 底层主要由数组、链表和红黑树组成。");
        card.setFollowUp("为什么链表长度超过阈值后不是一定立即转红黑树？");
        card.setKeyPoints("数组定位桶\n链表处理冲突");
        card.setDifficulty("MEDIUM");
        card.setTags("基础,集合");
        card.setSourceName("小林 coding");
        card.setSourceUrl("https://xiaolincoding.com/interview/");
        card.setEnabled(enabled);
        card.setSortOrder(1);
        card.setCreatedAt(LocalDateTime.now());
        card.setUpdatedAt(LocalDateTime.now());
        return card;
    }
}
