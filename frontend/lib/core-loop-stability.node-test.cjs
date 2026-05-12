const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");

const root = path.resolve(__dirname, "..");

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), "utf8");
}

test("SSE stream decisions are centralized and keep streamDiagnosis as the primary path", () => {
  const workspace = read("components/ProblemWorkspace.tsx");
  const helper = read("lib/agentStreamState.ts");

  assert.match(workspace, /streamDiagnosis\(/);
  assert.match(workspace, /createAgentStreamSession/);
  assert.match(workspace, /isCurrentAgentStream/);
  assert.match(workspace, /shouldRunSyncFallback/);
  assert.match(helper, /export function createAgentStreamSession/);
  assert.match(helper, /export function isCurrentAgentStream/);
  assert.match(helper, /export function shouldRunSyncFallback/);
  assert.doesNotMatch(helper, /fetch\(|agentApi|localStorage/);
});

test("sync analyze remains fallback-only in ProblemWorkspace", () => {
  const workspace = read("components/ProblemWorkspace.tsx");
  const analyzeMatches = workspace.match(/agentApi\.analyze/g) ?? [];
  const fallbackStart = workspace.indexOf("const runSyncFallback");
  const analyzeIndex = workspace.indexOf("agentApi.analyze");

  assert.equal(analyzeMatches.length, 1);
  assert.ok(fallbackStart >= 0, "runSyncFallback should exist");
  assert.ok(analyzeIndex > fallbackStart, "agentApi.analyze must stay inside fallback flow");
  assert.match(workspace, /onError:[\s\S]*runSyncFallback/);
  assert.match(workspace, /onEnd:[\s\S]*shouldRunSyncFallback/);
});

test("old stream events are guarded and active streams are aborted on resubmit and unmount", () => {
  const workspace = read("components/ProblemWorkspace.tsx");

  assert.match(workspace, /currentStreamIdRef\.current\s*=\s*createAgentStreamSession/);
  assert.match(workspace, /streamControllerRef\.current\?\.abort\(\)/);
  assert.match(workspace, /return \(\) => \{\s*streamControllerRef\.current\?\.abort\(\);/);
  assert.match(workspace, /if \(!isCurrentAgentStream\(streamId,\s*currentStreamIdRef\.current\)\) return;/);
});

test("Dashboard uses MySQL-backed userApi only and keeps empty states instead of mock data", () => {
  const dashboard = read("app/dashboard/page.tsx");
  const weakness = read("components/WeaknessList.tsx");
  const mistakes = read("components/MistakeCards.tsx");
  const submissions = read("components/SubmissionHistory.tsx");
  const plan = read("components/TrainingPlan.tsx");
  const stats = read("components/ErrorStats.tsx");

  assert.doesNotMatch(dashboard, /mock|lib\/mock|@\/lib\/mock/);
  assert.match(dashboard, /userApi\.stats/);
  assert.match(dashboard, /userApi\.weaknesses/);
  assert.match(dashboard, /userApi\.mistakes/);
  assert.match(dashboard, /userApi\.latestPlan/);
  assert.match(dashboard, /userApi\.recentSubmissions/);
  assert.match(dashboard, /userApi\.errorStats/);
  for (const source of [weakness, mistakes, submissions, plan, stats]) {
    assert.match(source, /还没有|暂无/);
  }
});
