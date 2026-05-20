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
  userFacingErrorTag: string;
  canonicalKey: string;
  canonicalName: string;
  knowledgePoint: string;
  patternTitle: string;
  rootCause: string;
  fixAction: string;
  reviewScript: string;
  reviewPoint: string;
  typicalProblems: string[];
  recommendedReview: string;
  nextTrainingAdvice: string;
  totalOccurrences: number;
  sourceCount: number;
  rawRecords: AggregatedMistakeRecord[];
  lastSeenAt?: string | null;
  recentLabel: string;
  status?: string | null;
}

interface MistakePattern {
  userFacingErrorTag: string;
  conclusion: string;
  rootCause: string;
  fixAction: string;
  reviewScript: string;
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
    const pattern = deriveMistakePattern(card, canonical);
    if (!pattern) {
      return;
    }
    const problemKey = card.problemId
      ? `problem:${card.problemId}`
      : `title:${card.problemTitle || "未知题目"}`;
    const groupKey = `${problemKey}-${canonical.key}-${pattern.userFacingErrorTag}`;
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
      userFacingErrorTag: pattern.userFacingErrorTag,
      canonicalKey: canonical.key,
      canonicalName: canonical.name,
      knowledgePoint: canonical.name,
      patternTitle: buildPatternTitle(card.problemTitle || "未知题目", pattern.conclusion),
      rootCause: pattern.rootCause,
      fixAction: pattern.fixAction,
      reviewScript: pattern.reviewScript,
      reviewPoint: pattern.conclusion,
      typicalProblems: uniqueCompactTexts(card.mistakeSummary, card.correctIdea).slice(0, 3),
      recommendedReview: recommendedReview(canonical.name),
      nextTrainingAdvice: nextTrainingAdvice(canonical.name),
      totalOccurrences: occurrenceCount(card),
      sourceCount: 1,
      rawRecords: [rawRecord],
      lastSeenAt: card.lastSeenAt,
      recentLabel: formatRecentTime(card.lastSeenAt),
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
      existing.recentLabel = current.recentLabel;
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

function occurrenceCount(card: MistakeCard): number {
  return Math.max(card.repeatCount || 1, 1);
}

function deriveMistakePattern(
  card: MistakeCard,
  canonical: CanonicalKnowledgePoint
): MistakePattern | null {
  const sourceText = joinText(
    card.problemTitle,
    card.knowledgePoint,
    card.mistakeSummary,
    card.correctIdea,
    card.errorType
  );
  const normalized = sourceText.toLowerCase();

  if (card.errorType === "SYSTEM_ERROR" && !matchesKnownAlgorithmPattern(normalized)) {
    return null;
  }

  if (
    canonical.key === "LINKED_LIST_REVERSE" &&
    hasAny(normalized, [
      "return head",
      "return prev",
      "返回 head",
      "返回head",
      "返回 prev",
      "返回prev",
      "旧头",
      "新头节点",
    ])
  ) {
    return {
      userFacingErrorTag: "返回值错误",
      conclusion: "返回了旧 head，而不是新头 prev",
      rootCause: "链表已经完成反转，但返回值仍指向旧 head；原 head 反转后是尾节点，所以结果会丢失后续节点。",
      fixAction: "循环结束后返回 prev，并在收尾前确认 prev 才是反转后的新头节点。",
      reviewScript: "链表反转完成后，新头节点一定是最后停留的 prev，不是原来的 head。",
    };
  }

  if (
    canonical.key === "LINKED_LIST_REVERSE" &&
    hasAny(normalized, ["prev", "cur", "next", "指针", "移动顺序", "pointer"])
  ) {
    return {
      userFacingErrorTag: "指针移动顺序错误",
      conclusion: "指针移动顺序混乱",
      rootCause: "反转链表时如果没有先保存 next，或者移动 prev / cur 的顺序不稳，链表连接会被提前改掉。",
      fixAction: "每轮固定三步：保存 next，再反转 cur.next，最后移动 prev 和 cur。",
      reviewScript: "链表题先保护后继节点，再改当前指针，最后整体向前推进。",
    };
  }

  if (
    canonical.key === "HASHMAP_USAGE" &&
    hasAny(normalized, ["put", "containskey", "查询顺序", "自匹配", "self pairing", "写入顺序"])
  ) {
    return {
      userFacingErrorTag: "HashMap 查询顺序错误",
      conclusion: "HashMap 查询顺序写反",
      rootCause: "遍历时先写入当前元素，会让当前元素参与本轮匹配，重复值或目标值为两倍当前值时容易自匹配。",
      fixAction: "先查 complement 是否存在，确认失败后再把当前值和下标写入 HashMap。",
      reviewScript: "两数之和先问之前有没有需要的数，再把当前数留给后面的元素匹配。",
    };
  }

  if (
    canonical.key === "GREEDY_STATE" &&
    hasAny(normalized, ["最低价", "最小价格", "最大利润", "状态", "更新时机", "minprice", "profit"])
  ) {
    return {
      userFacingErrorTag: "状态维护错误",
      conclusion: "最低价更新时机混乱",
      rootCause: "最大利润依赖当前位置之前的最低买入价；如果最低价和利润更新顺序混乱，状态就不能表达真实最优选择。",
      fixAction: "遍历价格时持续维护历史最低价，再用当前价格减最低价更新最大利润。",
      reviewScript: "股票题每一步只维护两个状态：到今天为止的最低价，以及当前能得到的最大利润。",
    };
  }

  if (
    canonical.key === "TREE_TRAVERSAL" &&
    hasAny(normalized, ["层序", "队列", "层边界", "bfs", "size", "level"])
  ) {
    return {
      userFacingErrorTag: "BFS 层边界错误",
      conclusion: "BFS 层边界没固定",
      rootCause: "层序遍历需要先固定当前层节点数；如果边遍历边使用变化中的队列长度，下一层节点会污染当前层。",
      fixAction: "每轮先记录 queue.size() 作为当前层长度，只循环处理这一批节点。",
      reviewScript: "BFS 分层时，进入一层前先拍下 size，这个 size 就是当前层边界。",
    };
  }

  const fallbackTitle = compactText(card.mistakeSummary || card.correctIdea || canonical.name, 18);
  return {
    userFacingErrorTag: canonical.name,
    conclusion: fallbackTitle || canonical.name,
    rootCause: compactText(card.mistakeSummary || `${canonical.name} 的理解或实现细节还不稳定。`, 90),
    fixAction: compactText(card.correctIdea || `先复盘 ${canonical.name} 的失败用例，再重写关键步骤。`, 90),
    reviewScript: `${canonical.name} 复盘时要先说清楚错误触发条件，再说明修正后的关键不变量。`,
  };
}

function buildPatternTitle(problemTitle: string, conclusion: string): string {
  return `${displayProblemTitle(problemTitle)}｜${conclusion}`;
}

export function formatRecentTime(value?: string | null): string {
  if (!value) {
    return "暂无最近记录";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "最近记录时间异常";
  }
  return date.toLocaleDateString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
  });
}

function hasAny(source: string, keywords: string[]): boolean {
  return keywords.some((keyword) => source.includes(keyword.toLowerCase()));
}

function matchesKnownAlgorithmPattern(source: string): boolean {
  return hasAny(source, [
    "return head",
    "return prev",
    "返回 head",
    "返回head",
    "返回 prev",
    "返回prev",
    "旧头",
    "新头节点",
    "put",
    "containskey",
    "查询顺序",
    "自匹配",
    "self pairing",
    "写入顺序",
    "最低价",
    "最小价格",
    "最大利润",
    "状态维护",
    "更新时机",
    "层边界",
    "queue.size",
    "当前层 size",
  ]);
}

function displayProblemTitle(problemTitle: string): string {
  return problemTitle.replace("二叉树的层序遍历", "二叉树层序遍历");
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
