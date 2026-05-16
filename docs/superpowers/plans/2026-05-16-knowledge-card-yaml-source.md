# Knowledge Card YAML Source Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move knowledge-card content maintenance from `scripts/knowledge_card_profiles.cjs` to topic-level YAML files while keeping MySQL `knowledge_card`, generated `data/knowledge_cards.sql`, backend APIs, frontend pages, and RAG runtime behavior unchanged.

**Architecture:** Runtime remains unchanged: `/knowledge` and Agent RAG continue to read MySQL-backed `knowledge_card` records. The maintenance source changes to `data/knowledge-cards/*.yml`; `scripts/generate_knowledge_cards_sql.cjs` becomes a validated generator that outputs the same SQL schema as before. Phase 1 uses a hybrid generator so one migrated YAML topic can coexist with old profile-backed topics; Phase 2 switches to YAML-only after all 24 topics are migrated.

**Tech Stack:** Node.js CommonJS scripts, `yaml` npm package for parsing, MySQL seed SQL, existing frontend Node tests, existing Spring Boot/MyBatis/RAG services.

---

## Scope Guardrails

- Do not modify `backend/src/main/java/com/interview/coach/controller/KnowledgeController.java`.
- Do not modify `knowledge_card` table columns in `data/schema.sql`.
- Do not modify `/knowledge` page structure or its API contract.
- Do not expose a public RAG chat or retrieval endpoint.
- Do not delete `scripts/knowledge_card_profiles.cjs` in Phase 1.
- Keep `data/knowledge_cards.sql` as the database import artifact.
- Keep SQL output columns exactly: `category`, `title`, `question`, `answer`, `follow_up`, `key_points`, `difficulty`, `tags`, `source_name`, `source_url`, `enabled`, `sort_order`, `created_at`, `updated_at`.

## File Structure

- Create `package.json`: root-level script and dev dependency for YAML parsing.
- Create `data/knowledge-cards/java-map.yml`: first migrated topic with 5 Map/HashMap cards.
- Modify `scripts/generate_knowledge_cards_sql.cjs`: read YAML topic files, merge with legacy profiles during Phase 1, validate quality, output compatible SQL.
- Modify `frontend/lib/knowledge-tree-coverage.node-test.cjs`: update generator expectations from explicit profile-only source to YAML-first hybrid source.
- Modify `docs/API.md`: document that YAML is the maintenance source and SQL remains the import artifact.
- Modify `docs/PROJECT_STATUS.md`: document the migration status and RAG rebuild reminder.
- Keep `scripts/knowledge_card_profiles.cjs`: Phase 1 fallback for unmigrated topics.

---

### Task 1: Add Root Tooling For YAML Generation

**Files:**
- Create: `package.json`
- Verify: `scripts/generate_knowledge_cards_sql.cjs`

- [ ] **Step 1: Create root `package.json`**

Create `package.json` at the repository root:

```json
{
  "name": "ai-study-maintenance",
  "private": true,
  "scripts": {
    "knowledge:generate": "node scripts/generate_knowledge_cards_sql.cjs",
    "knowledge:test": "node frontend/lib/knowledge-tree-coverage.node-test.cjs"
  },
  "devDependencies": {
    "yaml": "^2.7.0"
  }
}
```

- [ ] **Step 2: Install the YAML parser**

Run:

```powershell
npm install
```

Expected:

```text
added packages and audited packages without a fatal npm error
```

This should create `package-lock.json` and `node_modules/` at the repository root. Do not edit `frontend/package.json`; this dependency is for root maintenance scripts.

- [ ] **Step 3: Confirm the old generator still runs before changes**

Run:

```powershell
node scripts/generate_knowledge_cards_sql.cjs
```

Expected:

```text
Wrote 120 knowledge cards to data\knowledge_cards.sql
```

- [ ] **Step 4: Commit tooling setup**

Run:

```powershell
git add package.json package-lock.json
git commit -m "chore: add knowledge card generation tooling"
```

---

### Task 2: Add The First Topic-Level YAML File

**Files:**
- Create: `data/knowledge-cards/java-map.yml`
- Reference: `scripts/knowledge_card_profiles.cjs`
- Reference: `data/knowledge_cards.sql`

- [ ] **Step 1: Create `data/knowledge-cards/java-map.yml`**

Create the file with this exact structure. Keep answers as real interview-style content, not generation guidance:

