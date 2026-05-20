const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");
const ts = require("typescript");

const root = path.resolve(__dirname, "..");

function loadTsModule(relativePath) {
  const filename = path.join(root, relativePath);
  const source = fs.readFileSync(filename, "utf8");
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2020,
      esModuleInterop: true,
    },
    fileName: filename,
  });
  const module = { exports: {} };
  const localRequire = (id) => {
    if (id === "./types") return {};
    throw new Error(`Unexpected runtime import: ${id}`);
  };
  new Function("require", "module", "exports", outputText)(
    localRequire,
    module,
    module.exports
  );
  return module.exports;
}

function aggregation() {
  return loadTsModule("lib/dashboardAggregation.ts");
}

test("normalizes HashMap and linked-list free text without broad map false positives", () => {
  const { normalizeKnowledgePoint } = aggregation();

  assert.deepEqual(
    normalizeKnowledgePoint("两数之和 HashMap 查询顺序 put 顺序"),
    { key: "HASHMAP_USAGE", name: "HashMap 使用逻辑" }
  );
  assert.deepEqual(
    normalizeKnowledgePoint("反转链表 return prev 链表指针更新顺序"),
    { key: "LINKED_LIST_REVERSE", name: "链表反转指针操作" }
  );
  assert.deepEqual(
    normalizeKnowledgePoint("Java Stream map 操作"),
    { key: "RAW:Java Stream map 操作", name: "Java Stream map 操作" }
  );
});

test("groups weakness records by canonical knowledge point and preserves unknown points", () => {
  const { groupWeaknesses } = aggregation();
  const result = groupWeaknesses([
    {
      id: 1,
      knowledgePoint: "HashMap 遍历逻辑",
      errorType: "ALGORITHM_ERROR",
      wrongCount: 1,
      weaknessScore: 11,
      trendLabel: "最近加重",
      lastDeltaScore: 3,
      lastEventAt: "2026-05-18T10:00:00",
    },
    {
      id: 2,
      knowledgePoint: "两数之和 HashMap 应用",
      errorType: "LOGIC_ERROR",
      wrongCount: 2,
      weaknessScore: 18,
      trendLabel: "新暴露问题",
      lastDeltaScore: 9,
      lastEventAt: "2026-05-19T10:00:00",
    },
    {
      id: 3,
      knowledgePoint: "滑动窗口收缩条件",
      errorType: "BOUNDARY_ERROR",
      wrongCount: 1,
      weaknessScore: 7,
      trendLabel: null,
      lastDeltaScore: null,
      lastEventAt: "2026-05-17T10:00:00",
    },
  ]);

  assert.equal(result.length, 2);
  assert.equal(result[0].canonicalKey, "HASHMAP_USAGE");
  assert.equal(result[0].canonicalName, "HashMap 使用逻辑");
  assert.equal(result[0].wrongCount, 3);
  assert.equal(result[0].weaknessScore, 18);
  assert.equal(result[0].errorType, "逻辑错误");
  assert.equal(result[0].lastDeltaScore, 9);
  assert.equal(result[1].canonicalName, "滑动窗口收缩条件");
});

test("groups mistake cards by problem fallback, error type, and canonical key", () => {
  const { groupMistakeCards } = aggregation();
  const result = groupMistakeCards([
    {
      id: 1,
      problemId: 206,
      problemTitle: "反转链表",
      errorType: "LOGIC_ERROR",
      knowledgePoint: "链表反转指针操作",
      mistakeSummary: "返回 head / prev 混淆，循环结束后没有返回新的头节点。",
      correctIdea: "保存 next 后再移动 prev 和 cur。",
      repeatCount: 2,
      lastSeenAt: "2026-05-18T10:00:00",
      status: "OPEN",
    },
    {
      id: 2,
      problemId: 206,
      problemTitle: "反转链表",
      errorType: "LOGIC_ERROR",
      knowledgePoint: "链表指针更新顺序",
      mistakeSummary: "指针更新顺序错误，cur.next 被覆盖后丢失后续节点。",
      correctIdea: "三指针反转模板：先保存 next，再反转当前指针。",
      repeatCount: null,
      lastSeenAt: "2026-05-19T10:00:00",
      status: "OPEN",
    },
    {
      id: 3,
      problemTitle: "复制随机链表",
      errorType: "LOGIC_ERROR",
      knowledgePoint: "链表指针更新顺序",
      mistakeSummary: "另一道题不能因为 problemId 缺失被合并到反转链表。",
      correctIdea: "按题目标题作为兜底 key。",
      repeatCount: 1,
      lastSeenAt: "2026-05-19T11:00:00",
      status: "OPEN",
    },
  ]);

  assert.equal(result.length, 2);
  const reverseList = result.find((item) => item.problemTitle === "反转链表");
  assert.ok(reverseList);
  assert.equal(reverseList.groupKey, "problem:206-LOGIC_ERROR-LINKED_LIST_REVERSE");
  assert.equal(reverseList.totalOccurrences, 3);
  assert.equal(reverseList.rawRecords.length, 2);
  assert.ok(reverseList.typicalProblems.length <= 3);
  assert.ok(reverseList.typicalProblems.every((text) => text.length <= 45));

  const copyList = result.find((item) => item.problemTitle === "复制随机链表");
  assert.ok(copyList);
  assert.equal(copyList.groupKey, "title:复制随机链表-LOGIC_ERROR-LINKED_LIST_REVERSE");
});
