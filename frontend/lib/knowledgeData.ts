import type {
  KnowledgeCardDetail as ApiKnowledgeCardDetail,
  KnowledgeCardListItem,
} from "./types";

export type KnowledgeCategory = "Java" | "MySQL" | "Redis" | "Spring" | "JVM";
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

export type KnowledgeDomain = "Java 核心" | "数据库" | "Spring";

export type KnowledgeSelection = {
  domain: KnowledgeDomain;
  section?: string;
  topic?: string;
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

export const knowledgeTopics: KnowledgeTopic[] = [
  {
    id: 1,
    title: "HashMap 在 JDK 1.8 中的底层结构是什么？",
    category: "Java",
    difficulty: "简单",
    tags: ["Java 集合", "HashMap", "Map", "数据结构"],
    question:
      "请简述 HashMap 在 JDK 1.8 版本中底层的核心数据结构，以及为什么在 1.8 中要做这样的改变。",
    answerKeywords: [
      ["数组", "链表", "红黑树"],
      ["8", "64", "树化"],
      ["O(log n)", "查询效率", "最坏情况"],
    ],
    referenceAnswer:
      "JDK 1.8 中 HashMap 的底层结构是数组 + 链表 + 红黑树。元素先根据 hash 定位到数组桶，发生哈希冲突时以链表保存；当链表长度达到 8 且数组长度达到 64 时，链表会转化为红黑树。这样可以避免大量冲突时链表查询退化过重，把最坏情况下的查找复杂度从 O(n) 优化到 O(log n)。",
    keyPoints: [
      "数组 + 链表 + 红黑树",
      "链表长度达到 8 且数组长度达到 64 时树化",
      "提高查询效率，最坏情况从 O(n) 优化到 O(log n)",
    ],
    followUpQuestions: [
      "为什么链表转化为红黑树的阈值是 8？",
      "红黑树退化为链表的阈值是多少？为什么是这个值？",
      "JDK 1.8 中 HashMap 为什么要从头插法改成尾插法？",
    ],
    mastered: false,
  },
  {
    id: 4,
    title: "ArrayList 和 LinkedList 的区别",
    category: "Java",
    difficulty: "简单",
    tags: ["Java 集合", "List", "ArrayList", "LinkedList"],
    question:
      "请从底层数据结构、查询性能、插入删除成本和典型使用场景对比 ArrayList 与 LinkedList。",
    answerKeywords: [
      ["数组", "连续内存", "随机访问"],
      ["链表", "节点", "指针"],
      ["查询快", "插入删除"],
    ],
    referenceAnswer:
      "ArrayList 底层基于数组，支持按下标 O(1) 随机访问，扩容和中间插入删除需要移动元素；LinkedList 底层是双向链表，按下标查询需要遍历，但在已定位节点后插入删除成本较低。面试中要结合访问模式说明选择：读多用 ArrayList，更频繁的头尾插入可考虑 LinkedList 或队列结构。",
    keyPoints: [
      "ArrayList 底层是动态数组",
      "LinkedList 底层是双向链表",
      "ArrayList 查询快，中间插入删除可能移动元素",
    ],
    followUpQuestions: [
      "ArrayList 扩容时为什么会有拷贝成本？",
      "为什么实际开发中 LinkedList 并不总比 ArrayList 适合插入删除？",
    ],
    mastered: false,
  },
  {
    id: 5,
    title: "HashSet 如何保证元素唯一",
    category: "Java",
    difficulty: "中等",
    tags: ["Java 集合", "Set", "HashSet", "唯一性"],
    question:
      "请解释 HashSet 的底层实现，以及它如何结合 hashCode 和 equals 保证元素唯一。",
    answerKeywords: [
      ["HashMap"],
      ["hashCode", "equals"],
      ["唯一", "key"],
    ],
    referenceAnswer:
      "HashSet 底层主要借助 HashMap 实现，元素会作为 HashMap 的 key 保存，value 使用固定占位对象。添加元素时先根据 hashCode 定位桶，再通过 equals 判断是否已经存在相等元素，因此要正确重写 hashCode 和 equals，否则会破坏去重语义。",
    keyPoints: [
      "HashSet 底层依赖 HashMap",
      "元素作为 HashMap 的 key 保存",
      "通过 hashCode 和 equals 共同判断唯一性",
    ],
    followUpQuestions: [
      "为什么只重写 equals 不重写 hashCode 会出问题？",
      "HashSet 和 TreeSet 的排序与唯一性判断有什么不同？",
    ],
    mastered: false,
  },
  {
    id: 2,
    title: "MySQL 索引失效场景及优化",
    category: "MySQL",
    difficulty: "困难",
    tags: ["MySQL 索引", "B+ 树", "最左前缀"],
    question:
      "在什么情况下 MySQL 的索引会失效？请结合执行计划说明如何优化慢查询。",
    answerKeywords: [
      ["最左前缀"],
      ["隐式类型转换"],
      ["函数", "运算"],
      ["LIKE", "%"],
      ["OR", "无索引"],
    ],
    referenceAnswer:
      "MySQL 索引常见失效场景包括违反联合索引最左前缀原则、发生隐式类型转换、在索引列上使用函数或运算、LIKE 以 % 开头、OR 条件中包含无索引列等。优化时应先用 EXPLAIN 观察 type、key、rows、Extra 等字段，确认是否走到合适索引，再调整 SQL 写法、补充合适的联合索引，或通过覆盖索引、索引下推等方式减少回表和扫描行数。",
    keyPoints: [
      "违反最左前缀原则",
      "隐式类型转换",
      "索引列上使用函数或运算",
      "LIKE 以 % 开头",
      "OR 条件中包含无索引列",
    ],
    followUpQuestions: [
      "什么是索引下推 ICP？",
      "为什么推荐主键使用自增 ID 而不是 UUID？",
    ],
    mastered: false,
  },
  {
    id: 3,
    title: "Redis 缓存击穿、穿透与雪崩",
    category: "Redis",
    difficulty: "中等",
    tags: ["Redis 缓存", "高并发"],
    question:
      "请解释什么是缓存穿透、缓存击穿和缓存雪崩，并分别给出对应的工业级解决方案。",
    answerKeywords: [
      ["缓存穿透", "布隆过滤器", "缓存空值"],
      ["缓存击穿", "热点 key", "互斥锁", "逻辑过期"],
      ["缓存雪崩", "随机过期", "同时过期"],
    ],
    referenceAnswer:
      "缓存穿透是请求查询不存在的数据，缓存和数据库都查不到，常用布隆过滤器或缓存空值解决。缓存击穿是热点 key 过期后，大量并发请求同时打到数据库，通常用互斥锁、逻辑过期或热点数据预热处理。缓存雪崩是大量 key 在同一时间失效或缓存集群异常导致请求集中压到数据库，可以通过过期时间加随机值、多级缓存、限流降级和高可用部署降低风险。",
    keyPoints: [
      "缓存穿透：查询不存在的数据，可用布隆过滤器或缓存空值",
      "缓存击穿：热点 key 过期，可用互斥锁或逻辑过期",
      "缓存雪崩：大量 key 同时过期，可加随机过期时间",
    ],
    followUpQuestions: [
      "布隆过滤器为什么会有误判？",
      "热点 key 过期时为什么不能让所有请求都打到数据库？",
    ],
    mastered: false,
  },
];

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
};

