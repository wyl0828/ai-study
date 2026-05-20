import type {
  KnowledgeCardDetail as ApiKnowledgeCardDetail,
  KnowledgeCardListItem,
} from "./types";
import { knowledgeSeedTopics } from "./knowledgeSeed";

export type KnowledgeCategory = "Java" | "MySQL" | "Redis" | "Spring" | "JVM" | "AI";
export type KnowledgeDifficulty = "简单" | "中等" | "困难";

export interface KnowledgeTopic {
  id: number;
  title: string;
  category: KnowledgeCategory;
  difficulty: KnowledgeDifficulty;
  tags: string[];
  question: string;
  answerKeywords: string[][];
  referenceAnswer: string;
  keyPoints: string[];
  followUpQuestions: string[];
  sourceName?: string | null;
  sourceUrl?: string | null;
  mastered: boolean;
}

export type KnowledgeDomain = "Java 核心" | "数据库" | "Spring" | "AI 工程";

export type KnowledgeSelection = {
  domain: KnowledgeDomain;
  section?: string;
  topic?: string;
  cardId?: number;
  cardTitle?: string;
};

export interface KnowledgeTopicMeta {
  title: string;
  description: string;
  iconLabel: string;
}

export interface SelfTestFeedback {
  score: number;
  level: "low" | "medium" | "high";
  comment: string;
  matchedKeyPoints: string[];
  missingKeyPoints: string[];
}

export type KnowledgeOutlineNode = KnowledgeSelection & {
  children?: KnowledgeOutlineNode[];
};

export const baseKnowledgeOutline: KnowledgeOutlineNode[] = [
  {
    domain: "Java 核心",
    children: [
      {
        domain: "Java 核心",
        section: "Java 基础",
        children: [
          { domain: "Java 核心", section: "Java 基础", topic: "面向对象" },
          { domain: "Java 核心", section: "Java 基础", topic: "数据类型" },
          { domain: "Java 核心", section: "Java 基础", topic: "异常处理" },
          { domain: "Java 核心", section: "Java 基础", topic: "反射与泛型" },
        ],
      },
      {
        domain: "Java 核心",
        section: "集合框架",
        children: [
          { domain: "Java 核心", section: "集合框架", topic: "List" },
          { domain: "Java 核心", section: "集合框架", topic: "Map" },
          { domain: "Java 核心", section: "集合框架", topic: "Set" },
        ],
      },
      { domain: "Java 核心", section: "并发编程（JUC）" },
      { domain: "Java 核心", section: "JVM 虚拟机" },
    ],
  },
  {
    domain: "数据库",
    children: [
      {
        domain: "数据库",
        section: "MySQL",
        children: [
          { domain: "数据库", section: "MySQL", topic: "索引" },
          { domain: "数据库", section: "MySQL", topic: "事务" },
          { domain: "数据库", section: "MySQL", topic: "锁" },
          { domain: "数据库", section: "MySQL", topic: "MVCC" },
        ],
      },
      {
        domain: "数据库",
        section: "Redis",
        children: [
          { domain: "数据库", section: "Redis", topic: "数据结构" },
          { domain: "数据库", section: "Redis", topic: "缓存问题" },
          { domain: "数据库", section: "Redis", topic: "持久化" },
          { domain: "数据库", section: "Redis", topic: "分布式锁" },
        ],
      },
    ],
  },
  {
    domain: "Spring",
    children: [
      { domain: "Spring", topic: "IOC" },
      { domain: "Spring", topic: "AOP" },
      { domain: "Spring", topic: "事务" },
      { domain: "Spring", topic: "Spring MVC" },
    ],
  },
  {
    domain: "AI 工程",
    children: [
      { domain: "AI 工程", topic: "Agent" },
      { domain: "AI 工程", topic: "RAG" },
      { domain: "AI 工程", topic: "LangChain" },
    ],
  },
];

export const knowledgeTopics: KnowledgeTopic[] = knowledgeSeedTopics;
export const defaultKnowledgeSelection: KnowledgeSelection = {
  domain: "Java 核心",
  section: "集合框架",
  topic: "Map",
};

