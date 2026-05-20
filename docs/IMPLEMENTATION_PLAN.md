# AI Interview Coach Agent 实现计划

## 1. Context

用户背景：

- 民办本科大三学生
- 目标岗位是 Java 后端开发
- 希望 2 到 3 周完成一个能写进简历、能现场演示、有 Agent 感的项目
- 前端经验较少，所以前端实现需要简单、直接、页面数量可控

项目定位：

```text
AI Interview Coach Agent = 基于 Agent Workflow 的 Java 代码诊断与面试训练系统
```

项目不是普通刷题平台，也不是 Spring Boot 调大模型 API 的聊天壳，而是一个能够调用判题工具、观察测试结果、检索相关知识和历史学习记忆、分析错误思维、分层提示、记录薄弱点、生成训练计划的面试训练 Agent。当前已在不削弱 Agent 主线的前提下，新增独立后端知识训练入口，并把 MySQL 结构化 RAG V1 接入 Agent Workflow。

Agent-first 核心闭环：

```text
Agent 收到任务
  -> Planner 决定要判题
  -> CodeExecutionTool 执行 Java 代码
  -> Observation 返回测试结果
  -> RagRetrieveTool 检索题目知识 / 知识卡 / 用户历史记忆（非核心，失败不阻塞）
  -> ErrorClassifierTool 分类错误
  -> WeaknessTrackerTool 更新长期弱点（非核心，失败不阻塞）
  -> TrainingPlannerTool 生成后续训练建议（非核心，失败不阻塞）
```

关键约束：

- 前端使用 Next.js 14 + Tailwind CSS + Monaco Editor
- 后端使用 Spring Boot 3 + Java 17
- 数据库使用 MySQL 8
- Java 持久层使用 MyBatis-Plus
- 预留 Redis 配置，热点题目/题目详情缓存待接入
- AI 接口使用 Anthropic 兼容 API，可接 Claude 或兼容服务
- 代码执行第一版使用 Piston API
- 第一版只支持 Java
- 后期通过接口替换为 Docker 沙箱
- Agent Workflow 使用状态机 + 工具链实现
- SSE 用于流式输出 Agent 步骤
- Agent Step / Trace 用于记录每一步执行状态和耗时

## 2. 技术栈

| 层 | 技术 | 说明 |
| --- | --- | --- |
| 前端 | Next.js 14 + Tailwind CSS + Monaco Editor | App Router，页面尽量少 |
| 后端 | Spring Boot 3 + Java 17 | REST API，核心业务逻辑 |
| 数据库 | MySQL 8 | 题目、提交记录、用户训练数据 |
| 持久层 | MyBatis-Plus | Mapper、CRUD、分页、SQL 扩展 |
| 缓存 | Redis | 当前仅预留配置；热点题目、题目详情和临时状态缓存待后续接入 |
| AI | Anthropic 兼容 API | 错误分析、Code Review；训练计划当前为确定性 fallback |
| Agent Workflow | 状态机 + Tool 链 | Planner、Tool 调用、Observation、Memory 更新 |
| RAG V1 | MySQL 结构化检索 | 检索题目、知识卡、AI 诊断和错题卡，不引入向量库 |
| 判题 | Piston API | 免费代码执行，后期替换为 Docker |
| 流式输出 | SSE | Agent 执行步骤和诊断结果流式返回 |
| Trace | AgentStep 记录 | 记录 Tool 输入摘要、输出摘要、状态和耗时 |

### 当前落地状态快照

- Phase 1 后端骨架、题目接口、Piston 判题和提交持久化已完成。
- Phase 2 Agent Workflow 后端已完成：`AgentRun`、`AgentStep`、AI 诊断、弱点记忆、错题卡、训练计划均已接入 MySQL。题目预设分层提示已迁移到后端 `problem` 表，AI 不再生成三层提示。
- 当前真实暴露的 Controller：`ProblemController`、`SubmissionController`、`AgentController`、`UserController`、`KnowledgeController`。
- 当前真实暴露的 Agent 接口：`POST /api/agent/analyze`、`GET /api/submissions/{submissionId}/diagnosis/stream`。
- Phase 3 前端核心页面已完成：`/`、`/problem/[id]`、`/dashboard` 均已按 `stitch_front_end_interface_design/mvp/` HTML 原型做紧凑 MVP 风格还原，并完成中文化。
- 知识训练页 V1 已完成：`/knowledge` 优先调用后端知识接口读取 `knowledge_card` 真实数据，接口失败时回退本地示例数据；支持搜索、难度筛选、分类筛选、模拟自测评分、点评反馈、标杆回答解析、高频追问和标记已掌握。
- 后端知识训练一期能力已具备：已有 `knowledge_card` 表和 120 张结构化知识卡片，侧边栏 24 个最终专题每个至少 5 张；内容参考小林 coding 和 JavaGuide 选题覆盖后原创整理，AI 工程内容按本项目设计原创组织。知识卡内容源为 `scripts/knowledge_card_profiles.cjs`，生成脚本会同步输出 `data/knowledge_cards.sql` 和 `frontend/lib/knowledgeSeed.ts`。
- 知识卡内容质量整改已完成：停用自动扩写，问题改为直白短问法，答案按真实面试训练口吻整理，并用内容质量测试拦截模板问法、模板语义污染、空泛 keyPoints、无关 followUps 和高风险卡核心关键词缺失。
- RAG V1 已完成：新增 `rag_document` / `rag_chunk`、`RagService`、`RagRetrieveTool` 和 `RAG_RETRIEVAL` Agent 步骤，检索结果作为错误诊断、AC 点评和训练计划知识卡选择的内部证据。
- 当前做题页已完成提示/诊断去重：左侧展示题目预设 Level 1/2/3 分层提示（优先从后端 `presetHints` 读取），右侧只保留测试结果和 AI 诊断；提交失败后通过 SSE 实时展示 Agent 步骤，完成后展示诊断结果；AC 代码点评生成中同样展示实时 Agent 步骤。
- Dashboard 已通过 `UserController` 接入真实 MySQL 学习数据，并完成教练化重排：统计卡片之后优先展示今日训练项和按天分页的完整训练计划，后续展示薄弱排行、错误类型分布、最近提交、合并错题卡、后端知识训练入口和确定性 AI 教练建议；训练计划条目支持完成/跳过，手动重新生成训练计划已暴露接口。单独 hint 查询和独立 accepted-code review REST 端点留到后续阶段。
- 训练计划 item 已兼容扩展为 `PROBLEM` / `KNOWLEDGE_CARD` 两类，`TrainingPlannerTool` 保留原有 3 条算法复盘项，并最多选取 3 张 RAG 命中的知识卡片；手动重新生成会为 3 天每天安排 1 个算法复盘任务和 1 个知识卡复习任务，不根据算法错因强行推荐八股。
- 题库提交模式已升级为 Hot100 精选 12 题统一 Solution 形态：用户提交非 `public` 的 `class Solution`，后端通过 `CodeWrapper` 注册表按题号包装为 `Main.java` 后送入 Piston。
- 12 题均已补齐面试式题面、3 个测试用例、三层预设提示、solution outline 和完整 Java 参考实现。
- `/problem/[id]` 页面现在由浏览器端 `ProblemWorkspace` 请求 `/api/problems/{id}/template`，Monaco 使用后端返回模板初始化；重置代码会重新读取后端模板而不是恢复写死默认值。
- 最新接口文档以 `docs/API.md` 为准。

