const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");

const root = path.resolve(__dirname, "..");

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), "utf8");
}

test("mock interview page exists and is positioned as interview training", () => {
  const page = read("app/mock-interview/page.tsx");
  const component = read("components/MockInterviewPage.tsx");
  const navbar = read("components/Navbar.tsx");

  assert.match(page, /MockInterviewPage/);
  assert.match(component, /模拟面试/);
  assert.match(component, /一问一答追问式训练，模拟 Java 后端真实面试/);
  assert.match(component, /当前场次/);
  assert.match(component, /系统设计面试/);
  assert.match(component, /当前状态/);
  assert.match(component, /AI 面试官/);
  assert.match(component, /实时反馈/);
  assert.match(component, /提前结束并生成报告/);
  assert.match(component, /本场面试预览/);
  assert.match(component, /训练边界/);
  assert.match(component, /max-w-\[1280px\]/);
  assert.match(component, /max-w-\[1440px\]/);
  assert.match(component, /lg:grid-cols-\[minmax\(0,1fr\)_400px\]/);
  assert.match(component, /xl:grid-cols-\[minmax\(0,1fr\)_420px\]/);
  assert.match(navbar, /href:\s*"\/mock-interview"/);
  assert.match(navbar, /label:\s*"模拟面试"/);
  assert.match(navbar, /面试进行中/);
  assert.doesNotMatch(component, /通用聊天|RAG 聊天产品|自由聊天|生成追问中|生成下一题/);
});