```yaml
category: JAVA
topic: Map
baseOrder: 60
difficulty: MEDIUM
sourceName: 小林 coding, JavaGuide
sourceUrl: https://javaguide.cn/home.html
tags:
  - Java 核心
  - 集合框架
  - Map
  - HashMap
  - ConcurrentHashMap
cards:
  - title: HashMap 在 JDK 1.8 中的底层结构
    question: HashMap 在 JDK 1.8 中为什么会从数组链表结构演进为数组、链表、红黑树组合结构？
    answer: |
      HashMap 在 JDK 1.8 中的底层结构可以概括为数组、链表和红黑树。数组负责按 hash 定位桶位，链表负责处理同一个桶上的 hash 冲突，红黑树负责在冲突链过长时降低查询复杂度。

      面试里要说清楚触发条件：当某个桶里的链表长度达到树化阈值，并且数组容量已经不小于 64 时，链表才会转成红黑树；如果数组容量还比较小，HashMap 会优先扩容，因为扩容后冲突可能自然分散。红黑树不是为了替代数组，而是为了处理极端冲突场景下链表查询退化的问题。

      还要能对比 JDK 1.7。JDK 1.7 主要是数组加链表，并且扩容迁移时的头插法在并发错误使用下可能带来链表成环风险；JDK 1.8 改成尾插并引入树化，主要改善长链表查询和迁移过程的稳定性。实际项目里 HashMap 仍然不是线程安全容器，并发场景应该使用 ConcurrentHashMap 或做好外部同步。
    keyPoints:
      - JDK 1.8 HashMap 由数组、链表、红黑树组成，数组定位桶位，链表和红黑树处理冲突。
      - 链表达到树化阈值且数组容量至少为 64 时才会树化，容量不足时优先扩容。
      - 红黑树解决的是极端 hash 冲突下链表查询退化问题，不改变 HashMap 非线程安全的事实。
      - JDK 1.7 以数组加链表为主，扩容头插法在并发误用下可能出现链表成环风险。
      - JDK 1.8 的尾插和树化提升了冲突场景下的稳定性和查询效率。
    followUps:
      - 为什么链表长度达到 8 后不一定马上树化？
      - 为什么树化还要求数组容量至少为 64？
      - JDK 1.7 扩容头插法为什么可能出现链表成环？

  - title: HashMap put 流程
    question: 一个键值对放入 HashMap 时，从 hash 计算到元素落桶大致经历哪些步骤？
    answer: |
      HashMap 的 put 流程可以按定位、插入、冲突处理和扩容四步理解。首先会对 key 的 hashCode 做扰动计算，让高位信息也参与桶定位；然后用 `(n - 1) & hash` 找到数组下标。这里要求容量是 2 的幂，才能用位运算替代取模并保持分布效果。

      找到桶位后，如果桶为空，就直接放入新节点。如果桶不为空，会先比较桶头节点的 hash 和 key；命中同一个 key 就覆盖旧值，否则继续沿链表或红黑树查找。链表中找到相同 key 会覆盖，没找到就追加到链表尾部；红黑树中则按树节点规则插入或更新。

      插入后 HashMap 会检查元素数量是否超过阈值，阈值通常是容量乘以负载因子。超过后触发 resize，数组容量扩大为原来的 2 倍。面试时不要只背源码分支，还要说清楚 put 的核心风险：key 的 hash 和 equals 必须稳定，否则元素可能放进去后找不回来；并发写 HashMap 也可能导致数据覆盖或结构异常。
    keyPoints:
      - put 会先做 hash 扰动，再用 `(n - 1) & hash` 定位桶位。
      - 桶为空直接插入，桶不为空需要比较 hash 和 key 来判断覆盖还是追加。
      - 冲突结构可能是链表或红黑树，链表追加到尾部，红黑树按树节点规则处理。
      - 插入后元素数量超过阈值会触发 resize，容量通常扩为 2 倍。
      - key 的 hashCode 和 equals 必须稳定，并发写入不能直接使用普通 HashMap。
    followUps:
      - HashMap 为什么要对 hashCode 做高低位扰动？
      - put 遇到相同 key 时是怎么判断覆盖的？
      - 为什么可变对象不适合作为 HashMap 的 key？

  - title: HashMap 扩容为什么是 2 的幂
    question: HashMap 为什么要求容量保持 2 的幂，而不是任意数组长度？
    answer: |
      HashMap 容量保持 2 的幂，核心是为了让 `(n - 1) & hash` 等价于对容量取模。位运算比取模更快，同时当 n 是 2 的幂时，`n - 1` 的低位全是 1，可以更充分地利用 hash 的低位信息来定位桶。

      扩容时容量从 n 变成 2n，节点的位置也能高效迁移。JDK 1.8 不需要重新完整计算每个节点的桶下标，只要看原 hash 中新增的那一位是 0 还是 1：如果是 0，节点留在原位置；如果是 1，节点移动到原位置加旧容量的位置。这让扩容迁移更清晰，也减少了不必要的计算。

      面试里还要补一句：2 的幂不是为了保证绝对均匀，均匀性仍然依赖 key 的 hash 分布和扰动函数。容量设计只是让定位和扩容更高效。如果 key 的 hashCode 写得很差，大量 key 仍然会落到同一个桶里，最终造成链表变长或树化。
    keyPoints:
      - 容量为 2 的幂时，`(n - 1) & hash` 可以替代取模定位桶位。
      - `n - 1` 低位全是 1，可以利用 hash 低位参与分布。
      - 扩容为 2 倍后，节点只会留在原位置或移动到原位置加旧容量。
      - JDK 1.8 迁移时通过新增高位判断位置变化，不需要完整重算下标。
      - 容量设计不能弥补糟糕的 hashCode，hash 分布差仍然会导致冲突集中。
    followUps:
      - `(n - 1) & hash` 和取模在什么条件下等价？
      - 扩容后节点为什么只会有两个可能位置？
      - 如果 hashCode 实现很差，容量是 2 的幂还能解决冲突吗？

  - title: ConcurrentHashMap 如何保证线程安全
    question: ConcurrentHashMap 在 JDK 1.8 中主要靠哪些机制保证并发访问下的安全性？
    difficulty: HARD
    tags:
      - Java 核心
      - 集合框架
      - ConcurrentHashMap
      - JUC
      - CAS
    answer: |
      JDK 1.8 的 ConcurrentHashMap 不再使用 JDK 1.7 的 Segment 分段锁结构，而是采用数组、链表、红黑树加 CAS 和 synchronized 的组合。空桶插入时通常用 CAS 抢占桶位，减少加锁成本；桶位已经有节点时，会对桶头节点加 synchronized，再在这个桶内部完成链表或红黑树的更新。

      这种设计的关键是锁粒度更细。不是整张表加锁，也不是固定 Segment 加锁，而是冲突发生在哪个桶，就主要锁哪个桶。读操作多数情况下不加锁，通过 volatile 语义保证能看到节点数组和节点字段的可见性。扩容时多个线程可以协助迁移，通过控制标记和迁移进度降低单线程扩容压力。

      面试表达时要避免说成“ConcurrentHashMap 完全无锁”。它只是把不同场景拆开处理：无冲突时尽量 CAS，有冲突修改时使用桶级 synchronized，读操作依赖可见性和结构约束。它适合高并发读写的 Map 场景，但复合操作仍然需要使用 `compute`、`merge` 这类原子方法或外部同步。
    keyPoints:
      - JDK 1.8 ConcurrentHashMap 使用数组、链表、红黑树、CAS 和 synchronized。
      - 空桶插入通常用 CAS，桶内冲突更新时锁桶头节点。
      - 锁粒度集中在桶级别，不是整表锁，也不是 JDK 1.7 的固定 Segment 锁。
      - 读操作多数不加锁，依赖 volatile 可见性和节点结构约束。
      - 复合业务操作仍需使用原子方法或额外同步，不能把线程安全理解成任意组合操作安全。
    followUps:
      - JDK 1.7 的 Segment 分段锁和 JDK 1.8 桶级锁有什么区别？
      - ConcurrentHashMap 的 size 为什么比 HashMap 更复杂？
      - `get` 后再 `put` 这种复合逻辑一定线程安全吗？

  - title: LinkedHashMap 如何实现 LRU
    question: LinkedHashMap 为什么能用来实现简单 LRU 缓存，关键扩展点是什么？
    answer: |
      LinkedHashMap 在 HashMap 的基础上维护了一条双向链表，用来记录元素顺序。它可以按插入顺序维护，也可以通过 `accessOrder = true` 改成访问顺序维护。访问顺序模式下，每次 get 或 put 命中已有节点，节点都会被移动到链表尾部，链表头部就自然是最久未访问的元素。

      实现简单 LRU 时，通常继承 LinkedHashMap，构造时开启 accessOrder，然后重写 `removeEldestEntry`。当 size 超过容量上限时返回 true，LinkedHashMap 会在插入新元素后移除最老节点。这个机制适合单机、小容量、逻辑简单的本地缓存。

      需要提醒面试官的是，LinkedHashMap 本身不是线程安全容器，也没有过期时间、淘汰权重、统计指标和异步刷新能力。生产级缓存通常会使用 Caffeine、Redis 或专门缓存组件。LinkedHashMap 的价值更适合作为理解 LRU 数据结构和面试手写题的基础。
    keyPoints:
      - LinkedHashMap 在 HashMap 基础上维护双向链表记录顺序。
      - `accessOrder = true` 后，访问过的节点会移动到链表尾部。
      - 链表头部代表最久未访问元素，可以作为淘汰对象。
      - 重写 `removeEldestEntry` 可以在容量超过上限时自动淘汰旧节点。
      - LinkedHashMap 不适合作为完整生产缓存，线程安全和缓存治理能力都有限。
    followUps:
      - accessOrder 为 false 和 true 时链表顺序有什么区别？
      - 为什么 LRU 淘汰的是链表头而不是链表尾？
      - LinkedHashMap 实现的 LRU 和 Caffeine 这类缓存库差在哪里？
```