## 3. 项目结构

```text
interview-coach/
├── frontend/
│   ├── app/
│   │   ├── page.tsx
│   │   ├── problem/
│   │   │   └── [id]/
│   │   │       └── page.tsx
│   │   ├── dashboard/
│   │   │   └── page.tsx
│   │   ├── knowledge/
│   │   │   └── page.tsx
│   │   └── layout.tsx
│   ├── components/
│   │   ├── CodeEditor.tsx
│   │   ├── TestResult.tsx
│   │   ├── ProblemHintPanel.tsx
│   │   ├── AiDiagnosis.tsx
│   │   ├── ResultPanel.tsx
│   │   ├── ProblemCard.tsx
│   │   ├── ProblemDescription.tsx
│   │   ├── ProblemWorkspace.tsx
│   │   ├── WeaknessList.tsx
│   │   ├── MistakeCards.tsx
│   │   ├── SubmissionHistory.tsx
│   │   ├── TrainingPlan.tsx
│   │   ├── KnowledgeTrainingPage.tsx
│   │   ├── KnowledgeCard.tsx
│   │   ├── KnowledgeSelfTest.tsx
│   │   ├── KnowledgeFeedback.tsx
│   │   └── KnowledgeTrainingEntry.tsx
│   └── lib/
│       ├── api.ts
│       ├── draft.ts
│       ├── i18n.ts
│       ├── knowledgeData.ts
│       ├── problemHints.ts
│       └── types.ts
│
├── backend/
│   └── src/main/java/com/interview/coach/
│       ├── controller/
│       │   ├── ProblemController.java
│       │   ├── SubmissionController.java
│       │   ├── AgentController.java
│       │   ├── UserController.java
│       │   └── KnowledgeController.java
│       ├── service/
│       │   ├── ProblemService.java
│       │   ├── SubmissionService.java
│       │   ├── JudgeService.java
│       │   ├── AgentService.java
│       │   ├── RagService.java
│       │   ├── LearningTracker.java
│       │   ├── TrainingPlanService.java
│       │   ├── KnowledgeCardService.java
│       │   ├── KnowledgeLearningService.java
│       │   ├── UserLearningService.java
│       │   └── impl/
│       │       ├── ProblemServiceImpl.java
│       │       ├── SubmissionServiceImpl.java
│       │       ├── CodeWrapper.java
│       │       ├── JudgeServiceImpl.java
│       │       ├── AgentServiceImpl.java
│       │       ├── LearningTrackerImpl.java
│       │       ├── RagServiceImpl.java
│       │       ├── KnowledgeCardServiceImpl.java
│       │       ├── KnowledgeLearningServiceImpl.java
│       │       ├── UserLearningServiceImpl.java
│       │       └── TrainingPlanServiceImpl.java
│       ├── agent/
│       │   ├── InterviewCoachAgent.java
│       │   ├── AgentContext.java
│       │   ├── AgentStep.java
│       │   └── tool/
│       │       ├── Tool.java
│       │       ├── CodeExecutionTool.java
│       │       ├── RagRetrieveTool.java
│       │       ├── ErrorClassifierTool.java
│       │       ├── CodeReviewTool.java
│       │       ├── WeaknessTrackerTool.java
│       │       └── TrainingPlannerTool.java
│       ├── integration/
│       │   ├── piston/
│       │   │   └── PistonClient.java
│       │   └── ai/
│       │       └── AnthropicCompatibleClient.java
│       ├── entity/
│       │   ├── User.java
│       │   ├── Problem.java
│       │   ├── TestCase.java
│       │   ├── Submission.java
│       │   ├── AgentRun.java
│       │   ├── AgentStepEntity.java
│       │   ├── AiDiagnosis.java
│       │   ├── HintRecord.java
│       │   ├── UserWeakness.java
│       │   ├── UserWeaknessEvent.java
│       │   ├── TrainingPlan.java
│       │   ├── TrainingPlanItem.java
│       │   ├── KnowledgeCard.java
│       │   ├── SelfTestRecord.java
│       │   ├── UserKnowledgeCardMastery.java
│       │   ├── RagDocument.java
│       │   ├── RagChunk.java
│       │   └── MistakeCard.java
│       ├── mapper/
│       │   ├── UserMapper.java
│       │   ├── ProblemMapper.java
│       │   ├── TestCaseMapper.java
│       │   ├── SubmissionMapper.java
│       │   ├── AgentRunMapper.java
│       │   ├── AgentStepMapper.java
│       │   ├── AiDiagnosisMapper.java
│       │   ├── HintRecordMapper.java
│       │   ├── UserWeaknessMapper.java
│       │   ├── UserWeaknessEventMapper.java
│       │   ├── TrainingPlanMapper.java
│       │   ├── TrainingPlanItemMapper.java
│       │   ├── KnowledgeCardMapper.java
│       │   ├── SelfTestRecordMapper.java
│       │   ├── UserKnowledgeCardMasteryMapper.java
│       │   ├── RagDocumentMapper.java
│       │   ├── RagChunkMapper.java
│       │   └── MistakeCardMapper.java
│       ├── dto/
│       ├── vo/
│       ├── enums/
│       ├── config/
│       │   ├── AiProperties.java
│       │   ├── PistonProperties.java
│       │   ├── SseConfig.java
│       │   ├── RedisConfig.java
│       │   └── CorsConfig.java
│       ├── handler/
│       └── CoachApplication.java
│
└── data/
    ├── schema.sql
    ├── problems.sql
    ├── knowledge_cards.sql
    └── knowledge_training_migration.sql
```

