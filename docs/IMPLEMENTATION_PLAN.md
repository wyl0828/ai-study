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

项目不是普通刷题平台，也不是 Spring Boot 调大模型 API 的聊天壳，而是一个能够调用判题工具、观察测试结果、分析错误思维、分层提示、记录薄弱点、生成训练计划的面试训练 Agent。

Agent-first 核心闭环：

```text
Agent 收到任务
  -> Planner 决定要判题
  -> CodeExecutionTool 执行 Java 代码
  -> Observation 返回测试结果
  -> ErrorClassifierTool 分类错误
  -> HintGeneratorTool 生成三层提示
  -> WeaknessTrackerTool 更新长期弱点
  -> TrainingPlannerTool 生成后续训练建议
```

关键约束：

- 前端使用 Next.js 14 + Tailwind CSS + Monaco Editor
- 后端使用 Spring Boot 3 + Java 17
- 数据库使用 MySQL 8
- Java 持久层使用 MyBatis-Plus
- 缓存使用 Redis
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
| 缓存 | Redis | 会话、热点题目、临时 Agent 状态 |
| AI | Anthropic 兼容 API | 错误分析、提示生成、Code Review、训练计划 |
| Agent Workflow | 状态机 + Tool 链 | Planner、Tool 调用、Observation、Memory 更新 |
| 判题 | Piston API | 免费代码执行，后期替换为 Docker |
| 流式输出 | SSE | Agent 执行步骤和诊断结果流式返回 |
| Trace | AgentStep 记录 | 记录 Tool 输入摘要、输出摘要、状态和耗时 |

### 当前落地状态快照

- Phase 1 后端骨架、题目接口、Piston 判题和提交持久化已完成。
- Phase 2 Agent Workflow 后端已完成：`AgentRun`、`AgentStep`、AI 诊断、三层提示、弱点记忆、错题卡、训练计划均已接入 MySQL。
- 当前真实暴露的 Controller：`ProblemController`、`SubmissionController`、`AgentController`、`UserController`。
- 当前真实暴露的 Agent 接口：`POST /api/agent/analyze`、`GET /api/submissions/{submissionId}/diagnosis/stream`。
- Phase 3 前端核心页面已完成：`/`、`/problem/[id]`、`/dashboard` 均已按 `stitch_front_end_interface_design/mvp/` HTML 原型做紧凑 MVP 风格还原，并完成中文化。
- 当前做题页提交失败后会自动调用同步 `POST /api/agent/analyze` 展示测试结果、AI 诊断和三层提示；后端 SSE 接口已保留，但前端暂未接入 SSE 流式展示。
- Dashboard 已通过 `UserController` 接入真实 MySQL 学习数据，展示统计、薄弱点、错题卡、最近提交和最新训练计划；单独 hint 查询、accepted-code review 和手动重新生成训练计划留到后续阶段。
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
│   │   └── layout.tsx
│   ├── components/
│   │   ├── CodeEditor.tsx
│   │   ├── TestResult.tsx
│   │   ├── HintPanel.tsx
│   │   ├── AiDiagnosis.tsx
│   │   ├── ResultPanel.tsx
│   │   ├── ProblemCard.tsx
│   │   ├── ProblemDescription.tsx
│   │   ├── ProblemWorkspace.tsx
│   │   ├── WeaknessList.tsx
│   │   ├── MistakeCards.tsx
│   │   ├── SubmissionHistory.tsx
│   │   └── TrainingPlan.tsx
│   └── lib/
│       ├── api.ts
│       ├── i18n.ts
│       ├── mock.ts
│       └── types.ts
│
├── backend/
│   └── src/main/java/com/interview/coach/
│       ├── controller/
│       │   ├── ProblemController.java
│       │   ├── SubmissionController.java
│       │   └── AgentController.java
│       ├── service/
│       │   ├── ProblemService.java
│       │   ├── SubmissionService.java
│       │   ├── JudgeService.java
│       │   ├── AgentService.java
│       │   ├── LearningTracker.java
│       │   ├── TrainingPlanService.java
│       │   └── impl/
│       │       ├── ProblemServiceImpl.java
│       │       ├── SubmissionServiceImpl.java
│       │       ├── JudgeServiceImpl.java
│       │       ├── AgentServiceImpl.java
│       │       ├── LearningTrackerImpl.java
│       │       └── TrainingPlanServiceImpl.java
│       ├── agent/
│       │   ├── InterviewCoachAgent.java
│       │   ├── AgentContext.java
│       │   ├── AgentStep.java
│       │   └── tool/
│       │       ├── Tool.java
│       │       ├── CodeExecutionTool.java
│       │       ├── ErrorClassifierTool.java
│       │       ├── HintGeneratorTool.java
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
│       │   ├── TrainingPlan.java
│       │   ├── TrainingPlanItem.java
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
│       │   ├── TrainingPlanMapper.java
│       │   ├── TrainingPlanItemMapper.java
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
    └── problems.sql
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
agent/tool：代码执行、错误分类、提示生成、弱点更新、训练计划等 Tool
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

