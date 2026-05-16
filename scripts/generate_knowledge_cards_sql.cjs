const fs = require("node:fs");
const path = require("node:path");

const repoRoot = path.resolve(__dirname, "..");
const outputPath = path.join(repoRoot, "data", "knowledge_cards.sql");
const seedOutputPath = path.join(repoRoot, "frontend", "lib", "knowledgeSeed.ts");
const cardProfiles = require("./knowledge_card_profiles.cjs");

const sources = {
  JAVA: ["小林 coding, JavaGuide", "https://javaguide.cn/home.html"],
  JVM: ["小林 coding, JavaGuide", "https://javaguide.cn/java/jvm/"],
  MYSQL: ["小林 coding, JavaGuide", "https://xiaolincoding.com/mysql/"],
  REDIS: ["小林 coding, JavaGuide", "https://xiaolincoding.com/redis/"],
  SPRING: ["JavaGuide", "https://javaguide.cn/system-design/framework/spring/"],
  AI: ["项目原创整理", ""],
};

const leaves = [
  {
    category: "JAVA",
    baseOrder: 10,
    tag: "面向对象",
    difficulty: "EASY",
    titles: ["什么是封装", "接口和抽象类如何取舍", "重载与重写的区别", "Java 对象创建过程", "组合优于继承的原因"],
  },
  {
    category: "JAVA",
    baseOrder: 20,
    tag: "数据类型",
    difficulty: "EASY",
    titles: ["基本类型和包装类型区别", "自动装箱与拆箱风险", "String 为什么不可变", "StringBuilder 和 StringBuffer 区别", "BigDecimal 为什么适合金额计算"],
  },
  {
    category: "JAVA",
    baseOrder: 30,
    tag: "异常处理",
    difficulty: "MEDIUM",
    titles: ["Checked Exception 和 RuntimeException 区别", "try-catch-finally 执行顺序", "业务异常如何设计", "异常被吞掉的排查思路", "全局异常处理的分层边界"],
  },
  {
    category: "JAVA",
    baseOrder: 40,
    tag: "反射与泛型",
    difficulty: "MEDIUM",
    titles: ["反射的使用场景和代价", "泛型擦除是什么", "Class 对象和类加载关系", "注解如何配合反射工作", "泛型通配符 extends 和 super 区别"],
  },
  {
    category: "JAVA",
    baseOrder: 50,
    tag: "List",
    difficulty: "EASY",
    titles: ["ArrayList 和 LinkedList 的区别", "ArrayList 扩容机制", "ArrayList 删除元素的坑", "CopyOnWriteArrayList 适用场景", "List 遍历时修改为什么会失败"],
  },
  {
    category: "JAVA",
    baseOrder: 60,
    tag: "Map",
    difficulty: "MEDIUM",
    titles: ["HashMap 在 JDK 1.8 中的底层结构", "HashMap put 流程", "HashMap 扩容为什么是 2 的幂", "ConcurrentHashMap 如何保证线程安全", "LinkedHashMap 如何实现 LRU"],
  },
  {
    category: "JAVA",
    baseOrder: 70,
    tag: "Set",
    difficulty: "EASY",
    titles: ["HashSet 如何保证元素唯一", "TreeSet 的排序和去重规则", "LinkedHashSet 如何保持插入顺序", "Set 中可变对象为什么危险", "HashSet 和 ConcurrentHashMap.newKeySet 的区别"],
  },
  {
    category: "JAVA",
    baseOrder: 80,
    tag: "JUC",
    difficulty: "MEDIUM",
    titles: ["synchronized 和 ReentrantLock 的区别", "ThreadLocal 使用场景和风险", "volatile 能保证什么", "线程池核心参数如何设置", "CompletableFuture 适合什么场景"],
  },
  {
    category: "JVM",
    baseOrder: 100,
    tag: "JVM",
    difficulty: "MEDIUM",
    titles: ["JVM 运行时内存区域", "CMS 和 G1 垃圾收集器区别", "类加载过程和双亲委派", "对象从创建到回收的过程", "线上 Full GC 如何排查"],
  },
  {
    category: "MYSQL",
    baseOrder: 200,
    tag: "索引",
    difficulty: "HARD",
    titles: ["MySQL 索引失效场景及优化", "InnoDB 为什么使用 B+ 树索引", "联合索引最左前缀原则", "覆盖索引和回表", "Explain 执行计划怎么看"],
  },
  {
    category: "MYSQL",
    baseOrder: 210,
    tag: "事务",
    difficulty: "HARD",
    titles: ["事务 ACID 如何理解", "隔离级别解决哪些问题", "可重复读为什么还可能有当前读", "事务传播到业务代码的边界", "长事务会带来什么风险"],
  },
  {
    category: "MYSQL",
    baseOrder: 220,
    tag: "锁",
    difficulty: "HARD",
    titles: ["行锁、表锁和间隙锁区别", "Next-Key Lock 解决什么问题", "死锁如何定位和避免", "乐观锁和悲观锁怎么选", "select for update 使用边界"],
  },
  {
    category: "MYSQL",
    baseOrder: 230,
    tag: "MVCC",
    difficulty: "HARD",
    titles: ["MVCC 是什么", "Read View 如何判断版本可见", "undo log 和版本链关系", "快照读和当前读区别", "MVCC 为什么不能完全替代锁"],
  },
  {
    category: "REDIS",
    baseOrder: 300,
    tag: "数据结构",
    difficulty: "MEDIUM",
    titles: ["Redis 为什么快", "String 的典型使用场景", "Hash 结构适合存什么", "List、Set、ZSet 如何选择", "大 key 会带来什么问题"],
  },
  {
    category: "REDIS",
    baseOrder: 310,
    tag: "缓存问题",
    difficulty: "MEDIUM",
    titles: ["Redis 缓存击穿、穿透与雪崩", "缓存与数据库一致性", "热点 key 如何治理", "布隆过滤器解决什么问题", "缓存预热和降级怎么设计"],
  },
  {
    category: "REDIS",
    baseOrder: 320,
    tag: "持久化",
    difficulty: "MEDIUM",
    titles: ["Redis RDB 和 AOF 持久化", "AOF 重写解决什么问题", "RDB 快照触发方式", "混合持久化的价值", "Redis 宕机恢复如何取舍"],
  },
  {
    category: "REDIS",
    baseOrder: 330,
    tag: "分布式锁",
    difficulty: "HARD",
    titles: ["Redis 分布式锁基本实现", "SET NX EX 为什么要原子", "锁续期和看门狗机制", "Redisson 分布式锁原理", "Redis 锁误删如何避免"],
  },
  {
    category: "SPRING",
    baseOrder: 400,
    tag: "IOC",
    difficulty: "MEDIUM",
    titles: ["Spring Bean 生命周期", "IOC 和依赖注入的关系", "BeanFactory 和 ApplicationContext 区别", "循环依赖三级缓存", "BeanPostProcessor 扩展点"],
  },
  {
    category: "SPRING",
    baseOrder: 410,
    tag: "AOP",
    difficulty: "MEDIUM",
    titles: ["Spring AOP 实现原理", "JDK 动态代理和 CGLIB 区别", "同类内部调用为什么绕过 AOP", "切点和通知如何组织", "AOP 适合哪些横切逻辑"],
  },
  {
    category: "SPRING",
    baseOrder: 420,
    tag: "事务",
    difficulty: "HARD",
    titles: ["Spring 事务失效场景", "事务传播行为怎么理解", "rollbackFor 什么时候需要配置", "声明式事务和编程式事务区别", "事务边界如何设计"],
  },
  {
    category: "SPRING",
    baseOrder: 430,
    tag: "Spring MVC",
    difficulty: "MEDIUM",
    titles: ["Spring MVC 请求处理流程", "DispatcherServlet 的职责", "HandlerMapping 和 HandlerAdapter 区别", "参数解析和返回值处理", "拦截器和过滤器区别"],
  },
  {
    category: "AI",
    baseOrder: 500,
    tag: "Agent",
    difficulty: "MEDIUM",
    titles: ["Agent 工作流为什么要拆成 Planner、Tool 和 Observation", "Tool Calling 的工程边界", "Agent Step Trace 如何帮助排查", "Agent 失败降级怎么设计", "Memory 在面试训练中的作用"],
  },
  {
    category: "AI",
    baseOrder: 510,
    tag: "RAG",
    difficulty: "MEDIUM",
    titles: ["RAG 在当前项目中为什么作为 Agent 内部 Tool", "RAG 检索结果为什么只是证据", "用户记忆检索为什么必须隔离", "MySQL 结构化 RAG 的取舍", "RAG 失败为什么不能阻塞诊断"],
  },
  {
    category: "AI",
    baseOrder: 520,
    tag: "LangChain",
    difficulty: "HARD",
    titles: ["LangChain 和本项目自定义 Agent 编排有什么区别", "Chain、Tool、Memory 分别解决什么问题", "为什么 MVP 阶段不强依赖 LangChain", "LangChain 接入 Spring Boot 的边界", "LCEL 表达式适合什么场景"],
  },
];

