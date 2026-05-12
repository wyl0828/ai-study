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
  assert.match(source, /回答过短/);
  assert.match(source, /机制\/触发条件\/优化目的/);
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
  assert.match(api, /同步 fallback/);
  assert.match(workspace, /formatApiError\(err,\s*"template"/);
  assert.match(workspace, /formatApiError\(err,\s*"sse"/);
  assert.match(api, /知识卡接口暂不可用/);
  assert.match(knowledgePage, /formatApiError\(err,\s*"knowledge"/);
});
