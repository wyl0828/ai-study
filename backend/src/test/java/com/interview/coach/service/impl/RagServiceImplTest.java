package com.interview.coach.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interview.coach.dto.RagChunkHit;
import com.interview.coach.dto.RagRetrieveQuery;
import com.interview.coach.dto.RagRetrieveResult;
import com.interview.coach.entity.KnowledgeCard;
import com.interview.coach.entity.Problem;
import com.interview.coach.entity.RagChunk;
import com.interview.coach.entity.RagDocument;
import com.interview.coach.enums.RagSourceTypeEnum;
import com.interview.coach.mapper.KnowledgeCardMapper;
import com.interview.coach.mapper.ProblemMapper;
import com.interview.coach.mapper.RagChunkMapper;
import com.interview.coach.mapper.RagDocumentMapper;
import java.util.List;
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
}