- [ ] **Step 2: Confirm the YAML contains exactly five cards**

Run:

```powershell
Select-String -Path data\knowledge-cards\java-map.yml -Pattern "  - title:" | Measure-Object
```

Expected:

```text
Count    : 5
```

- [ ] **Step 3: Commit the first YAML topic**

Run:

```powershell
git add data/knowledge-cards/java-map.yml
git commit -m "chore: add YAML source for Java Map knowledge cards"
```

---

### Task 3: Refactor The Generator To YAML-First Hybrid Mode

**Files:**
- Modify: `scripts/generate_knowledge_cards_sql.cjs`
- Read: `data/knowledge-cards/java-map.yml`
- Keep: `scripts/knowledge_card_profiles.cjs`
- Output: `data/knowledge_cards.sql`

- [ ] **Step 1: Replace the generator with a YAML-first hybrid implementation**

Replace `scripts/generate_knowledge_cards_sql.cjs` with this implementation:

```js
const fs = require("node:fs");
const path = require("node:path");
const YAML = require("yaml");

const repoRoot = path.resolve(__dirname, "..");
const outputPath = path.join(repoRoot, "data", "knowledge_cards.sql");
const yamlDir = path.join(repoRoot, "data", "knowledge-cards");
const cardProfiles = require("./knowledge_card_profiles.cjs");

const allowedCategories = new Set(["JAVA", "JVM", "SPRING", "MYSQL", "REDIS", "AI"]);
const forbiddenTemplatePhrases = [
  "如果面试官继续追问",
  "回答要回到前面的机制",
  "不要另起一个无关话题",
  "推荐表达顺序",
  "项目落点",
  "讲成一次可追问的诊断思路",
  "不是孤立背诵",
];

const sources = {
  JAVA: ["小林 coding, JavaGuide", "https://javaguide.cn/home.html"],
  JVM: ["小林 coding, JavaGuide", "https://javaguide.cn/java/jvm/"],
  MYSQL: ["小林 coding, JavaGuide", "https://xiaolincoding.com/mysql/"],
  REDIS: ["小林 coding, JavaGuide", "https://xiaolincoding.com/redis/"],
  SPRING: ["JavaGuide", "https://javaguide.cn/system-design/framework/spring/"],
  AI: ["项目原创整理", ""],
};

const leaves = [
  { category: "JAVA", baseOrder: 10, tag: "面向对象", difficulty: "EASY", titles: ["封装、继承、多态的面试表达", "接口和抽象类如何取舍", "重载与重写的区别", "Java 对象创建过程", "组合优于继承的原因"] },
  { category: "JAVA", baseOrder: 20, tag: "数据类型", difficulty: "EASY", titles: ["基本类型和包装类型区别", "自动装箱与拆箱风险", "String 为什么不可变", "StringBuilder 和 StringBuffer 区别", "BigDecimal 为什么适合金额计算"] },
  { category: "JAVA", baseOrder: 30, tag: "异常处理", difficulty: "MEDIUM", titles: ["Checked Exception 和 RuntimeException 区别", "try-catch-finally 执行顺序", "业务异常如何设计", "异常被吞掉的排查思路", "全局异常处理的分层边界"] },
  { category: "JAVA", baseOrder: 40, tag: "反射与泛型", difficulty: "MEDIUM", titles: ["反射的使用场景和代价", "泛型擦除是什么", "Class 对象和类加载关系", "注解如何配合反射工作", "泛型通配符 extends 和 super 区别"] },
  { category: "JAVA", baseOrder: 50, tag: "List", difficulty: "EASY", titles: ["ArrayList 和 LinkedList 的区别", "ArrayList 扩容机制", "ArrayList 删除元素的坑", "CopyOnWriteArrayList 适用场景", "List 遍历时修改为什么会失败"] },
  { category: "JAVA", baseOrder: 60, tag: "Map", difficulty: "MEDIUM", titles: ["HashMap 在 JDK 1.8 中的底层结构", "HashMap put 流程", "HashMap 扩容为什么是 2 的幂", "ConcurrentHashMap 如何保证线程安全", "LinkedHashMap 如何实现 LRU"] },
  { category: "JAVA", baseOrder: 70, tag: "Set", difficulty: "EASY", titles: ["HashSet 如何保证元素唯一", "TreeSet 的排序和去重规则", "LinkedHashSet 如何保持插入顺序", "Set 中可变对象为什么危险", "HashSet 和 ConcurrentHashMap.newKeySet 的区别"] },
  { category: "JAVA", baseOrder: 80, tag: "JUC", difficulty: "MEDIUM", titles: ["synchronized 和 ReentrantLock 的区别", "ThreadLocal 使用场景和风险", "volatile 能保证什么", "线程池核心参数如何设置", "CompletableFuture 适合什么场景"] },
  { category: "JVM", baseOrder: 100, tag: "JVM", difficulty: "MEDIUM", titles: ["JVM 运行时内存区域", "CMS 和 G1 垃圾收集器区别", "类加载过程和双亲委派", "对象从创建到回收的过程", "线上 Full GC 如何排查"] },
  { category: "MYSQL", baseOrder: 200, tag: "索引", difficulty: "HARD", titles: ["MySQL 索引失效场景及优化", "InnoDB 为什么使用 B+ 树索引", "联合索引最左前缀原则", "覆盖索引和回表", "Explain 执行计划怎么看"] },
  { category: "MYSQL", baseOrder: 210, tag: "事务", difficulty: "HARD", titles: ["事务 ACID 如何理解", "隔离级别解决哪些问题", "可重复读为什么还可能有当前读", "事务传播到业务代码的边界", "长事务会带来什么风险"] },
  { category: "MYSQL", baseOrder: 220, tag: "锁", difficulty: "HARD", titles: ["行锁、表锁和间隙锁区别", "Next-Key Lock 解决什么问题", "死锁如何定位和避免", "乐观锁和悲观锁怎么选", "select for update 使用边界"] },
  { category: "MYSQL", baseOrder: 230, tag: "MVCC", difficulty: "HARD", titles: ["MVCC 是什么", "Read View 如何判断版本可见", "undo log 和版本链关系", "快照读和当前读区别", "MVCC 为什么不能完全替代锁"] },
  { category: "REDIS", baseOrder: 300, tag: "数据结构", difficulty: "MEDIUM", titles: ["Redis 为什么快", "String 的典型使用场景", "Hash 结构适合存什么", "List、Set、ZSet 如何选择", "大 key 会带来什么问题"] },
  { category: "REDIS", baseOrder: 310, tag: "缓存问题", difficulty: "MEDIUM", titles: ["Redis 缓存击穿、穿透与雪崩", "缓存与数据库一致性", "热点 key 如何治理", "布隆过滤器解决什么问题", "缓存预热和降级怎么设计"] },
  { category: "REDIS", baseOrder: 320, tag: "持久化", difficulty: "MEDIUM", titles: ["Redis RDB 和 AOF 持久化", "AOF 重写解决什么问题", "RDB 快照触发方式", "混合持久化的价值", "Redis 宕机恢复如何取舍"] },
  { category: "REDIS", baseOrder: 330, tag: "分布式锁", difficulty: "HARD", titles: ["Redis 分布式锁基本实现", "SET NX EX 为什么要原子", "锁续期和看门狗机制", "Redisson 分布式锁原理", "Redis 锁误删如何避免"] },
  { category: "SPRING", baseOrder: 400, tag: "IOC", difficulty: "MEDIUM", titles: ["Spring Bean 生命周期", "IOC 和依赖注入的关系", "BeanFactory 和 ApplicationContext 区别", "循环依赖三级缓存", "BeanPostProcessor 扩展点"] },
  { category: "SPRING", baseOrder: 410, tag: "AOP", difficulty: "MEDIUM", titles: ["Spring AOP 实现原理", "JDK 动态代理和 CGLIB 区别", "同类内部调用为什么绕过 AOP", "切点和通知如何组织", "AOP 适合哪些横切逻辑"] },
  { category: "SPRING", baseOrder: 420, tag: "事务", difficulty: "HARD", titles: ["Spring 事务失效场景", "事务传播行为怎么理解", "rollbackFor 什么时候需要配置", "声明式事务和编程式事务区别", "事务边界如何设计"] },
  { category: "SPRING", baseOrder: 430, tag: "Spring MVC", difficulty: "MEDIUM", titles: ["Spring MVC 请求处理流程", "DispatcherServlet 的职责", "HandlerMapping 和 HandlerAdapter 区别", "参数解析和返回值处理", "拦截器和过滤器区别"] },
  { category: "AI", baseOrder: 500, tag: "Agent", difficulty: "MEDIUM", titles: ["Agent 工作流为什么要拆成 Planner、Tool 和 Observation", "Tool Calling 的工程边界", "Agent Step Trace 如何帮助排查", "Agent 失败降级怎么设计", "Memory 在面试训练中的作用"] },
  { category: "AI", baseOrder: 510, tag: "RAG", difficulty: "MEDIUM", titles: ["RAG 在当前项目中为什么作为 Agent 内部 Tool", "RAG 检索结果为什么只是证据", "用户记忆检索为什么必须隔离", "MySQL 结构化 RAG 的取舍", "RAG 失败为什么不能阻塞诊断"] },
  { category: "AI", baseOrder: 520, tag: "LangChain", difficulty: "HARD", titles: ["LangChain 和本项目自定义 Agent 编排有什么区别", "Chain、Tool、Memory 分别解决什么问题", "为什么 MVP 阶段不强依赖 LangChain", "LangChain 接入 Spring Boot 的边界", "LCEL 表达式适合什么场景"] },
];

function sql(value) {
  if (value === null || value === undefined || value === "") return "NULL";
  return `'${String(value).replace(/'/g, "''")}'`;
}