### MyBatis-Plus 接入约定

后端默认使用 MyBatis-Plus，不使用 JPA。

推荐分层：

```text
Controller -> Service -> Mapper -> MySQL
```

开发约定：

- Mapper 放在 `backend/src/main/java/com/interview/coach/mapper/`。
- 简单 CRUD 使用 `BaseMapper<T>`。
- 复杂查询可以使用 XML 或注解 SQL。
- Controller 不直接调用 Mapper。
- Controller 不直接返回数据库实体，优先返回 VO。
- 分页查询优先使用 MyBatis-Plus 分页插件。

### 后端分包职责

```text
controller：接收 HTTP 请求，调用 service，返回 VO
service：业务接口
service/impl：业务实现和流程编排
agent：Agent 编排器、上下文、步骤记录和 AI-native 逻辑
agent/tool：代码执行、错误分类、代码点评、弱点更新、训练计划等 Tool
integration/piston：Piston 代码执行 API 接入
integration/ai：Anthropic 兼容模型 API 接入
entity：数据库实体
mapper：MyBatis-Plus 数据访问
dto：请求参数和内部命令对象
vo：响应结果对象
enums：Agent 状态、提交状态、难度、语言、错误类型、提示等级枚举
config：配置类
handler：全局异常处理和统一响应处理
```

## 4. 实现阶段

### 阶段 1：后端骨架与题库，Day 1-3

状态：已完成并通过本地验证。

落地结果：

- 已创建 Spring Boot 后端、MySQL、Redis 预留配置、CORS 配置、基础实体、Mapper 和统一响应结构。
- 已实现 `ProblemController`、`SubmissionController`、`ProblemService`、`SubmissionService`、`JudgeService`、`PistonClient`。
- 当前判题路径为 `SubmissionController -> SubmissionService -> JudgeService -> PistonClient`，Controller 不直接调用 Mapper 或 Piston。
- 当前 MVP 种子数据为 Hot100 精选 12 道题、36 条测试用例、1 个 demo 用户。
- `SubmissionServiceImpl` 保存用户原始 `class Solution` 代码；当前题库仅在送入 Piston 前通过 `CodeWrapper.wrap(...)` 包装为 `Main.java`。
- 本地 Piston API 地址为 `http://localhost:2000/api/v2`，`PistonClient` 使用 HTTP/1.1。

### 阶段 2：Agent Workflow 核心，Day 4-7

状态：已完成并通过本地验证。

落地结果：

- 已实现 `AnthropicCompatibleClient`、`AiProperties`、`Tool<I, O>`、`InterviewCoachAgent`、`AgentContext`、`AgentStep` 和核心 Tool。
- 当前 Tool 包括 `CodeExecutionTool`、`RagRetrieveTool`、`ErrorClassifierTool`、`CodeReviewTool`、`WeaknessTrackerTool`、`TrainingPlannerTool`。
- 失败提交路径：重新判题 -> `RagRetrieveTool` 检索证据 -> 错误分类 -> 弱点/错题卡持久化 -> 确定性 3 天训练计划。
- AC 提交路径：重新判题 -> `RagRetrieveTool` 检索证据 -> `CodeReviewTool` 生成轻量代码点评，跳过错题卡和训练计划写入；代码点评失败时记录失败 step 并降级返回 accepted 结果。
- `CodeExecutionTool` 调用 `SubmissionService.rejudge(...)`，不直接调用 Piston。
- `hint_record` 表保留，当前 `HINT_GENERATION` 步骤停用，不再写入新 AI hint 记录。
- SSE 事件名为 `agent_step`、`done`、`error`，同步 `POST /api/agent/analyze` 保留为 fallback 和 API 层演示入口。

### 阶段 3：前端页面，Day 8-12

状态：已完成核心页面，并通过本地构建验证。

落地结果：

- 已实现 `/`、`/problem/[id]`、`/dashboard` 和 `/knowledge`。
- 做题页采用三栏布局：左侧题目描述与预设提示，中间 Monaco Editor，右侧测试结果 / AI 诊断。
- 模板加载由浏览器端 `ProblemWorkspace` 请求 `/api/problems/{id}/template`，重置代码会清草稿并重新读取后端模板。
- 前端 SSE 已使用 `fetch + ReadableStream` 接入；`done` 丢失时 fallback 到同步 `POST /api/agent/analyze`。
- 做题页右侧只保留“测试结果”和“AI 诊断”，不再展示右侧分层提示 tab。
- Dashboard 从 `/api/users/1/...` 读取真实 MySQL 数据，包括统计、薄弱点、错题、训练计划、错误统计和最近提交。
- 已运行 `npm run build`，Next.js 编译、类型检查和页面生成通过。

### 阶段 4：训练计划与错题本，Day 13-16

状态：已完成真实数据闭环。Phase 2 已完成后端持久化闭环；Phase 4 已补齐 Dashboard 查询接口，并将前端 Dashboard 从 mock 数据切换为真实 MySQL 数据。

目标：实现学习闭环，让项目不只是一次性 AI 分析。

落地结果：

- `LearningTracker` 在失败诊断流程中持久化 `ai_diagnosis`、`user_weakness`、`mistake_card`。
- `LearningTracker` 同步写入 `user_weakness_event`，错题卡按 fingerprint 合并重复错误。
- `TrainingPlanService` 保存 3 天训练计划和 `training_plan_item`。
- `TrainingPlanService` 支持条目状态更新、计划完成状态和手动重新生成。
- `UserController` 已暴露 Dashboard 查询接口和学习连续性写接口：统计、薄弱点、弱点事件、错题、最新训练计划、训练计划状态更新、手动重新生成、最近提交、错误统计、知识卡自测提交和最近自测记录。
- Dashboard 从 MySQL 持久化数据读取学习数据，无数据时显示空状态，不回退 mock 数据。
- 当前仍不暴露单独 hint 查询接口。