目标：Spring Boot 项目跑通，能查题目、提交代码、拿到 Piston 执行结果。

任务：

1. 初始化 Spring Boot 3 项目。
2. 添加依赖：Spring Web、MyBatis-Plus Spring Boot 3 Starter、MySQL Driver、Spring Data Redis、Lombok。
3. 配置 MySQL、Redis、CORS。
4. 建立核心表：`problem`、`test_case`、`submission`、`user`。
5. 实现基础实体和 Mapper：
   - `ProblemMapper`
   - `TestCaseMapper`
   - `SubmissionMapper`
   - `UserMapper`
6. 实现 `ProblemController`：
   - `GET /api/problems`
   - `GET /api/problems/{id}`
   - `GET /api/problems/{id}/template`
7. 实现 `JudgeService` 和 `PistonClient`。
8. 实现 `SubmissionController`：
   - `POST /api/submissions`
   - 保存提交记录
   - 调用 Piston API
   - 返回测试结果
9. 准备 MVP 题库 10 道，后续扩展到 30 道。

关键文件：

- `backend/src/main/java/com/interview/coach/service/JudgeService.java`
- `backend/src/main/java/com/interview/coach/service/impl/JudgeServiceImpl.java`
- `backend/src/main/java/com/interview/coach/integration/piston/PistonClient.java`
- `backend/src/main/java/com/interview/coach/controller/SubmissionController.java`
- `backend/src/main/java/com/interview/coach/mapper/ProblemMapper.java`
- `backend/src/main/java/com/interview/coach/mapper/SubmissionMapper.java`
- `data/problems.sql`

验收标准：

- 后端能启动。
- 可以查询题目列表和详情。
- 可以提交 Java 代码。
- 可以拿到 Piston API 返回结果。
- 提交记录能保存到 MySQL。

本阶段实际落地说明：

- 已创建 `backend/` Maven 项目，使用 Spring Boot 3.5.12、Java 17、MyBatis-Plus 3.5.16、MySQL Driver、Redis、Lombok。
- 已创建 `data/schema.sql` 和 `data/problems.sql`，当前 MVP 种子数据为 8 道题、24 条测试用例、1 个 demo 用户。
- 已实现 `ProblemController`、`SubmissionController`、`ProblemService`、`SubmissionService`、`JudgeService`、`PistonClient`。
- 当前判题路径为 `SubmissionController -> SubmissionService -> JudgeService -> PistonClient`，Controller 不直接调用 Mapper 或 Piston。
- 本地 Piston 使用 Docker Desktop 启动，API 地址为 `http://localhost:2000/api/v2`，当前安装 runtime 为 `java 15.0.2`。
- `PistonClient` 强制使用 HTTP/1.1 访问本地 Piston；Java 默认 HTTP/2 请求会被当前本地 Piston API 返回 `400 Bad Request`。
- Piston 成功编译运行时可能没有 `compile` 字段，`JudgeServiceImpl` 仅在 `compile` 字段存在且失败时判定为 `COMPILE_ERROR`。
- 已验证 `POST /api/submissions` 使用 Two Sum 正确代码返回 `ACCEPTED`，`passedCount=3`，`totalCount=3`。

### 阶段 2：Agent Workflow 核心，Day 4-7

状态：已完成并通过本地验证。

目标：把项目从“AI 分析接口”升级为可解释的 Agent Workflow。后端维护 Agent 状态机，代码执行、错误分类、提示生成、弱点更新和训练计划都封装为 Tool，LLM 只负责需要语义判断的节点。

任务：

