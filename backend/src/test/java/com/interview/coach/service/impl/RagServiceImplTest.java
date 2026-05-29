package com.interview.coach.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.interview.coach.config.EmbeddingProperties;
import com.interview.coach.config.RagVectorProperties;
import com.interview.coach.dto.RagChunkHit;
import com.interview.coach.dto.RagRetrieveQuery;
import com.interview.coach.dto.RagRetrieveResult;
import com.interview.coach.dto.RagVectorHit;
import com.interview.coach.entity.KnowledgeCard;
import com.interview.coach.entity.Problem;
import com.interview.coach.entity.RagChunk;
import com.interview.coach.entity.RagDocument;
import com.interview.coach.enums.RagSourceTypeEnum;
import com.interview.coach.integration.ai.EmbeddingClient;
import com.interview.coach.integration.vector.RagVectorStore;
import com.interview.coach.mapper.KnowledgeCardMapper;
import com.interview.coach.mapper.ProblemMapper;
import com.interview.coach.mapper.RagChunkMapper;
import com.interview.coach.mapper.RagDocumentMapper;
import com.interview.coach.vo.RagHealthVO;
import com.interview.coach.vo.RagSystemRebuildVO;
import com.interview.coach.vo.RagVectorRetryVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.BadSqlGrammarException;

@ExtendWith(MockitoExtension.class)
class RagServiceImplTest {

    @Mock
    private RagDocumentMapper ragDocumentMapper;

    @Mock
    private RagChunkMapper ragChunkMapper;

    @Mock
    private ProblemMapper problemMapper;

    @Mock
    private KnowledgeCardMapper knowledgeCardMapper;

    @Mock
    private RagVectorProperties ragVectorProperties;

    @Mock
    private EmbeddingProperties embeddingProperties;

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private RagVectorStore ragVectorStore;

    @InjectMocks
    private RagServiceImpl ragService;

    @Test
    void retrievePrefersSameProblemAndKnowledgePoint() {
        RagChunk sameProblem = chunk(1L, 10L, RagSourceTypeEnum.PROBLEM, null, 1L,
                "HashMap complement lookup for Two Sum", "HashMap 基础查找", null, "HashMap,Two Sum");
        RagChunk unrelated = chunk(2L, 20L, RagSourceTypeEnum.KNOWLEDGE_CARD, null, null,
                "Redis cache avalanche", "Redis 缓存", null, "Redis");
        when(ragChunkMapper.selectList(any())).thenReturn(List.of(unrelated, sameProblem));
        when(ragDocumentMapper.selectBatchIds(any())).thenReturn(List.of(
                document(10L, "两数之和题目"),
                document(20L, "Redis 缓存雪崩")));

        RagRetrieveQuery query = new RagRetrieveQuery();
        query.setUserId(1L);
        query.setProblemId(1L);
        query.setKnowledgePoint("HashMap 基础查找");
        query.setProblemTitle("两数之和");
        query.setProblemCategory("HashMap");
        query.setKeywords(List.of("HashMap", "Two Sum"));

        RagRetrieveResult result = ragService.retrieve(query);

        assertThat(result.getHits())
                .extracting(RagChunkHit::getSourceId)
                .containsExactly(1L, 20L);
        assertThat(result.getHits().get(0).getScore()).isGreaterThan(result.getHits().get(1).getScore());
        assertThat(result.getHits().get(0).getMatchReason())
                .contains("同题目", "同知识点", "关键词");
        assertThat(result.toPromptBlock()).contains("reason=");
    }

    @Test
    void retrieveDoesNotLeakOtherUsersMistakeMemory() {
        RagChunk currentUserMemory = chunk(1L, 101L, RagSourceTypeEnum.MISTAKE_CARD, 1L, 1L,
                "self pairing mistake", "HashMap 基础查找", "LOGIC_ERROR", "HashMap");
        RagChunk otherUserMemory = chunk(2L, 202L, RagSourceTypeEnum.MISTAKE_CARD, 2L, 1L,
                "other user private mistake", "HashMap 基础查找", "LOGIC_ERROR", "HashMap");
        RagChunk systemCard = chunk(3L, 303L, RagSourceTypeEnum.KNOWLEDGE_CARD, null, null,
                "HashMap lookup", "HashMap 基础查找", null, "HashMap");
        when(ragChunkMapper.selectList(any())).thenReturn(List.of(currentUserMemory, otherUserMemory, systemCard));
        when(ragDocumentMapper.selectBatchIds(any())).thenReturn(List.of(
                document(101L, "当前用户错题"),
                document(202L, "其他用户错题"),
                document(303L, "HashMap 知识卡")));

        RagRetrieveQuery query = new RagRetrieveQuery();
        query.setUserId(1L);
        query.setProblemId(1L);
        query.setKnowledgePoint("HashMap 基础查找");
        query.setErrorType("LOGIC_ERROR");

        RagRetrieveResult result = ragService.retrieve(query);

        assertThat(result.getHits())
                .extracting(RagChunkHit::getSourceId)
                .contains(101L, 303L)
                .doesNotContain(202L);
    }

    @Test
    void retrieveForChatUsesQuestionKeywordsAndKeepsUserMemoryIsolated() {
        RagChunk currentUserMemory = chunk(1L, 101L, RagSourceTypeEnum.AI_DIAGNOSIS, 1L, 1L,
                "Two Sum 中 HashMap 要先查询 complement 再写入当前元素，避免同一个元素被重复使用。",
                "HashMap 基础查找", "LOGIC_ERROR", "HashMap,Two Sum");
        RagChunk otherUserMemory = chunk(2L, 202L, RagSourceTypeEnum.MISTAKE_CARD, 2L, 1L,
                "other user private Two Sum mistake", "HashMap 基础查找", "LOGIC_ERROR", "HashMap");
        RagChunk systemCard = chunk(3L, 303L, RagSourceTypeEnum.KNOWLEDGE_CARD, null, null,
                "HashMap 查询和写入顺序会影响 Two Sum 是否自匹配。",
                "HashMap 基础查找", null, "HashMap,Two Sum");
        when(ragChunkMapper.selectList(any())).thenReturn(List.of(otherUserMemory, systemCard, currentUserMemory));
        when(ragDocumentMapper.selectBatchIds(any())).thenReturn(List.of(
                document(101L, "当前用户 Two Sum 诊断"),
                document(202L, "其他用户错题"),
                document(303L, "HashMap 使用逻辑")));

        RagRetrieveResult result = ragService.retrieveForChat(1L,
                "HashMap 查询和写入顺序为什么会导致 Two Sum 出错？", 5);

        assertThat(result.getHits())
                .extracting(RagChunkHit::getSourceId)
                .contains(101L, 303L)
                .doesNotContain(202L);
        assertThat(result.getHits().get(0).getScore()).isGreaterThanOrEqualTo(10);
    }

