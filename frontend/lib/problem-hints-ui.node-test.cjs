const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");

const root = path.resolve(__dirname, "..");

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), "utf8");
}

test("right result panel only exposes test result and AI diagnosis tabs", () => {
  const resultPanel = read("components/ResultPanel.tsx");
  const workspace = read("components/ProblemWorkspace.tsx");

  assert.doesNotMatch(resultPanel, /HintPanel/);
  assert.doesNotMatch(resultPanel, /key:\s*"hint"/);
  assert.doesNotMatch(resultPanel, /分层提示/);
  assert.match(resultPanel, /label:\s*"测试结果"/);
  assert.match(resultPanel, /label:\s*"AI 诊断"/);
  assert.match(workspace, /useState<"test" \| "diagnosis">/);
  assert.doesNotMatch(workspace, /"test" \| "diagnosis" \| "hint"/);
});

test("problem description renders preset layered hints from backend data without legacy fallbacks", () => {
  const description = read("components/ProblemDescription.tsx");
  const panel = read("components/ProblemHintPanel.tsx");
  const hints = read("lib/problemHints.ts");

  assert.match(description, /ProblemHintPanel/);
  assert.match(description, /ProblemSolutionPanel/);
  assert.match(description, /label:\s*"题目"/);
  assert.match(description, /label:\s*"提示"/);
  assert.match(description, /label:\s*"题解"/);
  assert.match(description, /useState<ProblemTab>\("description"\)/);
  assert.match(description, /problem\.presetHints \?\? getProblemPresetHints\(problem\.id\)/);
  assert.doesNotMatch(description, /solutionModeHints/);
  assert.doesNotMatch(panel, /默认展开/);
  assert.equal((panel.match(/defaultOpen:\s*false/g) ?? []).length, 3);
  assert.equal((panel.match(/defaultOpen:\s*true/g) ?? []).length, 0);
  assert.match(hints, /export interface ProblemPresetHints/);
  assert.match(hints, /export function getProblemPresetHints/);
  for (const legacyId of [101, 102, 103, 104, 105, 106, 107, 108]) {
    assert.doesNotMatch(hints, new RegExp(`${legacyId}:\\s*{`));
  }
  assert.match(hints, /return null/);
});

test("AI diagnosis copy no longer says it generates hints", () => {
  const diagnosis = read("components/AiDiagnosis.tsx");

  assert.match(diagnosis, /AI 正在诊断错误原因并生成训练建议/);
  assert.doesNotMatch(diagnosis, /正在诊断错误原因并生成提示/);
  assert.doesNotMatch(diagnosis, /hintLevel1|hintLevel2|hintLevel3/);
});

test("AI diagnosis renders coach report sections with legacy fallbacks", () => {
  const diagnosis = read("components/AiDiagnosis.tsx");
  const types = read("lib/types.ts");

  assert.match(types, /failurePhenomenon\?: string \| null/);
  assert.match(types, /rootCause\?: string \| null/);
  assert.match(types, /repairDirection\?: string \| null/);
  assert.match(types, /interviewReminder\?: string \| null/);
  assert.match(types, /suggestion\?: string \| null/);
  assert.match(diagnosis, /失败现象/);
  assert.match(diagnosis, /根本原因/);
  assert.match(diagnosis, /修改方向/);
  assert.match(diagnosis, /面试提醒/);
  assert.match(diagnosis, /d\.failurePhenomenon \?\? d\.specificError/);
  assert.match(diagnosis, /d\.rootCause \?\? d\.diagnosis \?\? d\.specificError/);
  assert.match(diagnosis, /d\.repairDirection \?\? d\.suggestion \?\? d\.diagnosis/);
  assert.match(diagnosis, /interviewReminderFallback/);
});

test("AI diagnosis summarizes stack traces before rendering failure phenomenon", () => {
  const diagnosis = read("components/AiDiagnosis.tsx");

  assert.match(diagnosis, /formatFailurePhenomenon/);
  assert.match(diagnosis, /OutOfMemoryError/);
  assert.match(diagnosis, /运行时异常/);
  assert.doesNotMatch(diagnosis, /failurePhenomenon:\s*d\.failurePhenomenon \?\? d\.specificError/);
});

test("accepted code review stays visible when current editor code is stale", () => {
  const diagnosis = read("components/AiDiagnosis.tsx");

  assert.match(diagnosis, /const hasCodeReview = Boolean\(d\?\.codeReview\)/);
  assert.match(diagnosis, /if \(hasCodeReview \|\| isAccepted\)/);
  assert.match(diagnosis, /该点评基于上次提交，当前代码已修改，仅供参考。/);
});

test("accepted code analysis shows live agent steps while review is generating", () => {
  const diagnosis = read("components/AiDiagnosis.tsx");

  assert.match(
    diagnosis,
    /AI 正在生成面试点评\.\.\.[\s\S]{0,250}agentSteps\.length > 0 && <AgentTimeline steps=\{agentSteps\}/
  );
});

test("reference solution panel exposes a copy code action", () => {
  const solutionPanel = read("components/ProblemSolutionPanel.tsx");

  assert.match(solutionPanel, /Clipboard/);
  assert.match(solutionPanel, /复制代码/);
  assert.match(solutionPanel, /navigator\.clipboard\.writeText/);
});
