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
    /可以这样答|展开讲|需要注意的是|关键触发条件是什么|同类方案相比|规避这个坑|追问时补充|如果线上出现和 .+ 相关的问题/;

  assert.ok(cards.length >= 120, `expected at least 120 cards, got ${cards.length}`);

  for (const selection of leafSelections) {
    const matches = cards.filter((card) => matchKnowledgeTopic(card, selection));
    assert.ok(
      matches.length >= 5,
      `${selection.domain}/${selection.section || ""}/${selection.topic || ""} has ${matches.length} cards`
    );
  }

  for (const card of cards) {
    assert.ok(card.referenceAnswer.length >= 260, `${card.title} answer is too short`);
    assert.ok(
      (card.referenceAnswer.match(/[。？！?]/g) || []).length >= 6,
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
    assert.ok(card.keyPoints.length >= 5, `${card.title} needs at least five key points`);
    assert.ok(card.followUpQuestions.length >= 3, `${card.title} needs at least three follow-ups`);
  }
});

test("knowledge generator requires explicit card profiles for every seeded card", () => {
  const cards = parseKnowledgeCardsSql();

  assert.match(generatorSource, /const cardProfiles\s*=/);
  assert.match(profileSource, /const cardProfiles\s*=/);
  assert.match(profileSource, /function profile\(answer,\s*keyPoints,\s*followUps\)/);
  assert.doesNotMatch(profileSource, /function profile\(keyPoints,\s*followUps\)/);
  assert.doesNotMatch(profileSource, /profile\(\s*\[/);
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

test("knowledge selection can target one concrete card", () => {
  const topics = [
    {
      id: 101,
      title: "封装、继承、多态的面试表达",
      category: "Java",
      difficulty: "EASY",
      tags: ["Java 核心", "OOP", "面向对象"],
      question: "请解释封装、继承、多态",
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
      title: "封装、继承、多态的面试表达",
      category: "Java",
      difficulty: "EASY",
      tags: ["Java 核心", "OOP", "面向对象"],
      question: "请解释封装、继承、多态",
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
        cardTitle: "封装、继承、多态的面试表达",
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
