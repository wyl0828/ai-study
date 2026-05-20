const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");

const root = path.resolve(__dirname, "..");

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), "utf8");
}

test("self-test feedback tracks matched and missing key points", () => {
  const source = read("lib/knowledgeData.ts");

  assert.match(source, /missingKeyPoints:\s*string\[\]/);
  assert.match(source, /missingKeyPoints\s*=\s*topic\.keyPoints\.filter/);
  assert.match(source, /buildInterviewFeedback/);
  assert.match(source, /你已经提到了/);
  assert.match(source, /面试中如果只停留在这些点/);
  assert.match(source, /建议补充/);
});

test("knowledge feedback renders matched and missing sections without empty missing lists", () => {
  const source = read("components/KnowledgeFeedback.tsx");

  assert.match(source, /命中的核心记忆点/);
  assert.match(source, /缺失要点/);
  assert.match(source, /feedback\.missingKeyPoints\.length\s*>\s*0/);
});

test("accepted code review has field fallbacks and stale warning copy", () => {
  const source = read("components/AiDiagnosis.tsx");

  assert.match(source, /isDiagnosisStale=\{isDiagnosisStale\}/);
  assert.match(source, /该点评基于上次提交/);
  assert.match(source, /formatReviewField/);
  assert.match(source, /暂未返回/);
});

test("API errors distinguish backend reachability, SSE diagnosis, and knowledge/template fallbacks", () => {
  const api = read("lib/api.ts");
  const workspace = read("components/ProblemWorkspace.tsx");
  const knowledgePage = read("components/KnowledgeTrainingPage.tsx");

  assert.match(api, /export function formatApiError/);
  assert.match(api, /localhost:8080/);
  assert.match(api, /Piston/);
  assert.match(api, /AI_BASE_URL/);
  assert.match(api, /AI_API_KEY/);
  assert.match(api, /后端 Agent 日志/);
  assert.match(api, /同步 fallback/);
  assert.match(workspace, /formatApiError\(err,\s*"template"/);
  assert.match(workspace, /formatApiError\(err,\s*"sse"/);
  assert.match(api, /知识卡接口暂不可用/);
  assert.match(knowledgePage, /formatApiError\(err,\s*"knowledge"/);
});

test("dashboard coach helpers prioritize today's action and grouped mistake review", () => {
  const learning = read("lib/learningView.ts");
  const focus = read("components/TodayTrainingFocus.tsx");
  const mistakes = read("components/MistakeCards.tsx");
  const submissions = read("components/SubmissionHistory.tsx");

  assert.match(learning, /export function selectTodayTrainingItem/);
  assert.match(learning, /status\.toUpperCase\(\) === "PENDING"/);
  assert.match(learning, /status\.toUpperCase\(\) === "NEEDS_REVIEW" \|\| status\.toUpperCase\(\) === "RETRY"/);
  assert.match(learning, /export function buildDashboardCoachAdvice/);
  assert.match(focus, /今日优先训练/);
  assert.match(focus, /今日训练已完成，可以复盘最近错题/);
  assert.match(`${focus}\n${learning}`, /去做题|去复习/);
  assert.match(mistakes, /出现 \{m\.totalOccurrences\} 次/);
  assert.match(mistakes, /展开同类错误/);
  assert.match(mistakes, /已合并 \$\{m\.sourceCount\} 条原始记录/);
  assert.match(mistakes, /xl:grid-cols-3/);
  assert.match(mistakes, /slice\(0,\s*6\)/);
  assert.match(mistakes, /查看全部错题/);
  assert.match(submissions, /slice\(0,\s*8\)/);
  assert.match(submissions, /查看全部提交记录/);
});
