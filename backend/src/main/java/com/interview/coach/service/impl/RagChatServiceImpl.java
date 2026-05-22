package com.interview.coach.service.impl;

import com.interview.coach.dto.RagChatAiResponse;
import com.interview.coach.dto.RagChunkHit;
import com.interview.coach.dto.RagRetrieveResult;
import com.interview.coach.handler.BusinessException;
import com.interview.coach.integration.ai.AnthropicCompatibleClient;
import com.interview.coach.service.RagChatService;
import com.interview.coach.service.RagService;
import com.interview.coach.service.UserLearningService;
import com.interview.coach.vo.MistakeCardVO;
import com.interview.coach.vo.RagChatResponseVO;
import com.interview.coach.vo.RagChatSourceVO;
import com.interview.coach.vo.UserWeaknessEventVO;
import com.interview.coach.vo.UserWeaknessVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RagChatServiceImpl implements RagChatService {

    private static final int CHAT_RAG_LIMIT = 5;

    private static final int MIN_RELEVANCE_SCORE = 10;

    private static final String INSUFFICIENT_MATERIAL =
            "我没有在当前知识库中找到足够相关的资料，建议换个更具体的问题。";

    private static final String NO_LEARNING_MEMORY =
            "我当前还没有找到你的历史错题卡或薄弱点记录，所以暂时无法判断最近错题集中在哪些知识点。你可以先完成一次错误提交，系统生成 AI 诊断和错题卡后，我就能基于这些记录总结。";

    private static final String OFF_TOPIC =
            "这个问题不在当前知识库问答范围内。我只能围绕题目、知识卡、历史诊断、错题卡和你的学习记录做复习问答。";

    private static final String FULL_CODE_REFUSAL =
            "我不能直接给完整 AC 代码，但可以帮你梳理思路、伪代码和易错点。建议先明确状态定义 / 遍历顺序 / 边界条件，再自己提交一次，系统会根据你的代码做诊断。";

    private final RagService ragService;

    private final AnthropicCompatibleClient aiClient;

    private final UserLearningService userLearningService;

    @Override
    public RagChatResponseVO ask(Long userId, String question) {
        if (userId == null) {
            throw new BusinessException("userId is required");
        }
        if (!StringUtils.hasText(question)) {
            throw new BusinessException("question is required");
        }

        String normalizedQuestion = question.trim();
        if (detectFullCodeRequest(normalizedQuestion)) {
            return response(FULL_CODE_REFUSAL, List.of());
        }
        if (detectOffTopicQuestion(normalizedQuestion)) {
            return response(OFF_TOPIC, List.of());
        }
        if (detectLearningMemoryQuestion(normalizedQuestion)) {
            return answerLearningMemory(userId);
        }

        RagRetrieveResult result = ragService.retrieveForChat(userId, normalizedQuestion, CHAT_RAG_LIMIT);
        if (!hasRelevantHits(result)) {
            return response(INSUFFICIENT_MATERIAL, List.of());
        }

        List<RankedHit> rankedHits = rerankForChat(normalizedQuestion, result.getHits());
        List<RagChatSourceVO> sources = rankedHits.stream()
                .map(this::toSource)
                .toList();
        try {
            RagChatAiResponse aiResponse = aiClient.askJson(systemPrompt(), userPrompt(normalizedQuestion, rankedHits),
                    RagChatAiResponse.class);
            String answer = aiResponse == null || !StringUtils.hasText(aiResponse.getAnswer())
                    ? INSUFFICIENT_MATERIAL
                    : aiResponse.getAnswer().trim();
            return response(answer, sources);
        } catch (RuntimeException ex) {
            return response("知识库问答暂时无法调用 AI，请检查 AI_BASE_URL、AI_API_KEY 和 AI_MODEL 配置后重试。", sources);
        }
    }

    private RagChatResponseVO answerLearningMemory(Long userId) {
        List<UserWeaknessVO> weaknesses = userLearningService.getWeaknesses(userId);
        List<MistakeCardVO> mistakes = userLearningService.getMistakes(userId);
        List<UserWeaknessEventVO> events = userLearningService.getRecentWeaknessEvents(userId, 5);
        if ((weaknesses == null || weaknesses.isEmpty()) && (mistakes == null || mistakes.isEmpty())) {
            return response(NO_LEARNING_MEMORY, List.of());
        }

        List<TopicSummary> top = buildTopicSummaries(weaknesses, mistakes, events).stream()
                .limit(3)
                .toList();
        return response(buildLearningAnswer(top), buildLearningSources(top, mistakes, events));
    }

    private boolean hasRelevantHits(RagRetrieveResult result) {
        return result != null
                && result.hasHits()
                && result.getHits().stream().anyMatch(hit -> hit.getScore() >= MIN_RELEVANCE_SCORE);
    }

    private RagChatResponseVO response(String answer, List<RagChatSourceVO> sources) {
        RagChatResponseVO vo = new RagChatResponseVO();
        vo.setAnswer(answer);
        vo.setSources(sources);
        return vo;
    }

    private List<TopicSummary> buildTopicSummaries(List<UserWeaknessVO> weaknesses,
            List<MistakeCardVO> mistakes, List<UserWeaknessEventVO> events) {
        Map<String, TopicSummary> summaries = new LinkedHashMap<>();
        if (weaknesses != null) {
            weaknesses.stream()
                    .sorted(Comparator.comparing(this::weaknessScore).reversed()
                            .thenComparing(weakness -> safeInt(weakness.getWrongCount()), Comparator.reverseOrder()))
                    .forEach(weakness -> {
                        TopicSummary summary = summaries.computeIfAbsent(safeTopic(weakness.getKnowledgePoint()),
                                TopicSummary::new);
                        summary.weaknessId = weakness.getId();
                        summary.errorType = weakness.getErrorType();
                        summary.wrongCount = safeInt(weakness.getWrongCount());
                        summary.weaknessScore = weaknessScore(weakness);
                        summary.trendLabel = weakness.getTrendLabel();
                    });
        }
        if (mistakes != null) {
            mistakes.stream()
                    .sorted(Comparator.comparing((MistakeCardVO mistake) -> safeInt(mistake.getRepeatCount()))
                            .reversed()
                            .thenComparing(mistake -> safeTime(mistake.getLastSeenAt()), Comparator.reverseOrder()))
                    .forEach(mistake -> {
                        TopicSummary summary = summaries.computeIfAbsent(safeTopic(mistake.getKnowledgePoint()),
                                TopicSummary::new);
                        summary.mistakeId = mistake.getId();
                        summary.problemTitle = mistake.getProblemTitle();
                        summary.repeatCount += Math.max(1, safeInt(mistake.getRepeatCount()));
                        summary.lastSeenAt = latest(summary.lastSeenAt, mistake.getLastSeenAt());
                    });
        }
        if (events != null) {
            events.forEach(event -> {
                TopicSummary summary = summaries.computeIfAbsent(safeTopic(event.getKnowledgePoint()), TopicSummary::new);
                summary.eventId = event.getId();
                summary.lastDeltaScore = event.getDeltaScore();
                summary.eventReason = event.getReason();
            });
        }
        return summaries.values().stream()
                .sorted(Comparator.comparing((TopicSummary summary) -> summary.weaknessScore).reversed()
                        .thenComparing(summary -> summary.wrongCount, Comparator.reverseOrder())
                        .thenComparing(summary -> summary.repeatCount, Comparator.reverseOrder()))
                .toList();
    }

    private String buildLearningAnswer(List<TopicSummary> top) {
        String topics = String.join("、", top.stream().map(summary -> summary.knowledgePoint).toList());
        StringBuilder builder = new StringBuilder();
        builder.append("你最近的错题主要集中在").append(topics).append("这几个知识点。");
        for (int i = 0; i < top.size(); i++) {
            TopicSummary summary = top.get(i);
            if (i == 0) {
                builder.append("其中 ");
            }
            builder.append(summary.knowledgePoint)
                    .append("错误 ").append(summary.wrongCount).append(" 次，薄弱分 ")
                    .append(formatScore(summary.weaknessScore));
            if (StringUtils.hasText(summary.trendLabel)) {
                builder.append("，趋势是").append(summary.trendLabel);
            }
            if (summary.repeatCount > 0) {
                builder.append("，错题卡重复出现 ").append(summary.repeatCount).append(" 次");
            }
            builder.append("，建议").append(reviewSuggestion(summary.knowledgePoint)).append("。");
        }
        builder.append("建议优先复盘")
                .append(String.join("，再练", top.stream().map(summary -> summary.knowledgePoint).toList()))
                .append("。");
        return builder.toString();
    }

    private List<RagChatSourceVO> buildLearningSources(List<TopicSummary> top,
            List<MistakeCardVO> mistakes, List<UserWeaknessEventVO> events) {
        List<RagChatSourceVO> sources = new ArrayList<>();
        for (TopicSummary summary : top) {
            if (summary.weaknessId != null) {
                sources.add(source("USER_WEAKNESS", summary.weaknessId, summary.knowledgePoint,
                        "错误 %d 次｜薄弱分 %s".formatted(summary.wrongCount, formatScore(summary.weaknessScore)),
                        100 + summary.weaknessScore.intValue(), "来自你的薄弱点记录"));
            }
            MistakeCardVO mistake = firstMistakeForTopic(mistakes, summary.knowledgePoint);
            if (mistake != null) {
                sources.add(source("MISTAKE_CARD", mistake.getId(), summary.knowledgePoint,
                        "出现 %d 次｜%s".formatted(Math.max(1, safeInt(mistake.getRepeatCount())),
                                StringUtils.hasText(mistake.getProblemTitle()) ? mistake.getProblemTitle() : "最近仍有重复错误"),
                        90 + Math.max(1, safeInt(mistake.getRepeatCount())), "来自你的错题卡"));
            }
            UserWeaknessEventVO event = firstEventForTopic(events, summary.knowledgePoint);
            if (event != null) {
                sources.add(source("WEAKNESS_EVENT", event.getId(), summary.knowledgePoint,
                        "最近变化 %s｜%s".formatted(formatScore(event.getDeltaScore()), safeReason(event.getReason())),
                        80, "来自你的薄弱点变化记录"));
            }
        }
        return sources.stream().limit(6).toList();
    }

    private RagChatSourceVO toSource(RankedHit rankedHit) {
        RagChunkHit hit = rankedHit.hit;
        RagChatSourceVO source = new RagChatSourceVO();
        source.setSourceType(hit.getSourceType());
        source.setSourceId(hit.getSourceId());
        source.setTitle(hit.getTitle());
        source.setScore(rankedHit.score);
        source.setSnippet(compact(hit.getChunkText(), 120));
        source.setMatchReason(rankedHit.matchReason);
        return source;
    }

    private String systemPrompt() {
        return """
                你是 AI Interview Coach Agent 的知识库问答工具，不是通用聊天助手。
                你只能回答当前项目知识库中与题目、知识卡、AI 诊断、历史错题相关的问题。
                必须只基于检索到的资料回答；如果资料不足，就说明知识库资料不足，不要自由发挥。
                不要输出完整 Java AC 代码。涉及算法题时，只讲思路、易错点、伪代码或面试表达。
                优先基于 sources 回答，不要编造 sources 中没有的数据。
                用中文回答，语气像 Java 后端面试教练，简洁具体。
                返回 JSON：{"answer":"..."}。
                """;
    }

    private String userPrompt(String question, List<RankedHit> rankedHits) {
        return """
                用户问题：
                %s

                检索到的资料：
                %s
                """.formatted(question, toPromptBlock(rankedHits));
    }

    private List<RankedHit> rerankForChat(String question, List<RagChunkHit> hits) {
        if (hits == null) {
            return List.of();
        }
        return hits.stream()
                .map(hit -> rank(question, hit))
                .sorted(Comparator.comparingInt((RankedHit hit) -> hit.score).reversed()
                        .thenComparing(hit -> hit.hit.getChunkId() == null ? Long.MAX_VALUE : hit.hit.getChunkId()))
                .limit(CHAT_RAG_LIMIT)
                .toList();
    }

    private RankedHit rank(String question, RagChunkHit hit) {
        String normalizedQuestion = normalize(question);
        String title = normalize(hit.getTitle());
        String chunk = normalize(hit.getChunkText());
        String knowledgePoint = normalize(hit.getKnowledgePoint());
        String text = String.join(" ", title, chunk, knowledgePoint);
        int boost = 0;
        String reason = "内容相关";

        if (containsTitleAlias(normalizedQuestion, title)) {
            boost += 80;
            reason = "PROBLEM".equals(hit.getSourceType()) ? "题目标题匹配" : "知识卡标题匹配";
        } else if (titleHit(normalizedQuestion, title)) {
            boost += 60;
            reason = "PROBLEM".equals(hit.getSourceType()) ? "题目标题匹配" : "知识卡标题匹配";
        } else if (corePhraseHit(normalizedQuestion, chunk)) {
            boost += 50;
            reason = "核心短语匹配";
        } else if (knowledgeHit(normalizedQuestion, knowledgePoint, text)) {
            boost += 35;
            reason = "知识点匹配";
        } else if (genericCategoryHit(normalizedQuestion, text)) {
            boost += 10;
            reason = "知识点匹配";
        }
        return new RankedHit(hit, hit.getScore() + boost, reason);
    }

    private boolean detectFullCodeRequest(String message) {
        String normalized = normalize(message);
        return containsAny(normalized, "完整代码", "ac代码", "ac 代码", "直接给答案", "java 参考实现", "完整实现",
                "class solution");
    }

    private boolean detectOffTopicQuestion(String message) {
        String normalized = normalize(message);
        return containsAny(normalized, "天气", "新闻", "股票", "旅游", "闲聊", "生活咨询", "基金", "汇率", "电影推荐");
    }

    private boolean detectLearningMemoryQuestion(String message) {
        String normalized = normalize(message);
        return containsAny(normalized, "我的错题", "最近错题", "错题集中", "哪里薄弱", "薄弱知识点", "最近错误",
                "我容易错", "我的学习记录", "我的弱点", "主要集中在哪些知识点");
    }

    private RagChatSourceVO source(String sourceType, Long sourceId, String title, String snippet, int score,
            String matchReason) {
        RagChatSourceVO source = new RagChatSourceVO();
        source.setSourceType(sourceType);
        source.setSourceId(sourceId);
        source.setTitle(title);
        source.setSnippet(snippet);
        source.setScore(score);
        source.setMatchReason(matchReason);
        return source;
    }

    private MistakeCardVO firstMistakeForTopic(List<MistakeCardVO> mistakes, String topic) {
        if (mistakes == null) {
            return null;
        }
        return mistakes.stream()
                .filter(mistake -> safeTopic(mistake.getKnowledgePoint()).equals(topic))
                .findFirst()
                .orElse(null);
    }

    private UserWeaknessEventVO firstEventForTopic(List<UserWeaknessEventVO> events, String topic) {
        if (events == null) {
            return null;
        }
        return events.stream()
                .filter(event -> safeTopic(event.getKnowledgePoint()).equals(topic))
                .findFirst()
                .orElse(null);
    }

    private String toPromptBlock(List<RankedHit> rankedHits) {
        if (rankedHits == null || rankedHits.isEmpty()) {
            return "没有检索到可用证据。";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < rankedHits.size(); i++) {
            RagChunkHit hit = rankedHits.get(i).hit;
            builder.append("%d. [%s#%s score=%d reason=%s] %s".formatted(
                            i + 1,
                            hit.getSourceType(),
                            hit.getSourceId(),
                            rankedHits.get(i).score,
                            rankedHits.get(i).matchReason,
                            compact(hit.getChunkText(), 180)))
                    .append("\n");
        }
        return builder.toString();
    }

    private String compact(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }

    private BigDecimal weaknessScore(UserWeaknessVO weakness) {
        return weakness.getWeaknessScore() == null ? BigDecimal.ZERO : weakness.getWeaknessScore();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private LocalDateTime safeTime(LocalDateTime value) {
        return value == null ? LocalDateTime.MIN : value;
    }

    private LocalDateTime latest(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    private String safeTopic(String value) {
        return StringUtils.hasText(value) ? value.trim() : "未分类知识点";
    }

    private String safeReason(String value) {
        return StringUtils.hasText(value) ? value.trim() : "最近仍有变化";
    }

    private String formatScore(BigDecimal score) {
        if (score == null) {
            return "0";
        }
        BigDecimal stripped = score.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0).toPlainString() : stripped.toPlainString();
    }

    private String reviewSuggestion(String topic) {
        String normalized = normalize(topic);
        if (normalized.contains("hashmap") || normalized.contains("哈希")) {
            return "复盘 HashMap 查询 / 写入顺序、key 是否重复和边界条件";
        }
        if (normalized.contains("链表") || normalized.contains("linked")) {
            return "复盘指针移动顺序、返回值和循环终止条件";
        }
        if (normalized.contains("贪心") || normalized.contains("股票")) {
            return "复盘状态维护、最优值更新时机和初始值";
        }
        return "结合最近错题重新讲一遍解题思路和易错点";
    }

    private boolean containsTitleAlias(String question, String title) {
        return (containsAny(question, "two sum", "两数之和") && title.contains("两数之和"))
                || (containsAny(question, "spring bean 生命周期", "bean 生命周期") && title.contains("spring bean 生命周期"));
    }

    private boolean titleHit(String question, String title) {
        return StringUtils.hasText(title)
                && (question.contains(title) || titleTokens(title).stream().allMatch(question::contains));
    }

    private boolean corePhraseHit(String question, String chunk) {
        if (!StringUtils.hasText(chunk)) {
            return false;
        }
        if (containsAny(question, "查询", "写入", "顺序")
                && containsAny(chunk, "查询", "写入", "complement", "target")) {
            return true;
        }
        return containsAny(question, "生命周期") && chunk.contains("生命周期");
    }

    private boolean knowledgeHit(String question, String knowledgePoint, String text) {
        if (StringUtils.hasText(knowledgePoint) && (question.contains(knowledgePoint) || knowledgePoint.contains(question))) {
            return true;
        }
        return containsAny(question, "hashmap", "哈希") && text.contains("hashmap")
                || containsAny(question, "spring", "bean") && text.contains("spring") && text.contains("bean");
    }

    private boolean genericCategoryHit(String question, String text) {
        return containsAny(question, "hashmap", "哈希") && text.contains("hashmap")
                || question.contains("spring") && text.contains("spring");
    }

    private List<String> titleTokens(String title) {
        return List.of(title.split("[\\s,，;；:：()（）/]+")).stream()
                .filter(StringUtils::hasText)
                .filter(token -> token.length() >= 2)
                .toList();
    }

    private boolean containsAny(String source, String... values) {
        for (String value : values) {
            if (source.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private static class RankedHit {

        private final RagChunkHit hit;

        private final int score;

        private final String matchReason;

        private RankedHit(RagChunkHit hit, int score, String matchReason) {
            this.hit = hit;
            this.score = score;
            this.matchReason = matchReason;
        }
    }

    private static class TopicSummary {

        private final String knowledgePoint;

        private Long weaknessId;

        private Long mistakeId;

        private Long eventId;

        private String errorType;

        private String trendLabel;

        private String problemTitle;

        private String eventReason;

        private int wrongCount;

        private int repeatCount;

        private BigDecimal weaknessScore = BigDecimal.ZERO;

        private BigDecimal lastDeltaScore;

        private LocalDateTime lastSeenAt;

        private TopicSummary(String knowledgePoint) {
            this.knowledgePoint = knowledgePoint;
        }
    }
}
