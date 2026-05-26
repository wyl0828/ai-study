package com.interview.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.coach.agent.AgentContext;
import com.interview.coach.config.EmbeddingProperties;
import com.interview.coach.config.RagVectorProperties;
import com.interview.coach.dto.AgentExecutionObservation;
import com.interview.coach.dto.RagChunkHit;
import com.interview.coach.dto.RagRetrieveQuery;
import com.interview.coach.dto.RagRetrieveResult;
import com.interview.coach.dto.RagVectorHit;
import com.interview.coach.entity.AiDiagnosis;
import com.interview.coach.entity.KnowledgeCard;
import com.interview.coach.entity.MistakeCard;
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
import com.interview.coach.service.RagService;
import com.interview.coach.vo.RagHealthVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private static final String ACTIVE = "ACTIVE";

    private static final String VECTOR_INDEXED = "INDEXED";

    private static final String VECTOR_FAILED = "FAILED";

    private static final int MAX_CANDIDATES = 200;

    private final RagDocumentMapper ragDocumentMapper;

    private final RagChunkMapper ragChunkMapper;

    private final ProblemMapper problemMapper;

    private final KnowledgeCardMapper knowledgeCardMapper;

    private final RagVectorProperties ragVectorProperties;

    private final EmbeddingProperties embeddingProperties;

    private final EmbeddingClient embeddingClient;

    private final RagVectorStore ragVectorStore;

    @Override
    public RagRetrieveResult retrieveForDiagnosis(AgentContext context, int limit) {
        RagRetrieveQuery query = new RagRetrieveQuery();
        query.setLimit(limit);
        query.setUserId(context.getUserId());
        query.setProblemId(context.getProblemId());
        if (context.getProblem() != null) {
            query.setProblemTitle(context.getProblem().getTitle());
            query.setProblemCategory(context.getProblem().getCategory());
        }
        if (context.getDiagnosis() != null) {
            query.setErrorType(context.getDiagnosis().getErrorType());
            query.setKnowledgePoint(context.getDiagnosis().getKnowledgePoint());
        } else if (context.getKnowledgePoints() != null && !context.getKnowledgePoints().isEmpty()) {
            query.setKnowledgePoint(context.getKnowledgePoints().get(0));
        }
        AgentExecutionObservation observation = context.getObservation();
        if (observation != null) {
            query.setExecutionStatus(observation.getStatus());
            query.setErrorMessage(observation.getErrorMessage());
        }
        query.setKeywords(buildKeywords(context));
        return retrieve(query);
    }

    @Override
    public RagRetrieveResult retrieveForChat(Long userId, String question, int limit) {
        RagRetrieveQuery query = new RagRetrieveQuery();
        query.setUserId(userId);
        query.setLimit(limit);
        query.setKeywords(buildQuestionKeywords(question));
        return retrieve(query);
    }

    @Override
    public RagRetrieveResult retrieve(RagRetrieveQuery query) {
        try {
            return doRetrieve(query);
        } catch (BadSqlGrammarException ex) {
            if (isMissingRagTable(ex)) {
                log.warn("RAG tables are missing; returning empty retrieval result. Run data/rag_mysql_migration.sql. Cause: {}",
                        rootMessage(ex));
                return new RagRetrieveResult();
            }
            throw ex;
        }
    }

    private RagRetrieveResult doRetrieve(RagRetrieveQuery query) {
        RagRetrieveQuery safeQuery = query == null ? new RagRetrieveQuery() : query;
        int limit = safeQuery.getLimit() <= 0 ? 5 : safeQuery.getLimit();
        List<RagChunk> chunks = loadCandidateChunks(safeQuery);
        Map<Long, RagVectorHit> vectorHits = vectorHits(safeQuery);
        chunks = mergeVectorOnlyChunks(chunks, vectorHits);
        if (chunks == null || chunks.isEmpty()) {
            return new RagRetrieveResult();
        }

        Map<Long, RagDocument> documents = loadDocuments(chunks);
        List<RagChunkHit> hits = chunks.stream()
                .filter(chunk -> isAllowedForUser(safeQuery.getUserId(), chunk))
                .filter(chunk -> isActiveDocument(documents.get(chunk.getDocumentId())))
                .map(chunk -> toHit(
                        safeQuery,
                        chunk,
                        documents.get(chunk.getDocumentId()),
                        score(safeQuery, chunk, documents.get(chunk.getDocumentId()))))
                .map(hit -> applyVectorScore(hit, vectorHits.get(hit.getChunkId())))
                .sorted(Comparator.comparingInt(RagChunkHit::getScore).reversed()
                        .thenComparing(hit -> hit.getChunkId() == null ? Long.MAX_VALUE : hit.getChunkId()))
                .limit(limit)
                .toList();

        RagRetrieveResult result = new RagRetrieveResult();
        result.setHits(hits);
        return result;
    }

    private List<RagChunk> loadCandidateChunks(RagRetrieveQuery query) {
        return ragChunkMapper.selectList(new LambdaQueryWrapper<RagChunk>()
                .and(wrapper -> {
                    if (query.getUserId() == null) {
                        wrapper.isNull(RagChunk::getUserId);
                    } else {
                        wrapper.isNull(RagChunk::getUserId)
                                .or()
                                .eq(RagChunk::getUserId, query.getUserId());
                    }
                })
                .last("LIMIT " + MAX_CANDIDATES));
    }

    private List<RagChunk> mergeVectorOnlyChunks(List<RagChunk> chunks, Map<Long, RagVectorHit> vectorHits) {
        if (vectorHits == null || vectorHits.isEmpty()) {
            return chunks == null ? List.of() : chunks;
        }
        Map<Long, RagChunk> merged = new HashMap<>();
        if (chunks != null) {
            for (RagChunk chunk : chunks) {
                if (chunk.getId() != null) {
                    merged.put(chunk.getId(), chunk);
                }
            }
        }

        List<Long> missingVectorChunkIds = vectorHits.keySet().stream()
                .filter(chunkId -> !merged.containsKey(chunkId))
                .toList();
        if (!missingVectorChunkIds.isEmpty()) {
            List<RagChunk> vectorChunks = ragChunkMapper.selectBatchIds(missingVectorChunkIds);
            if (vectorChunks != null) {
                for (RagChunk chunk : vectorChunks) {
                    if (chunk.getId() != null) {
                        merged.put(chunk.getId(), chunk);
                    }
                }
            }
        }
        return new ArrayList<>(merged.values());
    }

    private Map<Long, RagVectorHit> vectorHits(RagRetrieveQuery query) {
        if (!vectorEnabled()) {
            return Map.of();
        }
        String queryText = vectorQueryText(query);
        if (!StringUtils.hasText(queryText)) {
            return Map.of();
        }
        try {
            float[] vector = embeddingClient.embed(queryText);
            List<RagVectorHit> hits = ragVectorStore.search(query, vector, vectorCandidateLimit());
            if (hits == null || hits.isEmpty()) {
                return Map.of();
            }
            return hits.stream()
                    .filter(hit -> hit.getChunkId() != null)
                    .collect(Collectors.toMap(
                            RagVectorHit::getChunkId,
                            Function.identity(),
                            (left, right) -> left.getSimilarity() >= right.getSimilarity() ? left : right));
        } catch (RuntimeException ex) {
            log.warn("Vector RAG search failed; downgrade to MySQL-only retrieval: {}", ex.getMessage());
            return Map.of();
        }
    }

    private RagChunkHit applyVectorScore(RagChunkHit hit, RagVectorHit vectorHit) {
        if (vectorHit == null || vectorHit.getSimilarity() == null) {
            return hit;
        }
        double similarity = Math.max(0, vectorHit.getSimilarity());
        int hybridScore = (int) Math.round(hit.getScore() * ruleWeight() + similarity * 100 * vectorWeight());
        hit.setScore(hybridScore);
        String vectorReason = "向量相似度 " + String.format(Locale.ROOT, "%.2f", similarity);
        hit.setMatchReason(joinNonBlank("、", hit.getMatchReason(), vectorReason));
        return hit;
    }

    private String vectorQueryText(RagRetrieveQuery query) {
        if (query == null) {
            return "";
        }
        List<String> keywords = query.getKeywords() == null ? List.of() : query.getKeywords();
        return joinNonBlank("\n",
                query.getProblemTitle(),
                query.getProblemCategory(),
                query.getKnowledgePoint(),
                query.getErrorType(),
                query.getExecutionStatus(),
                query.getErrorMessage(),
                String.join("\n", keywords));
    }

    private boolean isMissingRagTable(BadSqlGrammarException ex) {
        String message = rootMessage(ex).toLowerCase();
        return (message.contains("rag_chunk") || message.contains("rag_document"))
                && (message.contains("doesn't exist") || message.contains("does not exist")
                || message.contains("unknown table") || message.contains("no such table"));
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? throwable.getMessage() : current.getMessage();
    }

    @Override
    public RagHealthVO checkHealth() {
        try {
            return doCheckHealth();
        } catch (BadSqlGrammarException ex) {
            if (!isMissingRagTable(ex)) {
                throw ex;
            }
            RagHealthVO health = new RagHealthVO();
            health.setTablesAvailable(false);
            health.setHealthy(false);
            health.setCheckedAt(LocalDateTime.now());
            health.getWarnings().add("RAG_TABLES_MISSING");
            return health;
        }
    }

    private RagHealthVO doCheckHealth() {
        LocalDateTime checkedAt = LocalDateTime.now();
        List<RagDocument> documents = safeList(ragDocumentMapper.selectList(new LambdaQueryWrapper<>()));
        List<RagChunk> chunks = safeList(ragChunkMapper.selectList(new LambdaQueryWrapper<>()));
        List<KnowledgeCard> enabledKnowledgeCards = safeList(knowledgeCardMapper.selectList(
                new LambdaQueryWrapper<KnowledgeCard>().eq(KnowledgeCard::getEnabled, true)));

        List<RagDocument> systemDocuments = documents.stream()
                .filter(document -> document.getUserId() == null)
                .toList();
        List<RagChunk> systemChunks = chunks.stream()
                .filter(chunk -> chunk.getUserId() == null)
                .toList();

        RagHealthVO health = new RagHealthVO();
        health.setCheckedAt(checkedAt);
        health.setTablesAvailable(true);
        health.setSystemDocumentCount(systemDocuments.size());
        health.setSystemChunkCount(systemChunks.size());
        health.setUserMemoryDocumentCount((int) documents.stream()
                .filter(document -> document.getUserId() != null)
                .count());
        health.setUserMemoryChunkCount((int) chunks.stream()
                .filter(chunk -> chunk.getUserId() != null)
                .count());
        health.setDuplicateSystemDocumentCount(countDuplicateSystemDocuments(systemDocuments));
        health.setStaleKnowledgeCardDocumentCount(countStaleKnowledgeCardDocuments(
                systemDocuments,
                enabledKnowledgeCards));

        addRagHealthWarnings(health);
        health.setHealthy(health.getWarnings().isEmpty());
        return health;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private int countDuplicateSystemDocuments(List<RagDocument> systemDocuments) {
        Map<String, Integer> counts = new HashMap<>();
        for (RagDocument document : systemDocuments) {
            if (!isSystemSource(document) || document.getSourceId() == null) {
                continue;
            }
            String key = document.getSourceType() + "#" + document.getSourceId();
            counts.merge(key, 1, Integer::sum);
        }
        return (int) counts.values().stream()
                .filter(count -> count > 1)
                .count();
    }

    private boolean isSystemSource(RagDocument document) {
        return RagSourceTypeEnum.PROBLEM.name().equals(document.getSourceType())
                || RagSourceTypeEnum.KNOWLEDGE_CARD.name().equals(document.getSourceType());
    }

    private int countStaleKnowledgeCardDocuments(
            List<RagDocument> systemDocuments,
            List<KnowledgeCard> enabledKnowledgeCards) {
        Map<Long, KnowledgeCard> cardsById = enabledKnowledgeCards.stream()
                .filter(card -> card.getId() != null)
                .collect(Collectors.toMap(KnowledgeCard::getId, Function.identity(), (first, ignored) -> first));
        return (int) systemDocuments.stream()
                .filter(document -> RagSourceTypeEnum.KNOWLEDGE_CARD.name().equals(document.getSourceType()))
                .filter(document -> isStaleKnowledgeCardDocument(document, cardsById.get(document.getSourceId())))
                .count();
    }

    private boolean isStaleKnowledgeCardDocument(RagDocument document, KnowledgeCard card) {
        if (card == null) {
            return true;
        }
        if (card.getUpdatedAt() == null) {
            return false;
        }
        return document.getUpdatedAt() == null || card.getUpdatedAt().isAfter(document.getUpdatedAt());
    }

    private void addRagHealthWarnings(RagHealthVO health) {
        if (health.getSystemDocumentCount() == 0 || health.getSystemChunkCount() == 0) {
            health.getWarnings().add("SYSTEM_INDEX_EMPTY");
        }
        if (health.getDuplicateSystemDocumentCount() > 0) {
            health.getWarnings().add("DUPLICATE_SYSTEM_DOCUMENTS");
        }
        if (health.getStaleKnowledgeCardDocumentCount() > 0) {
            health.getWarnings().add("STALE_KNOWLEDGE_CARD_DOCUMENTS");
        }
    }

    @Override
    @Transactional
    public void indexProblem(Problem problem) {
        if (problem == null || problem.getId() == null) {
            return;
        }
        RagDocument document = upsertDocument(
                RagSourceTypeEnum.PROBLEM,
                problem.getId(),
                null,
                problem.getId(),
                problem.getTitle(),
                problem.getCategory(),
                null,
                problem.getCategory());
        int index = 0;
        index = insertChunk(document, index, joinNonBlank("\n", problem.getTitle(), problem.getCategory(), problem.getDescription()));
        index = insertChunk(document, index, joinNonBlank("\n", problem.getHintLevel1(), problem.getHintLevel2(), problem.getHintLevel3()));
        insertChunk(document, index, problem.getSolutionOutline());
    }

    @Override
    @Transactional
    public void indexKnowledgeCard(KnowledgeCard card) {
        if (card == null || card.getId() == null) {
            return;
        }
        RagDocument document = upsertDocument(
                RagSourceTypeEnum.KNOWLEDGE_CARD,
                card.getId(),
                null,
                null,
                card.getTitle(),
                card.getTitle(),
                null,
                joinNonBlank(",", card.getCategory(), card.getTags()));
        int index = 0;
        index = insertChunk(document, index, card.getQuestion());
        index = insertChunk(document, index, card.getAnswer());
        index = insertChunk(document, index, card.getKeyPoints());
        insertChunk(document, index, card.getFollowUp());
    }

    @Override
    @Transactional
    public void indexLearningMemory(AgentContext context, AiDiagnosis diagnosis, MistakeCard mistakeCard) {
        if (context == null || diagnosis == null) {
            return;
        }
        Long diagnosisSourceId = diagnosis.getId() == null ? context.getSubmissionId() : diagnosis.getId();
        RagDocument diagnosisDocument = upsertDocument(
                RagSourceTypeEnum.AI_DIAGNOSIS,
                diagnosisSourceId,
                context.getUserId(),
                context.getProblemId(),
                "AI 诊断：" + safeTitle(context),
                diagnosis.getKnowledgePoint(),
                diagnosis.getErrorType(),
                diagnosis.getKnowledgePoint());
        insertChunk(diagnosisDocument, 0,
                joinNonBlank("\n", diagnosis.getSpecificError(), diagnosis.getDiagnosis(), diagnosis.getSuggestion()));

        if (mistakeCard == null || mistakeCard.getId() == null) {
            return;
        }
        RagDocument mistakeDocument = upsertDocument(
                RagSourceTypeEnum.MISTAKE_CARD,
                mistakeCard.getId(),
                context.getUserId(),
                context.getProblemId(),
                "错题卡：" + safeTitle(context),
                mistakeCard.getKnowledgePoint(),
                mistakeCard.getErrorType(),
                mistakeCard.getKnowledgePoint());
        insertChunk(mistakeDocument, 0,
                joinNonBlank("\n", mistakeCard.getMistakeSummary(), mistakeCard.getCorrectIdea()));
    }

    @Override
    @Transactional
    public void rebuildSystemIndex() {
        deleteSystemVectorChunks();
        ragChunkMapper.delete(new LambdaQueryWrapper<RagChunk>().isNull(RagChunk::getUserId));
        ragDocumentMapper.delete(new LambdaQueryWrapper<RagDocument>().isNull(RagDocument::getUserId));

        List<Problem> problems = problemMapper.selectList(new LambdaQueryWrapper<Problem>()
                .eq(Problem::getEnabled, true)
                .orderByAsc(Problem::getId));
        for (Problem problem : problems) {
            indexProblem(problem);
        }

        List<KnowledgeCard> cards = knowledgeCardMapper.selectList(new LambdaQueryWrapper<KnowledgeCard>()
                .eq(KnowledgeCard::getEnabled, true)
                .orderByAsc(KnowledgeCard::getSortOrder)
                .orderByAsc(KnowledgeCard::getId));
        for (KnowledgeCard card : cards) {
            indexKnowledgeCard(card);
        }
    }

    private void deleteSystemVectorChunks() {
        if (!vectorEnabled()) {
            return;
        }
        try {
            ragVectorStore.deleteSystemChunks();
        } catch (RuntimeException ex) {
            log.warn("Failed to delete system vector chunks; continue rebuilding MySQL RAG index: {}",
                    ex.getMessage());
        }
    }

    private RagDocument upsertDocument(RagSourceTypeEnum sourceType, Long sourceId, Long userId,
            Long problemId, String title, String knowledgePoint, String errorType, String tags) {
        LocalDateTime now = LocalDateTime.now();
        RagDocument document = ragDocumentMapper.selectOne(sourceQuery(sourceType, sourceId, userId));
        if (document == null) {
            document = new RagDocument();
            document.setSourceType(sourceType.name());
            document.setSourceId(sourceId);
            document.setUserId(userId);
            document.setCreatedAt(now);
        }
        document.setProblemId(problemId);
        document.setTitle(StringUtils.hasText(title) ? title : sourceType.name() + "#" + sourceId);
        document.setKnowledgePoint(knowledgePoint);
        document.setErrorType(errorType);
        document.setTags(tags);
        document.setStatus(ACTIVE);
        document.setUpdatedAt(now);
        if (document.getId() == null) {
            ragDocumentMapper.insert(document);
        } else {
            ragDocumentMapper.updateById(document);
            deleteDocumentVectorChunks(document.getId());
            ragChunkMapper.delete(new LambdaQueryWrapper<RagChunk>()
                    .eq(RagChunk::getDocumentId, document.getId()));
        }
        return document;
    }

    private LambdaQueryWrapper<RagDocument> sourceQuery(RagSourceTypeEnum sourceType, Long sourceId, Long userId) {
        LambdaQueryWrapper<RagDocument> query = new LambdaQueryWrapper<RagDocument>()
                .eq(RagDocument::getSourceType, sourceType.name())
                .eq(RagDocument::getSourceId, sourceId);
        if (userId == null) {
            query.isNull(RagDocument::getUserId);
        } else {
            query.eq(RagDocument::getUserId, userId);
        }
        return query.last("LIMIT 1");
    }

    private int insertChunk(RagDocument document, int chunkIndex, String text) {
        if (document == null || !StringUtils.hasText(text)) {
            return chunkIndex;
        }
        RagChunk chunk = new RagChunk();
        chunk.setDocumentId(document.getId());
        chunk.setSourceType(document.getSourceType());
        chunk.setSourceId(document.getSourceId());
        chunk.setUserId(document.getUserId());
        chunk.setProblemId(document.getProblemId());
        chunk.setChunkIndex(chunkIndex);
        chunk.setChunkText(text.trim());
        chunk.setKnowledgePoint(document.getKnowledgePoint());
        chunk.setErrorType(document.getErrorType());
        chunk.setTags(document.getTags());
        chunk.setCreatedAt(LocalDateTime.now());
        ragChunkMapper.insert(chunk);
        indexVectorChunk(chunk);
        return chunkIndex + 1;
    }

    private void indexVectorChunk(RagChunk chunk) {
        if (!vectorEnabled() || chunk == null || chunk.getId() == null || !StringUtils.hasText(chunk.getChunkText())) {
            return;
        }
        try {
            float[] vector = embeddingClient.embed(chunk.getChunkText());
            ragVectorStore.upsertChunk(chunk, vector);
            chunk.setVectorPointId(String.valueOf(chunk.getId()));
            chunk.setEmbeddingModel(embeddingModel());
            chunk.setEmbeddingDim(vector.length);
            chunk.setVectorStatus(VECTOR_INDEXED);
            ragChunkMapper.updateById(chunk);
        } catch (RuntimeException ex) {
            chunk.setEmbeddingModel(embeddingModel());
            chunk.setEmbeddingDim(embeddingDimensions());
            chunk.setVectorStatus(VECTOR_FAILED);
            ragChunkMapper.updateById(chunk);
            log.warn("Vector RAG indexing failed for rag_chunk#{}; keep MySQL chunk available: {}",
                    chunk.getId(), ex.getMessage());
        }
    }

    private void deleteDocumentVectorChunks(Long documentId) {
        if (!vectorEnabled() || documentId == null) {
            return;
        }
        try {
            ragVectorStore.deleteDocumentChunks(documentId);
        } catch (RuntimeException ex) {
            log.warn("Failed to delete old vector chunks for rag_document#{}; continue MySQL index update: {}",
                    documentId, ex.getMessage());
        }
    }

    private Map<Long, RagDocument> loadDocuments(List<RagChunk> chunks) {
        List<Long> documentIds = chunks.stream()
                .map(RagChunk::getDocumentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (documentIds.isEmpty()) {
            return Map.of();
        }
        List<RagDocument> documents = ragDocumentMapper.selectBatchIds(documentIds);
        if (documents == null) {
            return Map.of();
        }
        return documents.stream()
                .filter(document -> document.getId() != null)
                .collect(Collectors.toMap(RagDocument::getId, Function.identity(), (left, right) -> left));
    }

    private boolean isAllowedForUser(Long queryUserId, RagChunk chunk) {
        if (chunk.getUserId() == null) {
            return true;
        }
        return queryUserId != null && queryUserId.equals(chunk.getUserId());
    }

    private boolean isActiveDocument(RagDocument document) {
        return document == null || !StringUtils.hasText(document.getStatus()) || ACTIVE.equals(document.getStatus());
    }

    private RagChunkHit toHit(RagRetrieveQuery query, RagChunk chunk, RagDocument document, int score) {
        RagChunkHit hit = new RagChunkHit();
        hit.setChunkId(chunk.getId());
        hit.setDocumentId(chunk.getDocumentId());
        hit.setSourceType(chunk.getSourceType());
        hit.setSourceId(chunk.getSourceId());
        hit.setUserId(chunk.getUserId());
        hit.setProblemId(chunk.getProblemId());
        hit.setTitle(document == null ? null : document.getTitle());
        hit.setKnowledgePoint(chunk.getKnowledgePoint());
        hit.setErrorType(chunk.getErrorType());
        hit.setChunkText(chunk.getChunkText());
        hit.setScore(score);
        hit.setMatchReason(matchReason(query, chunk, document));
        return hit;
    }

    private String matchReason(RagRetrieveQuery query, RagChunk chunk, RagDocument document) {
        List<String> reasons = new ArrayList<>();
        if (query.getUserId() != null && query.getUserId().equals(chunk.getUserId())) {
            reasons.add("当前用户记忆");
        }
        if (query.getProblemId() != null && query.getProblemId().equals(chunk.getProblemId())) {
            reasons.add("同题目");
        }
        if (sameText(query.getKnowledgePoint(), chunk.getKnowledgePoint())) {
            reasons.add("同知识点");
        }
        if (sameText(query.getErrorType(), chunk.getErrorType())) {
            reasons.add("同错误类型");
        }
        List<String> keywords = queryKeywords(query);
        if (keywordHit(keywords, joinNonBlank(" ", document == null ? null : document.getTitle(),
                chunk.getTags(), chunk.getKnowledgePoint(), chunk.getChunkText()))) {
            reasons.add("关键词命中");
        }
        if (reasons.isEmpty()) {
            reasons.add("候选召回");
        }
        return String.join("、", reasons);
    }

    private int score(RagRetrieveQuery query, RagChunk chunk, RagDocument document) {
        int score = 0;
        if (query.getUserId() != null && query.getUserId().equals(chunk.getUserId())) {
            score += 60;
        }
        if (query.getProblemId() != null && query.getProblemId().equals(chunk.getProblemId())) {
            score += 50;
        }
        if (sameText(query.getKnowledgePoint(), chunk.getKnowledgePoint())) {
            score += 40;
        }
        if (sameText(query.getErrorType(), chunk.getErrorType())) {
            score += 30;
        }
        if (keywordHit(queryKeywords(query), joinNonBlank(" ", document == null ? null : document.getTitle(),
                chunk.getTags(), chunk.getKnowledgePoint()))) {
            score += 20;
        }
        if (keywordHit(queryKeywords(query), chunk.getChunkText())) {
            score += 10;
        }
        return score;
    }

    private boolean sameText(String left, String right) {
        return StringUtils.hasText(left)
                && StringUtils.hasText(right)
                && normalize(left).equals(normalize(right));
    }

    private boolean keywordHit(List<String> keywords, String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String normalizedText = normalize(text);
        return keywords.stream()
                .filter(StringUtils::hasText)
                .map(this::normalize)
                .anyMatch(normalizedText::contains);
    }

    private List<String> queryKeywords(RagRetrieveQuery query) {
        Set<String> keywords = new LinkedHashSet<>();
        addKeyword(keywords, query.getProblemTitle());
        addKeyword(keywords, query.getProblemCategory());
        addKeyword(keywords, query.getKnowledgePoint());
        addKeyword(keywords, query.getErrorType());
        addKeyword(keywords, query.getExecutionStatus());
        addKeyword(keywords, query.getErrorMessage());
        if (query.getKeywords() != null) {
            query.getKeywords().forEach(keyword -> addKeyword(keywords, keyword));
        }
        return keywords.stream().limit(20).toList();
    }

    private List<String> buildKeywords(AgentContext context) {
        Set<String> keywords = new LinkedHashSet<>();
        if (context.getProblem() != null) {
            addKeyword(keywords, context.getProblem().getTitle());
            addKeyword(keywords, context.getProblem().getCategory());
        }
        if (context.getKnowledgePoints() != null) {
            context.getKnowledgePoints().forEach(keyword -> addKeyword(keywords, keyword));
        }
        if (context.getObservation() != null) {
            addKeyword(keywords, context.getObservation().getStatus());
            addKeyword(keywords, context.getObservation().getErrorMessage());
        }
        return new ArrayList<>(keywords);
    }

    private List<String> buildQuestionKeywords(String question) {
        Set<String> keywords = new LinkedHashSet<>();
        addKeyword(keywords, question);
        return new ArrayList<>(keywords);
    }

    private void addKeyword(Set<String> keywords, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String trimmed = value.trim();
        keywords.add(trimmed);
        for (String token : trimmed.split("[\\s,，;；:：()\\[\\]{}]+")) {
            if (StringUtils.hasText(token) && token.trim().length() >= 2) {
                keywords.add(token.trim());
            }
        }
    }

    private String joinNonBlank(String delimiter, String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                parts.add(value.trim());
            }
        }
        return String.join(delimiter, parts);
    }

    private String safeTitle(AgentContext context) {
        if (context.getProblem() != null && StringUtils.hasText(context.getProblem().getTitle())) {
            return context.getProblem().getTitle();
        }
        return "problem#" + context.getProblemId();
    }

    private boolean vectorEnabled() {
        return ragVectorProperties != null && ragVectorProperties.isEnabled()
                && embeddingClient != null
                && ragVectorStore != null;
    }

    private int vectorCandidateLimit() {
        if (ragVectorProperties == null || ragVectorProperties.getVectorCandidateLimit() <= 0) {
            return 30;
        }
        return ragVectorProperties.getVectorCandidateLimit();
    }

    private double vectorWeight() {
        if (ragVectorProperties == null || ragVectorProperties.getHybridVectorWeight() == null) {
            return 0.60;
        }
        return ragVectorProperties.getHybridVectorWeight().doubleValue();
    }

    private double ruleWeight() {
        if (ragVectorProperties == null || ragVectorProperties.getHybridRuleWeight() == null) {
            return 0.40;
        }
        return ragVectorProperties.getHybridRuleWeight().doubleValue();
    }

    private String embeddingModel() {
        return embeddingProperties == null ? null : embeddingProperties.getModel();
    }

    private Integer embeddingDimensions() {
        return embeddingProperties == null || embeddingProperties.getDimensions() <= 0
                ? null
                : embeddingProperties.getDimensions();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