export const knowledgeCategories: Array<"全部分类" | KnowledgeCategory> = [
  "全部分类",
  "Java",
  "MySQL",
  "Redis",
  "Spring",
  "JVM",
];

export const knowledgeDifficulties: Array<"全部" | KnowledgeDifficulty> = [
  "全部",
  "简单",
  "中等",
  "困难",
];

const topicKeywords: Record<string, string[]> = {
  Map: ["Map", "HashMap", "ConcurrentHashMap", "Hashtable", "LinkedHashMap", "TreeMap"],
  List: ["List", "ArrayList", "LinkedList", "Vector"],
  Set: ["Set", "HashSet", "TreeSet", "LinkedHashSet"],
  索引: ["索引", "B+树", "B+ 树", "最左前缀", "EXPLAIN"],
  事务: ["事务", "ACID", "隔离级别"],
  锁: ["锁", "间隙锁", "行锁", "表锁"],
  MVCC: ["MVCC", "版本链", "ReadView"],
  数据结构: ["数据结构", "String", "Hash", "List", "Set", "ZSet"],
  缓存问题: ["缓存", "穿透", "击穿", "雪崩"],
  持久化: ["RDB", "AOF", "持久化"],
  分布式锁: ["分布式锁", "Redisson", "SETNX"],
  IOC: ["IOC", "IoC", "依赖注入", "Bean"],
  AOP: ["AOP", "切面", "代理"],
  "Spring MVC": ["Spring MVC", "DispatcherServlet", "MVC"],
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
};