export const knowledgeTopicMeta: Record<string, KnowledgeTopicMeta> = {
  "Java 核心": {
    title: "Java 核心",
    description: "围绕 Java 语言基础、集合、并发和 JVM 形成后端面试知识闭环。",
    iconLabel: "Java",
  },
  "Java 核心/Java 基础": {
    title: "Java 基础",
    description: "梳理面向对象、数据类型、异常处理、反射与泛型等高频基础题。",
    iconLabel: "基础",
  },
  "Java 核心/集合框架": {
    title: "集合框架",
    description: "掌握 List、Map、Set 等集合的底层结构、复杂度和线程安全边界。",
    iconLabel: "集合",
  },
  "Java 核心/集合框架/Map": {
    title: "Map（映射）",
    description:
      "掌握 HashMap、ConcurrentHashMap 等核心键值对数据结构的底层原理，应对大厂高频面试。",
    iconLabel: "Map",
  },
  "Java 核心/集合框架/List": {
    title: "List（线性表）",
    description:
      "理解 ArrayList、LinkedList 的结构差异、扩容机制和典型性能取舍。",
    iconLabel: "List",
  },
  "Java 核心/集合框架/Set": {
    title: "Set（去重集合）",
    description:
      "理解 HashSet、TreeSet 的唯一性判断、底层结构和排序语义。",
    iconLabel: "Set",
  },
  "Java 核心/并发编程（JUC）": {
    title: "并发编程（JUC）",
    description: "复习线程安全、锁、并发容器和 JUC 工具类的面试表达。",
    iconLabel: "JUC",
  },
  "Java 核心/JVM 虚拟机": {
    title: "JVM 虚拟机",
    description: "掌握类加载、内存区域、GC 和性能调优的核心面试题。",
    iconLabel: "JVM",
  },
  "数据库": {
    title: "数据库",
    description: "聚焦 MySQL 与 Redis 的数据结构、事务、缓存和高并发治理。",
    iconLabel: "DB",
  },
  "数据库/MySQL": {
    title: "MySQL",
    description: "复习索引、事务、锁、MVCC 和慢查询优化等后端面试高频知识。",
    iconLabel: "SQL",
  },
  "数据库/Redis": {
    title: "Redis",
    description: "复习 Redis 数据结构、缓存问题、持久化和分布式锁。",
    iconLabel: "Redis",
  },
  Spring: {
    title: "Spring",
    description: "掌握 IOC、AOP、事务和 Spring MVC 的核心原理与面试表达。",
    iconLabel: "Spring",
  },
  "AI 工程": {
    title: "AI 工程",
    description: "围绕 Agent 工作流、RAG 检索增强和 LangChain 工具链建立项目面试表达。",
    iconLabel: "AI",
  },
  "AI 工程/Agent": {
    title: "Agent",
    description: "理解 Planner、Tool Calling、Observation、Memory 等 Agent 工作流核心概念。",
    iconLabel: "Agent",
  },
  "AI 工程/RAG": {
    title: "RAG",
    description: "掌握检索增强生成的数据流、证据召回、用户记忆隔离和失败降级设计。",
    iconLabel: "RAG",
  },
  "AI 工程/LangChain": {
    title: "LangChain",
    description: "了解 LangChain 的链、工具、记忆和工程化封装思路，便于横向对比项目实现。",
    iconLabel: "LC",
  },
};

export const knowledgeCategories: Array<"全部分类" | KnowledgeCategory> = [
  "全部分类",
  "Java",
  "MySQL",
  "Redis",
  "Spring",
  "JVM",
  "AI",
];

export const knowledgeDifficulties: Array<"全部" | KnowledgeDifficulty> = [
  "全部",
  "简单",
  "中等",
  "困难",
];

