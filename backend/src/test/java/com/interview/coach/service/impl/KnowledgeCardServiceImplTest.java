package com.interview.coach.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.entity.KnowledgeCard;
import com.interview.coach.handler.BusinessException;
import com.interview.coach.mapper.KnowledgeCardMapper;
import com.interview.coach.service.KnowledgeCardCacheService;
import com.interview.coach.vo.KnowledgeCacheRefreshVO;
import com.interview.coach.vo.KnowledgeCacheStatusVO;
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

    @Mock
    private KnowledgeCardCacheService knowledgeCardCacheService;

    @InjectMocks
    private KnowledgeCardServiceImpl knowledgeCardService;

    @Test
    void listCardsReturnsEnabledCardsWithAnswersForTrainingPage() {
        when(knowledgeCardCacheService.getCards("JAVA")).thenReturn(java.util.Optional.empty());
        when(knowledgeCardMapper.selectList(any())).thenReturn(List.of(card(1L, "JAVA", true)));

        List<KnowledgeCardVO> cards = knowledgeCardService.listCards("JAVA");

        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).getCategory()).isEqualTo("JAVA");
        assertThat(cards.get(0).getTitle()).isEqualTo("HashMap 底层结构");
        assertThat(cards.get(0).getTags()).containsExactly("基础", "集合");
        assertThat(cards.get(0).getKeyPoints()).containsExactly("数组定位桶", "链表处理冲突");
        assertThat(cards.get(0).getAnswer()).contains("数组、链表和红黑树");
        assertThat(cards.get(0).getFollowUp()).contains("为什么链表");
        verify(knowledgeCardCacheService).putCards(eq("JAVA"), eq(cards));
    }

    @Test
    void listCardsReturnsCachedValueWithoutQueryingMysql() {
        KnowledgeCardVO cached = new KnowledgeCardVO();
        cached.setId(1L);
        cached.setCategory("JAVA");
        cached.setTitle("cached");
        when(knowledgeCardCacheService.getCards("JAVA")).thenReturn(java.util.Optional.of(List.of(cached)));

        List<KnowledgeCardVO> cards = knowledgeCardService.listCards("JAVA");

        assertThat(cards).extracting(KnowledgeCardVO::getTitle).containsExactly("cached");
        verify(knowledgeCardMapper, never()).selectList(any());
    }

    @Test
    void listCardsDowngradesToMysqlWhenCacheReadFails() {
        when(knowledgeCardCacheService.getCards("JAVA")).thenThrow(new IllegalStateException("redis down"));
        when(knowledgeCardMapper.selectList(any())).thenReturn(List.of(card(1L, "JAVA", true)));

        List<KnowledgeCardVO> cards = knowledgeCardService.listCards("JAVA");

        assertThat(cards).hasSize(1);
    }

    @Test
    void getCardDetailReturnsAnswerAndStructuredLists() {
        when(knowledgeCardCacheService.getCardDetail(1L)).thenReturn(java.util.Optional.empty());
        when(knowledgeCardMapper.selectOne(any())).thenReturn(card(1L, "JAVA", true));

        KnowledgeCardVO detail = knowledgeCardService.getCardDetail(1L);

        assertThat(detail.getAnswer()).contains("数组、链表和红黑树");
        assertThat(detail.getFollowUp()).contains("为什么链表");
        assertThat(detail.getKeyPoints()).containsExactly("数组定位桶", "链表处理冲突");
        assertThat(detail.getSourceUrl()).isEqualTo("https://xiaolincoding.com/interview/");
        verify(knowledgeCardCacheService).putCardDetail(1L, detail);
    }

    @Test
    void getCardDetailReturnsCachedValueWithoutQueryingMysql() {
        KnowledgeCardVO cached = new KnowledgeCardVO();
        cached.setId(1L);
        cached.setTitle("cached detail");
        when(knowledgeCardCacheService.getCardDetail(1L)).thenReturn(java.util.Optional.of(cached));

        KnowledgeCardVO detail = knowledgeCardService.getCardDetail(1L);

        assertThat(detail.getTitle()).isEqualTo("cached detail");
        verify(knowledgeCardMapper, never()).selectOne(any());
    }

    @Test
    void getCardDetailDowngradesToMysqlWhenCacheReadFails() {
        when(knowledgeCardCacheService.getCardDetail(1L)).thenThrow(new IllegalStateException("redis down"));
        when(knowledgeCardMapper.selectOne(any())).thenReturn(card(1L, "JAVA", true));

        KnowledgeCardVO detail = knowledgeCardService.getCardDetail(1L);

        assertThat(detail.getTitle()).isEqualTo("HashMap 底层结构");
    }

    @Test
    void getCardDetailIgnoresCacheWriteFailure() {
        when(knowledgeCardCacheService.getCardDetail(1L)).thenReturn(java.util.Optional.empty());
        when(knowledgeCardMapper.selectOne(any())).thenReturn(card(1L, "JAVA", true));
        org.mockito.Mockito.doThrow(new IllegalStateException("redis down"))
                .when(knowledgeCardCacheService).putCardDetail(eq(1L), any());

        KnowledgeCardVO detail = knowledgeCardService.getCardDetail(1L);

        assertThat(detail.getTitle()).isEqualTo("HashMap 底层结构");
    }

    @Test
    void getCardDetailThrowsWhenCardDoesNotExist() {
        when(knowledgeCardCacheService.getCardDetail(404L)).thenReturn(java.util.Optional.empty());
        when(knowledgeCardMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> knowledgeCardService.getCardDetail(404L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("knowledge card not found");
    }

    @Test
    void listCategoriesUsesFixedOrderAndCountsEnabledCards() {
        KnowledgeCard javaCard = card(1L, "JAVA", true);
        KnowledgeCard springCard = card(2L, "SPRING", true);
        KnowledgeCard aiCard = card(3L, "AI", true);
        when(knowledgeCardCacheService.getCategories()).thenReturn(java.util.Optional.empty());
        when(knowledgeCardMapper.selectList(any())).thenReturn(List.of(springCard, javaCard, aiCard));

        List<KnowledgeCategoryVO> categories = knowledgeCardService.listCategories();

        assertThat(categories)
                .extracting(KnowledgeCategoryVO::getCategory)
                .containsExactly("JAVA", "JVM", "SPRING", "MYSQL", "REDIS", "AI");
        assertThat(categories.get(0).getCount()).isEqualTo(1);
        assertThat(categories.get(2).getCount()).isEqualTo(1);
        assertThat(categories.get(4).getCount()).isZero();
        assertThat(categories.get(5).getCount()).isEqualTo(1);
        verify(knowledgeCardCacheService).putCategories(categories);
    }

    @Test
    void listCategoriesReturnsCachedValueWithoutQueryingMysql() {
        KnowledgeCategoryVO cached = new KnowledgeCategoryVO();
        cached.setCategory("JAVA");
        cached.setLabel("Java");
        cached.setCount(12);
        when(knowledgeCardCacheService.getCategories()).thenReturn(java.util.Optional.of(List.of(cached)));

        List<KnowledgeCategoryVO> categories = knowledgeCardService.listCategories();

        assertThat(categories).extracting(KnowledgeCategoryVO::getCount).containsExactly(12);
        verify(knowledgeCardMapper, never()).selectList(any());
    }

    @Test
    void listCategoriesDowngradesToMysqlWhenCacheReadFails() {
        when(knowledgeCardCacheService.getCategories()).thenThrow(new IllegalStateException("redis down"));
        when(knowledgeCardMapper.selectList(any())).thenReturn(List.of(card(1L, "JAVA", true)));

        List<KnowledgeCategoryVO> categories = knowledgeCardService.listCategories();

        assertThat(categories.get(0).getCategory()).isEqualTo("JAVA");
        assertThat(categories.get(0).getCount()).isEqualTo(1);
    }

    @Test
    void listCategoriesIgnoresCacheWriteFailure() {
        when(knowledgeCardCacheService.getCategories()).thenReturn(java.util.Optional.empty());
        when(knowledgeCardMapper.selectList(any())).thenReturn(List.of(card(1L, "JAVA", true)));
        org.mockito.Mockito.doThrow(new IllegalStateException("redis down"))
                .when(knowledgeCardCacheService).putCategories(any());

        List<KnowledgeCategoryVO> categories = knowledgeCardService.listCategories();

        assertThat(categories.get(0).getCount()).isEqualTo(1);
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

    @Test
    void refreshKnowledgeCacheEvictsAndWarmsCategoriesListsAndDetailsFromMysql() {
        KnowledgeCacheStatusVO status = new KnowledgeCacheStatusVO();
        status.setEnabled(true);
        status.setRedisAvailable(true);
        when(knowledgeCardCacheService.status()).thenReturn(status);
        when(knowledgeCardMapper.selectList(any())).thenReturn(List.of(
                card(1L, "JAVA", true),
                card(2L, "REDIS", "Redis 缓存雪崩", true)));

        KnowledgeCacheRefreshVO result = knowledgeCardService.refreshKnowledgeCache();

        assertThat(result.getEnabled()).isTrue();
        assertThat(result.getRedisAvailable()).isTrue();
        assertThat(result.getCategoryWarmAttempted()).isTrue();
        assertThat(result.getListWarmAttemptedCount()).isEqualTo(6);
        assertThat(result.getDetailWarmAttemptedCount()).isEqualTo(2);
        assertThat(result.getTotalWarmAttemptedCount()).isEqualTo(9);
        assertThat(result.getSummary()).isEqualTo("Knowledge cache warm-up attempted 9 keys, failed 0.");
        assertThat(result.getStatusLabel()).isEqualTo("READY");
        assertThat(result.getMaintenanceAction())
                .isEqualTo("Knowledge cache refreshed; use GET /api/knowledge/cache/status to confirm warmed keys.");
        assertThat(result.getRefreshedAt()).isNotNull();
        verify(knowledgeCardCacheService).evictAll();
        verify(knowledgeCardCacheService).putCategories(any());
        verify(knowledgeCardCacheService).putCards(eq("JAVA"), any());
        verify(knowledgeCardCacheService).putCardDetail(eq(1L), any());
    }

    @Test
    void refreshKnowledgeCacheSkipsWhenDisabledOrRedisUnavailable() {
        KnowledgeCacheStatusVO status = new KnowledgeCacheStatusVO();
        status.setEnabled(true);
        status.setRedisAvailable(false);
        when(knowledgeCardCacheService.status()).thenReturn(status);

        KnowledgeCacheRefreshVO result = knowledgeCardService.refreshKnowledgeCache();

        assertThat(result.getRedisAvailable()).isFalse();
        assertThat(result.getTotalWarmAttemptedCount()).isZero();
        assertThat(result.getMessage()).contains("refresh skipped");
        assertThat(result.getSummary()).contains("skipped");
        assertThat(result.getStatusLabel()).isEqualTo("SKIPPED");
        assertThat(result.getMaintenanceAction())
                .isEqualTo("Enable KNOWLEDGE_CACHE_ENABLED and Redis, then retry POST /api/knowledge/cache/refresh.");
        assertThat(result.getRefreshedAt()).isNotNull();
        verify(knowledgeCardCacheService, never()).evictAll();
    }

    @Test
    void refreshKnowledgeCacheReportsPartialFailureWithRetryAction() {
        KnowledgeCacheStatusVO status = new KnowledgeCacheStatusVO();
        status.setEnabled(true);
        status.setRedisAvailable(true);
        when(knowledgeCardCacheService.status()).thenReturn(status);
        doThrow(new RuntimeException("redis evict failed")).when(knowledgeCardCacheService).evictAll();
        when(knowledgeCardMapper.selectList(any())).thenReturn(List.of(card(1L, "JAVA", true)));

        KnowledgeCacheRefreshVO result = knowledgeCardService.refreshKnowledgeCache();

        assertThat(result.getFailedCount()).isEqualTo(1);
        assertThat(result.getStatusLabel()).isEqualTo("PARTIAL_FAILED");
        assertThat(result.getMaintenanceAction())
                .isEqualTo("Check cache warm-up warnings, then retry POST /api/knowledge/cache/refresh.");
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