1. 实现 `AiProperties`，配置 Anthropic 兼容 API 地址、模型名、API Key、最大 token 和 Anthropic version。
2. 在 `integration/ai` 下实现 `AnthropicCompatibleClient`，封装模型调用和结构化 JSON 输出解析。
3. 定义 Agent 核心模型：
   - `AgentState`：`PLANNING`、`CODE_EXECUTION`、`OBSERVATION`、`ERROR_CLASSIFICATION`、`HINT_GENERATION`、`MEMORY_UPDATE`、`TRAINING_PLAN`、`COMPLETED`、`FAILED`。
   - `AgentContext`：保存 submission、problem、executionResult、diagnosis、hints、weaknessUpdate 等上下文。
   - `AgentStep`：记录 stepName、toolName、status、inputSummary、outputSummary、durationMs、errorMessage。
4. 定义 `Tool<I, O>` 通用接口，约定 Tool 只暴露清晰输入输出，不直接依赖 Controller。
5. 实现核心 Tool：
   - `CodeExecutionTool`：调用 `JudgeService`，输出判题 Observation。
   - `ErrorClassifierTool`：调用 LLM，输出错误类型、知识点、具体错误、置信度。
   - `HintGeneratorTool`：调用 LLM，输出三层提示。
   - `WeaknessTrackerTool`：调用 `LearningTracker`，更新弱点和错题卡。
   - `TrainingPlannerTool`：调用 `TrainingPlanService` 或 LLM，生成训练建议。
6. 实现 `InterviewCoachAgent` 编排器，按状态机执行 Tool，并记录每一步 `AgentStep`。
7. 实现 `AgentService` 和 `AgentServiceImpl`，作为业务入口创建 `AgentRun`、组装 `AgentContext` 并调用 `InterviewCoachAgent`。
8. 实现 `AgentController`：
   - `POST /api/agent/analyze`
   - `GET /api/submissions/{submissionId}/diagnosis/stream`
9. 实现 SSE 步骤流：
   - 推送 Planning、Tool 调用、Observation、错误分类、提示生成、弱点更新、最终结果。
   - 最终事件返回结构化诊断 JSON。
10. 实现 `LearningTracker` 和 `LearningTrackerImpl`：
   - 记录每次提交的错误类型。
   - 更新用户薄弱点。
   - 创建错题卡片。
11. 实现 `TrainingPlanService` 和 `TrainingPlanServiceImpl`：
   - 保存 3 天训练计划。
   - 保存训练计划 item。

关键文件：

- `backend/src/main/java/com/interview/coach/agent/InterviewCoachAgent.java`
- `backend/src/main/java/com/interview/coach/enums/AgentState.java`
- `backend/src/main/java/com/interview/coach/agent/AgentContext.java`
- `backend/src/main/java/com/interview/coach/agent/AgentStep.java`
- `backend/src/main/java/com/interview/coach/agent/tool/Tool.java`
- `backend/src/main/java/com/interview/coach/agent/tool/CodeExecutionTool.java`
- `backend/src/main/java/com/interview/coach/agent/tool/ErrorClassifierTool.java`
- `backend/src/main/java/com/interview/coach/agent/tool/HintGeneratorTool.java`
- `backend/src/main/java/com/interview/coach/agent/tool/WeaknessTrackerTool.java`
- `backend/src/main/java/com/interview/coach/agent/tool/TrainingPlannerTool.java`
- `backend/src/main/java/com/interview/coach/service/AgentService.java`
- `backend/src/main/java/com/interview/coach/service/impl/AgentServiceImpl.java`
- `backend/src/main/java/com/interview/coach/service/LearningTracker.java`
- `backend/src/main/java/com/interview/coach/service/impl/LearningTrackerImpl.java`
- `backend/src/main/java/com/interview/coach/controller/AgentController.java`

验收标准：

- 给定失败提交，`InterviewCoachAgent` 能完整执行 Tool 链。
- `CodeExecutionTool` 的输出会作为 Observation 进入后续步骤。
- AI 能返回合法结构化 JSON。
- 能生成 3 级提示，且不直接给完整 Java 答案。
- SSE 能流式返回 Agent 步骤，而不是只返回模型文本。
- AI 诊断结果能保存到 `ai_diagnosis`。
- 用户薄弱点能更新到 `user_weakness`。
- Agent Step 至少能在日志或数据库中看到每一步状态和耗时。