    @Test
    void vectorDisabledDoesNotCallEmbeddingOrQdrant() {
        RagChunk systemCard = chunk(3L, 303L, RagSourceTypeEnum.KNOWLEDGE_CARD, null, null,
                "HashMap lookup", "HashMap 基础查找", null, "HashMap");
        when(ragChunkMapper.selectList(any())).thenReturn(List.of(systemCard));
        when(ragDocumentMapper.selectBatchIds(any())).thenReturn(List.of(document(303L, "HashMap 知识卡")));

        RagRetrieveResult result = ragService.retrieveForChat(1L, "HashMap 怎么查找？", 5);

        assertThat(result.hasHits()).isTrue();
        assertThat(result.isVectorEnabled()).isFalse();
        assertThat(result.summary()).contains("MySQL-only", "知识卡 1");
        verify(ragVectorProperties, atLeastOnce()).isEnabled();
        verifyNoInteractions(embeddingClient, ragVectorStore);
    }

    @Test
    void vectorSearchFailureDowngradesToMysqlOnly() {
        when(ragVectorProperties.isEnabled()).thenReturn(true);
        when(ragVectorProperties.getVectorCandidateLimit()).thenReturn(30);
        when(embeddingClient.embed(any())).thenReturn(new float[] {0.1f, 0.2f, 0.3f});
        when(ragVectorStore.search(any(), any(), anyInt())).thenThrow(new IllegalStateException("qdrant down"));
        RagChunk systemCard = chunk(3L, 303L, RagSourceTypeEnum.KNOWLEDGE_CARD, null, null,
                "HashMap lookup", "HashMap 基础查找", null, "HashMap");
        when(ragChunkMapper.selectList(any())).thenReturn(List.of(systemCard));
        when(ragDocumentMapper.selectBatchIds(any())).thenReturn(List.of(document(303L, "HashMap 知识卡")));

        RagRetrieveResult result = ragService.retrieveForChat(1L, "HashMap 怎么查找？", 5);

        assertThat(result.getHits()).hasSize(1);
        assertThat(result.getHits().get(0).getSourceId()).isEqualTo(303L);
        assertThat(result.isVectorEnabled()).isTrue();
        assertThat(result.isVectorDowngraded()).isTrue();
        assertThat(result.summary()).contains("MySQL-only fallback", "知识卡 1");
    }

    @Test
    void embeddingFailureDuringRetrieveDowngradesToMysqlOnly() {
        when(ragVectorProperties.isEnabled()).thenReturn(true);
        when(embeddingClient.embed(any())).thenThrow(new IllegalStateException("embedding down"));
        RagChunk systemCard = chunk(3L, 303L, RagSourceTypeEnum.KNOWLEDGE_CARD, null, null,
                "HashMap lookup", "HashMap 基础查找", null, "HashMap");
        when(ragChunkMapper.selectList(any())).thenReturn(List.of(systemCard));
        when(ragDocumentMapper.selectBatchIds(any())).thenReturn(List.of(document(303L, "HashMap 知识卡")));

        RagRetrieveResult result = ragService.retrieveForChat(1L, "HashMap 怎么查找？", 5);

        assertThat(result.getHits()).hasSize(1);
        assertThat(result.getHits().get(0).getSourceId()).isEqualTo(303L);
        assertThat(result.isVectorEnabled()).isTrue();
        assertThat(result.isVectorDowngraded()).isTrue();
        assertThat(result.summary()).contains("MySQL-only fallback", "知识卡 1");
        verify(ragVectorStore, never()).search(any(), any(), anyInt());
    }

    @Test
    void vectorSimilarityCanPromoteLowerRuleScoreHit() {
        when(ragVectorProperties.isEnabled()).thenReturn(true);
        when(ragVectorProperties.getVectorCandidateLimit()).thenReturn(30);
        when(ragVectorProperties.getHybridRuleWeight()).thenReturn(new BigDecimal("0.40"));
        when(ragVectorProperties.getHybridVectorWeight()).thenReturn(new BigDecimal("0.60"));
        when(embeddingClient.embed(any())).thenReturn(new float[] {0.1f, 0.2f, 0.3f});
        when(ragVectorStore.search(any(), any(), anyInt())).thenReturn(List.of(vectorHit(2L, 0.95f)));
        RagChunk sameProblem = chunk(1L, 10L, RagSourceTypeEnum.KNOWLEDGE_CARD, null, null,
                "Two Sum problem statement", "HashMap 基础查找", null, "两数之和");
        RagChunk semanticHit = chunk(2L, 20L, RagSourceTypeEnum.KNOWLEDGE_CARD, null, null,
                "Complement lookup should happen before inserting current number", "HashMap", null, "HashMap");
        when(ragChunkMapper.selectList(any())).thenReturn(List.of(sameProblem, semanticHit));
        when(ragDocumentMapper.selectBatchIds(any())).thenReturn(List.of(
                document(10L, "两数之和"),
                document(20L, "HashMap 查找顺序")));
        RagRetrieveQuery query = new RagRetrieveQuery();
        query.setUserId(1L);
        query.setProblemTitle("两数之和");
        query.setLimit(5);

        RagRetrieveResult result = ragService.retrieve(query);

        assertThat(result.getHits())
                .extracting(RagChunkHit::getChunkId)
                .containsExactly(2L, 1L);
        assertThat(result.getHits().get(0).getMatchReason()).contains("向量相似度");
    }

    @Test
    void vectorOnlyHitIsLoadedFromMysqlAndMergedIntoResults() {
        when(ragVectorProperties.isEnabled()).thenReturn(true);
        when(ragVectorProperties.getVectorCandidateLimit()).thenReturn(30);
        when(ragVectorProperties.getHybridRuleWeight()).thenReturn(new BigDecimal("0.40"));
        when(ragVectorProperties.getHybridVectorWeight()).thenReturn(new BigDecimal("0.60"));
        when(embeddingClient.embed(any())).thenReturn(new float[] {0.1f, 0.2f, 0.3f});
        when(ragVectorStore.search(any(), any(), anyInt())).thenReturn(List.of(vectorHit(9L, 0.98f)));
        RagChunk mysqlCandidate = chunk(1L, 10L, RagSourceTypeEnum.KNOWLEDGE_CARD, null, null,
                "Two Sum problem statement", "HashMap 基础查找", null, "两数之和");
        RagChunk vectorOnly = chunk(9L, 90L, RagSourceTypeEnum.KNOWLEDGE_CARD, null, null,
                "Semantic complement lookup memory", "HashMap", null, "HashMap");
        when(ragChunkMapper.selectList(any())).thenReturn(List.of(mysqlCandidate));
        when(ragChunkMapper.selectBatchIds(any())).thenReturn(List.of(vectorOnly));
        when(ragDocumentMapper.selectBatchIds(any())).thenReturn(List.of(
                document(10L, "两数之和"),
                document(90L, "HashMap 查找顺序")));
        RagRetrieveQuery query = new RagRetrieveQuery();
        query.setUserId(1L);
        query.setProblemTitle("两数之和");
        query.setLimit(5);

        RagRetrieveResult result = ragService.retrieve(query);

        assertThat(result.getHits())
                .extracting(RagChunkHit::getChunkId)
                .containsExactly(9L, 1L);
        assertThat(result.getHits().get(0).getMatchReason()).contains("向量相似度");
    }

