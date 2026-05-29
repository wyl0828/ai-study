package com.interview.coach.controller;

import com.interview.coach.service.KnowledgeCardCacheService;
import com.interview.coach.service.KnowledgeCardService;
import com.interview.coach.vo.ApiResponse;
import com.interview.coach.vo.KnowledgeCacheRefreshVO;
import com.interview.coach.vo.KnowledgeCacheStatusVO;
import com.interview.coach.vo.KnowledgeCardVO;
import com.interview.coach.vo.KnowledgeCategoryVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeCardService knowledgeCardService;

    private final KnowledgeCardCacheService knowledgeCardCacheService;

    @GetMapping("/cache/status")
    public ApiResponse<KnowledgeCacheStatusVO> cacheStatus() {
        return ApiResponse.success(knowledgeCardCacheService.status());
    }

    @PostMapping("/cache/refresh")
    public ApiResponse<KnowledgeCacheRefreshVO> refreshCache() {
        return ApiResponse.success(knowledgeCardService.refreshKnowledgeCache());
    }

    @GetMapping("/categories")
    public ApiResponse<List<KnowledgeCategoryVO>> listCategories() {
        return ApiResponse.success(knowledgeCardService.listCategories());
    }

    @GetMapping("/cards")
    public ApiResponse<List<KnowledgeCardVO>> listCards(@RequestParam(required = false) String category) {
        return ApiResponse.success(knowledgeCardService.listCards(category));
    }

    @GetMapping("/cards/{id}")
    public ApiResponse<KnowledgeCardVO> getCardDetail(@PathVariable Long id) {
        return ApiResponse.success(knowledgeCardService.getCardDetail(id));
    }
}