export function selectionKey(selection: KnowledgeSelection): string {
  return [selection.domain, selection.section, selection.topic]
    .filter(Boolean)
    .join("/");
}

export function getKnowledgeTopicMeta(selection: KnowledgeSelection): KnowledgeTopicMeta {
  const key = selectionKey(selection);
  return (
    knowledgeTopicMeta[key] ||
    knowledgeTopicMeta[selection.domain] || {
      title: selection.topic || selection.section || selection.domain,
      description: "围绕当前专题进行高频面试知识训练。",
      iconLabel: selection.topic || selection.section || selection.domain,
    }
  );
}

export function getSelectionBreadcrumb(selection: KnowledgeSelection): string[] {
  return ["知识训练", selection.domain, selection.section, selection.topic].filter(
    Boolean
  ) as string[];
}

export function matchKnowledgeTopic(
  topic: KnowledgeTopic,
  selection: KnowledgeSelection
): boolean {
  const key = selectionKey(selection);
  const categories = sectionCategoryMap[key] || sectionCategoryMap[selection.domain];
  if (categories && categories.length > 0 && !categories.includes(topic.category)) {
    return false;
  }

  if (!selection.topic) {
    return !categories || categories.length === 0 ? false : categories.includes(topic.category);
  }

  const keywords = topicKeywords[selection.topic];
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

export function inferKnowledgeSelection(topic: KnowledgeTopic): KnowledgeSelection | null {
  const candidates: KnowledgeSelection[] = [
    { domain: "Java 核心", section: "集合框架", topic: "Map" },
    { domain: "Java 核心", section: "集合框架", topic: "List" },
    { domain: "Java 核心", section: "集合框架", topic: "Set" },
    { domain: "Java 核心", section: "JVM 虚拟机" },
    { domain: "数据库", section: "MySQL" },
    { domain: "数据库", section: "Redis" },
    { domain: "Spring" },
  ];

  return candidates.find((candidate) => matchKnowledgeTopic(topic, candidate)) || null;
}

const categoryMap: Record<string, KnowledgeCategory> = {
  JAVA: "Java",
  MYSQL: "MySQL",
  REDIS: "Redis",
  SPRING: "Spring",
  JVM: "JVM",
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
      comment: "回答较完整，基本覆盖面试核心点",
      matchedKeyPoints,
      missingKeyPoints,
    };
  }

  if (matchRatio >= 0.35) {
    return {
      score: 70,
      level: "medium",
      comment: "覆盖了部分要点，但缺少触发条件或优化目的",
      matchedKeyPoints,
      missingKeyPoints,
    };
  }

  return {
    score: 45,
    level: "low",
    comment:
      "回答过短，面试官会继续追问机制/触发条件/优化目的，建议先把核心概念和为什么这样优化说清楚。",
    matchedKeyPoints,
    missingKeyPoints,
  };
}
