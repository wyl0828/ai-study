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

  assert.ok(cards.length >= 120, `expected at least 120 cards, got ${cards.length}`);

  for (const selection of leafSelections) {
    const matches = cards.filter((card) => matchKnowledgeTopic(card, selection));
    assert.ok(
      matches.length >= 5,
      `${selection.domain}/${selection.section || ""}/${selection.topic || ""} has ${matches.length} cards`
    );
  }

  for (const card of cards) {
    assert.ok(card.referenceAnswer.length >= 120, `${card.title} answer is too short`);
    assert.ok(card.keyPoints.length >= 5, `${card.title} needs at least five key points`);
    assert.ok(card.followUpQuestions.length >= 3, `${card.title} needs at least three follow-ups`);
  }
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
