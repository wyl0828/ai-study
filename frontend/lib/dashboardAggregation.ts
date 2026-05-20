import type { MistakeCard, UserWeakness } from "./types";

export interface CanonicalKnowledgePoint {
  key: string;
  name: string;
}

interface KnowledgePointAlias extends CanonicalKnowledgePoint {
  keywords: string[];
}

export interface AggregatedWeakness {
  id: number;
  canonicalKey: string;
  canonicalName: string;
  knowledgePoint: string;
  errorType: string;
  wrongCount: number;
  weaknessScore: number;
  trendLabel?: string | null;
  lastDeltaScore?: number | null;
  lastEventAt?: string | null;
  sourceCount: number;
  rawRecords: UserWeakness[];
}

export interface AggregatedMistakeRecord {
  id: number;
  summary: string;
  correctIdea: string;
  lastSeenAt?: string | null;
}

export interface AggregatedMistakeCard {
  id: number;
  groupKey: string;
  problemKey: string;
  problemId?: number | null;
  problemTitle: string;
  errorType: string;
  canonicalKey: string;
  canonicalName: string;
  knowledgePoint: string;
  reviewPoint: string;
  typicalProblems: string[];
  recommendedReview: string;
  nextTrainingAdvice: string;
  totalOccurrences: number;
  sourceCount: number;
  rawRecords: AggregatedMistakeRecord[];
  lastSeenAt?: string | null;
  status?: string | null;
}

const KNOWLEDGE_POINT_ALIASES: KnowledgePointAlias[] = [
  {
    key: "HASHMAP_USAGE",
    name: "HashMap 使用逻辑",
    keywords: [
      "hashmap",
      "hash map",
      "哈希表",
      "哈希",
      "两数之和",
      "key 判断",
      "key判断",
      "查询顺序",
      "put 顺序",
      "put顺序",
      "containskey",
    ],
  },
  {
    key: "LINKED_LIST_REVERSE",
    name: "链表反转指针操作",
    keywords: [
      "反转链表",
      "链表反转",
      "链表指针",
      "指针更新",
      "指针操作",
      "prev",
      "cur",
      "next",
      "return prev",
      "返回 prev",
      "新头节点",
    ],
  },
  {
    key: "GREEDY_STATE",
    name: "贪心维护最优值",
    keywords: ["贪心", "股票", "最大利润", "最小价格", "最优值"],
  },
  {
    key: "TREE_TRAVERSAL",
    name: "二叉树遍历逻辑",
    keywords: ["二叉树", "层序遍历", "dfs", "bfs", "递归", "遍历"],
  },
  {
    key: "DP_STATE",
    name: "动态规划状态转移",
    keywords: ["动态规划", "dp", "状态转移", "爬楼梯", "打家劫舍"],
  },
];

const ERROR_TYPE_NAMES: Record<string, string> = {
  LOGIC_ERROR: "逻辑错误",
  BOUNDARY_ERROR: "边界条件错误",
  SYNTAX_ERROR: "语法错误",
  ALGORITHM_ERROR: "算法思路错误",
  TIMEOUT: "超时",
  RUNTIME_ERROR: "运行时错误",
  SYSTEM_ERROR: "系统错误",
  ACCEPTED_REVIEW: "通过代码点评",
};

export function normalizeKnowledgePoint(inputText: string): CanonicalKnowledgePoint {
  const source = inputText.trim();
  const normalized = source.toLowerCase();
  const match = KNOWLEDGE_POINT_ALIASES.find((alias) =>
    alias.keywords.some((keyword) => normalized.includes(keyword.toLowerCase()))
  );

  if (match) {
    return { key: match.key, name: match.name };
  }

  const fallback = source || "未分类知识点";
  return { key: `RAW:${fallback}`, name: fallback };
}

export function normalizeErrorType(errorType: string | null | undefined): string {
  if (!errorType) {
    return "未分类错误";
  }
  return ERROR_TYPE_NAMES[errorType] || errorType;
}

export function groupWeaknesses(weaknesses: UserWeakness[]): AggregatedWeakness[] {
  const groups = new Map<string, AggregatedWeakness>();

  weaknesses.forEach((weakness) => {
    const canonical = normalizeKnowledgePointWithFallback(
      joinText(weakness.knowledgePoint, weakness.errorType, weakness.trendLabel),
      weakness.knowledgePoint
    );
    const current: AggregatedWeakness = {
      id: weakness.id,
      canonicalKey: canonical.key,
      canonicalName: canonical.name,
      knowledgePoint: canonical.name,
      errorType: normalizeErrorType(weakness.errorType),
      wrongCount: weakness.wrongCount,
      weaknessScore: roundScore(weakness.weaknessScore),
      trendLabel: weakness.trendLabel,
      lastDeltaScore: weakness.lastDeltaScore,
      lastEventAt: weakness.lastEventAt,
      sourceCount: 1,
      rawRecords: [weakness],
    };

    const existing = groups.get(canonical.key);
    if (!existing) {
      groups.set(canonical.key, current);
      return;
    }

    existing.wrongCount += current.wrongCount;
    existing.sourceCount += 1;
    existing.rawRecords.push(weakness);

    if (current.weaknessScore > existing.weaknessScore) {
      existing.weaknessScore = current.weaknessScore;
      existing.errorType = current.errorType;
      existing.id = current.id;
    }

    if (isAfter(current.lastEventAt, existing.lastEventAt)) {
      existing.trendLabel = current.trendLabel;
      existing.lastDeltaScore = current.lastDeltaScore;
      existing.lastEventAt = current.lastEventAt;
    }
  });

  return Array.from(groups.values()).sort(compareWeaknesses);
}

