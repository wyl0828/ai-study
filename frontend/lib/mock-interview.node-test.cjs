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
  assert.match(component, /max-w-\[1500px\]/);
  assert.match(component, /lg:grid-cols-\[minmax\(0,1fr\)_420px\]/);
  assert.match(component, /grid-cols-\[minmax\(0,1fr\)_380px\]/);
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
  assert.match(report, /report\.recommendedCardIds\.length/);
  assert.match(report, /同类面试复测/);
  assert.match(report, /暂无推荐卡时，直接进入同类面试复测/);
  assert.match(report, /href="\/mock-interview"/);
});

test("conversation uses an independently scrolling message area with fixed composer and auto-scroll", () => {
  const conversation = read("components/InterviewConversation.tsx");

  assert.match(conversation, /messagesEndRef/);
  assert.match(conversation, /scrollIntoView/);
  assert.match(conversation, /overflow-y-auto/);
  assert.match(conversation, /h-\[calc\(100dvh-300px\)\]/);
  assert.match(conversation, /min-h-0/);
  assert.match(conversation, /grid-rows-\[auto_minmax\(0,1fr\)_auto\]/);
  assert.match(conversation, /border-t/);
  assert.match(conversation, /最近问答记录/);
  assert.doesNotMatch(conversation, /mt-auto/);
});

test("conversation presents current question as a compact workbench instead of a chat bubble", () => {
  const conversation = read("components/InterviewConversation.tsx");
  const currentQuestionCard = conversation.match(/function CurrentQuestionCard[\s\S]*?function AnswerComposer/)[0];
  const returnBlock = conversation.match(/return \([\s\S]*?<\/section>\s*\);/)[0];

  assert.match(conversation, /function CurrentQuestionCard/);
  assert.match(currentQuestionCard, /当前问题/);
  assert.match(conversation, /面试官追问/);
  assert.match(conversation, /面试官主问题/);
  assert.match(currentQuestionCard, /回答建议/);
  assert.match(currentQuestionCard, /session\.currentQuestion/);
  assert.match(conversation, /function AnswerComposer/);
  assert.match(conversation, /你的回答/);
  assert.match(conversation, /min-h-\[88px\]/);
  assert.doesNotMatch(conversation, /min-h-28/);
  assert.ok(
    returnBlock.indexOf("<CurrentQuestionCard") < returnBlock.indexOf("最近问答记录"),
    "current question card should render before history"
  );
  assert.ok(
    returnBlock.indexOf("<AnswerComposer") > returnBlock.indexOf("最近问答记录"),
    "answer composer should render after history"
  );
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
  assert.doesNotMatch(component, /min-h-\[500px\]/);
  assert.doesNotMatch(component, /<aside className="space-y-5">[\s\S]*<button[\s\S]*提前结束并生成报告/);
});

