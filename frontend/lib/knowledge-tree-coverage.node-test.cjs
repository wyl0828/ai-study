const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");
const ts = require("typescript");

require.extensions[".ts"] = function loadTs(module, filename) {
  const source = fs.readFileSync(filename, "utf8");
  const output = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2020,
      esModuleInterop: true,
    },
  }).outputText;
  module._compile(output, filename);
};

const frontendRoot = path.resolve(__dirname, "..");
const repoRoot = path.resolve(frontendRoot, "..");
const {
  buildKnowledgeOutline,
  getSelectionBreadcrumb,
  knowledgeTopics,
  matchKnowledgeTopic,
  selectionKey,
} = require("./knowledgeData.ts");

const generatorSource = fs.readFileSync(
  path.join(repoRoot, "scripts", "generate_knowledge_cards_sql.cjs"),
  "utf8"
);
const profileSource = fs.readFileSync(
  path.join(repoRoot, "scripts", "knowledge_card_profiles.cjs"),
  "utf8"
);

const leafSelections = [
  { domain: "Java 核心", section: "Java 基础", topic: "面向对象" },
  { domain: "Java 核心", section: "Java 基础", topic: "数据类型" },
  { domain: "Java 核心", section: "Java 基础", topic: "异常处理" },
  { domain: "Java 核心", section: "Java 基础", topic: "反射与泛型" },
  { domain: "Java 核心", section: "集合框架", topic: "List" },
  { domain: "Java 核心", section: "集合框架", topic: "Map" },
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
  { domain: "Spring", topic: "IOC" },
  { domain: "Spring", topic: "AOP" },
  { domain: "Spring", topic: "事务" },
  { domain: "Spring", topic: "Spring MVC" },
  { domain: "AI 工程", topic: "Agent" },
  { domain: "AI 工程", topic: "RAG" },
  { domain: "AI 工程", topic: "LangChain" },
];

function splitSqlValues(row) {
  const values = [];
  let current = "";
  let quoted = false;
  for (let index = 0; index < row.length; index += 1) {
    const char = row[index];
    const next = row[index + 1];
    if (char === "'" && next === "'") {
      current += "'";
      index += 1;
      continue;
    }
    if (char === "'") {
      quoted = !quoted;
      continue;
    }
    if (char === "," && !quoted) {
      values.push(current.trim());
      current = "";
      continue;
    }
    current += char;
  }
  values.push(current.trim());
  return values;
}

function parseKnowledgeCardsSql() {
  const sql = fs.readFileSync(path.join(repoRoot, "data", "knowledge_cards.sql"), "utf8");
  const valuesStart = sql.indexOf("VALUES");
  assert.ok(valuesStart >= 0, "knowledge_cards.sql should contain INSERT VALUES");
  const valuesSql = sql.slice(valuesStart + "VALUES".length);
  const rows = [];
  let current = "";
  let quoted = false;
  let depth = 0;

  for (let index = 0; index < valuesSql.length; index += 1) {
    const char = valuesSql[index];
    const next = valuesSql[index + 1];
    if (char === "'" && next === "'") {
      current += char + next;
      index += 1;
      continue;
    }
    if (char === "'") {
      quoted = !quoted;
      current += char;
      continue;
    }
    if (!quoted && char === "(") {
      if (depth > 0) current += char;
      depth += 1;
      continue;
    }
    if (!quoted && char === ")") {
      depth -= 1;
      if (depth === 0) {
        rows.push(splitSqlValues(current));
        current = "";
        continue;
      }
    }
    if (depth > 0) {
      current += char;
    }
  }

  return rows.map((values) => ({
    category: {
      JAVA: "Java",
      JVM: "JVM",
      SPRING: "Spring",
      MYSQL: "MySQL",
      REDIS: "Redis",
      AI: "AI",
    }[values[0]] || values[0],
    title: values[1],
    question: values[2],
    referenceAnswer: values[3],
    followUpQuestions: values[4].split(/\r?\n/).filter(Boolean),
    keyPoints: values[5].split(/\r?\n/).filter(Boolean),
    difficulty: values[6],
    tags: values[7].split(",").map((tag) => tag.trim()).filter(Boolean),
  }));
}

