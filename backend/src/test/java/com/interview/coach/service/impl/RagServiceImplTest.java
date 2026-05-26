package com.interview.coach.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
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
        assertThat(result.summary()).isEqualTo("未检索到 RAG 证据");
    }

    @Test
    void retrieveReturnsEmptyResultWhenRagTablesAreMissing() {
        when(ragChunkMapper.selectList(any())).thenThrow(new BadSqlGrammarException(
                "selectList",
                "SELECT id FROM rag_chunk",
                new java.sql.SQLSyntaxErrorException("Table 'ai_interview_coach.rag_chunk' doesn't exist")));

        RagRetrieveResult result = ragService.retrieve(new RagRetrieveQuery());

        assertThat(result.hasHits()).isFalse();
        assertThat(result.summary()).isEqualTo("未检索到 RAG 证据");
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
        assertThat(health.getWarnings()).contains("SYSTEM_INDEX_EMPTY");
    }

    @Test
    void checkHealthReportsDuplicateAndStaleKnowledgeCardDocuments() {
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
        RagChunk userChunk = chunk(2L, 40L, RagSourceTypeEnum.MISTAKE_CARD, 1L, 1L,
                "private mistake", "HashMap", "LOGIC_ERROR", "HashMap");
        when(ragChunkMapper.selectList(any())).thenReturn(List.of(systemChunk, userChunk));
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
        assertThat(health.getStaleKnowledgeCardDocumentCount()).isEqualTo(3);
        assertThat(health.getWarnings()).contains(
                "DUPLICATE_SYSTEM_DOCUMENTS",
                "STALE_KNOWLEDGE_CARD_DOCUMENTS");
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