test("right feedback is actionable instead of showing fixed scoring dimensions", () => {
  const component = read("components/MockInterviewPage.tsx");

  assert.match(component, /本轮主要问题/);
  assert.match(component, /缺失要点/);
  assert.match(component, /下一轮怎么补答/);
  assert.match(component, /displayMissingPoints/);
  assert.match(component, /expressionAdvice/);
  assert.match(component, /visibleMissingPoints/);
  assert.match(component, /还有 \{hiddenMissingCount\} 个要点待补齐/);
  assert.match(component, /h-full flex-col overflow-hidden/);
  assert.match(component, /flex-1 overflow-y-auto/);
  assert.doesNotMatch(component, /dimensionPresets/);
  assert.doesNotMatch(component, /buildFeedbackDimensions/);
  assert.doesNotMatch(component, /回答维度/);
  assert.doesNotMatch(component, /概念覆盖|机制理解|关键细节|表达结构/);
  assert.doesNotMatch(component, /style=\{\{ width: `\$\{Math\.max\(item\.value/);
  assert.doesNotMatch(component, /保持冷静，结构化表达能有效提升面试表现/);
});

test("workspace uses wider container with a narrower feedback rail", () => {
  const component = read("components/MockInterviewPage.tsx");
  const workspace = component.match(/function MockInterviewWorkspace[\s\S]*?export default function MockInterviewPage/)[0];

  assert.match(workspace, /max-w-\[1500px\]/);
  assert.match(workspace, /px-6/);
  assert.match(workspace, /xl:px-10/);
  assert.match(workspace, /gap-5/);
  assert.match(workspace, /grid-cols-\[minmax\(0,1fr\)_380px\]/);
  assert.match(workspace, /grid-rows-\[auto_minmax\(0,1fr\)_auto\]/);
  assert.doesNotMatch(workspace, /lg:grid-cols-\[minmax\(0,1fr\)_400px\]/);
  assert.doesNotMatch(workspace, /xl:grid-cols-\[minmax\(0,1fr\)_420px\]/);
});

test("right feedback uses backend turn fields without hard-coded Bean lifecycle advice", () => {
  const component = read("components/MockInterviewPage.tsx");
  const feedbackFunction = component.match(/function RealtimeFeedback[\s\S]*?function MockInterviewStartView/)[0];

  assert.match(feedbackFunction, /latest\.score/);
  assert.doesNotMatch(feedbackFunction, /averageScore\(session\?\.turns/);
  assert.doesNotMatch(component, /function enrichedMissingPoints/);
  assert.doesNotMatch(component, /function suggestedAnswerFramework/);
  assert.doesNotMatch(component, /function isBeanLifecycleTurn/);
  assert.doesNotMatch(feedbackFunction, /BeanPostProcessor|singleton 和 prototype|Spring Bean 生命周期大致包括/);
});

test("page restores session from backend and avoids durable localStorage training data", () => {
  const component = read("components/MockInterviewPage.tsx");
  const appPage = read("app/mock-interview/page.tsx");

  assert.match(appPage, /Suspense/);
  assert.match(component, /useSearchParams/);
  assert.match(component, /sessionId/);
  assert.match(component, /const sessionId = Number\(restoredSessionId\)/);
  assert.match(component, /mockInterviewApi\.get\(sessionId\)/);
  assert.match(component, /mockInterviewApi\.get\(session\.sessionId\)/);
  assert.match(component, /window\.history\.replaceState/);
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
  assert.match(conversation, /面试官正在分析你的回答/);
  assert.match(conversation, /分析完成后会给出追问或进入下一题/);
  assert.match(conversation, /const disabled = displayState === "EVALUATING" \|\| displayState === "REPORTED" \|\| loading/);
  assert.match(conversation, /disabled=\{disabled\}/);
  assert.match(conversation, /评估中/);
});

test("conversation keeps answered turns attached to their original questions", () => {
  const conversation = read("components/InterviewConversation.tsx");
  const turnMapIndex = conversation.indexOf("session.turns.map");
  const turnQuestionIndex = conversation.indexOf("turn.question");
  const turnAnswerIndex = conversation.indexOf("turn.userAnswer");
  const currentQuestionCardIndex = conversation.indexOf("<CurrentQuestionCard");
  const recentRecordIndex = conversation.indexOf("最近问答记录");

  assert.ok(turnMapIndex >= 0, "conversation should render persisted turns");
  assert.ok(turnQuestionIndex > turnMapIndex, "each completed turn must render its own question");
  assert.ok(turnQuestionIndex < turnAnswerIndex, "turn question must appear before that turn answer");
  assert.ok(currentQuestionCardIndex >= 0, "currentQuestion should render in the workbench card");
  assert.ok(currentQuestionCardIndex < recentRecordIndex, "current question card should appear before the history section");
});

test("recent interview records render as structured review cards instead of chat buttons", () => {
  const conversation = read("components/InterviewConversation.tsx");
  const reviewCard = conversation.match(/function ReviewTurnCard[\s\S]*?export default function InterviewConversation/)[0];
  const turnMapIndex = conversation.indexOf("session.turns.map");
  const reviewCardUseIndex = conversation.indexOf("<ReviewTurnCard", turnMapIndex);

  assert.match(reviewCard, /面试官/);
  assert.match(reviewCard, /你的回答/);
  assert.match(reviewCard, /\{question\}/);
  assert.match(reviewCard, /\{answer\}/);
  assert.match(reviewCard, /bg-surface-container-low|bg-slate-50/);
  assert.ok(reviewCardUseIndex > turnMapIndex, "completed turns should render through the review card");
  assert.match(conversation, /question=\{turn\.question\}/);
  assert.match(conversation, /answer=\{turn\.userAnswer\}/);
  assert.doesNotMatch(reviewCard, /bg-primary[\s\S]*\{answer\}/);
  assert.doesNotMatch(conversation, /function interviewerQuestion/);
  assert.doesNotMatch(conversation, /function userAnswer/);
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