test("knowledge SQL seed gives every final outline topic at least five detailed cards", () => {
  const cards = parseKnowledgeCardsSql();
  const genericTemplatePattern =
    /可以这样答|展开讲|需要注意的是|关键触发条件是什么|同类方案相比|规避这个坑|追问时补充|如果线上出现和 .+ 相关的问题|如果面试官继续追问|回答要回到前面的机制|不要另起一个无关话题|先说明发生条件|如果继续追到项目场景|如果继续追到线上排查|如果继续追到|展开时先把核心机制讲完整|落到后端项目里|真实调用链|数据流转|从三个层面说明|先讲背景问题|体现工程思维|面试官会认可|这样回答能让面试官看到/;
  const genericQuestionPattern =
    /面试官问到|面试官问「|面试官追问|面试官让你|你会如何结合后端项目说明|你会从哪些维度说明|你会怎样先讲背景问题|你会按哪些阶段梳理/;
  const genericKeyPointPattern = /理解机制|结合项目|注意边界|项目场景|工程思维/;

  assert.ok(cards.length >= 120, `expected at least 120 cards, got ${cards.length}`);

  for (const selection of leafSelections) {
    const matches = cards.filter((card) => matchKnowledgeTopic(card, selection));
    assert.ok(
      matches.length >= 5,
      `${selection.domain}/${selection.section || ""}/${selection.topic || ""} has ${matches.length} cards`
    );
  }

  for (const card of cards) {
    assert.doesNotMatch(
      card.question,
      /^请结合后端面试场景解释：.+？$/,
      `${card.title} question should read like a real interviewer prompt`
    );
    assert.ok(
      !card.referenceAnswer.includes(card.question),
      `${card.title} answer should not echo the whole question`
    );
    if (card.keyPoints[0] && card.keyPoints[0].length >= 30) {
      assert.ok(
        !card.question.includes(card.keyPoints[0].slice(0, 30)),
        `${card.title} question should not quote the first answer key point`
      );
    }
    assert.ok(card.referenceAnswer.length >= 120, `${card.title} answer is too short`);
    assert.ok(
      (card.referenceAnswer.match(/[。？！?]/g) || []).length >= 3,
      `${card.title} answer should not be only one or two compressed sentences`
    );
    assert.doesNotMatch(
      card.referenceAnswer,
      /推荐表达顺序|项目落点|第 \d+ 张卡|讲成一次可追问的诊断思路|不是孤立背诵/,
      `${card.title} answer should read like an interview answer, not card-generation guidance`
    );
    assert.match(
      card.referenceAnswer,
      /\n\n/,
      `${card.title} answer should keep paragraph breaks for readable analysis`
    );
    assert.doesNotMatch(
      [card.referenceAnswer, ...card.keyPoints, ...card.followUpQuestions].join("\n"),
      genericTemplatePattern,
      `${card.title} should not contain generic generated templates`
    );
    assert.doesNotMatch(
      card.question,
      genericQuestionPattern,
      `${card.title} question should be a direct interview question`
    );
    assert.doesNotMatch(
      card.question,
      /如何.+是什么|怎么.+是什么|如何理解怎么理解|怎么理解怎么理解|为什么.+怎么理解|什么.+是什么/,
      `${card.title} question should not be mechanically rewritten`
    );
    assert.ok(card.keyPoints.length >= 3, `${card.title} needs at least three key points`);
    assert.ok(card.followUpQuestions.length >= 2, `${card.title} needs at least two follow-ups`);
    for (const point of card.keyPoints) {
      assert.doesNotMatch(point, genericKeyPointPattern, `${card.title} key point is too vague`);
    }
  }
});

