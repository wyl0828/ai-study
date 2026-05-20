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
  const aggregation = read("lib/dashboardAggregation.ts");

  assert.doesNotMatch(dashboard, /mock|lib\/mock|@\/lib\/mock/);
  assert.match(dashboard, /userApi\.stats/);
  assert.match(dashboard, /userApi\.weaknesses/);
  assert.match(dashboard, /userApi\.mistakes/);
  assert.match(dashboard, /userApi\.latestPlan/);
  assert.match(dashboard, /userApi\.recentSubmissions/);
  assert.match(dashboard, /userApi\.errorStats/);
  assert.match(dashboard, /groupWeaknesses\(weaknesses\)/);
  assert.match(dashboard, /groupMistakeCards\(mistakes\)/);
  assert.match(aggregation, /export function groupWeaknesses/);
  assert.match(aggregation, /export function groupMistakeCards/);
  for (const source of [weakness, mistakes, submissions, plan, stats]) {
    assert.match(source, /还没有|暂无/);
  }
});

test("Dashboard keeps one page scroll and training plan items link to their tasks", () => {
  const dashboard = read("app/dashboard/page.tsx");
  const plan = read("components/TrainingPlan.tsx");
  const learning = read("lib/learningView.ts");
  const types = read("lib/types.ts");
  const knowledgePage = read("components/KnowledgeTrainingPage.tsx");
  const todayFocusIndex = dashboard.indexOf("<TodayTrainingFocus");
  const weaknessIndex = dashboard.indexOf("<WeaknessList");
  const submissionsIndex = dashboard.indexOf("<SubmissionHistory submissions");
  const mistakesIndex = dashboard.indexOf("<MistakeCards");
  const trainingPlanIndex = dashboard.indexOf("<TrainingPlan\n");
  const errorStatsIndex = dashboard.indexOf("<ErrorStats stats");

  assert.doesNotMatch(dashboard, /overflow-y-auto|h-screen|max-h-screen/);
  assert.match(dashboard, /xl:grid-cols-\[minmax\(0,1\.35fr\)_minmax\(360px,0\.95fr\)\]/);
  assert.match(dashboard, /lg:sticky lg:top-24 xl:sticky xl:top-24 self-start/);
  assert.ok(todayFocusIndex >= 0, "Dashboard should render today's priority training");
  assert.ok(weaknessIndex >= 0, "Dashboard should still render weakness ranking");
  assert.ok(submissionsIndex >= 0, "Dashboard should render recent submissions");
  assert.ok(mistakesIndex >= 0, "Dashboard should render mistake review");
  assert.ok(trainingPlanIndex >= 0, "Dashboard should render the full training plan");
  assert.ok(errorStatsIndex >= 0, "Dashboard should render error pattern analysis");
  assert.ok(
    todayFocusIndex < weaknessIndex,
    "today's priority training should appear before weakness ranking"
  );
  assert.ok(
    weaknessIndex < submissionsIndex,
    "recent submissions should stay in the main command area after weakness ranking"
  );
  assert.ok(
    trainingPlanIndex < errorStatsIndex,
    "training plan should lead the auxiliary sidebar before error analysis"
  );
  assert.ok(
    errorStatsIndex < mistakesIndex,
    "mistake cards should render after the command grid as a full-width review area"
  );
  assert.match(types, /problemId\?: number \| null/);
  assert.match(`${plan}\n${learning}`, /item\.itemType === "PROBLEM"/);
  assert.match(learning, /trainingPlanItemHref/);
  assert.match(learning, /`\/problem\/\$\{item\.problemId \|\| inferredProblemId\(item\)\}`/);
  assert.match(learning, /`\/knowledge\?cardId=\$\{item\.knowledgeCardId\}`/);
  assert.doesNotMatch(plan, /action\.href \?/);
  assert.match(`${plan}\n${learning}`, /去做题/);
  assert.match(`${plan}\n${learning}`, /去复习/);
  assert.match(knowledgePage, /useSearchParams/);
  assert.match(knowledgePage, /cardId/);
});

