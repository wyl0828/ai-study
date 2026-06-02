const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");

const root = path.resolve(__dirname, "..");

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), "utf8");
}

test("problem page presents a three-panel interview workbench on desktop", () => {
  const page = read("app/problem/[id]/page.tsx");
  const description = read("components/ProblemDescription.tsx");
  const workspace = read("components/ProblemWorkspace.tsx");
  const editor = read("components/CodeEditor.tsx");
  const resultPanel = read("components/ResultPanel.tsx");
  const diagnosis = read("components/AiDiagnosis.tsx");
  const timeline = read("components/AgentTimeline.tsx");

  assert.match(page, /md:grid-cols-\[minmax\(280px,24vw\)_minmax\(0,1fr\)_minmax\(320px,30vw\)\]/);
  assert.match(page, /md:overflow-hidden/);
  assert.match(description, /coach-panel/);
  assert.doesNotMatch(description, /md:w-\[25%\]/);
  assert.match(workspace, /aria-label="代码编辑区"/);
  assert.match(workspace, /aria-label="运行结果和 AI 诊断"/);
  assert.doesNotMatch(workspace, /md:w-\[30%\]/);
  assert.match(editor, /Java 17/);
  assert.match(editor, /自动保存/);
  assert.match(editor, /title="重置代码"/);
  assert.match(editor, /title=\{buttonLabel\}/);
  assert.match(resultPanel, /执行观察/);
  assert.match(diagnosis, /Agent 诊断过程/);
  assert.match(timeline, /statusLabel/);
  assert.match(timeline, /formatDuration/);
});

test("global UI tokens provide reusable cards, buttons, tabs, and empty states", () => {
  const globals = read("app/globals.css");
  const nav = read("components/Navbar.tsx");
  const problemCard = read("components/ProblemCard.tsx");

  assert.match(globals, /\.coach-card/);
  assert.match(globals, /\.coach-primary-button/);
  assert.match(globals, /\.coach-secondary-button/);
  assert.match(globals, /\.coach-tab-list/);
  assert.match(globals, /\.coach-empty-state/);
  assert.match(nav, /coach-shell/);
  assert.match(problemCard, /coach-card/);
});

test("problem list behaves like a focused training entry page", () => {
  const home = read("components/HomeClient.tsx");
  const problemCard = read("components/ProblemCard.tsx");
  const sidebar = read("components/ProblemTrainingSidebar.tsx");

  assert.match(home, /题库训练台/);
  assert.match(home, /Hot100 Java Solution 模式/);
  assert.match(home, /difficultyCounts/);
  assert.match(home, /当前筛选/);
  assert.match(home, /筛选与搜索/);
  assert.match(home, /activeFilterSummary/);
  assert.match(home, /coach-card mb-5/);
  assert.match(home, /coach-empty-state/);
  assert.match(problemCard, /训练重点/);
  assert.match(problemCard, /进入训练/);
  assert.match(problemCard, /ArrowRight/);
  assert.match(sidebar, /coach-card/);
  assert.match(sidebar, /登录后会显示今日训练建议/);
  assert.match(sidebar, /if \(!userId\)/);
});

test("knowledge training page behaves like a structured knowledge workbench", () => {
  const page = read("components/KnowledgeTrainingPage.tsx");
  const sidebar = read("components/KnowledgeSidebar.tsx");
  const header = read("components/KnowledgeTopicHeader.tsx");
  const filterBar = read("components/KnowledgeFilterBar.tsx");
  const card = read("components/KnowledgeCard.tsx");

  assert.match(page, /coach-shell/);
  assert.match(page, /KnowledgeMetric/);
  assert.match(page, /知识卡总数/);
  assert.match(page, /当前专题/);
  assert.match(page, /需复习/);
  assert.match(page, /coach-empty-state/);
  assert.match(sidebar, /coach-panel/);
  assert.match(sidebar, /知识体系大纲/);
  assert.match(header, /知识训练台/);
  assert.match(header, /coach-pill/);
  assert.match(filterBar, /coach-card mb-5 p-4/);
  assert.match(filterBar, /筛选与搜索/);
  assert.match(card, /coach-card coach-card-hover/);
  assert.match(card, /面试题卡/);
  assert.match(card, /核心标签/);
});

test("mock interview page behaves like an interview training workbench", () => {
  const page = read("components/MockInterviewPage.tsx");
  const setup = read("components/InterviewSetupPanel.tsx");
  const conversation = read("components/InterviewConversation.tsx");
  const report = read("components/InterviewReport.tsx");

  assert.match(page, /coach-shell/);
  assert.match(page, /模拟面试训练台/);
  assert.match(page, /本场面试预览/);
  assert.match(page, /coach-pill/);
  assert.match(page, /coach-card/);
  assert.match(page, /面试状态/);
  assert.match(page, /当前场次/);
  assert.match(setup, /coach-card/);
  assert.match(setup, /coach-primary-button/);
  assert.match(setup, /面试配置/);
  assert.match(setup, /选择训练方向/);
  assert.match(conversation, /coach-card/);
  assert.match(conversation, /当前问题/);
  assert.match(conversation, /最近问答记录/);
  assert.match(report, /coach-card/);
  assert.match(report, /面试报告/);
});