test("knowledge generator requires explicit card profiles for every seeded card", () => {
  const cards = parseKnowledgeCardsSql();

  assert.match(generatorSource, /const cardProfiles\s*=/);
  assert.match(profileSource, /const cardProfiles\s*=/);
  assert.match(profileSource, /function profile\(question,\s*answer,\s*keyPoints,\s*followUps\)/);
  assert.doesNotMatch(profileSource, /function profile\(keyPoints,\s*followUps\)/);
  assert.doesNotMatch(profileSource, /function profile\(answer,\s*keyPoints,\s*followUps\)/);
  assert.doesNotMatch(profileSource, /function enrichAnswer/);
  assert.doesNotMatch(profileSource, /answer:\s*enrichAnswer/);
  assert.doesNotMatch(profileSource, /profile\(\s*\[/);
  assert.doesNotMatch(profileSource, /function detailShortAnswers|function closingFor|function cleanPoint/);
  assert.doesNotMatch(generatorSource, /function question\s*\(/);
  assert.doesNotMatch(generatorSource, /function keyPoints\s*\(/);
  assert.doesNotMatch(generatorSource, /function followUps\s*\(/);

  for (const card of cards) {
    assert.match(
      profileSource,
      new RegExp(`["']${card.title.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}["']\\s*:`),
      `${card.title} must have an explicit card profile`
    );
  }
});

test("HashMap structure card stays focused on JDK 1.8 bottom-level structure", () => {
  const cards = parseKnowledgeCardsSql();
  const card = cards.find((item) => item.title === "HashMap 在 JDK 1.8 中的底层结构");
  assert.ok(card, "HashMap structure card should exist");

  assert.match(card.referenceAnswer, /put/);
  assert.match(card.referenceAnswer, /JDK 1\.7/);
  assert.match(card.referenceAnswer, /头插法|尾插/);
  assert.match(card.referenceAnswer, /链表成环|长链表/);

  const keyPointText = card.keyPoints.join("\n");
  const followUpText = card.followUpQuestions.join("\n");

  assert.match(keyPointText, /数组/);
  assert.match(keyPointText, /链表/);
  assert.match(keyPointText, /红黑树/);
  assert.match(keyPointText, /树化/);
  assert.match(keyPointText, /64/);
  assert.doesNotMatch(keyPointText, /ConcurrentHashMap|CAS|synchronized|分段/);

  assert.match(followUpText, /阈值.*8|8.*阈值/);
  assert.match(followUpText, /64|扩容/);
  assert.match(followUpText, /JDK 1\.7|1\.7/);
  assert.match(followUpText, /退化|链表/);
});

test("collection cards follow interview-note style instead of short summaries", () => {
  const cards = parseKnowledgeCardsSql();
  const byTitle = new Map(cards.map((card) => [card.title, card]));
  const arrayList = byTitle.get("ArrayList 扩容机制");
  const listDiff = byTitle.get("ArrayList 和 LinkedList 的区别");
  const hashSet = byTitle.get("HashSet 如何保证元素唯一");

  assert.ok(arrayList, "ArrayList expansion card should exist");
  assert.match(arrayList.referenceAnswer, /第一次 add|第一次添加/);
  assert.match(arrayList.referenceAnswer, /默认容量 10|扩到 10/);
  assert.match(arrayList.referenceAnswer, /1\.5 倍/);
  assert.match(arrayList.referenceAnswer, /数组拷贝|拷贝旧元素/);

  assert.ok(listDiff, "ArrayList vs LinkedList card should exist");
  assert.match(listDiff.referenceAnswer, /动态数组/);
  assert.match(listDiff.referenceAnswer, /双向链表/);
  assert.match(listDiff.referenceAnswer, /O\(1\)/);
  assert.match(listDiff.referenceAnswer, /O\(n\)/);

  assert.ok(hashSet, "HashSet unique card should exist");
  assert.match(hashSet.referenceAnswer, /HashMap/);
  assert.match(hashSet.referenceAnswer, /hashCode/);
  assert.match(hashSet.referenceAnswer, /equals/);
  assert.match(hashSet.referenceAnswer, /占位对象/);
});

test("Redis combined-topic cards split subquestions into separate sections", () => {
  const cards = parseKnowledgeCardsSql();
  const byTitle = new Map(cards.map((card) => [card.title, card]));
  const cacheProblems = byTitle.get("Redis 缓存击穿、穿透与雪崩");
  const persistence = byTitle.get("Redis RDB 和 AOF 持久化");

  assert.ok(cacheProblems, "Redis cache problem card should exist");
  assert.ok(
    cacheProblems.referenceAnswer.length >= 450,
    "Redis cache problem card should be detailed enough for interview explanation"
  );
  assert.match(cacheProblems.referenceAnswer, /1\.\s*缓存穿透/);
  assert.match(cacheProblems.referenceAnswer, /2\.\s*缓存击穿/);
  assert.match(cacheProblems.referenceAnswer, /3\.\s*缓存雪崩/);
  assert.match(cacheProblems.referenceAnswer, /查不存在/);
  assert.match(cacheProblems.referenceAnswer, /单个热点 key/);
  assert.match(cacheProblems.referenceAnswer, /大量 key|大面积缓存/);
  assert.ok(cacheProblems.keyPoints.length >= 7, "Redis cache problem card needs separate key points");

  assert.ok(persistence, "Redis persistence card should exist");
  assert.ok(
    persistence.referenceAnswer.length >= 500,
    "Redis persistence card should be detailed enough for interview explanation"
  );
  assert.match(persistence.referenceAnswer, /RDB 的做法|RDB 保存/);
  assert.match(persistence.referenceAnswer, /AOF 的做法|AOF 保存/);
  assert.match(persistence.referenceAnswer, /fork/);
  assert.match(persistence.referenceAnswer, /fsync/);
  assert.match(persistence.referenceAnswer, /混合持久化/);
  assert.ok(persistence.keyPoints.length >= 7, "Redis persistence card needs separate key points");
});

test("high-risk cards keep concrete mechanisms and avoid generic guidance", () => {
  const cards = parseKnowledgeCardsSql();
  const byTitle = new Map(cards.map((card) => [card.title, card]));
  const encapsulation = byTitle.get("什么是封装");
  const hashMapPower = byTitle.get("HashMap 扩容为什么是 2 的幂");
  const bloomFilter = byTitle.get("布隆过滤器解决什么问题");
  const springBean = byTitle.get("Spring Bean 生命周期");
  const springTx = byTitle.get("Spring 事务失效场景");
  const mysqlMvcc = byTitle.get("MVCC 是什么");
  const arrayList = byTitle.get("ArrayList 扩容机制");

  assert.equal(byTitle.has("封装、继承、多态的面试表达"), false);
  assert.ok(encapsulation, "Encapsulation card should exist as a single-topic question");
  assert.equal(encapsulation.question, "什么是封装？");
  assert.doesNotMatch(encapsulation.question, /继承|多态|面试表达/);
  assert.match(encapsulation.referenceAnswer, /封装/);
  assert.match(encapsulation.referenceAnswer, /状态/);
  assert.match(encapsulation.referenceAnswer, /规则|约束/);
  assert.match(encapsulation.referenceAnswer, /getter\/setter/);
  assert.doesNotMatch(encapsulation.referenceAnswer, /封装、继承、多态/);
  assert.notEqual(
    encapsulation.referenceAnswer.replace(/\s/g, "").slice(0, 40),
    encapsulation.question.replace(/\s/g, "").slice(0, 40),
    "Encapsulation answer should not simply repeat the question"
  );

  assert.ok(hashMapPower, "HashMap power-of-two card should exist");
  assert.match(hashMapPower.referenceAnswer, /\(n - 1\) & hash/);
  assert.match(hashMapPower.referenceAnswer, /取模/);
  assert.match(hashMapPower.referenceAnswer, /2 倍/);
  assert.match(hashMapPower.referenceAnswer, /原位置/);
  assert.match(hashMapPower.referenceAnswer, /原位置加旧容量/);

  assert.ok(bloomFilter, "Bloom filter card should exist");
  assert.match(bloomFilter.referenceAnswer, /缓存穿透/);
  assert.match(bloomFilter.referenceAnswer, /bit 数组/);
  assert.match(bloomFilter.referenceAnswer, /多个 hash|多 hash/);
  assert.match(bloomFilter.referenceAnswer, /一定不存在/);
  assert.match(bloomFilter.referenceAnswer, /可能存在/);
  assert.match(bloomFilter.referenceAnswer, /误判/);
  assert.match(bloomFilter.referenceAnswer, /hash 冲突/);
  assert.match(bloomFilter.referenceAnswer, /删除/);

  assert.ok(springBean, "Spring Bean lifecycle card should exist");
  assert.match(springBean.referenceAnswer, /实例化/);
  assert.match(springBean.referenceAnswer, /属性填充/);
  assert.match(springBean.referenceAnswer, /Aware/);
  assert.match(springBean.referenceAnswer, /BeanPostProcessor/);
  assert.match(springBean.referenceAnswer, /@PostConstruct/);
  assert.match(springBean.referenceAnswer, /InitializingBean/);
  assert.match(springBean.referenceAnswer, /init-method/);
  assert.match(springBean.referenceAnswer, /AOP/);
  assert.match(springBean.referenceAnswer, /销毁/);
  assert.match(springBean.referenceAnswer, /prototype/);

  assert.ok(springTx, "Spring transaction failure card should exist");
  assert.match(springTx.referenceAnswer, /代理/);
  assert.match(springTx.referenceAnswer, /传播行为/);
  assert.match(springTx.referenceAnswer, /隔离级别/);
  assert.match(springTx.referenceAnswer, /失效/);
  assert.match(springTx.referenceAnswer, /同类内部调用|this\./);
  assert.match(springTx.referenceAnswer, /catch/);
  assert.match(springTx.referenceAnswer, /rollbackFor/);

  assert.ok(mysqlMvcc, "MySQL MVCC card should exist");
  assert.match(mysqlMvcc.referenceAnswer, /undo log/);
  assert.match(mysqlMvcc.referenceAnswer, /Read View/);
  assert.match(mysqlMvcc.referenceAnswer, /版本链/);
  assert.match(mysqlMvcc.referenceAnswer, /可见性|可见/);

  assert.ok(arrayList, "ArrayList expansion card should exist");
  assert.match(arrayList.referenceAnswer, /第一次 add|第一次添加/);
  assert.match(arrayList.referenceAnswer, /默认容量 10|扩到默认容量 10/);
  assert.match(arrayList.referenceAnswer, /1\.5 倍/);
  assert.match(arrayList.referenceAnswer, /数组.*拷贝|拷贝.*数组/);
});

test("knowledge selection can target one concrete card", () => {
  const topics = [
    {
      id: 101,
      title: "什么是封装",
      category: "Java",
      difficulty: "EASY",
      tags: ["Java 核心", "OOP", "面向对象"],
      question: "什么是封装？",
    },
    {
      id: 102,
      title: "接口和抽象类如何取舍",
      category: "Java",
      difficulty: "EASY",
      tags: ["Java 核心", "OOP", "面向对象"],
      question: "请解释接口和抽象类",
    },
  ];

  const selection = {
    domain: "Java 核心",
    section: "Java 基础",
    topic: "面向对象",
    cardId: 102,
    cardTitle: "接口和抽象类如何取舍",
  };

  assert.deepEqual(
    topics.filter((topic) => matchKnowledgeTopic(topic, selection)).map((topic) => topic.id),
    [102]
  );
  assert.equal(selectionKey(selection), "Java 核心/Java 基础/面向对象/card:102");
  assert.deepEqual(getSelectionBreadcrumb(selection), [
    "知识训练",
    "Java 核心",
    "Java 基础",
    "面向对象",
    "接口和抽象类如何取舍",
  ]);
});

test("knowledge outline appends concrete card nodes under final topics", () => {
  const topics = [
    {
      id: 101,
      title: "什么是封装",
      category: "Java",
      difficulty: "EASY",
      tags: ["Java 核心", "OOP", "面向对象"],
      question: "什么是封装？",
    },
    {
      id: 102,
      title: "接口和抽象类如何取舍",
      category: "Java",
      difficulty: "EASY",
      tags: ["Java 核心", "OOP", "面向对象"],
      question: "请解释接口和抽象类",
    },
  ];

  const outline = buildKnowledgeOutline(topics);
  const javaCore = outline.find((node) => node.domain === "Java 核心");
  const javaBase = javaCore.children.find((node) => node.section === "Java 基础");
  const oop = javaBase.children.find((node) => node.topic === "面向对象");

  assert.deepEqual(
    oop.children.map((node) => ({
      cardId: node.cardId,
      cardTitle: node.cardTitle,
      key: selectionKey(node),
    })),
    [
      {
        cardId: 101,
        cardTitle: "什么是封装",
        key: "Java 核心/Java 基础/面向对象/card:101",
      },
      {
        cardId: 102,
        cardTitle: "接口和抽象类如何取舍",
        key: "Java 核心/Java 基础/面向对象/card:102",
      },
    ]
  );
});

test("local fallback knowledge topics mirror SQL seed content", () => {
  const cards = parseKnowledgeCardsSql();
  const fallbackByTitle = new Map(knowledgeTopics.map((topic) => [topic.title, topic]));

  assert.equal(knowledgeTopics.length, cards.length);

  for (const card of cards) {
    const fallback = fallbackByTitle.get(card.title);
    assert.ok(fallback, `${card.title} should exist in local fallback topics`);
    assert.equal(fallback.question, card.question, `${card.title} fallback question should match SQL`);
    assert.equal(
      fallback.referenceAnswer,
      card.referenceAnswer,
      `${card.title} fallback answer should match SQL`
    );
    assert.deepEqual(
      fallback.keyPoints,
      card.keyPoints,
      `${card.title} fallback key points should match SQL`
    );
  }
});

test("knowledge cards avoid artificial interview-expression wording", () => {
  const cards = parseKnowledgeCardsSql();

  for (const card of cards) {
    assert.doesNotMatch(card.title, /面试表达/, `${card.title} title should be a real topic`);
    assert.doesNotMatch(card.question, /面试表达/, `${card.title} question should be natural`);
    assert.doesNotMatch(
      card.referenceAnswer,
      /面试表达/,
      `${card.title} answer should not expose content-generation phrasing`
    );
  }
});