本阶段实际落地说明：

- 已新增 Phase 2 表：`agent_run`、`agent_step`、`ai_diagnosis`、`hint_record`、`user_weakness`、`mistake_card`、`training_plan`、`training_plan_item`。
- 已实现 `AnthropicCompatibleClient`，使用 Anthropic-compatible `/v1/messages` 协议，并从模型返回文本中提取 JSON。
- 已实现 `AiProperties`，配置前缀为 `coach.ai.*`，环境变量包括 `AI_BASE_URL`、`AI_API_KEY`、`AI_MODEL`、`AI_MAX_TOKENS`、`AI_ANTHROPIC_VERSION`。
- 当前 `AI_MAX_TOKENS` 默认值为 `3000`，用于兼容会先返回 thinking block 的模型。
- 已实现 `Tool<I, O>`、`InterviewCoachAgent`、`AgentContext`、`AgentStep` 和 5 个 Tool。
- `CodeExecutionTool` 调用 `SubmissionService.rejudge(...)`，由 `SubmissionService` 复用 `JudgeService` 完成重新判题，不直接调用 Piston。
- `LearningTrackerImpl` 会插入 `AiDiagnosis`、3 条 `HintRecord`、更新或创建 `UserWeakness`、插入 `MistakeCard`。
- `weaknessScoreDelta` 为空或小于等于 0 时按默认 `+5` 处理，避免模型返回负数导致弱点分下降；弱点分最高封顶 `100`。
- `TrainingPlannerTool` 当前使用确定性 fallback 生成 3 天训练计划，并调用 `TrainingPlanService.savePlan(...)` 持久化。
- SSE 事件名为 `agent_step`、`done`、`error`。
- 已使用 `mimo-v2.5-pro` 兼容 Anthropic 接口完成真实流式诊断验证：Two Sum 重复元素 bug 被分类为 `LOGIC_ERROR` / HashMap 相关知识点，三层提示和训练计划均成功生成并落库。

### 阶段 3：前端页面，Day 8-12

状态：已完成核心页面，并通过本地构建验证。

目标：做出能演示的前端，有题目列表、代码编辑器、测试结果和 AI 提示面板。

任务：

1. 初始化 Next.js 14 项目，使用 App Router 和 Tailwind CSS。
2. 实现首页 `/`：
   - 题目列表。
   - 按类型和难度筛选。
3. 实现做题页 `/problem/[id]`：
   - 左侧题目描述。
   - 右侧 Monaco Editor。
   - 下方测试结果。
   - Agent 诊断步骤流式输出。
   - 分层提示面板。
4. 实现个人中心 `/dashboard`：
   - 薄弱知识点排行。
   - 最近提交记录。
   - 训练计划。
   - 错题卡片。
5. 封装 `frontend/lib/api.ts`。

关键文件：

- `frontend/app/page.tsx`
- `frontend/app/problem/[id]/page.tsx`
- `frontend/app/dashboard/page.tsx`
- `frontend/components/CodeEditor.tsx`
- `frontend/components/HintPanel.tsx`
- `frontend/components/TestResult.tsx`
- `frontend/components/AiDiagnosis.tsx`
- `frontend/components/ResultPanel.tsx`
- `frontend/components/ProblemWorkspace.tsx`
- `frontend/components/ProblemCard.tsx`
- `frontend/lib/i18n.ts`

验收标准：

- 浏览器能打开题目列表。
- 能进入做题页。
- Monaco Editor 能输入 Java 代码。
- 点击提交后能展示测试结果。
- 测试失败后能展示 AI 诊断和分层提示。
- Dashboard 能展示弱点和训练计划。

本阶段实际落地说明：