const topicKeywords: Record<string, string[]> = {
  面向对象: ["面向对象", "OOP", "封装", "继承", "多态", "组合优于继承"],
  数据类型: ["数据类型", "基本类型", "包装类型", "装箱", "String", "BigDecimal"],
  异常处理: ["异常", "Exception", "RuntimeException", "try-catch", "全局异常"],
  反射与泛型: ["反射", "泛型", "注解", "Class", "类型擦除", "通配符"],
  Map: ["Map", "HashMap", "ConcurrentHashMap", "Hashtable", "LinkedHashMap", "TreeMap"],
  List: ["List", "ArrayList", "LinkedList", "Vector"],
  Set: ["Set", "HashSet", "TreeSet", "LinkedHashSet"],
  "Java 核心/并发编程（JUC）": ["JUC", "并发", "线程", "线程池", "锁", "volatile", "ThreadLocal", "CompletableFuture"],
  "Java 核心/JVM 虚拟机": ["JVM", "内存区域", "GC", "类加载", "Full GC"],
  索引: ["索引", "B+树", "B+ 树", "最左前缀", "EXPLAIN"],
  "数据库/MySQL/事务": ["事务", "ACID", "隔离级别", "长事务", "当前读"],
  锁: ["锁", "间隙锁", "行锁", "表锁"],
  MVCC: ["MVCC", "版本链", "ReadView"],
  数据结构: ["数据结构", "String", "Hash", "List", "Set", "ZSet"],
  缓存问题: ["缓存", "穿透", "击穿", "雪崩"],
  持久化: ["RDB", "AOF", "持久化"],
  分布式锁: ["分布式锁", "Redisson", "SETNX"],
  IOC: ["IOC", "IoC", "依赖注入", "Bean"],
  AOP: ["AOP", "切面", "代理"],
  "Spring/事务": ["Spring事务", "事务", "传播行为", "rollbackFor", "声明式事务", "编程式事务"],
  "Spring MVC": ["Spring MVC", "DispatcherServlet", "MVC"],
  Agent: ["Agent", "Planner", "Tool", "Observation", "Memory", "工具调用"],
  RAG: ["RAG", "检索", "召回", "知识库", "向量", "证据"],
  LangChain: ["LangChain", "Chain", "LCEL", "Tool", "Memory"],
};

const sectionCategoryMap: Record<string, KnowledgeCategory[]> = {
  "Java 核心": ["Java", "JVM"],
  "Java 核心/Java 基础": ["Java"],
  "Java 核心/集合框架": ["Java"],
  "Java 核心/并发编程（JUC）": ["Java"],
  "Java 核心/JVM 虚拟机": ["JVM"],
  数据库: ["MySQL", "Redis"],
  "数据库/MySQL": ["MySQL"],
  "数据库/Redis": ["Redis"],
  Spring: ["Spring"],
  "AI 工程": ["AI"],
  "AI 工程/Agent": ["AI"],
  "AI 工程/RAG": ["AI"],
  "AI 工程/LangChain": ["AI"],
};

export function selectionKey(selection: KnowledgeSelection): string {
  return [
    selection.domain,
    selection.section,
    selection.topic,
    selection.cardId ? `card:${selection.cardId}` : undefined,
  ]
    .filter(Boolean)
    .join("/");
}

export function getKnowledgeTopicMeta(selection: KnowledgeSelection): KnowledgeTopicMeta {
  if (selection.cardId && selection.cardTitle) {
    return {
      title: selection.cardTitle,
      description: "聚焦当前知识卡进行模拟自测、标杆回答解析和高频追问复盘。",
      iconLabel: "题目",
    };
  }

  const key = selectionKey(selection);
  const exactMeta = knowledgeTopicMeta[key];
  if (exactMeta) return exactMeta;

  if (selection.topic || selection.section) {
    const title = selection.topic || selection.section || selection.domain;
    return {
      title,
      description: "围绕当前专题进行高频面试知识训练。",
      iconLabel: title,
    };
  }

  return (
    knowledgeTopicMeta[selection.domain] || {
      title: selection.domain,
      description: "围绕当前专题进行高频面试知识训练。",
      iconLabel: selection.domain,
    }
  );
}

export function getSelectionBreadcrumb(selection: KnowledgeSelection): string[] {
  return [
    "知识训练",
    selection.domain,
    selection.section,
    selection.topic,
    selection.cardTitle,
  ].filter(Boolean) as string[];
}

