package com.interview.coach.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.service.KnowledgeCardCacheService;
import com.interview.coach.service.KnowledgeCardService;
import com.interview.coach.vo.ApiResponse;
import com.interview.coach.vo.KnowledgeCacheRefreshVO;
import com.interview.coach.vo.KnowledgeCacheStatusVO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class KnowledgeControllerTest {

    @Test
    void cacheStatusReturnsKnowledgeCacheBoundarySummary() {
        KnowledgeCardService knowledgeCardService = Mockito.mock(KnowledgeCardService.class);
        KnowledgeCardCacheService knowledgeCardCacheService = Mockito.mock(KnowledgeCardCacheService.class);
        KnowledgeCacheStatusVO status = new KnowledgeCacheStatusVO();
        status.setEnabled(true);
        status.setProvider("Redis");
        status.setRedisAvailable(true);
        when(knowledgeCardCacheService.status()).thenReturn(status);
        KnowledgeController controller = new KnowledgeController(knowledgeCardService, knowledgeCardCacheService);

        ApiResponse<KnowledgeCacheStatusVO> response = controller.cacheStatus();

        assertTrue(response.getData().getEnabled());
        assertEquals("Redis", response.getData().getProvider());
        verify(knowledgeCardCacheService).status();
    }

    @Test
    void refreshCacheUsesKnowledgeServiceMaintenanceAction() {
        KnowledgeCardService knowledgeCardService = Mockito.mock(KnowledgeCardService.class);
        KnowledgeCardCacheService knowledgeCardCacheService = Mockito.mock(KnowledgeCardCacheService.class);
        KnowledgeCacheRefreshVO refresh = new KnowledgeCacheRefreshVO();
        refresh.setCategoryWarmAttempted(true);
        when(knowledgeCardService.refreshKnowledgeCache()).thenReturn(refresh);
        KnowledgeController controller = new KnowledgeController(knowledgeCardService, knowledgeCardCacheService);

        ApiResponse<KnowledgeCacheRefreshVO> response = controller.refreshCache();

        assertTrue(response.getData().getCategoryWarmAttempted());
        verify(knowledgeCardService).refreshKnowledgeCache();
    }
}