### 阶段 4.5：后端知识训练一期

状态：后端基础能力已完成；前端 `/knowledge` 页面 V1 已接入真实知识接口，并保留本地示例数据兜底。

目标：在不改变算法诊断主线的前提下，增加独立后端知识训练入口，并让训练计划可以同时展示算法题训练和知识卡片复习。

已完成内容：

1. 新增 `knowledge_card` 表和 `KnowledgeCard`、`KnowledgeCardMapper`、`KnowledgeCardService`、`KnowledgeController`。
2. 新增 `GET /api/knowledge/categories`、`GET /api/knowledge/cards`、`GET /api/knowledge/cards/{id}`。
3. 新增 `/knowledge` 页面，前端 V1 知识树组织为 Java 核心 / 数据库 / Spring / AI 工程，后端分类为 Java / MySQL / Redis / Spring / JVM / AI。
4. 新增 120 张结构化知识卡片种子数据，侧边栏 24 个最终专题每个至少 5 张；来源字段标记为“小林 coding / JavaGuide”“JavaGuide”“项目原创整理”等。内容维护从 `scripts/knowledge_card_profiles.cjs` 开始，运行 `node scripts/generate_knowledge_cards_sql.cjs` 后同时生成 MySQL SQL 和前端离线 fallback seed。
5. 扩展 `training_plan_item`：`item_type`、`knowledge_card_id`、`knowledge_card_title`。
6. 训练计划展示区支持区分“算法题：xxx”和“知识卡片：xxx”。
7. 前端 V1 优先使用 `GET /api/knowledge/categories`、`GET /api/knowledge/cards` 和 `GET /api/knowledge/cards/{id}`，接口失败时回退本地示例知识点。
8. 用户学习接口已支持知识卡自测提交和最近自测记录查询，自测记录写入 `self_test_record`，并更新 `user_knowledge_card_mastery`。

边界约束：

- `/knowledge` 页面不是 RAG 问答入口；知识内容来自 MySQL 结构化种子数据，并会被 RAG V1 索引为 Agent 内部证据。
- 算法错误只推荐算法相关训练，不强行映射到 MySQL / Redis / Spring。
- Agent 自动训练计划保留算法复盘项，并最多补入 3 张 RAG 命中的知识卡片；手动重新生成计划按 3 天组织，每天 1 个算法复盘任务和 1 个知识卡复习任务。
- 暂不把收藏、独立 RAG 聊天 / 检索 REST 接口或 `knowledge_weakness` 表落到后端；知识卡掌握度、自测记录和 RAG V1 内部索引已持久化。
- 知识卡不是运行时 AI 生成内容；问题、答案、keyPoints、followUps 都应在 profile 中显式维护，不恢复 `enrichAnswer` 这类统一扩写逻辑。
- 导入新 `data/knowledge_cards.sql` 后，如库中已有 `rag_document` / `rag_chunk`，需要执行 `RagService.rebuildSystemIndex()` 或等价维护流程，保证 Agent 使用新知识卡 chunk。

### 阶段 5：产品打磨，Day 17-20

状态：进行中。SSE 前端接入、题目预设提示迁移后端、ProblemNavigator 上下题切换、README、知识训练页 V1、4.1 小范围产品增强、4.2 核心闭环回归护栏和 RAG V1 内部检索层已完成；完整 demo 复盘、截图录屏和面试 Q&A 留到最终阶段。

目标：在不扩大 MVP 边界的前提下，优先完成小范围产品增强，再稳定核心闭环。

第一优先级：小范围产品增强

1. 优化知识训练反馈：
   - 给点评反馈区补“缺失要点”，根据核心记忆点展示用户还没覆盖的内容。
   - 低分反馈更自然，明确指出回答过短、缺少机制、触发条件或优化目的。
   - 自测评分仍在前端轻量计算，后端持久化自测记录和知识卡掌握度；低分自测写入弱点事件。
2. 完善 AC 代码点评展示：
   - 复用后端已有 `CodeReviewTool` / `codeReview` 分支。
   - 前端把 AC 后的复杂度、代码风格、面试表达建议和可优化点展示得更清楚。
   - 代码通过后即使用户继续编辑但未重新提交，右侧仍保留上次 AC 点评，并用 stale warning 提醒“基于上次提交，仅供参考”。
   - AC 点评生成过程中展示 SSE Agent 步骤，避免通过分支没有实时流程。
   - 不新增单独 accepted-code review REST 接口，不生成完整答案。
3. 改善错误状态：
   - 后端、Piston、AI、SSE 任一服务未启动时，前端给出更明确的排查提示。
   - 避免只显示“请求失败”，优先提示检查 Spring Boot、Piston、AI 配置或 SSE fallback。

第二优先级：稳定核心闭环

4. 保持问题页边界：左侧只放题目预设提示，右侧只放测试结果和 AI 诊断 / AC 点评。
5. 稳住 SSE 流程：用 streamId、AbortController 和回归测试保护多次提交、旧流覆盖、用户中断、fallback 同步分析等边界。
6. Dashboard 数据继续收口：统计、薄弱点、错题卡、错误分布、训练计划都来自 MySQL，不回退 mock。

4.2 当前落地结果：