test("Dashboard coach view avoids duplicate weak-point panels", () => {
  const dashboard = read("app/dashboard/page.tsx");
  const stats = read("components/ErrorStats.tsx");
  const plan = read("components/TrainingPlan.tsx");
  const focus = read("components/TodayTrainingFocus.tsx");

  assert.match(dashboard, /buildDashboardCoachAdvice/);
  assert.match(focus, /今日优先训练/);
  assert.match(plan, /完整训练计划/);
  assert.doesNotMatch(stats, /最薄弱知识点/);
  assert.doesNotMatch(stats, /TrendingDown/);
});

test("Knowledge page keeps selection, breadcrumb, topic filtering, and training state centralized", () => {
  const knowledgePage = read("components/KnowledgeTrainingPage.tsx");
  const sidebar = read("components/KnowledgeSidebar.tsx");
  const header = read("components/KnowledgeTopicHeader.tsx");
  const filterBar = read("components/KnowledgeFilterBar.tsx");
  const card = read("components/KnowledgeCard.tsx");
  const data = read("lib/knowledgeData.ts");
  const seed = read("lib/knowledgeSeed.ts");
  const knowledgeSources = `${data}\n${seed}`;

  assert.match(knowledgePage, /type KnowledgeSelection/);
  assert.match(knowledgePage, /KnowledgeSidebar/);
  assert.match(knowledgePage, /KnowledgeTopicHeader/);
  assert.match(knowledgePage, /KnowledgeFilterBar/);
  assert.match(header, /知识训练/);
  assert.doesNotMatch(header, /题库/);
  assert.match(data, /matchKnowledgeTopic/);
  assert.match(data, /cardId\?: number/);
  assert.match(data, /cardTitle\?: string/);
  assert.match(data, /buildKnowledgeOutline/);
  assert.match(data, /card:\$\{selection\.cardId\}/);
  assert.match(data, /ArrayList|LinkedList/);
  assert.match(data, /HashMap|ConcurrentHashMap/);
  assert.match(sidebar, /知识体系大纲/);
  assert.match(data, /domain:\s*"Spring"/);
  assert.doesNotMatch(sidebar, /主流框架/);
  assert.doesNotMatch(sidebar, /系统设计/);
  assert.match(sidebar, /useState/);
  assert.match(sidebar, /expandedKeys/);
  assert.match(sidebar, /aria-expanded/);
  assert.match(sidebar, /buildKnowledgeOutline\(topics\)/);
  assert.match(sidebar, /isCardNode/);
  assert.match(sidebar, /!isCardNode/);
  assert.doesNotMatch(sidebar, /"Java 核心\/Java 基础\/面向对象"/);
  assert.match(data, /domain:\s*"AI 工程"/);
  assert.match(data, /topic:\s*"Agent"/);
  assert.match(data, /topic:\s*"RAG"/);
  assert.match(data, /topic:\s*"LangChain"/);
  assert.match(sidebar, /aria-label=\{expanded \? `收起 \$\{label\}` : `展开 \$\{label\}`\}/);
  assert.match(sidebar, /onClick=\{\(\) => onToggleExpand\(key\)\}/);
  assert.match(data, /"AI 工程"/);
  assert.match(knowledgeSources, /Agent 工作流为什么要拆成 Planner、Tool 和 Observation/);
  assert.match(knowledgeSources, /RAG 在当前项目中为什么作为 Agent 内部 Tool/);
  assert.match(knowledgeSources, /LangChain 和本项目自定义 Agent 编排有什么区别/);
  assert.match(filterBar, /未练/);
  assert.match(filterBar, /已掌握/);
  assert.match(filterBar, /需复习/);
  assert.match(card, /最近得分/);
  assert.match(card, /未自测/);
  assert.match(knowledgePage, /activeCardId/);
  assert.match(knowledgePage, /pendingScrollCardId/);
  assert.match(knowledgePage, /scrollIntoView/);
  assert.match(knowledgePage, /pendingAnchorAdjustmentRef/);
  assert.match(knowledgePage, /getBoundingClientRect\(\)\.top/);
  assert.match(knowledgePage, /window\.scrollBy/);
  assert.match(knowledgePage, /setSelection\(inferred\)/);
  assert.match(knowledgePage, /nextSelection\.cardId/);
  assert.doesNotMatch(knowledgePage, /setSelection\(\{\s*\.\.\.inferred,\s*cardId:\s*targetTopic\.id/);
  assert.match(sidebar, /activeCardId/);
});
