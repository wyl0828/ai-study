const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");

const root = path.resolve(__dirname, "..");

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), "utf8");
}

test("knowledge QA page exists and avoids generic chat positioning", () => {
  const page = read("app/rag-chat/page.tsx");
  const qaPage = read("components/RagChatPage.tsx");

  assert.match(page, /RagChatPage/);
  assert.match(qaPage, /知识库问答/);
  assert.match(qaPage, /学习资料问答/);
  assert.doesNotMatch(qaPage, /独立 RAG 聊天产品/);
  assert.doesNotMatch(qaPage, /通用聊天/);
});

test("frontend calls POST /api/rag/chat with temporary page state only", () => {
  const api = read("lib/api.ts");
  const types = read("lib/types.ts");
  const qaPage = read("components/RagChatPage.tsx");

  assert.match(api, /ragChatApi/);
  assert.match(api, /"\/api\/rag\/chat"/);
  assert.match(api, /method:\s*"POST"/);
  assert.match(types, /export interface RagChatRequest/);
  assert.match(types, /export interface RagChatResponse/);
  assert.match(types, /export interface RagChatSource/);
  assert.doesNotMatch(qaPage, /localStorage|EventSource|streamDiagnosis/);
});

test("assistant message keeps its own sources and source panel is selection based", () => {
  const qaPage = read("components/RagChatPage.tsx");
  const message = read("components/RagChatMessage.tsx");

  assert.match(qaPage, /sources\?: RagChatSource\[\]/);
  assert.match(qaPage, /selectedMessageId/);
  assert.match(qaPage, /lastAssistantWithSources/);
  assert.match(qaPage, /selectedSources/);
  assert.doesNotMatch(qaPage, /const \[sources,\s*setSources\]\s*=\s*useState<RagChatSource\[\]>/);
  assert.match(message, /sources\?: RagChatSource\[\]/);
  assert.match(message, /onSelect\?: \(\) => void/);
});

test("source panel does not globally erase previous message sources", () => {
  const qaPage = read("components/RagChatPage.tsx");

  assert.match(qaPage, /response\.data\.sources/);
  assert.match(qaPage, /sources:\s*response\.data\.sources \|\| \[\]/);
  assert.match(qaPage, /lastAssistantWithSources/);
  assert.doesNotMatch(qaPage, /setSources\(response\.data\.sources \|\| \[\]\)/);
  assert.doesNotMatch(qaPage, /setSources\(\[\]\)/);
});

test("sources component shows source type, title, score, and match reason", () => {
  const sources = read("components/RagChatSources.tsx");
  const types = read("lib/types.ts");

  assert.match(sources, /source\.sourceType/);
  assert.match(sources, /source\.title/);
  assert.match(sources, /source\.score/);
  assert.match(sources, /source\.snippet/);
  assert.match(sources, /source\.matchReason/);
  assert.match(sources, /命中原因/);
  assert.match(types, /matchReason:\s*string/);
});

test("learning record source labels include learning records, mistakes, and diagnosis", () => {
  const sources = read("components/RagChatSources.tsx");

  assert.match(sources, /USER_WEAKNESS:\s*"学习记录"/);
  assert.match(sources, /WEAKNESS_EVENT:\s*"学习记录"/);
  assert.match(sources, /MISTAKE_CARD:\s*"错题卡"/);
  assert.match(sources, /AI_DIAGNOSIS:\s*"历史诊断"/);
});

test("frontend preserves controlled refusal and full-code boundary answers from backend", () => {
  const qaPage = read("components/RagChatPage.tsx");
  const sources = read("components/RagChatSources.tsx");

  assert.match(qaPage, /content:\s*response\.data\.answer/);
  assert.doesNotMatch(qaPage, /天气|股票|完整 AC 代码/);
  assert.match(sources, /学习记录/);
});

test("knowledge QA page provides the four expected example questions", () => {
  const qaPage = read("components/RagChatPage.tsx");

  assert.match(qaPage, /HashMap 查询和写入顺序为什么会出错？/);
  assert.match(qaPage, /反转链表为什么最后要返回 prev？/);
  assert.match(qaPage, /我的最近错题主要集中在哪些知识点？/);
  assert.match(qaPage, /Spring Bean 生命周期怎么回答更像面试？/);
});