function defaultTags(leaf) {
  const common = {
    JAVA: "Java 核心",
    JVM: "JVM",
    MYSQL: "MySQL",
    REDIS: "Redis",
    SPRING: "Spring",
    AI: "AI 工程",
  }[leaf.category];
  const extras = {
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

function normalizeTags(value) {
  if (Array.isArray(value)) return value.map(String).map((item) => item.trim()).filter(Boolean).join(",");
  if (typeof value === "string") return value.split(",").map((item) => item.trim()).filter(Boolean).join(",");
  throw new Error("tags must be an array or comma-separated string");
}

function requireString(card, field, sourceLabel) {
  if (typeof card[field] !== "string" || card[field].trim() === "") {
    throw new Error(`${sourceLabel}: ${field} is required`);
  }
  return card[field].trim();
}

function requireList(card, field, sourceLabel, minLength) {
  if (!Array.isArray(card[field]) || card[field].length < minLength) {
    throw new Error(`${sourceLabel}: ${field} must contain at least ${minLength} items`);
  }
  return card[field].map((item) => String(item).trim()).filter(Boolean);
}

function hasChineseLength(value, minLength) {
  return Array.from(value.replace(/\s/g, "")).length >= minLength;
}

function validateCard(card, sourceLabel) {
  if (!allowedCategories.has(card.category)) {
    throw new Error(`${sourceLabel}: invalid category ${card.category}`);
  }
  requireString(card, "title", sourceLabel);
  requireString(card, "question", sourceLabel);
  const answer = requireString(card, "answer", sourceLabel);
  requireList(card, "keyPoints", sourceLabel, 4);
  requireList(card, "followUps", sourceLabel, 3);
  if (!hasChineseLength(answer, 300)) {
    throw new Error(`${sourceLabel}: answer must contain at least 300 non-space characters`);
  }
  if (/^请结合后端面试场景解释：.+？$/.test(card.question)) {
    throw new Error(`${sourceLabel}: question must not use the old generic template`);
  }
  const combined = [card.question, card.answer, ...card.keyPoints, ...card.followUps].join("\n");
  for (const phrase of forbiddenTemplatePhrases) {
    if (combined.includes(phrase)) {
      throw new Error(`${sourceLabel}: forbidden template phrase "${phrase}"`);
    }
  }
}

function readYamlCards() {
  if (!fs.existsSync(yamlDir)) return [];
  return fs.readdirSync(yamlDir)
    .filter((file) => file.endsWith(".yml") || file.endsWith(".yaml"))
    .sort()
    .flatMap((file) => {
      const filePath = path.join(yamlDir, file);
      const topic = YAML.parse(fs.readFileSync(filePath, "utf8"));
      if (!topic || typeof topic !== "object") throw new Error(`${file}: topic YAML must be an object`);
      if (!allowedCategories.has(topic.category)) throw new Error(`${file}: invalid category ${topic.category}`);
      if (!Array.isArray(topic.cards) || topic.cards.length === 0) throw new Error(`${file}: cards must be a non-empty array`);
      return topic.cards.map((item, index) => {
        const card = {
          category: item.category || topic.category,
          title: item.title,
          question: item.question,
          answer: item.answer,
          followUps: item.followUps,
          keyPoints: item.keyPoints,
          difficulty: item.difficulty || topic.difficulty,
          tags: normalizeTags(item.tags || topic.tags),
          sourceName: item.sourceName ?? topic.sourceName,
          sourceUrl: item.sourceUrl ?? topic.sourceUrl,
          sortOrder: Number(item.sortOrder ?? topic.baseOrder + index),
          topic: topic.topic,
          source: "yaml",
        };
        validateCard(card, `${file}:${card.title || index}`);
        return card;
      });
    });
}

function legacyCards(excludedTitles) {
  return leaves.flatMap((leaf) =>
    leaf.titles.map((title, index) => {
      if (excludedTitles.has(title)) return null;
      const [sourceName, sourceUrl] = sources[leaf.category];
      const cardProfile = cardProfiles[title];
      if (!cardProfile) throw new Error(`Missing explicit knowledge card profile: ${title}`);
      const card = {
        category: leaf.category,
        title,
        question: cardProfile.question,
        answer: cardProfile.answer,
        followUps: cardProfile.followUps,
        keyPoints: cardProfile.keyPoints,
        difficulty: leaf.difficulty,
        tags: defaultTags(leaf),
        sourceName,
        sourceUrl,
        sortOrder: leaf.baseOrder + index,
        topic: leaf.tag,
        source: "legacy",
      };
      validateCard(card, `legacy:${title}`);
      return card;
    }).filter(Boolean)
  );
}

function validateAllCards(cards) {
  if (cards.length !== 120) throw new Error(`Expected exactly 120 cards, got ${cards.length}`);
  const byTitle = new Map();
  const bySortOrder = new Map();
  const byTopic = new Map();
  for (const card of cards) {
    const titleKey = `${card.category}:${card.title}`;
    if (byTitle.has(titleKey)) throw new Error(`Duplicate title in category: ${titleKey}`);
    byTitle.set(titleKey, card);
    if (bySortOrder.has(card.sortOrder)) throw new Error(`Duplicate sortOrder ${card.sortOrder}: ${card.title}`);
    bySortOrder.set(card.sortOrder, card);
    const topicKey = `${card.category}:${card.topic}`;
    byTopic.set(topicKey, (byTopic.get(topicKey) || 0) + 1);
  }
  if (byTopic.size !== 24) throw new Error(`Expected 24 final topics, got ${byTopic.size}`);
  for (const [topicKey, count] of byTopic.entries()) {
    if (count < 5) throw new Error(`${topicKey} has only ${count} cards`);
  }
}

const yamlCards = readYamlCards();
const yamlTitles = new Set(yamlCards.map((card) => card.title));
const cards = [...yamlCards, ...legacyCards(yamlTitles)].sort((a, b) => a.sortOrder - b.sortOrder || a.title.localeCompare(b.title));
validateAllCards(cards);

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
  sql(card.followUps.join("\n")),
  sql(card.keyPoints.join("\n")),
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
console.log(`Wrote ${cards.length} knowledge cards to ${path.relative(repoRoot, outputPath)} (${yamlCards.length} YAML, ${cards.length - yamlCards.length} legacy)`);
```

- [ ] **Step 2: Run the hybrid generator**

Run:

```powershell
node scripts/generate_knowledge_cards_sql.cjs
```

Expected:

```text
Wrote 120 knowledge cards to data\knowledge_cards.sql (5 YAML, 115 legacy)
```

- [ ] **Step 3: Confirm SQL compatibility**

Run:

```powershell
Select-String -Path data\knowledge_cards.sql -Pattern "INSERT INTO knowledge_card"
Select-String -Path data\knowledge_cards.sql -Pattern "\(category, title, question, answer, follow_up, key_points, difficulty, tags, source_name, source_url, enabled, sort_order, created_at, updated_at\)"
```

Expected: both commands print matching lines from `data/knowledge_cards.sql`.

- [ ] **Step 4: Commit hybrid generator**

Run:

```powershell
git add scripts/generate_knowledge_cards_sql.cjs data/knowledge_cards.sql
git commit -m "chore: generate knowledge cards from YAML first"
```

---

### Task 4: Update Coverage Tests For YAML-First Source

**Files:**
- Modify: `frontend/lib/knowledge-tree-coverage.node-test.cjs`

- [ ] **Step 1: Update source checks**

In `frontend/lib/knowledge-tree-coverage.node-test.cjs`, replace the `profileSource` file read with a YAML directory read:

```js
const yamlSourceDir = path.join(repoRoot, "data", "knowledge-cards");
const yamlSourceFiles = fs.existsSync(yamlSourceDir)
  ? fs.readdirSync(yamlSourceDir).filter((file) => file.endsWith(".yml") || file.endsWith(".yaml"))
  : [];
const profileSourcePath = path.join(repoRoot, "scripts", "knowledge_card_profiles.cjs");
const profileSource = fs.existsSync(profileSourcePath)
  ? fs.readFileSync(profileSourcePath, "utf8")
  : "";
```

- [ ] **Step 2: Replace the profile-only test**

Replace the test named `"knowledge generator requires explicit card profiles for every seeded card"` with:

```js
test("knowledge generator uses YAML-first sources with legacy fallback during migration", () => {
  const cards = parseKnowledgeCardsSql();

  assert.match(generatorSource, /readYamlCards/);
  assert.match(generatorSource, /legacyCards/);
  assert.match(generatorSource, /validateAllCards/);
  assert.match(generatorSource, /forbiddenTemplatePhrases/);
  assert.match(generatorSource, /Expected exactly 120 cards/);
  assert.ok(yamlSourceFiles.includes("java-map.yml"), "java-map.yml should be the first migrated YAML topic");
  assert.match(profileSource, /const cardProfiles\s*=/, "legacy profiles remain during Phase 1 migration");

  for (const title of [
    "HashMap 在 JDK 1.8 中的底层结构",
    "HashMap put 流程",
    "HashMap 扩容为什么是 2 的幂",
    "ConcurrentHashMap 如何保证线程安全",
    "LinkedHashMap 如何实现 LRU",
  ]) {
    const card = cards.find((item) => item.title === title);
    assert.ok(card, `${title} should still be generated`);
    assert.doesNotMatch(card.question, /^请结合后端面试场景解释：.+？$/);
  }
});
```

- [ ] **Step 3: Run the coverage test**

Run:

```powershell
node frontend/lib/knowledge-tree-coverage.node-test.cjs
```

Expected:

```text
# pass
```

If one of the Map card wording tests fails, fix only `data/knowledge-cards/java-map.yml` and regenerate SQL. Do not loosen the content-quality tests.

- [ ] **Step 4: Commit test updates**

Run:

```powershell
git add frontend/lib/knowledge-tree-coverage.node-test.cjs data/knowledge_cards.sql
git commit -m "test: cover YAML-first knowledge card generation"
```

---

### Task 5: Document Phase 1 Maintenance Flow

**Files:**
- Modify: `docs/API.md`
- Modify: `docs/PROJECT_STATUS.md`

- [ ] **Step 1: Update `docs/API.md` knowledge data paragraph**

Find the paragraph that starts with `真实数据由 data/knowledge_cards.sql 提供 120 张结构化知识卡` and replace it with:

```markdown
真实运行数据仍由 `knowledge_card` 表提供，`data/knowledge_cards.sql` 仍是数据库导入产物。知识卡维护源正在从 `scripts/knowledge_card_profiles.cjs` 迁移到 `data/knowledge-cards/*.yml`：Phase 1 采用 YAML-first + legacy profiles fallback，已迁移专题优先读取 YAML，未迁移专题继续由旧 profiles 生成；Phase 2 全部 24 个专题迁移完成后，生成器将切换为 YAML-only。当前目标规模仍是 120 张结构化知识卡，侧边栏 24 个最终专题每个至少 5 张。内容参考小林 coding 和 JavaGuide 的公开面试知识目录做选题覆盖后重新整理，AI 工程内容按本项目 Agent/RAG 设计原创组织，不复制原文。`/knowledge` 页面本身不是 RAG 查询入口；这些知识卡会被 RAG V1 索引为 Agent 诊断和训练计划的内部证据来源。
```

- [ ] **Step 2: Update `docs/PROJECT_STATUS.md` knowledge data import risk**

In the knowledge data import dependency section, add this sentence after the existing import instructions:

```markdown
知识卡内容维护源采用专题级 YAML 迁移策略：Phase 1 保留 `scripts/knowledge_card_profiles.cjs` 作为未迁移专题 fallback；导入重新生成的 `data/knowledge_cards.sql` 后，需要确认 `RagService.rebuildSystemIndex()` 可重新索引最新知识卡，避免 RAG chunk 仍停留在旧内容。
```

- [ ] **Step 3: Run documentation grep**

Run:

```powershell
rg -n "knowledge-cards|YAML-first|RagService.rebuildSystemIndex|knowledge_card_profiles" docs\API.md docs\PROJECT_STATUS.md
```

Expected: output includes both updated docs.

- [ ] **Step 4: Commit docs**

Run:

```powershell
git add docs/API.md docs/PROJECT_STATUS.md
git commit -m "docs: document YAML knowledge card maintenance flow"
```

---

### Task 6: Phase 1 Verification

**Files:**
- Verify: `scripts/generate_knowledge_cards_sql.cjs`
- Verify: `data/knowledge_cards.sql`
- Verify: `frontend/lib/knowledge-tree-coverage.node-test.cjs`
- Verify: `backend/src/test/java/com/interview/coach/service/impl/RagServiceImplTest.java`

- [ ] **Step 1: Regenerate SQL**

Run:

```powershell
node scripts/generate_knowledge_cards_sql.cjs
```

Expected:

```text
Wrote 120 knowledge cards to data\knowledge_cards.sql (5 YAML, 115 legacy)
```

- [ ] **Step 2: Run knowledge coverage**

Run:

```powershell
node frontend/lib/knowledge-tree-coverage.node-test.cjs
```

Expected:

```text
# pass
```

- [ ] **Step 3: Run backend RAG unit verification**

Run:

```powershell
cd backend
.\mvnw test -Dtest=RagServiceImplTest
```

Expected:

```text
BUILD SUCCESS
```

This verifies the existing `rebuildSystemIndex()` behavior still preserves user-memory chunks and indexes enabled knowledge cards through the existing service boundary.

- [ ] **Step 4: Inspect generated SQL for YAML-sourced Map questions**

Run:

```powershell
Select-String -Path data\knowledge_cards.sql -Pattern "HashMap 在 JDK 1.8 中为什么会从数组链表结构演进"
Select-String -Path data\knowledge_cards.sql -Pattern "请结合后端面试场景解释：HashMap"
```

Expected: first command prints a match; second command prints no Map-card generic question matches.

---

### Task 7: Phase 2 Follow-Up Plan For Full Migration

**Files:**
- Create remaining files under: `data/knowledge-cards/*.yml`
- Modify: `scripts/generate_knowledge_cards_sql.cjs`
- Modify: `frontend/lib/knowledge-tree-coverage.node-test.cjs`
- Remove or archive: `scripts/knowledge_card_profiles.cjs`

- [ ] **Step 1: Migrate remaining 23 topic files**

Create one YAML file for each remaining final topic:

```text
data/knowledge-cards/java-oop.yml
data/knowledge-cards/java-type.yml
data/knowledge-cards/java-exception.yml
data/knowledge-cards/java-reflection-generics.yml
data/knowledge-cards/java-list.yml
data/knowledge-cards/java-set.yml
data/knowledge-cards/java-juc.yml
data/knowledge-cards/jvm-core.yml
data/knowledge-cards/mysql-index.yml
data/knowledge-cards/mysql-transaction.yml
data/knowledge-cards/mysql-lock.yml
data/knowledge-cards/mysql-mvcc.yml
data/knowledge-cards/redis-structure.yml
data/knowledge-cards/redis-cache.yml
data/knowledge-cards/redis-persistence.yml
data/knowledge-cards/redis-lock.yml
data/knowledge-cards/spring-ioc.yml
data/knowledge-cards/spring-aop.yml
data/knowledge-cards/spring-transaction.yml
data/knowledge-cards/spring-mvc.yml
data/knowledge-cards/ai-agent.yml
data/knowledge-cards/ai-rag.yml
data/knowledge-cards/ai-langchain.yml
```

Each file must follow the same schema as `java-map.yml`, contain exactly 5 cards, and pass the generator quality checks.

- [ ] **Step 2: Switch generator to YAML-only**

After all 24 topic files exist and generation reports `120 YAML, 0 legacy`, remove the `cardProfiles` require and the `legacyCards()` fallback from `scripts/generate_knowledge_cards_sql.cjs`. Keep `validateAllCards(cards)` unchanged.

- [ ] **Step 3: Archive or delete legacy profiles**

Rename:

```powershell
Move-Item scripts\knowledge_card_profiles.cjs scripts\knowledge_card_profiles.backup.cjs
```

If the project owner prefers a smaller repo, delete it in a separate commit after confirming `node scripts/generate_knowledge_cards_sql.cjs` no longer requires it.

- [ ] **Step 4: Tighten tests to YAML-only**

Update `frontend/lib/knowledge-tree-coverage.node-test.cjs` so it asserts:

```js
assert.equal(yamlSourceFiles.length, 24, "all 24 final knowledge topics should be YAML files");
assert.doesNotMatch(generatorSource, /legacyCards/);
assert.doesNotMatch(generatorSource, /knowledge_card_profiles/);
```

- [ ] **Step 5: Final full verification**

Run:

```powershell
node scripts/generate_knowledge_cards_sql.cjs
node frontend/lib/knowledge-tree-coverage.node-test.cjs
cd backend
.\mvnw test -Dtest=RagServiceImplTest
```

Expected:

```text
Wrote 120 knowledge cards to data\knowledge_cards.sql (120 YAML, 0 legacy)
# pass
BUILD SUCCESS
```

---

## RAG Rebuild Note For Local Demo Databases

After importing a regenerated `data/knowledge_cards.sql` into MySQL, rebuild system RAG chunks before demo replay. The code path is `RagService.rebuildSystemIndex()`, which indexes enabled `knowledge_card` rows and problem rows while preserving user-memory chunks. This plan does not add a public REST endpoint for that operation; run it through the existing local maintenance path or a controlled test/admin hook already used in the project.

## Self-Review

- Spec coverage: the plan keeps runtime unchanged, adds topic YAML, supports default fields and single-card overrides, preserves SQL compatibility, retains Phase 1 fallback, adds quality validation, updates tests/docs, and includes RAG rebuild verification.
- Placeholder scan: no task depends on unfinished marker text, unspecified files, or unspecified commands.
- Type consistency: YAML fields use `followUps` and `keyPoints`; SQL fields remain `follow_up` and `key_points`; `tags` are arrays in YAML and comma-separated strings in SQL.