    @Test
    void vectorOnlyHitStillUsesMysqlUserIsolation() {
        when(ragVectorProperties.isEnabled()).thenReturn(true);
        when(ragVectorProperties.getVectorCandidateLimit()).thenReturn(30);
        when(embeddingClient.embed(any())).thenReturn(new float[] {0.1f, 0.2f, 0.3f});
        when(ragVectorStore.search(any(), any(), anyInt())).thenReturn(List.of(vectorHit(9L, 0.98f)));
        RagChunk mysqlCandidate = chunk(1L, 10L, RagSourceTypeEnum.KNOWLEDGE_CARD, null, null,
                "system HashMap card", "HashMap", null, "HashMap");
        RagChunk otherUserVectorHit = chunk(9L, 90L, RagSourceTypeEnum.MISTAKE_CARD, 2L, 1L,
                "other user's private memory", "HashMap", "LOGIC_ERROR", "HashMap");
        when(ragChunkMapper.selectList(any())).thenReturn(List.of(mysqlCandidate));
        when(ragChunkMapper.selectBatchIds(any())).thenReturn(List.of(otherUserVectorHit));
        when(ragDocumentMapper.selectBatchIds(any())).thenReturn(List.of(
                document(10L, "系统知识卡"),
                document(90L, "其他用户错题")));

        RagRetrieveResult result = ragService.retrieveForChat(1L, "HashMap 怎么查找？", 5);

        assertThat(result.getHits())
                .extracting(RagChunkHit::getChunkId)
                .containsExactly(1L);
    }

    @Test
    void retrieveReturnsEmptyResultWhenNoChunksExist() {
        when(ragChunkMapper.selectList(any())).thenReturn(List.of());

        RagRetrieveResult result = ragService.retrieve(new RagRetrieveQuery());

        assertThat(result.hasHits()).isFalse();
        assertThat(result.summary()).contains("未检索到 RAG 证据", "MySQL-only");
    }

    @Test
    void retrieveReturnsEmptyResultWhenRagTablesAreMissing() {
        when(ragChunkMapper.selectList(any())).thenThrow(new BadSqlGrammarException(
                "selectList",
                "SELECT id FROM rag_chunk",
                new java.sql.SQLSyntaxErrorException("Table 'ai_interview_coach.rag_chunk' doesn't exist")));

        RagRetrieveResult result = ragService.retrieve(new RagRetrieveQuery());

        assertThat(result.hasHits()).isFalse();
        assertThat(result.summary()).contains("未检索到 RAG 证据", "MySQL-only");
    }

    @Test
    void checkHealthReportsEmptySystemIndex() {
        when(ragDocumentMapper.selectList(any())).thenReturn(List.of());
        when(ragChunkMapper.selectList(any())).thenReturn(List.of());
        when(knowledgeCardMapper.selectList(any())).thenReturn(List.of());

        RagHealthVO health = ragService.checkHealth();

        assertThat(health.getTablesAvailable()).isTrue();
        assertThat(health.getHealthy()).isFalse();
        assertThat(health.getSystemDocumentCount()).isZero();
        assertThat(health.getSystemChunkCount()).isZero();
        assertThat(health.getVectorEnabled()).isFalse();
        assertThat(health.getVectorIndexedChunkCount()).isZero();
        assertThat(health.getVectorFailedChunkCount()).isZero();
        assertThat(health.getVectorPendingChunkCount()).isZero();
        assertThat(health.getWarnings()).contains("SYSTEM_INDEX_EMPTY");
        assertThat(health.getMaintenanceActions()).contains("POST /api/rag/system-index/rebuild");
        assertThat(health.getStatusLabel()).isEqualTo("NEEDS_REBUILD");
        assertThat(health.getMaintenanceSummary()).contains("系统索引为空", "POST /api/rag/system-index/rebuild");
        assertThat(health.getPreferredMaintenanceAction()).isEqualTo("POST /api/rag/system-index/rebuild");
        assertThat(health.getNextMaintenanceEndpoint()).isEqualTo("/api/rag/system-index/rebuild");
        assertThat(ragHealthValue(health, "maintenancePriority")).isEqualTo("HIGH");
        assertThat(ragHealthValue(health, "maintenanceReason")).asString()
                .contains("system index", "diagnosis");
    }

    @Test
    void checkHealthReportsDuplicateAndStaleSystemDocuments() {
        RagDocument problemDocument = systemDocument(10L, RagSourceTypeEnum.PROBLEM, 1L,
                LocalDateTime.of(2026, 5, 24, 10, 0));
        RagDocument firstKnowledgeCardDocument = systemDocument(20L, RagSourceTypeEnum.KNOWLEDGE_CARD, 2L,
                LocalDateTime.of(2026, 5, 23, 10, 0));
        RagDocument duplicateKnowledgeCardDocument = systemDocument(21L, RagSourceTypeEnum.KNOWLEDGE_CARD, 2L,
                LocalDateTime.of(2026, 5, 23, 10, 0));
        RagDocument missingKnowledgeCardDocument = systemDocument(30L, RagSourceTypeEnum.KNOWLEDGE_CARD, 3L,
                LocalDateTime.of(2026, 5, 24, 10, 0));
        RagDocument userMemoryDocument = systemDocument(40L, RagSourceTypeEnum.MISTAKE_CARD, 4L,
                LocalDateTime.of(2026, 5, 24, 10, 0));
        userMemoryDocument.setUserId(1L);
        when(ragDocumentMapper.selectList(any())).thenReturn(List.of(
                problemDocument,
                firstKnowledgeCardDocument,
                duplicateKnowledgeCardDocument,
                missingKnowledgeCardDocument,
                userMemoryDocument));
        RagChunk systemChunk = chunk(1L, 10L, RagSourceTypeEnum.PROBLEM, null, 1L,
                "Two Sum", "HashMap", null, "HashMap");
        systemChunk.setVectorStatus("INDEXED");
        RagChunk userChunk = chunk(2L, 40L, RagSourceTypeEnum.MISTAKE_CARD, 1L, 1L,
                "private mistake", "HashMap", "LOGIC_ERROR", "HashMap");
        userChunk.setVectorStatus("FAILED");
        when(ragChunkMapper.selectList(any())).thenReturn(List.of(systemChunk, userChunk));
        Problem problem = new Problem();
        problem.setId(1L);
        problem.setUpdatedAt(LocalDateTime.of(2026, 5, 24, 12, 0));
        when(problemMapper.selectList(any())).thenReturn(List.of(problem));
        KnowledgeCard card = new KnowledgeCard();
        card.setId(2L);
        card.setUpdatedAt(LocalDateTime.of(2026, 5, 24, 12, 0));
        when(knowledgeCardMapper.selectList(any())).thenReturn(List.of(card));

        RagHealthVO health = ragService.checkHealth();

        assertThat(health.getTablesAvailable()).isTrue();
        assertThat(health.getHealthy()).isFalse();
        assertThat(health.getSystemDocumentCount()).isEqualTo(4);
        assertThat(health.getSystemChunkCount()).isEqualTo(1);
        assertThat(health.getUserMemoryDocumentCount()).isEqualTo(1);
        assertThat(health.getUserMemoryChunkCount()).isEqualTo(1);
        assertThat(health.getDuplicateSystemDocumentCount()).isEqualTo(1);
        assertThat(health.getStaleProblemDocumentCount()).isEqualTo(1);
        assertThat(health.getStaleKnowledgeCardDocumentCount()).isEqualTo(3);
        assertThat(health.getDocumentSourceTypeCounts())
                .containsEntry("PROBLEM", 1)
                .containsEntry("KNOWLEDGE_CARD", 3)
                .containsEntry("MISTAKE_CARD", 1);
        assertThat(health.getChunkSourceTypeCounts())
                .containsEntry("PROBLEM", 1)
                .containsEntry("MISTAKE_CARD", 1);
        assertThat(health.getVectorIndexedChunkCount()).isEqualTo(1);
        assertThat(health.getVectorFailedChunkCount()).isEqualTo(1);
        assertThat(health.getVectorPendingChunkCount()).isZero();
        assertThat(health.getWarnings()).contains(
                "DUPLICATE_SYSTEM_DOCUMENTS",
                "STALE_PROBLEM_DOCUMENTS",
                "STALE_KNOWLEDGE_CARD_DOCUMENTS");
        assertThat(health.getMaintenanceActions()).contains("POST /api/rag/system-index/rebuild");
        assertThat(health.getStatusLabel()).isEqualTo("NEEDS_REBUILD");
        assertThat(health.getMaintenanceSummary()).contains("系统索引需要重建");
        assertThat(health.getMaintenanceSummary()).contains("staleProblem=1");
        assertThat(health.getPreferredMaintenanceAction()).isEqualTo("POST /api/rag/system-index/rebuild");
        assertThat(health.getNextMaintenanceEndpoint()).isEqualTo("/api/rag/system-index/rebuild");
    }