function sql(value) {
  if (value === null || value === undefined || value === "") return "NULL";
  return `'${String(value).replace(/'/g, "''")}'`;
}

function tags(leaf, title) {
  const common = {
    JAVA: "Java 核心",
    JVM: "JVM",
    MYSQL: "MySQL",
    REDIS: "Redis",
    SPRING: "Spring",
    AI: "AI 工程",
  }[leaf.category];
  const titleExtras = {
    "什么是封装": "OOP,封装",
  }[title];
  const extras = titleExtras || {
    面向对象: "OOP,封装,继承,多态",
    数据类型: "数据类型,包装类型,String",
    异常处理: "异常处理,RuntimeException,全局异常",
    反射与泛型: "反射,泛型,注解",
    List: "集合框架,List,ArrayList,LinkedList",
    Map: "集合框架,Map,HashMap,ConcurrentHashMap",
    Set: "集合框架,Set,HashSet,TreeSet",
    JUC: "并发编程,JUC,锁,线程池,volatile,ThreadLocal",
    JVM: "JVM,内存区域,GC,类加载",
    索引: "MySQL索引,B+树,最左前缀,EXPLAIN",
    事务: leaf.category === "MYSQL" ? "MySQL事务,ACID,隔离级别" : "Spring事务,AOP,传播行为",
    锁: "MySQL锁,行锁,间隙锁,死锁",
    MVCC: "MVCC,Read View,undo log,版本链",
    数据结构: "Redis数据结构,String,Hash,List,Set,ZSet",
    缓存问题: "Redis缓存,缓存穿透,缓存击穿,缓存雪崩,布隆过滤器",
    持久化: "Redis持久化,RDB,AOF",
    分布式锁: "Redis分布式锁,SETNX,Redisson,锁续期",
    IOC: "Spring,IOC,Bean,依赖注入",
    AOP: "Spring,AOP,动态代理,切面",
    "Spring MVC": "Spring MVC,DispatcherServlet,HandlerMapping,HandlerAdapter",
    Agent: "AI 工程,Agent,Planner,Tool Calling,Observation,Memory",
    RAG: "AI 工程,RAG,检索,证据,用户记忆,MySQL 检索",
    LangChain: "AI 工程,LangChain,Chain,Tool,Memory,LCEL",
  }[leaf.tag];
  return `${common},${extras}`;
}

