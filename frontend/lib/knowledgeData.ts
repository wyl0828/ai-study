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
  mastered: boolean;
}

export interface SelfTestFeedback {
  score: number;
  level: "low" | "medium" | "high";
  comment: string;
  matchedKeyPoints: string[];
}

export const knowledgeTopics: KnowledgeTopic[] = [
  {
    id: 1,
    title: "HashMap 在 JDK 1.8 中的底层结构是什么？",
    category: "Java",
    difficulty: "简单",
    tags: ["Java 集合", "数据结构"],
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

  const matchRatio = matchedKeyPoints.length / topic.keyPoints.length;
  if (matchRatio >= 0.75) {
    return {
      score: 85,
      level: "high",
      comment: "回答较完整，基本覆盖面试核心点",
      matchedKeyPoints,
    };
  }

  if (matchRatio >= 0.35) {
    return {
      score: 70,
      level: "medium",
      comment: "覆盖了部分要点，但缺少触发条件或优化目的",
      matchedKeyPoints,
    };
  }

  return {
    score: 45,
    level: "low",
    comment: "回答过于简略，没有覆盖核心机制",
    matchedKeyPoints,
  };
}