    @Test
    void checkHealthReportsEnabledSourcesMissingFromSystemIndex() {
        RagDocument indexedProblem = systemDocument(10L, RagSourceTypeEnum.PROBLEM, 1L,
                LocalDateTime.of(2026, 5, 24, 10, 0));
        RagDocument indexedCard = systemDocument(20L, RagSourceTypeEnum.KNOWLEDGE_CARD, 2L,
                LocalDateTime.of(2026, 5, 24, 10, 0));
        when(ragDocumentMapper.selectList(any())).thenReturn(List.of(indexedProblem, indexedCard));
        when(ragChunkMapper.selectList(any())).thenReturn(List.of(
                chunk(1L, 10L, RagSourceTypeEnum.PROBLEM, null, 1L,
                        "Two Sum", "HashMap", null, "HashMap"),
                chunk(2L, 20L, RagSourceTypeEnum.KNOWLEDGE_CARD, null, null,
                        "HashMap card", "HashMap", null, "HashMap")));
        Problem indexedProblemSource = new Problem();
        indexedProblemSource.setId(1L);
        Problem missingProblemSource = new Problem();
        missingProblemSource.setId(206L);
        when(problemMapper.selectList(any())).thenReturn(List.of(indexedProblemSource, missingProblemSource));
        KnowledgeCard indexedCardSource = new KnowledgeCard();
        indexedCardSource.setId(2L);
        KnowledgeCard missingCardSource = new KnowledgeCard();
        missingCardSource.setId(3L);
        when(knowledgeCardMapper.selectList(any())).thenReturn(List.of(indexedCardSource, missingCardSource));

        RagHealthVO health = ragService.checkHealth();

        assertThat(health.getEnabledProblemCount()).isEqualTo(2);
        assertThat(health.getEnabledKnowledgeCardCount()).isEqualTo(2);
        assertThat(health.getMissingSystemProblemDocumentCount()).isEqualTo(1);
        assertThat(health.getMissingSystemKnowledgeCardDocumentCount()).isEqualTo(1);
        assertThat(health.getHealthy()).isFalse();
        assertThat(health.getWarnings()).contains("MISSING_SYSTEM_DOCUMENTS");
        assertThat(health.getMaintenanceActions()).contains("POST /api/rag/system-index/rebuild");
        assertThat(health.getStatusLabel()).isEqualTo("NEEDS_REBUILD");
        assertThat(health.getMaintenanceSummary()).contains("系统索引缺少", "problem=1", "knowledgeCard=1");
        assertThat(health.getPreferredMaintenanceAction()).isEqualTo("POST /api/rag/system-index/rebuild");
        assertThat(health.getNextMaintenanceEndpoint()).isEqualTo("/api/rag/system-index/rebuild");
    }

    @Test
    void checkHealthWarnsWhenVectorEnabledAndFailedChunksExist() {
        when(ragVectorProperties.isEnabled()).thenReturn(true);
        RagDocument document = systemDocument(10L, RagSourceTypeEnum.KNOWLEDGE_CARD, 1L,
                LocalDateTime.of(2026, 5, 24, 10, 0));
        RagChunk indexed = chunk(1L, 10L, RagSourceTypeEnum.KNOWLEDGE_CARD, null, null,
                "indexed", "HashMap", null, "HashMap");
        indexed.setVectorStatus("INDEXED");
        RagChunk failed = chunk(2L, 10L, RagSourceTypeEnum.KNOWLEDGE_CARD, null, null,
                "failed", "HashMap", null, "HashMap");
        failed.setVectorStatus("FAILED");
        RagChunk pending = chunk(3L, 10L, RagSourceTypeEnum.KNOWLEDGE_CARD, null, null,
                "pending", "HashMap", null, "HashMap");
        when(ragDocumentMapper.selectList(any())).thenReturn(List.of(document));
        when(ragChunkMapper.selectList(any())).thenReturn(List.of(indexed, failed, pending));
        KnowledgeCard card = new KnowledgeCard();
        card.setId(1L);
        when(knowledgeCardMapper.selectList(any())).thenReturn(List.of(card));

        RagHealthVO health = ragService.checkHealth();

        assertThat(health.getVectorEnabled()).isTrue();
        assertThat(health.getVectorIndexedChunkCount()).isEqualTo(1);
        assertThat(health.getVectorFailedChunkCount()).isEqualTo(1);
        assertThat(health.getVectorPendingChunkCount()).isEqualTo(1);
        assertThat(health.getHealthy()).isFalse();
        assertThat(health.getWarnings()).contains("VECTOR_INDEX_FAILED", "VECTOR_INDEX_PENDING");
        assertThat(health.getMaintenanceActions()).contains("POST /api/rag/vector/retry-failed?limit=50");
        assertThat(health.getStatusLabel()).isEqualTo("VECTOR_RETRY_REQUIRED");
        assertThat(health.getMaintenanceSummary()).contains("向量索引需要补偿", "failed=1", "pending=1");
        assertThat(health.getPreferredMaintenanceAction()).isEqualTo("POST /api/rag/vector/retry-failed?limit=50");
        assertThat(health.getNextMaintenanceEndpoint()).isEqualTo("/api/rag/vector/retry-failed?limit=50");
    }