function toKeywordGroup(point) {
  const fragments = point
    .split(/[：:，,、+（）()\/\s]+/)
    .map((fragment) => fragment.trim())
    .filter((fragment) => fragment.length >= 2);

  return Array.from(new Set([point, ...fragments]));
}

function writeFrontendSeed(cards) {
  const categoryMap = {
    JAVA: "Java",
    JVM: "JVM",
    MYSQL: "MySQL",
    REDIS: "Redis",
    SPRING: "Spring",
    AI: "AI",
  };
  const difficultyMap = {
    EASY: "简单",
    MEDIUM: "中等",
    HARD: "困难",
  };
  const seedCards = cards.map((card, index) => {
    const keyPoints = card.keyPoints.split(/\r?\n/).filter(Boolean);
    return {
      id: index + 1,
      title: card.title,
      category: categoryMap[card.category],
      difficulty: difficultyMap[card.difficulty],
      tags: card.tags.split(",").map((tag) => tag.trim()).filter(Boolean),
      question: card.question,
      answerKeywords: keyPoints.map(toKeywordGroup),
      referenceAnswer: card.answer,
      keyPoints,
      followUpQuestions: card.followUp.split(/\r?\n/).filter(Boolean),
      sourceName: card.sourceName,
      sourceUrl: card.sourceUrl || null,
      mastered: false,
    };
  });
  const source = [
    'import type { KnowledgeTopic } from "./knowledgeData";',
    "",
    "// Generated from scripts/generate_knowledge_cards_sql.cjs so the offline fallback matches backend seed content.",
    `export const knowledgeSeedTopics: KnowledgeTopic[] = ${JSON.stringify(seedCards, null, 2)};`,
    "",
  ].join("\n");

  fs.writeFileSync(seedOutputPath, source, "utf8");
}

const cards = leaves.flatMap((leaf) =>
  leaf.titles.map((title, index) => {
    const [sourceName, sourceUrl] = sources[leaf.category];
    const cardProfile = cardProfiles[title];
    if (!cardProfile) {
      throw new Error(`Missing explicit knowledge card profile: ${title}`);
    }
    return {
      category: leaf.category,
      title,
      question: cardProfile.question,
      answer: cardProfile.answer,
      followUp: cardProfile.followUps.join("\n"),
      keyPoints: cardProfile.keyPoints.join("\n"),
      difficulty: leaf.difficulty,
      tags: tags(leaf, title),
      sourceName,
      sourceUrl,
      sortOrder: leaf.baseOrder + index,
    };
  })
);

const header = `USE ai_interview_coach;

DELETE FROM knowledge_card
WHERE source_name IN ('小林 coding', 'JavaGuide', '小林 coding, JavaGuide', '项目原创整理');

INSERT INTO knowledge_card
(category, title, question, answer, follow_up, key_points, difficulty, tags, source_name, source_url, enabled, sort_order, created_at, updated_at)
VALUES
`;

const rows = cards.map((card) => `(${[
  sql(card.category),
  sql(card.title),
  sql(card.question),
  sql(card.answer),
  sql(card.followUp),
  sql(card.keyPoints),
  sql(card.difficulty),
  sql(card.tags),
  sql(card.sourceName),
  sql(card.sourceUrl),
  "1",
  String(card.sortOrder),
  "NOW()",
  "NOW()",
].join(", ")})`);

fs.writeFileSync(outputPath, `${header}${rows.join(",\n")};\n`, "utf8");
writeFrontendSeed(cards);
console.log(`Wrote ${cards.length} knowledge cards to ${path.relative(repoRoot, outputPath)}`);
console.log(`Wrote ${cards.length} fallback topics to ${path.relative(repoRoot, seedOutputPath)}`);