export function groupMistakeCards(cards: MistakeCard[]): AggregatedMistakeCard[] {
  const groups = new Map<string, AggregatedMistakeCard>();

  cards.forEach((card) => {
    const canonical = normalizeKnowledgePointWithFallback(
      joinText(card.knowledgePoint, card.mistakeSummary, card.correctIdea, card.problemTitle),
      card.knowledgePoint || card.problemTitle
    );
    const normalizedErrorType = normalizeErrorType(card.errorType);
    const problemKey = card.problemId
      ? `problem:${card.problemId}`
      : `title:${card.problemTitle || "未知题目"}`;
    const groupKey = `${problemKey}-${stableErrorType(card.errorType)}-${canonical.key}`;
    const rawRecord = {
      id: card.id,
      summary: compactText(card.mistakeSummary || card.correctIdea || canonical.name),
      correctIdea: compactText(card.correctIdea || card.mistakeSummary || canonical.name),
      lastSeenAt: card.lastSeenAt,
    };

    const current: AggregatedMistakeCard = {
      id: card.id,
      groupKey,
      problemKey,
      problemId: card.problemId,
      problemTitle: card.problemTitle || "未知题目",
      errorType: normalizedErrorType,
      canonicalKey: canonical.key,
      canonicalName: canonical.name,
      knowledgePoint: canonical.name,
      reviewPoint: canonical.name,
      typicalProblems: uniqueCompactTexts(card.mistakeSummary, card.correctIdea).slice(0, 3),
      recommendedReview: recommendedReview(canonical.name),
      nextTrainingAdvice: nextTrainingAdvice(canonical.name),
      totalOccurrences: occurrenceCount(card),
      sourceCount: 1,
      rawRecords: [rawRecord],
      lastSeenAt: card.lastSeenAt,
      status: card.status || "OPEN",
    };

    const existing = groups.get(groupKey);
    if (!existing) {
      groups.set(groupKey, current);
      return;
    }

    existing.totalOccurrences += current.totalOccurrences;
    existing.sourceCount += 1;
    existing.rawRecords.push(rawRecord);
    existing.typicalProblems = uniqueCompactTexts(
      ...existing.typicalProblems,
      ...current.typicalProblems
    ).slice(0, 3);

    if (isAfter(current.lastSeenAt, existing.lastSeenAt)) {
      existing.id = current.id;
      existing.lastSeenAt = current.lastSeenAt;
      existing.status = current.status;
    }
  });

  return Array.from(groups.values()).sort(compareMistakeCards);
}

export function compactText(text: string | null | undefined, max = 42): string {
  const value = (text || "").trim();
  if (value.length <= max) {
    return value;
  }
  return `${value.slice(0, max)}...`;
}

function joinText(...values: Array<string | null | undefined>): string {
  return values.filter(Boolean).join(" ");
}

function normalizeKnowledgePointWithFallback(
  sourceText: string,
  fallbackText: string | null | undefined
): CanonicalKnowledgePoint {
  const canonical = normalizeKnowledgePoint(sourceText);
  if (!canonical.key.startsWith("RAW:")) {
    return canonical;
  }
  return normalizeKnowledgePoint(fallbackText || sourceText);
}

function stableErrorType(errorType: string | null | undefined): string {
  return errorType || "UNKNOWN_ERROR";
}

function occurrenceCount(card: MistakeCard): number {
  return Math.max(card.repeatCount || 1, 1);
}

function uniqueCompactTexts(...values: Array<string | null | undefined>): string[] {
  const seen = new Set<string>();
  const result: string[] = [];

  values.forEach((value) => {
    const compacted = compactText(value);
    if (!compacted || seen.has(compacted)) {
      return;
    }
    seen.add(compacted);
    result.push(compacted);
  });

  return result;
}

function recommendedReview(canonicalName: string): string {
  if (canonicalName.includes("链表反转")) {
    return "三指针反转模板";
  }
  if (canonicalName.includes("HashMap")) {
    return "HashMap 查询与写入顺序";
  }
  if (canonicalName.includes("贪心")) {
    return "最优值维护过程";
  }
  if (canonicalName.includes("二叉树")) {
    return "遍历顺序与递归边界";
  }
  if (canonicalName.includes("动态规划")) {
    return "状态定义和转移方程";
  }
  return canonicalName;
}

function nextTrainingAdvice(canonicalName: string): string {
  if (canonicalName.includes("链表反转")) {
    return "先口述 prev、cur、next 的移动顺序，再重写反转链表。";
  }
  if (canonicalName.includes("HashMap")) {
    return "先判断目标值是否存在，再写入当前元素，避免顺序错误。";
  }
  return `复盘 ${canonicalName} 的失败用例，再做一次同类题。`;
}

function compareWeaknesses(a: AggregatedWeakness, b: AggregatedWeakness): number {
  const scoreDiff = b.weaknessScore - a.weaknessScore;
  if (scoreDiff !== 0) return scoreDiff;
  const wrongDiff = b.wrongCount - a.wrongCount;
  if (wrongDiff !== 0) return wrongDiff;
  return timestamp(b.lastEventAt) - timestamp(a.lastEventAt);
}

function compareMistakeCards(a: AggregatedMistakeCard, b: AggregatedMistakeCard): number {
  const occurrenceDiff = b.totalOccurrences - a.totalOccurrences;
  if (occurrenceDiff !== 0) return occurrenceDiff;
  return timestamp(b.lastSeenAt) - timestamp(a.lastSeenAt);
}

function isAfter(candidate?: string | null, current?: string | null): boolean {
  return timestamp(candidate) > timestamp(current);
}

function timestamp(value?: string | null): number {
  if (!value) {
    return 0;
  }
  const time = new Date(value).getTime();
  return Number.isNaN(time) ? 0 : time;
}

function roundScore(score: number): number {
  return Math.round(score * 10) / 10;
}