    @Test
    void checkHealthReportsBothRebuildAndVectorRetryWhenSystemIndexAndVectorsNeedMaintenance() {
        when(ragVectorProperties.isEnabled()).thenReturn(true);
        RagDocument indexedProblem = systemDocument(10L, RagSourceTypeEnum.PROBLEM, 1L,
                LocalDateTime.of(2026, 5, 24, 10, 0));
        when(ragDocumentMapper.selectList(any())).thenReturn(List.of(indexedProblem));

        RagChunk indexed = chunk(1L, 10L, RagSourceTypeEnum.PROBLEM, null, 1L,
                "Two Sum", "HashMap", null, "HashMap");
        indexed.setVectorStatus("INDEXED");
        RagChunk failed = chunk(2L, 10L, RagSourceTypeEnum.PROBLEM, null, 1L,
                "failed vector", "HashMap", null, "HashMap");
        failed.setVectorStatus("FAILED");
        RagChunk pending = chunk(3L, 10L, RagSourceTypeEnum.PROBLEM, null, 1L,
                "pending vector", "HashMap", null, "HashMap");
        when(ragChunkMapper.selectList(any())).thenReturn(List.of(indexed, failed, pending));

        Problem indexedProblemSource = new Problem();
        indexedProblemSource.setId(1L);
        Problem missingProblemSource = new Problem();
        missingProblemSource.setId(206L);
        when(problemMapper.selectList(any())).thenReturn(List.of(indexedProblemSource, missingProblemSource));
        when(knowledgeCardMapper.selectList(any())).thenReturn(List.of());

        RagHealthVO health = ragService.checkHealth();

        assertThat(health.getWarnings()).contains(
                "MISSING_SYSTEM_DOCUMENTS",
                "VECTOR_INDEX_FAILED",
                "VECTOR_INDEX_PENDING");
        assertThat(health.getMaintenanceActions())
                .contains("POST /api/rag/system-index/rebuild")
                .contains("POST /api/rag/vector/retry-failed?limit=50");
        assertThat(health.getPreferredMaintenanceAction()).isEqualTo("POST /api/rag/system-index/rebuild");
        assertThat(health.getNextMaintenanceEndpoint()).isEqualTo("/api/rag/system-index/rebuild");
        assertThat(health.getMaintenanceSummary())
                .contains("系统索引缺少", "problem=1")
                .contains("向量索引也需要补偿", "failed=1", "pending=1");
        assertThat(ragHealthValue(health, "maintenancePriority")).isEqualTo("HIGH");
        assertThat(ragHealthValue(health, "maintenanceReason")).asString()
                .contains("System index maintenance is prioritized before vector retry");
    }

    @Test
    void checkHealthReportsMissingRagTablesWithoutThrowing() {
        when(ragDocumentMapper.selectList(any())).thenThrow(new BadSqlGrammarException(
                "selectList",
                "SELECT id FROM rag_document",
                new java.sql.SQLSyntaxErrorException("Table 'ai_interview_coach.rag_document' doesn't exist")));

        RagHealthVO health = ragService.checkHealth();

        assertThat(health.getTablesAvailable()).isFalse();
        assertThat(health.getHealthy()).isFalse();
        assertThat(health.getWarnings()).contains("RAG_TABLES_MISSING");
        assertThat(health.getMaintenanceActions()).contains("Run data/rag_mysql_migration.sql before using RAG maintenance endpoints.");
        assertThat(health.getStatusLabel()).isEqualTo("TABLES_MISSING");
        assertThat(health.getMaintenanceSummary()).contains("RAG 表不存在", "data/rag_mysql_migration.sql");
        assertThat(health.getPreferredMaintenanceAction()).contains("rag_mysql_migration.sql");
        assertThat(health.getNextMaintenanceEndpoint()).isNull();
        assertThat(ragHealthValue(health, "maintenancePriority")).isEqualTo("BLOCKED");
        assertThat(ragHealthValue(health, "maintenanceReason")).asString()
                .contains("RAG tables are missing");
    }

    @Test
    void checkHealthReportsNoMaintenanceActionWhenIndexIsHealthy() {
        RagDocument document = systemDocument(10L, RagSourceTypeEnum.KNOWLEDGE_CARD, 1L,
                LocalDateTime.of(2026, 5, 24, 10, 0));
        RagChunk chunk = chunk(1L, 10L, RagSourceTypeEnum.KNOWLEDGE_CARD, null, null,
                "HashMap lookup", "HashMap", null, "HashMap");
        chunk.setVectorStatus("INDEXED");
        KnowledgeCard card = new KnowledgeCard();
        card.setId(1L);
        card.setUpdatedAt(LocalDateTime.of(2026, 5, 23, 10, 0));
        when(ragDocumentMapper.selectList(any())).thenReturn(List.of(document));
        when(ragChunkMapper.selectList(any())).thenReturn(List.of(chunk));
        when(knowledgeCardMapper.selectList(any())).thenReturn(List.of(card));

        RagHealthVO health = ragService.checkHealth();

        assertThat(health.getHealthy()).isTrue();
        assertThat(health.getWarnings()).isEmpty();
        assertThat(health.getMaintenanceActions()).containsExactly("No RAG maintenance action required.");
        assertThat(health.getStatusLabel()).isEqualTo("HEALTHY");
        assertThat(health.getMaintenanceSummary()).isEqualTo("RAG index is healthy; no maintenance action required.");
        assertThat(health.getPreferredMaintenanceAction()).isEqualTo("No RAG maintenance action required.");
        assertThat(health.getNextMaintenanceEndpoint()).isNull();
        assertThat(ragHealthValue(health, "maintenancePriority")).isEqualTo("NONE");
        assertThat(ragHealthValue(health, "maintenanceReason")).asString()
                .contains("RAG index is healthy");
        assertThat(health.getCheckedAt()).isNotNull();
    }

    @Test
    void rebuildSystemIndexPreservesUserMemoryChunks() {
        Problem problem = new Problem();
        problem.setId(1L);
        problem.setTitle("两数之和");
        problem.setCategory("HashMap");
        problem.setDescription("Find two numbers.");
        problem.setHintLevel1("Use a map.");
        problem.setSolutionOutline("Check complement before insert.");
        KnowledgeCard card = new KnowledgeCard();
        card.setId(2L);
        card.setTitle("HashMap 底层结构");
        card.setQuestion("HashMap 是什么？");
        card.setAnswer("数组、链表和红黑树。");
        card.setKeyPoints("数组\n链表\n红黑树");
        card.setTags("HashMap,集合");
        when(problemMapper.selectList(any())).thenReturn(List.of(problem));
        when(knowledgeCardMapper.selectList(any())).thenReturn(List.of(card));

        ragService.rebuildSystemIndex();

        verify(ragChunkMapper).delete(any());
        verify(ragDocumentMapper).delete(any());
        verify(ragDocumentMapper, atLeastOnce()).insert(any(RagDocument.class));
        verify(ragChunkMapper, atLeastOnce()).insert(any(RagChunk.class));
    }

    @Test
    void vectorDisabledIndexingKeepsMysqlOnlyBehavior() {
        doAnswer(invocation -> {
            RagDocument document = invocation.getArgument(0);
            document.setId(10L);
            return 1;
        }).when(ragDocumentMapper).insert(any(RagDocument.class));
        doAnswer(invocation -> {
            RagChunk chunk = invocation.getArgument(0);
            chunk.setId(100L + chunk.getChunkIndex());
            return 1;
        }).when(ragChunkMapper).insert(any(RagChunk.class));

        ragService.indexProblem(problem());

        verify(ragChunkMapper, atLeastOnce()).insert(any(RagChunk.class));
        verify(embeddingClient, never()).embed(any());
        verify(ragVectorStore, never()).upsertChunk(any(), any());
    }