export function matchKnowledgeTopic(
  topic: KnowledgeTopic,
  selection: KnowledgeSelection
): boolean {
  if (selection.cardId) {
    return topic.id === selection.cardId;
  }

  const key = selectionKey(selection);
  const categories = sectionCategoryMap[key] || sectionCategoryMap[selection.domain];
  if (categories && categories.length > 0 && !categories.includes(topic.category)) {
    return false;
  }

  const keywords = topicKeywords[key] || topicKeywords[selection.topic || selection.section || ""];

  if (!selection.topic && !keywords) {
    return !categories || categories.length === 0 ? false : categories.includes(topic.category);
  }

  if (!keywords) {
    return true;
  }

  const haystack = [
    topic.category,
    topic.title,
    topic.question,
    ...topic.tags,
  ].join(" ");

  return keywords.some((keyword) =>
    haystack.toLowerCase().includes(keyword.toLowerCase())
  );
}

export function buildKnowledgeOutline(topics: KnowledgeTopic[]): KnowledgeOutlineNode[] {
  const appendCards = (node: KnowledgeOutlineNode): KnowledgeOutlineNode => {
    if (node.children?.length) {
      return {
        ...node,
        children: node.children.map(appendCards),
      };
    }

    const children = topics
      .filter((topic) => matchKnowledgeTopic(topic, node))
      .map((topic) => ({
        ...node,
        cardId: topic.id,
        cardTitle: topic.title,
      }));

    return children.length > 0 ? { ...node, children } : node;
  };

  return baseKnowledgeOutline.map(appendCards);
}

export function inferKnowledgeSelection(topic: KnowledgeTopic): KnowledgeSelection | null {
  const candidates: KnowledgeSelection[] = [
    { domain: "Java 核心", section: "Java 基础", topic: "面向对象" },
    { domain: "Java 核心", section: "Java 基础", topic: "数据类型" },
    { domain: "Java 核心", section: "Java 基础", topic: "异常处理" },
    { domain: "Java 核心", section: "Java 基础", topic: "反射与泛型" },
    { domain: "Java 核心", section: "集合框架", topic: "Map" },
    { domain: "Java 核心", section: "集合框架", topic: "List" },
    { domain: "Java 核心", section: "集合框架", topic: "Set" },
    { domain: "Java 核心", section: "并发编程（JUC）" },
    { domain: "Java 核心", section: "JVM 虚拟机" },
    { domain: "数据库", section: "MySQL", topic: "索引" },
    { domain: "数据库", section: "MySQL", topic: "事务" },
    { domain: "数据库", section: "MySQL", topic: "锁" },
    { domain: "数据库", section: "MySQL", topic: "MVCC" },
    { domain: "数据库", section: "Redis", topic: "数据结构" },
    { domain: "数据库", section: "Redis", topic: "缓存问题" },
    { domain: "数据库", section: "Redis", topic: "持久化" },
    { domain: "数据库", section: "Redis", topic: "分布式锁" },
    { domain: "数据库", section: "MySQL" },
    { domain: "数据库", section: "Redis" },
    { domain: "Spring", topic: "IOC" },
    { domain: "Spring", topic: "AOP" },
    { domain: "Spring", topic: "事务" },
    { domain: "Spring", topic: "Spring MVC" },
    { domain: "AI 工程", topic: "Agent" },
    { domain: "AI 工程", topic: "RAG" },
    { domain: "AI 工程", topic: "LangChain" },
  ];

  return candidates.find((candidate) => matchKnowledgeTopic(topic, candidate)) || null;
}

const categoryMap: Record<string, KnowledgeCategory> = {
  JAVA: "Java",
  MYSQL: "MySQL",
  REDIS: "Redis",
  SPRING: "Spring",
  JVM: "JVM",
  AI: "AI",
  Java: "Java",
  MySQL: "MySQL",
  Redis: "Redis",
  Spring: "Spring",
};

const difficultyMap: Record<string, KnowledgeDifficulty> = {
  EASY: "简单",
  MEDIUM: "中等",
  HARD: "困难",
  简单: "简单",
  中等: "中等",
  困难: "困难",
};

