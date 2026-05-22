package com.interview.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.AgentExecutionObservation;
import com.interview.coach.dto.RagChunkHit;
import com.interview.coach.dto.RagRetrieveQuery;
import com.interview.coach.dto.RagRetrieveResult;
import com.interview.coach.entity.AiDiagnosis;
import com.interview.coach.entity.KnowledgeCard;
import com.interview.coach.entity.MistakeCard;
import com.interview.coach.entity.Problem;
import com.interview.coach.entity.RagChunk;
import com.interview.coach.entity.RagDocument;
import com.interview.coach.enums.RagSourceTypeEnum;
import com.interview.coach.mapper.KnowledgeCardMapper;
import com.interview.coach.mapper.ProblemMapper;
import com.interview.coach.mapper.RagChunkMapper;
import com.interview.coach.mapper.RagDocumentMapper;
import com.interview.coach.service.RagService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
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

    private static final int MAX_CANDIDATES = 200;

    private final RagDocumentMapper ragDocumentMapper;

    private final RagChunkMapper ragChunkMapper;

    private final ProblemMapper problemMapper;

    private final KnowledgeCardMapper knowledgeCardMapper;

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
        List<RagChunk> chunks = ragChunkMapper.selectList(new LambdaQueryWrapper<RagChunk>()
                .and(wrapper -> {
                    if (safeQuery.getUserId() == null) {
                        wrapper.isNull(RagChunk::getUserId);
                    } else {
                        wrapper.isNull(RagChunk::getUserId)
                                .or()
                                .eq(RagChunk::getUserId, safeQuery.getUserId());
                    }
                })
                .last("LIMIT " + MAX_CANDIDATES));
        if (chunks == null || chunks.isEmpty()) {
            return new RagRetrieveResult();
        }

        Map<Long, RagDocument> documents = loadDocuments(chunks);
        List<RagChunkHit> hits = chunks.stream()
                .filter(chunk -> isAllowedForUser(safeQuery.getUserId(), chunk))
                .filter(chunk -> isActiveDocument(documents.get(chunk.getDocumentId())))
                .map(chunk -> toHit(chunk, documents.get(chunk.getDocumentId()), score(safeQuery, chunk, documents.get(chunk.getDocumentId()))))
                .sorted(Comparator.comparingInt(RagChunkHit::getScore).reversed()
                        .thenComparing(hit -> hit.getChunkId() == null ? Long.MAX_VALUE : hit.getChunkId()))
                .limit(limit)
                .toList();

        RagRetrieveResult result = new RagRetrieveResult();
        result.setHits(hits);
        return result;
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
        return chunkIndex + 1;
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

    private RagChunkHit toHit(RagChunk chunk, RagDocument document, int score) {
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
        return hit;
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

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