- 做题页继续保持左侧 `ProblemDescription + ProblemHintPanel`，Level 1/2/3 题目预设提示默认折叠；右侧 `ResultPanel` 只保留“测试结果”和“AI 诊断”，AC 点评在 AI 诊断 tab 内展示。
- SSE 仍是前端主路径，`ProblemWorkspace` 只在 SSE error、SSE 正常结束但无 `done`、或 `done` 数据无效时调用同步 `POST /api/agent/analyze` fallback。
- `AiDiagnosis` 不再把 AC 点评展示绑定到当前编辑器代码是否等于上次提交快照；只要最终结果包含 `codeReview`，就展示点评。当前代码变更时只切换为 stale warning，而不是隐藏点评。
- AC 点评生成中与失败诊断一样展示 `AgentTimeline`，实时呈现 `PLANNING -> CODE_EXECUTION -> OBSERVATION -> RAG_RETRIEVAL -> CODE_REVIEW -> COMPLETED`。
- `frontend/lib/agentStreamState.ts` 抽出 streamId 新鲜度和 fallback 决策，便于用 Node 测试覆盖旧流保护。
- 再次提交和组件卸载会 abort 当前流；旧流的 step / done / error / end 不得覆盖新提交状态。
- Dashboard 继续只从 `userApi` 的 MySQL-backed endpoints 加载统计、薄弱点、错题卡、错误分布、训练计划和最近提交；空数组或 null 展示空状态，不回退 `frontend/lib/mock.ts`。
- Dashboard 展示顺序已调整为“统计 -> 今日优先训练 -> 完整训练计划 -> 薄弱排行 / 错误类型分布 -> 最近提交 -> 合并错题卡 -> AI 教练建议”。完整训练计划以第 1/2/3 天分页展示，今日训练项复用最新训练计划中的 `PENDING`、`NEEDS_REVIEW` 或 `RETRY` 条目，跳转仍使用 `/problem/{id}` 和 `/knowledge?cardId=...`。
- 错误统计区只保留错误类型分布，避免和“薄弱知识点排行”重复；错题卡继续复用 fingerprint / repeatCount，并在前端按题目、知识点和用户可读错误模式聚合，展示“出现 N 次”、本质问题、修复动作和复盘口令。
- `frontend/lib/core-loop-stability.node-test.cjs` 覆盖 tab 边界、预设提示边界、SSE fallback 条件、旧流拦截、abort 行为、Dashboard no mock、今日优先训练前置、训练计划跳转和空状态文案。

第三优先级：暂不做但保留

7. 单独 hint 查询接口。
8. 单独 accepted-code review REST 接口。
9. 知识卡收藏。
10. 独立 RAG 聊天 / 检索 REST 接口。
11. Redis 热点缓存真正接入。

验收标准：

- 核心闭环边界保持清晰：题目预设提示在左侧，AI 诊断解释本次提交错误。
- AC 提交通过后能展示清晰的代码点评；点评生成中能展示实时 Agent 步骤；用户改动代码但未重新提交时，旧点评保留并显示 stale warning。失败提交仍展示错误诊断和 Agent 步骤。
- 知识训练低分反馈能指出缺失要点；跳过自测仍不显示评分和点评。
- 本地依赖未启动时，错误文案能指向后端、Piston、AI 或 SSE 诊断方向。
- `node --test frontend\lib\*.node-test.cjs` 覆盖 Phase 5 产品增强和核心闭环边界，并保持通过。
- README 能说明项目定位、技术栈、架构图、启动方式和演示流程。
- 体验增强不引入多语言、Docker 沙箱、大规模题库或复杂权限系统。

最终阶段再集中考虑：跑通 `1/206/121` 三题完整 demo 并记录失败点、对齐 API/实现/设计文档细节、明确 `hint_record` 最终策略、采集截图或录屏、整理 Agent Workflow 面试 Q&A。

## 5. 核心接口设计

### 5.1 题目接口

```http
GET /api/problems
GET /api/problems/{id}
GET /api/problems/{id}/template
```

### 5.2 提交与判题接口

```http
POST /api/submissions
```

请求：

```json
{
  "problemId": 1,
  "userId": 1,
  "language": "java",
  "code": "class Solution { ... }"
}
```

响应：

```json
{
  "submissionId": 1001,
  "status": "WRONG_ANSWER",
  "passedCount": 7,
  "totalCount": 10,
  "runtime": 120,
  "memory": 32768,
  "errorMessage": "case 8 failed"
}
```

### 5.3 Agent Workflow 接口

```http
POST /api/agent/analyze
GET /api/submissions/{submissionId}/diagnosis/stream
```

`POST /api/agent/analyze` 请求：

```json
{
  "submissionId": 1001
}
```

响应：

```json
{
  "agentRunId": 1,
  "submissionId": 1001,
  "errorType": "LOGIC_ERROR",
  "knowledgePoint": "HashMap Lookup in Array Traversal",
  "specificError": "Self-matching due to incorrect map operation order",
  "diagnosis": "Code adds current element before checking, allowing same-element pairing.",
  "codeReview": null,
  "hintLevel1": null,
  "hintLevel2": null,
  "hintLevel3": null,
  "trainingPlanTitle": "3-day recovery plan: HashMap Lookup in Array Traversal",
  "steps": [
    {
      "stepName": "PLANNING",
      "toolName": null,
      "status": "SUCCESS",
      "inputSummary": "Prepare agent context",
      "outputSummary": "Context ready",
      "durationMs": 8,
      "errorMessage": null
    }
  ]
}
```

SSE 示例：

```text
event:agent_step
data:{"stepName":"PLANNING","toolName":null,"status":"RUNNING","inputSummary":"Prepare agent context","outputSummary":null,"durationMs":null,"errorMessage":null}

event:agent_step
data:{"stepName":"RAG_RETRIEVAL","toolName":"RagRetrieveTool","status":"SUCCESS","inputSummary":"Retrieve problem knowledge and user learning memory","outputSummary":"RAG evidence ready","durationMs":18,"errorMessage":null}

event:agent_step
data:{"stepName":"ERROR_CLASSIFICATION","toolName":"ErrorClassifierTool","status":"SUCCESS","inputSummary":"Classify execution observation","outputSummary":"Diagnosis ready","durationMs":23548,"errorMessage":null}

event:done
data:{"agentRunId":1,"submissionId":1001,"errorType":"LOGIC_ERROR","knowledgePoint":"HashMap Lookup in Array Traversal","codeReview":null,"hintLevel1":null,"hintLevel2":null,"hintLevel3":null,"trainingPlanTitle":"3-day recovery plan: HashMap Lookup in Array Traversal","steps":[...]}
```

### 5.4 用户学习接口

状态：Dashboard 读取接口已通过 `UserController` 暴露；手动重新生成训练计划、训练项状态更新、知识卡自测提交和最近自测记录查询均已实现。

```http
GET /api/users/{userId}/dashboard/stats
GET /api/users/{userId}/weaknesses
GET /api/users/{userId}/weakness-events/recent
GET /api/users/{userId}/training-plans/latest
PATCH /api/users/{userId}/training-plans/items/{itemId}/status
POST /api/users/{userId}/training-plans/regenerate
GET /api/users/{userId}/mistakes
GET /api/users/{userId}/dashboard/error-stats
GET /api/users/{userId}/submissions/recent
POST /api/users/{userId}/knowledge/cards/{cardId}/self-tests
GET /api/users/{userId}/knowledge/cards/{cardId}/self-tests/recent
```

