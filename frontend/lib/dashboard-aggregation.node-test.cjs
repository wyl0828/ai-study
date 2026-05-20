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

test("groups mistake cards by problem fallback, user-facing pattern, and canonical key", () => {
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
      errorType: "ALGORITHM_ERROR",
      knowledgePoint: "链表指针更新顺序",
      mistakeSummary: "循环结束后仍然 return head，导致只返回旧头节点。",
      correctIdea: "应该 return prev，prev 才是新的头节点。",
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
  assert.equal(reverseList.groupKey, "problem:206-LINKED_LIST_REVERSE-返回值错误");
  assert.equal(reverseList.userFacingErrorTag, "返回值错误");
  assert.equal(reverseList.patternTitle, "反转链表｜返回了旧 head，而不是新头 prev");
  assert.match(reverseList.rootCause, /旧 head/);
  assert.match(reverseList.fixAction, /返回 prev/);
  assert.match(reverseList.reviewScript, /新头节点/);
  assert.equal(reverseList.totalOccurrences, 3);
  assert.equal(reverseList.rawRecords.length, 2);
  assert.ok(reverseList.typicalProblems.length <= 3);
  assert.ok(reverseList.typicalProblems.every((text) => text.length <= 45));

  const copyList = result.find((item) => item.problemTitle === "复制随机链表");
  assert.ok(copyList);
  assert.equal(copyList.groupKey, "title:复制随机链表-LINKED_LIST_REVERSE-指针移动顺序错误");
});

test("filters environment system errors unless they match a concrete algorithm pattern", () => {
  const { groupMistakeCards } = aggregation();
  const result = groupMistakeCards([
    {
      id: 11,
      problemId: 1,
      problemTitle: "两数之和",
      errorType: "SYSTEM_ERROR",
      knowledgePoint: "HashMap 使用逻辑",
      mistakeSummary: "检查本地 Piston 服务是否在端口 223 运行，后端判题服务暂不可用。",
      correctIdea: "启动 Piston 后重新提交。",
      repeatCount: 1,
      lastSeenAt: "2026-05-19T10:00:00",
      status: "OPEN",
    },
    {
      id: 12,
      problemId: 1,
      problemTitle: "两数之和",
      errorType: "SYSTEM_ERROR",
      knowledgePoint: "HashMap 使用逻辑",
      mistakeSummary: "HashMap put 顺序写反，可能触发自匹配。",
      correctIdea: "先 containsKey 查询 complement，再 put 当前值。",
      repeatCount: 1,
      lastSeenAt: "2026-05-20T10:00:00",
      status: "OPEN",
    },
  ]);

  assert.equal(result.length, 1);
  assert.equal(result[0].id, 12);
  assert.equal(result[0].patternTitle, "两数之和｜HashMap 查询顺序写反");
  assert.equal(result[0].userFacingErrorTag, "HashMap 查询顺序错误");
  assert.equal(result[0].knowledgePoint, "HashMap 使用逻辑");
});

test("uses stronger pattern titles and canonical display casing", () => {
  const { groupMistakeCards } = aggregation();
  const result = groupMistakeCards([
    {
      id: 21,
      problemId: 206,
      problemTitle: "反转链表",
      errorType: "LOGIC_ERROR",
      knowledgePoint: "链表反转指针操作",
      mistakeSummary: "循环结束后 return head，返回了旧头节点。",
      correctIdea: "return prev，prev 是新头节点。",
      repeatCount: 1,
      lastSeenAt: "2026-05-20T09:00:00",
      status: "OPEN",
    },
    {
      id: 22,
      problemId: 102,
      problemTitle: "二叉树的层序遍历",
      errorType: "LOGIC_ERROR",
      knowledgePoint: "bfs 层序遍历",
      mistakeSummary: "没有固定当前层 size，队列长度会被下一层节点污染。",
      correctIdea: "每层开始先记录 queue.size()。",
      repeatCount: 1,
      lastSeenAt: "2026-05-20T10:00:00",
      status: "OPEN",
    },
  ]);

  assert.equal(result[0].patternTitle, "二叉树层序遍历｜BFS 层边界没固定");
  assert.equal(result[0].knowledgePoint, "二叉树遍历逻辑");
  assert.equal(result[1].patternTitle, "反转链表｜返回了旧 head，而不是新头 prev");
});
