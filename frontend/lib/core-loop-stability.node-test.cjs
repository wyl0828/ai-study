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

test("RAG retrieval appears only as an agent timeline step", () => {
  const workspace = read("components/ProblemWorkspace.tsx");
  const resultPanel = read("components/ResultPanel.tsx");
  const i18n = read("lib/i18n.ts");

  assert.match(workspace, /"RAG_RETRIEVAL"/);
  assert.match(i18n, /RAG_RETRIEVAL:\s*"检索相关知识和历史错题"/);
  assert.match(resultPanel, /测试结果/);
  assert.match(resultPanel, /AI 诊断/);
  assert.doesNotMatch(resultPanel, /分层提示/);
  assert.doesNotMatch(resultPanel, /RAG/);
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

test("Dashboard keeps one page scroll and training plan items link to their tasks", () => {
  const dashboard = read("app/dashboard/page.tsx");
  const plan = read("components/TrainingPlan.tsx");
  const types = read("lib/types.ts");
  const knowledgePage = read("components/KnowledgeTrainingPage.tsx");

  assert.doesNotMatch(dashboard, /overflow-y-auto|h-screen|max-h-screen|sticky\s+top-/);
  assert.match(types, /problemId\?: number \| null/);
  assert.match(plan, /item\.itemType === "PROBLEM"/);
  assert.match(plan, /problemHref\(item\)/);
  assert.match(plan, /`\/problem\/\$\{problemId\}`/);
  assert.match(plan, /`\/knowledge\?cardId=\$\{item\.knowledgeCardId\}`/);
  assert.doesNotMatch(plan, /action\.href \?/);
  assert.match(plan, /去做题/);
  assert.match(plan, /去复习/);
  assert.match(knowledgePage, /useSearchParams/);
  assert.match(knowledgePage, /cardId/);
});

test("Knowledge page keeps selection, breadcrumb, topic filtering, and training state centralized", () => {
  const knowledgePage = read("components/KnowledgeTrainingPage.tsx");
  const sidebar = read("components/KnowledgeSidebar.tsx");
  const header = read("components/KnowledgeTopicHeader.tsx");
  const filterBar = read("components/KnowledgeFilterBar.tsx");
  const card = read("components/KnowledgeCard.tsx");
  const data = read("lib/knowledgeData.ts");

  assert.match(knowledgePage, /type KnowledgeSelection/);
  assert.match(knowledgePage, /KnowledgeSidebar/);
  assert.match(knowledgePage, /KnowledgeTopicHeader/);
  assert.match(knowledgePage, /KnowledgeFilterBar/);
  assert.match(header, /知识训练/);
  assert.doesNotMatch(header, /题库/);
  assert.match(data, /matchKnowledgeTopic/);
  assert.match(data, /ArrayList|LinkedList/);
  assert.match(data, /HashMap|ConcurrentHashMap/);
  assert.match(sidebar, /知识体系大纲/);
  assert.match(sidebar, /domain:\s*"Spring"/);
  assert.doesNotMatch(sidebar, /主流框架/);
  assert.doesNotMatch(sidebar, /系统设计/);
  assert.match(filterBar, /未练/);
  assert.match(filterBar, /已掌握/);
  assert.match(filterBar, /需复习/);
  assert.match(card, /最近得分/);
  assert.match(card, /未自测/);
});