联调注意事项：

- 如果 Dashboard 请求 `/api/users/1/...` 返回 `No static resource api/users/...`，优先检查后端是否重启到了包含 `UserController` 的最新代码。
- 如果其他工具或同学判断“后端没有 UserController”，检查他们看的是否是旧提交、远程仓库，或是否忽略了 untracked 新文件。
- 完成提交或交付前，需要把 `UserController`、`UserLearningService`、`TrainingPlanService`、`KnowledgeLearningService`、对应 service impl、Dashboard / 训练计划 / 自测 VO 和相关测试一起纳入版本控制。
- 验证命令：`cd backend; mvn test` 和 `cd frontend; npm run build`。

## 6. Prompt 设计

### 6.1 ErrorClassifierTool System Prompt

```text
你是 AI Interview Coach Agent 中的 ErrorClassifierTool。你的任务是分析用户的代码提交、题目信息、CodeExecutionTool 返回的 Observation，以及 RagRetrieveTool 返回的辅助证据，判断错误类型。

你不能直接给出完整答案。你需要指出错误类型、相关知识点、具体错误原因，并给出适合面试训练的建议。

错误类型分类：
- SYNTAX_ERROR: 语法错误
- LOGIC_ERROR: 逻辑错误，结果不对
- BOUNDARY_ERROR: 边界条件遗漏
- ALGORITHM_ERROR: 算法选择不当
- TIMEOUT: 时间复杂度过高
- RUNTIME_ERROR: 运行时异常

常见错误模式：
- 哈希表：key 判断错误、遗漏重复元素、查询和写入顺序错误
- 二叉树：递归终止条件错误、空节点处理错误、回溯顺序错误
- 动态规划：状态定义错误、转移方程错误、初始化错误
- 链表：指针操作错误、dummy node 使用错误、环检测遗漏
- 数组：边界下标错误、双指针移动条件错误

请只输出 JSON：
{
  "errorType": "LOGIC_ERROR",
  "knowledgePoint": "hash_table",
  "specificError": "未处理重复元素导致覆盖",
  "diagnosis": "错误原因说明",
  "suggestion": "改进建议",
  "confidence": 0.86,
  "weaknessScoreDelta": 5
}
```

约束：

- `weaknessScoreDelta` 对失败提交必须为 `1` 到 `10` 的正数。
- 后端会把空值或小于等于 0 的 delta 兜底为 `5`，并将弱点分封顶为 `100`。

### 6.2 HintGeneratorTool System Prompt（已废弃）

`HintGeneratorTool` 已从 Agent 工作流中移除，代码中仅作为 `@Deprecated` 的历史兼容类保留，不再注册为 Spring Bean。题目预设分层提示由后端 `problem` 表（`hint_level1/2/3`）提供，通过 `ProblemDetailVO.presetHints` 返回，不再通过 AI 生成。`AgentAnalyzeVO.hintLevel1/2/3` 字段保留兼容，但不再写入新数据。`hint_record` 表保留但当前不再写入。

### 6.3 TrainingPlannerTool System Prompt

当前实现说明：`TrainingPlannerTool` 目前使用确定性 fallback 计划，不调用 LLM。保留下面的 prompt 作为后续升级到 AI 训练计划时的设计参考。

```text
你是 AI Interview Coach Agent 中的 TrainingPlannerTool。根据 WeaknessTrackerTool 维护的用户薄弱点记忆，生成 3 天训练计划。

要求：
- 每天推荐 2 到 3 道题。
- 说明推荐原因。
- 给出当天复习重点。
- 不要推荐过多题目。

请只输出 JSON：
{
  "title": "3 天哈希表与递归专项训练",
  "summary": "...",
  "items": [
    {
      "dayIndex": 1,
      "knowledgePoint": "HashMap",
      "problemTitle": "Two Sum",
      "reason": "用于训练查询和写入顺序",
      "reviewFocus": "重复元素和边界输入"
    }
  ]
}
```

## 7. 验证方式

### 本地 Phase 1/2 启动方式

MySQL 可以使用本机服务；Redis 当前只有配置预留，核心 demo 不依赖实际缓存逻辑；Piston 使用 Docker 容器。

1. 确认 MySQL 可连接，当前本机验证账号为 `root / 123456`。
2. 导入数据库和题库：

```powershell
mysql -uroot -p123456 < D:\code\ai-study\data\schema.sql
mysql -uroot -p123456 < D:\code\ai-study\data\problems.sql
```

3. Redis 为预留配置，后续接入热点缓存时再确认 `localhost:6379` 可用；当前核心闭环不依赖 Redis 读写。
4. 启动本地 Piston：

```powershell
cd D:\code\piston
docker compose up -d api
cd cli
npm install
node .\index.js ppman install java
```

5. 验证 Piston runtime：

```powershell
curl.exe http://localhost:2000/api/v2/runtimes
```

当前应看到：

```json
[
  {
    "language": "java",
    "version": "15.0.2",
    "aliases": []
  }
]
```

6. 启动后端。IDEA 运行配置需要包含：

```text
MYSQL_USERNAME=root;MYSQL_PASSWORD=123456;REDIS_HOST=localhost;REDIS_PORT=6379;PISTON_BASE_URL=http://localhost:2000/api/v2;AI_BASE_URL=<anthropic-compatible-base-url>;AI_MODEL=<model>;AI_API_KEY=<your-api-key>;AI_MAX_TOKENS=3000
```

也可以用 PowerShell 启动：

```powershell
cd D:\code\ai-study\backend
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="123456"
# Redis 目前是预留配置，核心 demo 不依赖缓存读写。
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
$env:PISTON_BASE_URL="http://localhost:2000/api/v2"
$env:AI_BASE_URL="<anthropic-compatible-base-url>"
$env:AI_MODEL="<model>"
$env:AI_API_KEY="<your-api-key>"
$env:AI_MAX_TOKENS="3000"
mvn spring-boot:run
```

7. 启动前端：

```powershell
cd D:\code\ai-study\frontend
npm run dev
```

前端默认使用 `http://127.0.0.1:4000`。Windows 更新、Docker/WSL 或 Hyper-V 可能保留 `3000` / `3001` 附近端口，导致 Next.js 在这些端口启动时报 `EACCES: permission denied`，所以 `npm run dev` 默认绑定 `4000`。

