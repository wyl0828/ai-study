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

test("problem description renders preset layered hints from problem data mapping", () => {
  const description = read("components/ProblemDescription.tsx");
  const panel = read("components/ProblemHintPanel.tsx");
  const hints = read("lib/problemHints.ts");

  assert.match(description, /ProblemHintPanel/);
  assert.match(description, /getProblemPresetHints\(problem\.id\)/);
  assert.doesNotMatch(description, /solutionModeHints/);
  assert.doesNotMatch(panel, /默认展开/);
  assert.equal((panel.match(/defaultOpen:\s*false/g) ?? []).length, 3);
  assert.equal((panel.match(/defaultOpen:\s*true/g) ?? []).length, 0);
  assert.match(hints, /export interface ProblemPresetHints/);
  assert.match(hints, /export function getProblemPresetHints/);
  for (const problemId of [101, 102, 103, 104, 105, 106, 107, 108]) {
    assert.match(hints, new RegExp(`${problemId}:\\s*{`));
  }
});

test("AI diagnosis copy no longer says it generates hints", () => {
  const diagnosis = read("components/AiDiagnosis.tsx");

  assert.match(diagnosis, /正在诊断错误原因并生成训练建议/);
  assert.doesNotMatch(diagnosis, /正在诊断错误原因并生成提示/);
  assert.doesNotMatch(diagnosis, /hintLevel1|hintLevel2|hintLevel3/);
});