- 首页 `/` 已实现题库标题区、难度筛选、分类筛选、搜索框、统计行和三列题目卡片。
- 首页数据仍从 `GET /api/problems` 获取，并在 8 道 MVP 题范围内并行请求 `GET /api/problems/{id}` 补齐描述和知识点，不新增后端接口。
- 做题页 `/problem/[id]` 已实现固定视口高度三栏布局：左侧题目描述、中间 Monaco Editor、右侧测试结果 / AI 诊断 / 分层提示。
- Monaco 容器已改为深色 loading 背景，避免编辑器加载前出现大面积浅色空白。
- 当前提交流程为：`POST /api/submissions` 判题，失败后自动调用同步 `POST /api/agent/analyze` 获取诊断结果；SSE 前端接入留到后续增强。
- Dashboard `/dashboard` 已实现统计卡、薄弱点排行、最近提交表格、错题卡片、训练计划和 AI 建议展示，当前数据来自 `/api/users/1/...` 用户学习查询接口。
- 前端页面已完成中文化，题目标题、难度、知识点、按钮、空状态、Dashboard 文案均按“国内互联网产品 + LeetCode 中文站”风格处理。
- 已运行 `npm run build`，Next.js 编译、类型检查和页面生成通过。

### 阶段 4：训练计划与错题本，Day 13-16

状态：已完成真实数据闭环。Phase 2 已完成后端持久化闭环；Phase 4 已补齐 Dashboard 查询接口，并将前端 Dashboard 从 mock 数据切换为真实 MySQL 数据。

目标：实现学习闭环，让项目不只是一次性 AI 分析。

任务：

1. 已完成：`LearningTracker` 在 Agent 诊断流程中持久化诊断、提示、弱点和错题卡。
2. 已完成：每次 Agent 成功诊断失败提交后更新用户弱点画像。
3. 已完成基础版：`TrainingPlanService` 保存 3 天训练计划和计划 item。
4. 已完成基础版：错题卡片持久化字段包括题目、提交、AgentRun、错误类型、知识点、错误摘要和正确思路。
5. 已完成：用户学习查询接口：
   - `GET /api/users/{userId}/dashboard/stats`
   - `GET /api/users/{userId}/weaknesses`
   - `GET /api/users/{userId}/training-plans/latest`
   - `GET /api/users/{userId}/mistakes`
   - `GET /api/users/{userId}/submissions/recent`
6. 已完成：Dashboard 页面展示真实统计、弱点、错题、最近提交和训练计划。
7. 待实现增强：手动重新生成训练计划、单独 hint 查询、accepted-code review。

验收标准：

- 已完成：提交失败并触发 Agent 诊断后能更新薄弱点。
- 已完成：能生成并保存 3 天训练计划。
- 已完成：能通过 REST API 查看错题卡片。
- 已完成：Dashboard 从 MySQL 持久化数据读取弱点、错题卡、最近提交和最新训练计划。
- 已完成：无数据时 Dashboard 显示空状态引导文案，不回退 mock 数据。

### 阶段 5：打磨与演示准备，Day 17-20

目标：修 bug、补数据、写 README、准备面试演示。

任务：

1. 修复核心流程 bug。
2. 补充题库到 20 到 30 道。
3. 优化异常提示。
4. Redis 缓存热门题目和题目详情。
5. 编写 README。
6. 准备项目截图。
7. 准备演示脚本。
8. 可选：录制演示 GIF。

验收标准：

- 能完整演示核心闭环。
- README 能说明项目定位、技术栈、架构图、启动方式和演示流程。
- 简历描述和面试讲解稿准备完成。

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
  "problemId": 101,
  "userId": 1,
  "language": "java",
  "code": "public class Main { ... }"
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
  "hintLevel1": "Consider the order of inserting elements into the map and checking for complements.",
  "hintLevel2": "The error is self-matching due to adding current element before lookup.",
  "hintLevel3": "For each element, first check if its complement exists in the map, then add it.",
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
data:{"stepName":"ERROR_CLASSIFICATION","toolName":"ErrorClassifierTool","status":"SUCCESS","inputSummary":"Classify execution observation","outputSummary":"Diagnosis ready","durationMs":23548,"errorMessage":null}

event:done
data:{"agentRunId":1,"submissionId":1001,"errorType":"LOGIC_ERROR","knowledgePoint":"HashMap Lookup in Array Traversal","hintLevel1":"...","hintLevel2":"...","hintLevel3":"...","trainingPlanTitle":"3-day recovery plan: HashMap Lookup in Array Traversal","steps":[...]}
```

### 5.4 用户学习接口

状态：Dashboard 读取接口已通过 `UserController` 暴露；手动重新生成训练计划暂不实现。

```http
GET /api/users/{userId}/dashboard/stats
GET /api/users/{userId}/weaknesses
GET /api/users/{userId}/training-plans/latest
GET /api/users/{userId}/mistakes
GET /api/users/{userId}/submissions/recent
```

## 6. Prompt 设计

### 6.1 ErrorClassifierTool System Prompt

```text
你是 AI Interview Coach Agent 中的 ErrorClassifierTool。你的任务是分析用户的代码提交、题目信息和 CodeExecutionTool 返回的 Observation，判断错误类型。

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