export function toKnowledgeTopic(
  card: KnowledgeCardListItem | ApiKnowledgeCardDetail
): KnowledgeTopic {
  const detail = card as Partial<ApiKnowledgeCardDetail>;
  const keyPoints = normalizeKeyPoints(detail.keyPoints, card.question);
  const followUpQuestions = splitFollowUp(detail.followUp);

  return {
    id: card.id,
    title: card.title,
    category: categoryMap[card.category] || categoryMap[card.label] || "Java",
    difficulty: difficultyMap[card.difficulty] || "中等",
    tags: card.tags || [],
    question: card.question,
    answerKeywords: keyPoints.map(toKeywordGroup),
    referenceAnswer: detail.answer || "暂无标杆回答，请先围绕核心记忆点组织自己的面试回答。",
    keyPoints,
    followUpQuestions,
    sourceName: card.sourceName,
    sourceUrl: card.sourceUrl,
    mastered: false,
  };
}

function normalizeKeyPoints(
  keyPoints: string[] | undefined,
  fallback: string
): string[] {
  const points = (keyPoints || [])
    .map((point) => point.trim())
    .filter(Boolean);

  if (points.length > 0) {
    return points;
  }

  return [fallback];
}

function splitFollowUp(followUp: string | null | undefined): string[] {
  if (!followUp) return [];
  return followUp
    .split(/\r?\n/)
    .map((question) => question.trim())
    .filter(Boolean);
}

function toKeywordGroup(point: string): string[] {
  const fragments = point
    .split(/[：:，,、+（）()\/\s]+/)
    .map((fragment) => fragment.trim())
    .filter((fragment) => fragment.length >= 2);

  return Array.from(new Set([point, ...fragments]));
}

export function evaluateSelfTest(
  answer: string,
  topic: KnowledgeTopic
): SelfTestFeedback {
  const normalized = answer.toLowerCase();
  const matchedKeyPoints = topic.keyPoints.filter((_, index) =>
    topic.answerKeywords[index]?.some((keyword) =>
      normalized.includes(keyword.toLowerCase())
    )
  );
  const matchedSet = new Set(matchedKeyPoints);
  const missingKeyPoints = topic.keyPoints.filter(
    (point) => !matchedSet.has(point)
  );

  const matchRatio = matchedKeyPoints.length / Math.max(topic.keyPoints.length, 1);
  if (matchRatio >= 0.75) {
    return {
      score: 85,
      level: "high",
      comment: buildInterviewFeedback("high", matchedKeyPoints, missingKeyPoints),
      matchedKeyPoints,
      missingKeyPoints,
    };
  }

  if (matchRatio >= 0.35) {
    return {
      score: 70,
      level: "medium",
      comment: buildInterviewFeedback("medium", matchedKeyPoints, missingKeyPoints),
      matchedKeyPoints,
      missingKeyPoints,
    };
  }

  return {
    score: 45,
    level: "low",
    comment: buildInterviewFeedback("low", matchedKeyPoints, missingKeyPoints),
    matchedKeyPoints,
    missingKeyPoints,
  };
}

function buildInterviewFeedback(
  level: SelfTestFeedback["level"],
  matchedKeyPoints: string[],
  missingKeyPoints: string[]
): string {
  if (level === "high") {
    const smallGap = missingKeyPoints.slice(0, 2).join("；");
    return smallGap
      ? `回答较完整，已经覆盖主要面试核心点。建议再补充：${smallGap}。`
      : "回答较完整，基本覆盖面试核心点，可以继续练习追问表达。";
  }

  const matchedText =
    matchedKeyPoints.length > 0
      ? `你已经提到了${matchedKeyPoints.slice(0, 2).join("、")}。`
      : "你还没有稳定说出本题的核心机制。";
  const missingText =
    missingKeyPoints.length > 0
      ? `建议补充：${missingKeyPoints.slice(0, 3).join("；")}。`
      : "建议补充触发条件、底层机制和常见坑。";

  if (level === "medium") {
    return `${matchedText}但面试中如果只停留在这些点，回答会显得不够完整。${missingText}`;
  }

  return `${matchedText}面试中如果只停留在这些点，会显得理解偏浅，面试官通常会继续追问机制、边界和常见坑。${missingText}`;
}