test("mock interview separates start view from active workspace", () => {
  const component = read("components/MockInterviewPage.tsx");
  const startView = component.match(/function MockInterviewStartView[\s\S]*?interface WorkspaceProps/)[0];
  const workspace = component.match(/function MockInterviewWorkspace[\s\S]*?export default function MockInterviewPage/)[0];
  const branch = component.match(/if \(!session\) \{[\s\S]*?return \([\s\S]*?<MockInterviewStartView/)[0];

  assert.match(component, /function MockInterviewStartView/);
  assert.match(component, /function MockInterviewWorkspace/);
  assert.match(branch, /if \(!session\)/);
  assert.match(startView, /InterviewSetupPanel/);
  assert.match(startView, /InterviewPreviewCard/);
  assert.match(startView, /InterviewBoundaryCard/);
  assert.doesNotMatch(startView, /StatusCard|RealtimeFeedback|InterviewConversation|提前结束并生成报告/);
  assert.match(workspace, /StatusCard/);
  assert.match(workspace, /InterviewConversation/);
  assert.match(workspace, /RealtimeFeedback/);
  assert.match(workspace, /InterviewReport/);
});

test("mock interview API covers create, restore, answer, and finish", () => {
  const api = read("lib/api.ts");
  const types = read("lib/types.ts");

  assert.match(api, /export const mockInterviewApi/);
  assert.match(api, /"\/api\/mock-interviews"/);
  assert.match(api, /`\/api\/mock-interviews\/\$\{sessionId\}`/);
  assert.match(api, /`\/api\/mock-interviews\/\$\{sessionId\}\/answers`/);
  assert.match(api, /`\/api\/mock-interviews\/\$\{sessionId\}\/finish`/);
  assert.match(types, /export interface MockInterviewSession/);
  assert.match(types, /currentQuestion: string \| null/);
  assert.match(types, /currentTurnType: "MAIN" \| "FOLLOW_UP" \| null/);
});

test("page renders setup, conversation, turn feedback, and report states", () => {
  const component = read("components/MockInterviewPage.tsx");
  const setup = read("components/InterviewSetupPanel.tsx");
  const conversation = read("components/InterviewConversation.tsx");
  const feedback = read("components/InterviewTurnFeedback.tsx");
  const report = read("components/InterviewReport.tsx");

  assert.match(component, /InterviewSetupPanel/);
  assert.match(component, /InterviewConversation/);
  assert.match(component, /InterviewReport/);
  assert.match(setup, /questionCount/);
  assert.match(setup, /interviewerStyle/);
  assert.match(setup, /面试风格/);
  assert.match(setup, /选择训练方向/);
  assert.match(setup, /大厂追问型/);
  assert.match(setup, /基础巩固型/);
  assert.match(conversation, /currentQuestion/);
  assert.match(conversation, /submitAnswer/);
  assert.doesNotMatch(conversation, /补充优缺点|补充边界情况|重新组织回答|我不确定，请追问/);
  assert.doesNotMatch(conversation, /quickActions|点击上方快捷按钮/);
  assert.match(conversation, /输入你的回答\.\.\./);
  assert.match(conversation, /Send/);
  assert.match(feedback, /AI 面试官观察/);
  assert.match(feedback, /performanceLevel/);
  assert.match(feedback, /followUpReason/);
  assert.match(report, /averageScore/);
  assert.match(report, /recommendedCardIds/);
});

test("conversation uses an independently scrolling message area with fixed composer and auto-scroll", () => {
  const conversation = read("components/InterviewConversation.tsx");

  assert.match(conversation, /messagesEndRef/);
  assert.match(conversation, /scrollIntoView/);
  assert.match(conversation, /overflow-y-auto/);
  assert.match(conversation, /min-h-\[620px\]/);
  assert.match(conversation, /border-t/);
  assert.match(conversation, /这里会保留本轮问答记录，方便你复盘表达/);
});

test("right feedback has a compact empty state and keeps finish action inside the card", () => {
  const component = read("components/MockInterviewPage.tsx");

  assert.match(component, /FeedbackEmptyState/);
  assert.match(component, /等待你的第一轮回答/);
  assert.match(component, /表达清晰/);
  assert.match(component, /边界意识/);
  assert.match(component, /本轮建议/);
  assert.match(component, /回答后会在这里给出最需要补充的一点/);
  assert.match(component, /RealtimeFeedback[\s\S]*提前结束并生成报告[\s\S]*<\/section>/);
  assert.doesNotMatch(component, /<aside className="space-y-5">[\s\S]*<button[\s\S]*提前结束并生成报告/);
});

test("page restores session from backend and avoids durable localStorage training data", () => {
  const component = read("components/MockInterviewPage.tsx");

  assert.match(component, /mockInterviewApi\.get\(session\.sessionId\)/);
  assert.doesNotMatch(component, /localStorage\.setItem|localStorage\.getItem/);
});

test("copy keeps complete AC code boundary", () => {
  const component = read("components/MockInterviewPage.tsx");
  const conversation = read("components/InterviewConversation.tsx");

  assert.match(`${component}\n${conversation}`, /不直接给完整 Java AC 代码/);
  assert.match(`${component}\n${conversation}`, /思路、追问、表达和复盘/);
});

test("answer submission shows interviewer waiting state and disables input", () => {
  const component = read("components/MockInterviewPage.tsx");
  const conversation = read("components/InterviewConversation.tsx");
  const submitAnswer = component.match(/const submitAnswer = async \(\) => \{[\s\S]*?\n  \};/)[0];

  assert.match(component, /pendingAnswer/);
  assert.ok(submitAnswer.indexOf('setAnswer("")') < submitAnswer.indexOf("mockInterviewApi.answer"));
  assert.match(submitAnswer, /catch[\s\S]*setAnswer\(submitted\)/);
  assert.match(conversation, /面试官正在评估你的回答/);
  assert.match(conversation, /面试官在判断是否继续追问/);
  assert.match(conversation, /disabled=\{loading/);
  assert.match(conversation, /评估中/);
});

test("conversation keeps answered turns attached to their original questions", () => {
  const conversation = read("components/InterviewConversation.tsx");
  const turnMapIndex = conversation.indexOf("session.turns.map");
  const turnQuestionIndex = conversation.indexOf("turn.question");
  const turnAnswerIndex = conversation.indexOf("turn.userAnswer");
  const currentQuestionAfterTurnsIndex = conversation.indexOf("{showCurrentQuestion");

  assert.ok(turnMapIndex >= 0, "conversation should render persisted turns");
  assert.ok(turnQuestionIndex > turnMapIndex, "each completed turn must render its own question");
  assert.ok(turnQuestionIndex < turnAnswerIndex, "turn question must appear before that turn answer");
  assert.ok(currentQuestionAfterTurnsIndex > turnAnswerIndex, "currentQuestion should be rendered after completed turns as the next unanswered question");
});

test("conversation hides implementation scoring details until final report", () => {
  const conversation = read("components/InterviewConversation.tsx");
  const feedback = read("components/InterviewTurnFeedback.tsx");
  const report = read("components/InterviewReport.tsx");

  assert.doesNotMatch(`${conversation}\n${feedback}`, /得分\s*\{turn\.score\}|得分 100|fallback|keyPoints|命中率|命中要点|缺失要点/);
  assert.match(feedback, /回答优点/);
  assert.match(feedback, /需要加强/);
  assert.match(report, /总体表现/);
});
