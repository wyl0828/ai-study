const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");
const ts = require("typescript");

const root = path.resolve(__dirname, "..");

function loadLearningView() {
  const filename = path.join(root, "lib/learningView.ts");
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
    if (id === "./dashboardAggregation") {
      return {
        groupMistakeCards: () => [],
        groupWeaknesses: () => [],
        normalizeErrorType: (value) => value,
        normalizeKnowledgePoint: (value) => value,
      };
    }
    if (id === "./i18n") {
      return {
        problemTitle: (value) => value || "算法题",
        trainingPlanText: (value) => value || "当前薄弱点",
      };
    }
    throw new Error(`Unexpected runtime import: ${id}`);
  };
  new Function("require", "module", "exports", outputText)(
    localRequire,
    module,
    module.exports
  );
  return module.exports;
}

test("dashboard next actions sort HIGH priority before MEDIUM while preserving stable order", () => {
  const { buildDashboardNextActions } = loadLearningView();

  const actions = buildDashboardNextActions({
    trainingPlan: {
      items: [
        {
          id: 1,
          itemType: "PROBLEM",
          status: "PENDING",
          problemId: 1,
          problemTitle: "两数之和",
          reason: "复盘 HashMap 查询顺序",
          reviewFocus: "HashMap 查询顺序",
          sourceType: "SUBMISSION_FAILED",
          sourceSummary: "失败提交",
        },
      ],
    },
    trainingPlanTrace: {
      nextItem: null,
      nextActionPriority: "MEDIUM",
      nextActionReason: "训练计划还有待办，但优先级低于进行中面试。",
      nextTargetHref: "/problem/1",
      nextTargetLabel: "去做题",
    },
    mockInterviewTrace: {
      latestSessionId: 31,
      latestSessionStatus: "ASKING_FOLLOW_UP",
      nextActionPriority: "HIGH",
      nextActionReason: "最近会话尚未生成报告，先继续面试。",
      nextTargetHref: "/mock-interview?sessionId=31",
      nextTargetLabel: "继续面试",
      sessionCount: 1,
      reportedSessionCount: 0,
      recommendedCardIds: [],
      latestWeaknessTags: [],
    },
    mockInterviewTrends: [
      {
        knowledgeCardId: 7,
        knowledgePoint: "Spring Bean 生命周期",
        latestScore: 65,
        deltaScore: -8,
        latestIssue: "初始化扩展点没有说清楚",
      },
    ],
  });

  assert.equal(actions.length, 3);
  assert.equal(actions[0].title, "继续最近模拟面试");
  assert.equal(actions[0].priority, "HIGH");
  assert.equal(actions[0].sourceLabel, "模拟面试");
  assert.equal(actions[1].title, "先完成：两数之和");
  assert.equal(actions[1].priority, "MEDIUM");
  assert.equal(actions[1].sourceLabel, "训练计划");
  assert.equal(actions[2].title, "复盘面试卡点：Spring Bean 生命周期");
  assert.equal(actions[2].priority, "MEDIUM");
  assert.equal(actions[2].sourceLabel, "面试趋势");
});