    @Test
    void embeddingFailureMarksChunkVectorStatusFailedWithoutBlockingMysqlChunk() {
        when(ragVectorProperties.isEnabled()).thenReturn(true);
        when(embeddingProperties.getModel()).thenReturn("test-embedding");
        when(embeddingProperties.getDimensions()).thenReturn(3);
        when(embeddingClient.embed(any())).thenThrow(new IllegalStateException("embedding down"));
        doAnswer(invocation -> {
            RagDocument document = invocation.getArgument(0);
            document.setId(10L);
            return 1;
        }).when(ragDocumentMapper).insert(any(RagDocument.class));
        doAnswer(invocation -> {
            RagChunk chunk = invocation.getArgument(0);
            chunk.setId(100L + chunk.getChunkIndex());
            return 1;
        }).when(ragChunkMapper).insert(any(RagChunk.class));

        ragService.indexProblem(problem());

        ArgumentCaptor<RagChunk> captor = ArgumentCaptor.forClass(RagChunk.class);
        verify(ragChunkMapper, atLeastOnce()).updateById(captor.capture());
        assertThat(captor.getAllValues())
                .allSatisfy(chunk -> {
                    assertThat(chunk.getVectorStatus()).isEqualTo("FAILED");
                    assertThat(chunk.getEmbeddingModel()).isEqualTo("test-embedding");
                    assertThat(chunk.getEmbeddingDim()).isEqualTo(3);
                });
    }

    @Test
    void vectorEnabledIndexingMarksChunksIndexedAndUpsertsVectors() {
        when(ragVectorProperties.isEnabled()).thenReturn(true);
        when(embeddingProperties.getModel()).thenReturn("test-embedding");
        when(embeddingClient.embed(any())).thenReturn(new float[] {0.1f, 0.2f, 0.3f});
        doAnswer(invocation -> {
            RagDocument document = invocation.getArgument(0);
            document.setId(10L);
            return 1;
        }).when(ragDocumentMapper).insert(any(RagDocument.class));
        doAnswer(invocation -> {
            RagChunk chunk = invocation.getArgument(0);
            chunk.setId(100L + chunk.getChunkIndex());
            return 1;
        }).when(ragChunkMapper).insert(any(RagChunk.class));

        ragService.indexProblem(problem());

        ArgumentCaptor<RagChunk> captor = ArgumentCaptor.forClass(RagChunk.class);
        verify(ragChunkMapper, atLeastOnce()).updateById(captor.capture());
        assertThat(captor.getAllValues())
                .allSatisfy(chunk -> {
                    assertThat(chunk.getVectorPointId()).isEqualTo(chunk.getId().toString());
                    assertThat(chunk.getVectorStatus()).isEqualTo("INDEXED");
                    assertThat(chunk.getEmbeddingModel()).isEqualTo("test-embedding");
                    assertThat(chunk.getEmbeddingDim()).isEqualTo(3);
                });
        verify(ragVectorStore, atLeastOnce()).upsertChunk(any(RagChunk.class), any());
    }

    @Test
    void rebuildSystemIndexDeletesOnlySystemVectorsWhenVectorEnabled() {
        when(ragVectorProperties.isEnabled()).thenReturn(true);
        when(problemMapper.selectList(any())).thenReturn(List.of());
        when(knowledgeCardMapper.selectList(any())).thenReturn(List.of());

        ragService.rebuildSystemIndex();

        verify(ragVectorStore).deleteSystemChunks();
        verify(ragVectorStore, never()).deleteDocumentChunks(any());
        verify(ragChunkMapper).delete(any());
        verify(ragDocumentMapper).delete(any());
    }