如果需要确认 Windows 保留端口范围，可以执行：

```powershell
netsh interface ipv4 show excludedportrange protocol=tcp
netsh interface ipv6 show excludedportrange protocol=tcp
```

### 后端验证

- 使用 Postman 或 curl 测试题目接口。
- 提交一段正确 Java 代码，确认判题通过。
- 提交一段错误 Java 代码，确认返回失败用例。
- 提交一段编译错误代码，确认返回编译错误。

Phase 1/2 已验证接口：

```http
GET http://localhost:8080/api/problems
GET http://localhost:8080/api/problems/1
GET http://localhost:8080/api/problems/1/template
POST http://localhost:8080/api/submissions
POST http://localhost:8080/api/agent/analyze
GET http://localhost:8080/api/submissions/{submissionId}/diagnosis/stream
```

Two Sum 正确提交示例：

```json
{
  "userId": 1,
  "problemId": 1,
  "language": "java",
  "code": "import java.util.*; class Solution { public int[] twoSum(int[] nums, int target) { Map<Integer,Integer> map = new HashMap<>(); for (int i = 0; i < nums.length; i++) { int need = target - nums[i]; if (map.containsKey(need)) { return new int[] {map.get(need), i}; } map.put(nums[i], i); } return new int[0]; } }"
}
```

已验证响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "submissionId": 11,
    "status": "ACCEPTED",
    "passedCount": 3,
    "totalCount": 3,
    "runtime": null,
    "memory": null,
    "errorMessage": null,
    "failedCases": []
  }
}
```

### AI Agent 验证

- 用 Two Sum 的重复元素场景测试错误分类。
- 用二叉树空节点场景测试递归错误分类。
- 用 DP 初始化错误测试动态规划错误分类。
- 检查 AI 是否输出合法 JSON。
- 检查 AI 是否没有直接给完整答案。
- 检查 `user_weakness.weakness_score` 对失败提交增加，不能因为模型返回负数而下降。
- 检查 `agent_run.status = SUCCESS` 且 `current_state = COMPLETED`。
- 检查失败提交的 `agent_step` 至少包含 `PLANNING`、`CODE_EXECUTION`、`OBSERVATION`、`RAG_RETRIEVAL`、`ERROR_CLASSIFICATION`、`MEMORY_UPDATE`、`TRAINING_PLAN`、`COMPLETED`；AC 提交至少包含 `PLANNING`、`CODE_EXECUTION`、`OBSERVATION`、`RAG_RETRIEVAL`、`CODE_REVIEW`、`COMPLETED`。
- 检查 `ai_diagnosis`、`user_weakness`、`mistake_card`、`training_plan` 均有最新记录；`hint_record` 当前不应产生新 AI hint 记录。

已验证的 Two Sum bug：

```text
代码在循环中先 map.put(nums[i], i)，再检查 complement。
重复元素或当前元素自匹配时会输出 0 0。
```

期望 Agent 结果：

```text
errorType: LOGIC_ERROR 或 BOUNDARY_ERROR
knowledgePoint: HashMap 相关
diagnosis/specificError: 指出 HashMap 查询和写入顺序问题，不给完整 Java 答案
```

### 前端验证

状态：Phase 4 Dashboard 真实数据接入已完成；Solution 模式模板加载、提示/诊断去重、Phase 5 产品增强和 4.2 核心闭环护栏后已运行前端 Node 测试和 `npm run build` 并通过。

- 打开首页，确认题目列表、筛选、搜索和卡片跳转可用。
- 进入做题页，确认 Monaco Editor 可用且首屏为三栏布局。
- 进入 `/problem/1`、`/problem/206`、`/problem/121`，确认浏览器 Network 能看到 `/api/problems/{id}/template`，编辑器显示 `class Solution` 模板。
- 确认旧 `101-108` 前端静态题目提示内容和 ACM `public class Main` 模板路径不再出现；`frontend/lib/problemHints.ts` 仅保留返回 `null` 的兼容 fallback 函数。
- 在左侧题目区确认“分层提示”存在，Level 1/2/3 默认收起，点击“查看”后展开。
- 点击“重置代码”，确认会重新请求后端模板并清除当前题草稿。
- 提交代码，确认测试结果展示。
- 提交失败后，确认 SSE 诊断步骤实时展示，`done` 后显示 AI 诊断；同步 `POST /api/agent/analyze` 仅作为 fallback。
- 连续提交两次或中断页面时，确认旧 SSE 流不会覆盖新提交状态，页面不会永久 loading。
- AC 提交后，确认 AI 诊断页在点评生成中展示实时 Agent 步骤，完成后展示轻量代码点评而不是错题诊断。
- AC 点评完成后修改编辑器代码但不提交，确认右侧点评不消失，并出现“基于上次提交，仅供参考”的 stale warning；把代码还原为上次提交快照后，该 warning 消失。
- 打开 Dashboard，确认统计、今日优先训练、完整训练计划、弱点排行、错误类型分布、最近提交、合并错题卡和 AI 教练建议都来自真实查询接口或现有前端学习数据聚合，不回退 mock。
- 在 Dashboard 将训练计划条目标记为完成/跳过，刷新后确认状态保持；点击重新生成计划后确认旧 `ACTIVE` 计划变为 `REGENERATED`，新计划可展示。
- 打开 `/knowledge`，确认知识训练页能加载，分类只显示全部分类 / Java / MySQL / Redis / Spring / JVM。
- 确认搜索、难度筛选、分类筛选有效。
- 展开知识点后默认只显示模拟自测输入框、提交按钮和“跳过自测，直接查看解析”。
- 提交自测后确认出现点评反馈/评分、命中记忆点、标杆回答解析、核心记忆要点、高频追问和标记已掌握。
- 提交自测后确认 `self_test_record` 有记录，低分自测会写入 `user_weakness_event`。
- 点击“跳过自测，直接查看解析”时确认显示解析区但不显示模拟评分。
- 点击“标记已掌握”后确认顶部已掌握数量变化。
- 在 Dashboard 点击“后端知识训练”入口，确认能跳转到 `/knowledge`。
- 在训练计划中确认算法题和知识卡片能用 `itemType` 区分展示。
- 首次无数据时，确认 Dashboard 显示空状态引导文案。
- 若 Dashboard 接口返回 404 或 `No static resource api/users/...`，重启后端并确认新增后端文件已纳入版本控制。

### 演示验证

完整走一遍：

```text
选择“两数之和” -> 查看左侧题目预设提示 -> 写 bug 代码 -> 提交失败 -> Agent 调用判题 Tool -> Observation -> RAG 检索 -> 错误分类 -> AI 诊断 -> 更新弱点 -> 生成训练计划
```

## 8. 关键风险和应对

| 风险 | 应对 |
| --- | --- |
| Piston 公共 API 需要授权 | 本地通过 Docker Desktop 自建 Piston，后端默认 `PISTON_BASE_URL=http://localhost:2000/api/v2` |
| 本地 Piston HTTP/2 请求返回 400 | `PistonClient` 使用 HTTP/1.1 请求工厂访问本地 Piston |
| Piston API 不稳定 | 后端保留 `JudgeService` 抽象，后期可替换为 Docker 沙箱或其他判题服务 |
| AI 响应慢 | 使用 SSE 流式输出 Agent 步骤，前端展示当前执行到哪个 Tool |
| 兼容模型先输出 thinking 导致 JSON text 超出 token | 默认 `AI_MAX_TOKENS=3000`，Prompt 要求 compact JSON |
| 模型返回负数 `weaknessScoreDelta` | `LearningTrackerImpl` 对空值或小于等于 0 的 delta 兜底为 `5` |
| AI 分类不准 | 准备 10 个固定错误样例调 Prompt |
| 前端做不完 | 已完成核心页面、SSE 接入和 Dashboard 真实数据接入；后续仅做小范围体验打磨 |
| 题库太多拖慢进度 | MVP 当前稳定 Hot100 精选 12 题，不急于扩到完整 Hot100 |
| Piston 测试用例封装复杂 | 当前题库统一 Solution 模式，通过 `CodeWrapper` 注册表统一输入构造和输出转换 |
| Solution 模式题目误显示旧模板 | 模板请求下沉到客户端 `ProblemWorkspace`，并对 fetch 使用 `cache: "no-store"`；必要时清理旧 localStorage 草稿 |
| Solution 模式误判题 | 后端按 `problem.id` 查找 adapter 包装，单测覆盖 12 题 adapter 和 `submit` / `rejudge` 主路径 |
| 旧本地库缺少 `knowledge_card` 表 | 已提供 `data/knowledge_training_migration.sql` 和 `data/knowledge_cards.sql`；PowerShell 下用 `cmd /c "mysql ... < file.sql"` 导入 |
| 知识训练扩展喧宾夺主 | 知识卡片保持独立入口，训练计划可以统一展示算法复盘和知识卡复习，但算法诊断不强行推荐八股 |
| 知识卡内容重新模板化 | 内容源只接受显式卡片内容；`knowledge-tree-coverage.node-test.cjs` 会拦截模板问法、模板套话、空泛 keyPoints 和高风险关键词缺失 |
| RAG 检索到旧知识卡 | 导入新 SQL 后重建系统 RAG 索引；重建只处理 `user_id IS NULL` 的系统题目和知识卡 chunk，不删除用户记忆 |