### 6.2 HintGeneratorTool System Prompt

```text
你是 AI Interview Coach Agent 中的 HintGeneratorTool。你需要根据 ErrorClassifierTool 的结构化诊断生成分层提示。

要求：
- Level 1 只给方向，不暴露解法。
- Level 2 指出相关知识点和需要检查的位置。
- Level 3 给伪代码或关键思路，但不要给完整 Java 答案。
- 语气像面试官引导候选人。

请只输出 JSON：
{
  "hintLevel1": "...",
  "hintLevel2": "...",
  "hintLevel3": "..."
}
```

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

MySQL 和 Redis 可以使用本机服务；Piston 使用 Docker 容器。

1. 确认 MySQL 可连接，当前本机验证账号为 `root / 123456`。
2. 导入数据库和题库：

```powershell
mysql -uroot -p123456 < D:\code\ai-study\data\schema.sql
mysql -uroot -p123456 < D:\code\ai-study\data\problems.sql
```

3. 启动 Redis，确认 `localhost:6379` 可用。
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
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
$env:PISTON_BASE_URL="http://localhost:2000/api/v2"
$env:AI_BASE_URL="<anthropic-compatible-base-url>"
$env:AI_MODEL="<model>"
$env:AI_API_KEY="<your-api-key>"
$env:AI_MAX_TOKENS="3000"
mvn spring-boot:run
```

### 后端验证

- 使用 Postman 或 curl 测试题目接口。
- 提交一段正确 Java 代码，确认判题通过。
- 提交一段错误 Java 代码，确认返回失败用例。
- 提交一段编译错误代码，确认返回编译错误。

Phase 1/2 已验证接口：

```http
GET http://localhost:8080/api/problems
GET http://localhost:8080/api/problems/101
GET http://localhost:8080/api/problems/101/template
POST http://localhost:8080/api/submissions
POST http://localhost:8080/api/agent/analyze
GET http://localhost:8080/api/submissions/{submissionId}/diagnosis/stream
```

Two Sum 正确提交示例：

```json
{
  "userId": 1,
  "problemId": 101,
  "language": "java",
  "code": "import java.util.*; public class Main { public static void main(String[] args){ Scanner sc=new Scanner(System.in); int n=sc.nextInt(); int[] nums=new int[n]; for(int i=0;i<n;i++) nums[i]=sc.nextInt(); int target=sc.nextInt(); Map<Integer,Integer> map=new HashMap<>(); for(int i=0;i<n;i++){ int need=target-nums[i]; if(map.containsKey(need)){ System.out.println(map.get(need)+\" \"+i); return; } map.put(nums[i], i); } System.out.println(\"-1 -1\"); } }"
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
- 检查 `agent_step` 至少包含 `PLANNING`、`CODE_EXECUTION`、`OBSERVATION`、`ERROR_CLASSIFICATION`、`HINT_GENERATION`、`MEMORY_UPDATE`、`TRAINING_PLAN`、`COMPLETED`。
- 检查 `ai_diagnosis`、`hint_record`、`user_weakness`、`mistake_card`、`training_plan` 均有最新记录。

已验证的 Two Sum bug：

```text
代码在循环中先 map.put(nums[i], i)，再检查 complement。
重复元素或当前元素自匹配时会输出 0 0。
```

期望 Agent 结果：

```text
errorType: LOGIC_ERROR 或 BOUNDARY_ERROR
knowledgePoint: HashMap 相关
hintLevel3: 只给检查顺序/伪代码，不给完整 Java 答案
```

### 前端验证

状态：Phase 4 Dashboard 真实数据接入已完成，2026-05-07 已运行 `npm run build` 并通过。

- 打开首页，确认题目列表、筛选、搜索和卡片跳转可用。
- 进入做题页，确认 Monaco Editor 可用且首屏为三栏布局。
- 提交代码，确认测试结果展示。
- 提交失败后，确认同步 `POST /api/agent/analyze` 返回的 AI 诊断和分层提示能展示。
- 打开 Dashboard，确认统计、弱点、错题卡、最近提交和训练计划来自真实查询接口。
- 首次无数据时，确认 Dashboard 显示空状态引导文案。

### 演示验证

完整走一遍：

```text
选择“两数之和” -> 写 bug 代码 -> 提交失败 -> Agent 调用判题 Tool -> Observation -> 错误分类 -> 分层提示 -> 更新弱点 -> 生成训练计划
```

## 8. 关键风险和应对

| 风险 | 应对 |
| --- | --- |
| Piston 公共 API 需要授权 | 本地通过 Docker Desktop 自建 Piston，后端默认 `PISTON_BASE_URL=http://localhost:2000/api/v2` |
| 本地 Piston HTTP/2 请求返回 400 | `PistonClient` 使用 HTTP/1.1 请求工厂访问本地 Piston |
| Piston API 不稳定 | 后端保留 `JudgeService` 抽象，后期可替换为 Docker 沙箱或其他判题服务；演示前准备固定样例 |
| AI 响应慢 | 使用 SSE 流式输出 Agent 步骤，前端展示当前执行到哪个 Tool |
| 兼容模型先输出 thinking 导致 JSON text 超出 token | 默认 `AI_MAX_TOKENS=3000`，Prompt 要求 compact JSON |
| 模型返回负数 `weaknessScoreDelta` | `LearningTrackerImpl` 对空值或小于等于 0 的 delta 兜底为 `5` |
| AI 分类不准 | 准备 10 个固定错误样例调 Prompt |
| 前端做不完 | 已完成核心页面和 Dashboard 真实数据接入；后续优先打磨演示稳定性 |
| 题库太多拖慢进度 | MVP 先做 10 道题，README 写可扩展到 30 道 |
| Piston 测试用例封装复杂 | 第一版使用固定 Java Main 模板拼接测试输入 |

## 9. 演示脚本

推荐面试演示流程：

1. 打开首页，展示题目列表。
2. 选择“两数之和”。
3. 解释项目不是刷题平台，也不是 AI 聊天壳，而是 Java 代码诊断 Agent。
4. 在 Monaco Editor 中写一段有 bug 的 Java 代码。
5. 点击提交，展示 Piston 返回测试失败结果。
6. 打开 Agent 诊断面板，展示测试结果、AI 错误诊断和三层提示；当前前端使用同步 `POST /api/agent/analyze`，后端 SSE 能力可作为接口层演示。
7. 展示错误分类：例如 `BOUNDARY_ERROR`、`HashMap`。
8. 展示 Level 1、Level 2、Level 3 分层提示。
9. 修改代码后重新提交并通过。
10. 通过数据库或后端日志展示 `agent_run`、`agent_step`、`ai_diagnosis`、`hint_record`、`user_weakness`、`mistake_card`、`training_plan`。
11. 讲解后端设计：Piston 封装、Agent Tool、Observation、Memory、SSE、MySQL、Redis。
12. 打开 Dashboard，展示真实 MySQL 数据驱动的薄弱点、错题卡、最近提交和训练计划。

## 10. 简历准备

简历项目名：

```text
AI Interview Coach Agent - 基于 Agent Workflow 的 Java 代码诊断与面试训练系统
```

简历描述：

```text
基于 Spring Boot 3、Next.js 14 和 LLM 构建 Java 代码诊断 Agent，支持 Java 算法题在线提交、Piston 代码执行、Agent Workflow 编排、测试结果 Observation、错误归因、分层提示、薄弱知识点记忆和个性化训练计划生成。
```

后端亮点：

```text
封装 Piston 代码执行服务并作为 `CodeExecutionTool` 接入 Agent Workflow，支持 Java 编译运行、测试用例判定和错误日志解析；基于 MyBatis-Plus 接入 MySQL 8，持久化题目、提交、诊断和训练数据；使用 Redis 缓存热门题目与用户训练状态，并通过 SSE 实现 Agent 执行步骤流式返回。
```

Agent 亮点：

```text
设计基于状态机的 Agent Workflow，将代码执行、错误分类、提示生成、弱点追踪、训练规划封装为独立 Tool，通过 Agent 编排器串联执行。代码执行是 Tool，测试结果是 Observation，后端维护 Agent 状态和步骤记录，LLM 只负责错误归因、提示生成和训练计划等语义判断节点。
```