    @Test
    void rebuildSystemIndexForMaintenanceReportsBeforeAfterCountsAndPreservesUserMemory() {
        Problem problem = problem();
        KnowledgeCard card = new KnowledgeCard();
        card.setId(2L);
        card.setTitle("HashMap 底层结构");
        card.setQuestion("HashMap 是什么？");
        card.setAnswer("数组、链表和红黑树。");
        card.setKeyPoints("数组\n链表\n红黑树");
        card.setTags("HashMap,集合");
        RagDocument beforeSystemDocument = systemDocument(10L, RagSourceTypeEnum.PROBLEM, 1L,
                LocalDateTime.of(2026, 5, 24, 10, 0));
        RagDocument beforeUserDocument = systemDocument(20L, RagSourceTypeEnum.MISTAKE_CARD, 5L,
                LocalDateTime.of(2026, 5, 24, 10, 0));
        beforeUserDocument.setUserId(1L);
        RagDocument afterProblemDocument = systemDocument(30L, RagSourceTypeEnum.PROBLEM, 1L,
                LocalDateTime.of(2026, 5, 26, 10, 0));
        RagDocument afterCardDocument = systemDocument(31L, RagSourceTypeEnum.KNOWLEDGE_CARD, 2L,
                LocalDateTime.of(2026, 5, 26, 10, 0));
        RagDocument afterUserDocument = systemDocument(20L, RagSourceTypeEnum.MISTAKE_CARD, 5L,
                LocalDateTime.of(2026, 5, 24, 10, 0));
        afterUserDocument.setUserId(1L);
        RagChunk beforeSystemChunk = chunk(1L, 10L, RagSourceTypeEnum.PROBLEM, null, 1L,
                "old system chunk", "HashMap", null, "HashMap");
        RagChunk beforeUserChunk = chunk(2L, 20L, RagSourceTypeEnum.MISTAKE_CARD, 1L, 1L,
                "private memory", "HashMap", "LOGIC_ERROR", "HashMap");
        RagChunk afterSystemChunk = chunk(3L, 30L, RagSourceTypeEnum.PROBLEM, null, 1L,
                "new system chunk", "HashMap", null, "HashMap");
        RagChunk afterUserChunk = chunk(2L, 20L, RagSourceTypeEnum.MISTAKE_CARD, 1L, 1L,
                "private memory", "HashMap", "LOGIC_ERROR", "HashMap");
        when(ragDocumentMapper.selectList(any()))
                .thenReturn(List.of(beforeSystemDocument, beforeUserDocument))
                .thenReturn(List.of(afterProblemDocument, afterCardDocument, afterUserDocument));
        when(ragChunkMapper.selectList(any()))
                .thenReturn(List.of(beforeSystemChunk, beforeUserChunk))
                .thenReturn(List.of(afterSystemChunk, afterUserChunk));
        when(problemMapper.selectList(any())).thenReturn(List.of(problem));
        when(knowledgeCardMapper.selectList(any()))
                .thenReturn(List.of(card))
                .thenReturn(List.of(card))
                .thenReturn(List.of(card));
        doAnswer(invocation -> {
            RagDocument document = invocation.getArgument(0);
            document.setId(document.getSourceType().equals(RagSourceTypeEnum.PROBLEM.name()) ? 30L : 31L);
            return 1;
        }).when(ragDocumentMapper).insert(any(RagDocument.class));
        doAnswer(invocation -> {
            RagChunk chunk = invocation.getArgument(0);
            chunk.setId(100L + chunk.getChunkIndex());
            return 1;
        }).when(ragChunkMapper).insert(any(RagChunk.class));

        RagSystemRebuildVO result = ragService.rebuildSystemIndexForMaintenance();

        assertThat(result.getAttempted()).isTrue();
        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getIndexedProblemCount()).isEqualTo(1);
        assertThat(result.getIndexedKnowledgeCardCount()).isEqualTo(1);
        assertThat(result.getBeforeSystemDocumentCount()).isEqualTo(1);
        assertThat(result.getAfterSystemDocumentCount()).isEqualTo(2);
        assertThat(result.getBeforeUserMemoryDocumentCount()).isEqualTo(1);
        assertThat(result.getAfterUserMemoryDocumentCount()).isEqualTo(1);
        assertThat(result.getBeforeUserMemoryChunkCount()).isEqualTo(1);
        assertThat(result.getAfterUserMemoryChunkCount()).isEqualTo(1);
        assertThat(result.getMessage()).contains("rebuilt");
        assertThat(result.getRebuiltAt()).isNotNull();
        assertThat(result.getStatusLabel()).isEqualTo("REBUILT");
        assertThat(result.getMaintenanceAction()).contains("No RAG rebuild follow-up required");
        assertThat(ragSystemRebuildValue(result, "boundary"))
                .asString()
                .contains("system problem and knowledge_card")
                .contains("does not delete user memory");
        assertThat(result.getSummary()).contains(
                "system documents 1 -> 2",
                "system chunks 1 -> 1",
                "user memory documents 1 -> 1",
                "user memory chunks 1 -> 1",
                "indexed problem=1",
                "knowledgeCard=1");
        verify(ragChunkMapper).delete(any());
        verify(ragDocumentMapper).delete(any());
    }

    @Test
    void rebuildSystemIndexForMaintenanceReportsMissingTablesWithoutRawFailure() {
        when(ragDocumentMapper.selectList(any())).thenThrow(new BadSqlGrammarException(
                "selectList",
                "SELECT id FROM rag_document",
                new java.sql.SQLSyntaxErrorException("Table 'ai_interview_coach.rag_document' doesn't exist")));
        when(ragChunkMapper.delete(any())).thenThrow(new BadSqlGrammarException(
                "delete",
                "DELETE FROM rag_chunk",
                new java.sql.SQLSyntaxErrorException("Table 'ai_interview_coach.rag_chunk' doesn't exist")));

        RagSystemRebuildVO result = ragService.rebuildSystemIndexForMaintenance();

        assertThat(result.getAttempted()).isTrue();
        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getWarnings()).contains("RAG_TABLES_MISSING");
        assertThat(result.getStatusLabel()).isEqualTo("TABLES_MISSING");
        assertThat(result.getMaintenanceAction()).contains("data/rag_mysql_migration.sql", "/api/rag/system-index/rebuild");
        assertThat(result.getMessage()).contains("rag_mysql_migration.sql");
        assertThat(result.getSummary()).contains("RAG tables missing", "data/rag_mysql_migration.sql");
    }

    private Object ragSystemRebuildValue(RagSystemRebuildVO rebuild, String fieldName) {
        try {
            java.lang.reflect.Field field = RagSystemRebuildVO.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(rebuild);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Missing RAG system rebuild field: " + fieldName, ex);
        }
    }

    private Object ragHealthValue(RagHealthVO health, String fieldName) {
        try {
            java.lang.reflect.Field field = RagHealthVO.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(health);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Missing RAG health field: " + fieldName, ex);
        }
    }

    @Test
    void retryFailedVectorsReindexesFailedChunks() {
        when(ragVectorProperties.isEnabled()).thenReturn(true);
        when(embeddingProperties.getModel()).thenReturn("test-embedding");
        when(embeddingClient.embed(any())).thenReturn(new float[] {0.1f, 0.2f, 0.3f});
        RagChunk failedChunk = chunk(100L, 10L, RagSourceTypeEnum.KNOWLEDGE_CARD, null, null,
                "HashMap complement lookup", "HashMap", null, "HashMap");
        failedChunk.setVectorStatus("FAILED");
        when(ragChunkMapper.selectList(any())).thenReturn(List.of(failedChunk));

        RagVectorRetryVO result = ragService.retryFailedVectors(10);

        assertThat(result.getEnabled()).isTrue();
        assertThat(result.getMatchedRetryableCount()).isEqualTo(1);
        assertThat(result.getAttemptedCount()).isEqualTo(1);
        assertThat(result.getIndexedCount()).isEqualTo(1);
        assertThat(result.getFailedCount()).isZero();
        assertThat(result.getSkippedCount()).isZero();
        assertThat(result.getRetriedAt()).isNotNull();
        assertThat(result.getStatusLabel()).isEqualTo("RETRY_COMPLETED");
        assertThat(result.getMaintenanceAction()).contains("No vector retry follow-up required");
        assertThat(result.getMessage()).contains("failed or pending");
        assertThat(result.getSummary()).contains(
                "matched=1",
                "attempted=1",
                "indexed=1",
                "failed=0",
                "skipped=0");
        assertThat(failedChunk.getVectorPointId()).isEqualTo("100");
        assertThat(failedChunk.getVectorStatus()).isEqualTo("INDEXED");
        assertThat(failedChunk.getEmbeddingModel()).isEqualTo("test-embedding");
        assertThat(failedChunk.getEmbeddingDim()).isEqualTo(3);
        verify(ragVectorStore).upsertChunk(any(RagChunk.class), any());
        verify(ragChunkMapper).updateById(failedChunk);
    }

    @Test
    void retryFailedVectorsAlsoIndexesPendingChunks() {
        when(ragVectorProperties.isEnabled()).thenReturn(true);
        when(embeddingProperties.getModel()).thenReturn("test-embedding");
        when(embeddingClient.embed(any())).thenReturn(new float[] {0.1f, 0.2f, 0.3f});
        RagChunk pendingChunk = chunk(101L, 10L, RagSourceTypeEnum.KNOWLEDGE_CARD, null, null,
                "pending HashMap chunk", "HashMap", null, "HashMap");
        when(ragChunkMapper.selectList(any())).thenReturn(List.of(pendingChunk));

        RagVectorRetryVO result = ragService.retryFailedVectors(10);

        assertThat(result.getMatchedRetryableCount()).isEqualTo(1);
        assertThat(result.getAttemptedCount()).isEqualTo(1);
        assertThat(result.getIndexedCount()).isEqualTo(1);
        assertThat(pendingChunk.getVectorStatus()).isEqualTo("INDEXED");
        verify(ragVectorStore).upsertChunk(any(RagChunk.class), any());
        verify(ragChunkMapper).updateById(pendingChunk);
    }

    @Test
    void retryFailedVectorsKeepsBatchAliveWhenOneChunkFails() {
        when(ragVectorProperties.isEnabled()).thenReturn(true);
        when(embeddingProperties.getModel()).thenReturn("test-embedding");
        when(embeddingProperties.getDimensions()).thenReturn(1536);
        when(embeddingClient.embed(any())).thenReturn(new float[] {0.1f, 0.2f, 0.3f});
        RagChunk firstChunk = chunk(101L, 10L, RagSourceTypeEnum.KNOWLEDGE_CARD, null, null,
                "first retryable chunk", "HashMap", null, "HashMap");
        firstChunk.setVectorStatus("FAILED");
        RagChunk secondChunk = chunk(102L, 10L, RagSourceTypeEnum.KNOWLEDGE_CARD, null, null,
                "second retryable chunk", "HashMap", null, "HashMap");
        secondChunk.setVectorStatus("FAILED");
        when(ragChunkMapper.selectList(any())).thenReturn(List.of(firstChunk, secondChunk));
        doThrow(new IllegalStateException("qdrant unavailable"))
                .doNothing()
                .when(ragVectorStore).upsertChunk(any(RagChunk.class), any());

        RagVectorRetryVO result = ragService.retryFailedVectors(10);

        assertThat(result.getMatchedRetryableCount()).isEqualTo(2);
        assertThat(result.getAttemptedCount()).isEqualTo(2);
        assertThat(result.getIndexedCount()).isEqualTo(1);
        assertThat(result.getFailedCount()).isEqualTo(1);
        assertThat(result.getSkippedCount()).isZero();
        assertThat(result.getStatusLabel()).isEqualTo("RETRY_PARTIAL_FAILED");
        assertThat(result.getMaintenanceAction()).contains("Inspect embedding/Qdrant connectivity");
        assertThat(result.getSummary()).contains("matched=2", "indexed=1", "failed=1", "skipped=0");
        assertThat(firstChunk.getVectorStatus()).isEqualTo("FAILED");
        assertThat(firstChunk.getEmbeddingModel()).isEqualTo("test-embedding");
        assertThat(firstChunk.getEmbeddingDim()).isEqualTo(1536);
        assertThat(secondChunk.getVectorStatus()).isEqualTo("INDEXED");
        verify(ragVectorStore, times(2)).upsertChunk(any(RagChunk.class), any());
        verify(ragChunkMapper).updateById(firstChunk);
        verify(ragChunkMapper).updateById(secondChunk);
    }

    @Test
    void retryFailedVectorsReportsEffectiveLimitForDefaultAndCappedRequests() {
        when(ragVectorProperties.isEnabled()).thenReturn(true);
        when(ragChunkMapper.selectList(any())).thenReturn(List.of());

        RagVectorRetryVO defaulted = ragService.retryFailedVectors(0);
        RagVectorRetryVO capped = ragService.retryFailedVectors(1000);

        assertThat(defaulted.getRequestedLimit()).isZero();
        assertThat(defaulted.getEffectiveLimit()).isEqualTo(50);
        assertThat(defaulted.getStatusLabel()).isEqualTo("NO_RETRYABLE_CHUNKS");
        assertThat(defaulted.getMaintenanceAction()).contains("No vector retry follow-up required");
        assertThat(defaulted.getSummary()).contains("requestedLimit=0", "effectiveLimit=50");
        assertThat(capped.getRequestedLimit()).isEqualTo(1000);
        assertThat(capped.getEffectiveLimit()).isEqualTo(500);
        assertThat(capped.getStatusLabel()).isEqualTo("NO_RETRYABLE_CHUNKS");
        assertThat(capped.getSummary()).contains("requestedLimit=1000", "effectiveLimit=500");
        verifyNoInteractions(embeddingClient, ragVectorStore);
    }

    @Test
    void retryFailedVectorsReportsMissingRagTablesWithoutThrowing() {
        when(ragVectorProperties.isEnabled()).thenReturn(true);
        when(ragChunkMapper.selectList(any())).thenThrow(new BadSqlGrammarException(
                "select",
                "SELECT * FROM rag_chunk",
                new java.sql.SQLSyntaxErrorException("Table 'ai_interview_coach.rag_chunk' doesn't exist")));

        RagVectorRetryVO result = ragService.retryFailedVectors(10);

        assertThat(result.getEnabled()).isTrue();
        assertThat(result.getMatchedRetryableCount()).isZero();
        assertThat(result.getAttemptedCount()).isZero();
        assertThat(result.getIndexedCount()).isZero();
        assertThat(result.getFailedCount()).isZero();
        assertThat(result.getSkippedCount()).isZero();
        assertThat(result.getStatusLabel()).isEqualTo("TABLES_MISSING");
        assertThat(result.getMaintenanceAction()).contains("data/rag_mysql_migration.sql", "limit=10");
        assertThat(result.getMessage()).contains("rag_mysql_migration.sql");
        assertThat(result.getSummary()).contains("RAG tables missing", "data/rag_mysql_migration.sql");
        verifyNoInteractions(embeddingClient, ragVectorStore);
    }

    @Test
    void retryFailedVectorsNoopsWhenVectorDisabled() {
        RagVectorRetryVO result = ragService.retryFailedVectors(10);

        assertThat(result.getEnabled()).isFalse();
        assertThat(result.getAttemptedCount()).isZero();
        assertThat(result.getStatusLabel()).isEqualTo("DISABLED");
        assertThat(result.getMaintenanceAction()).contains("RAG_VECTOR_ENABLED=true");
        assertThat(result.getMessage()).contains("disabled");
        assertThat(result.getSummary()).contains("Vector RAG disabled", "retry skipped");
        verify(ragChunkMapper, never()).selectList(any());
        verifyNoInteractions(embeddingClient, ragVectorStore);
    }

    private RagChunk chunk(Long id, Long documentId, RagSourceTypeEnum sourceType, Long userId,
            Long problemId, String text, String knowledgePoint, String errorType, String tags) {
        RagChunk chunk = new RagChunk();
        chunk.setId(id);
        chunk.setDocumentId(documentId);
        chunk.setSourceType(sourceType.name());
        chunk.setSourceId(sourceType == RagSourceTypeEnum.PROBLEM ? problemId : documentId);
        chunk.setUserId(userId);
        chunk.setProblemId(problemId);
        chunk.setChunkText(text);
        chunk.setKnowledgePoint(knowledgePoint);
        chunk.setErrorType(errorType);
        chunk.setTags(tags);
        return chunk;
    }

    private RagDocument document(Long id, String title) {
        RagDocument document = new RagDocument();
        document.setId(id);
        document.setSourceId(id);
        document.setTitle(title);
        return document;
    }

    private RagDocument systemDocument(Long id, RagSourceTypeEnum sourceType, Long sourceId, LocalDateTime updatedAt) {
        RagDocument document = new RagDocument();
        document.setId(id);
        document.setSourceType(sourceType.name());
        document.setSourceId(sourceId);
        document.setStatus("ACTIVE");
        document.setUpdatedAt(updatedAt);
        return document;
    }

    private RagVectorHit vectorHit(Long chunkId, Float similarity) {
        RagVectorHit hit = new RagVectorHit();
        hit.setChunkId(chunkId);
        hit.setSimilarity(similarity);
        return hit;
    }

    private Problem problem() {
        Problem problem = new Problem();
        problem.setId(1L);
        problem.setTitle("两数之和");
        problem.setCategory("HashMap");
        problem.setDescription("Find two numbers.");
        problem.setHintLevel1("Use a map.");
        problem.setHintLevel2("Check complement.");
        problem.setHintLevel3("Return indexes.");
        problem.setSolutionOutline("Check complement before insert.");
        return problem;
    }
}