## 9. 演示脚本

完整演示复盘、截图录屏和面试 Q&A 归入最终阶段；本节仅保留参考流程。

推荐面试演示流程：

1. 打开首页，展示题目列表。
2. 选择“两数之和”。
3. 解释项目不是刷题平台，也不是 AI 聊天壳，而是 Java 代码诊断 Agent。
4. 在 Monaco Editor 中写一段有 bug 的 Java 代码。
5. 点击提交，展示 Piston 返回测试失败结果。
6. 打开 Agent 诊断面板，展示测试结果和 SSE 实时步骤，完成后显示 AI 错误诊断；同步 `POST /api/agent/analyze` 保留为 fallback。
7. 展示错误分类：例如 `BOUNDARY_ERROR`、`HashMap`。
8. 回到左侧题目区逐层展开 Level 1、Level 2、Level 3 预设提示，说明它不依赖本次 AI 调用。
9. 修改代码后重新提交并通过。
10. 通过数据库或后端日志展示 `agent_run`、`agent_step`、`ai_diagnosis`、`user_weakness`、`mistake_card`、`training_plan`；说明 `hint_record` 当前为兼容保留表，不写入新 AI hint。
11. 讲解后端设计：Piston 封装、Agent Tool、Observation、RAG V1、Memory、SSE、MySQL，以及 Redis 预留配置和后续热点缓存扩展点。
12. 打开 Dashboard，展示真实 MySQL 数据驱动的薄弱点、错题卡、最近提交和训练计划。
13. 打开 `/knowledge`，展示知识训练作为独立模块：先模拟自测，再看标杆回答解析和高频追问，而不是由算法错误强行推荐八股。

## 10. 简历准备

简历项目名：

```text
AI Interview Coach Agent - 基于 Agent Workflow 的 Java 代码诊断与面试训练系统
```

简历描述：

```text
基于 Spring Boot 3、Next.js 14 和 LLM 构建 Java 代码诊断与后端面试训练系统，支持 Java 算法题在线提交、Piston 代码执行、Agent Workflow 编排、测试结果 Observation、RAG 辅助错误归因、AC 代码点评、题目预设分层提示、后端知识卡片训练、薄弱知识点记忆和统一训练计划生成。
```

后端亮点：

```text
封装 Piston 代码执行服务并作为 `CodeExecutionTool` 接入 Agent Workflow，支持 Java 编译运行、测试用例判定和错误日志解析；基于 MyBatis-Plus 接入 MySQL 8，持久化题目、提交、诊断、训练计划、后端知识卡片和 RAG chunk；当前预留 Redis 配置，热点题目和题目详情缓存待接入；并通过 SSE 实现 Agent 执行步骤流式返回。
```

Agent 亮点：

```text
设计基于状态机的 Agent Workflow，将代码执行、RAG 检索、错误分类、代码点评、弱点追踪、训练规划封装为独立 Tool，通过 Agent 编排器串联执行。代码执行是 Tool，测试结果是 Observation，后端维护 Agent 状态和步骤记录，LLM 只负责错误归因和代码点评等语义判断节点。
```
