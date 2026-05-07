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
│   │   ├── DiagnosisStream.tsx
│   │   └── WeaknessChart.tsx
│   └── lib/
│       └── api.ts
│
├── backend/
│   └── src/main/java/com/interview/coach/
│       ├── controller/
│       │   ├── ProblemController.java
│       │   ├── SubmissionController.java
│       │   ├── AgentController.java
│       │   └── UserLearningController.java
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
│       │   ├── AgentState.java
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
│       │   ├── AiDiagnosisMapper.java
│       │   ├── UserWeaknessMapper.java
│       │   ├── TrainingPlanMapper.java
│       │   └── MistakeCardMapper.java
│       ├── dto/
│       ├── vo/
│       ├── enums/
│       ├── config/
│       │   ├── AiConfig.java
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
agent：Agent 编排器、状态、上下文、步骤记录和 AI-native 逻辑
agent/tool：代码执行、错误分类、提示生成、弱点更新、训练计划等 Tool
integration/piston：Piston 代码执行 API 接入
integration/ai：Anthropic 兼容模型 API 接入
entity：数据库实体
mapper：MyBatis-Plus 数据访问
dto：请求参数和内部命令对象
vo：响应结果对象
enums：状态、难度、语言、错误类型、提示等级枚举
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

目标：把项目从“AI 分析接口”升级为可解释的 Agent Workflow。后端维护 Agent 状态机，代码执行、错误分类、提示生成、弱点更新和训练计划都封装为 Tool，LLM 只负责需要语义判断的节点。

任务：

1. 实现 `AiConfig`，配置 Anthropic 兼容 API 地址、模型名、API Key。
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
7. 实现 `AgentService` 和 `AgentServiceImpl`，作为业务入口调用 `InterviewCoachAgent`，并负责诊断结果持久化。
8. 实现 `AgentController`：
   - `POST /api/agent/analyze`
   - `POST /api/agent/review`
   - `POST /api/agent/hint`
   - `GET /api/submissions/{submissionId}/diagnosis/stream`
9. 实现 SSE 步骤流：
   - 推送 Planning、Tool 调用、Observation、错误分类、提示生成、弱点更新、最终结果。
   - 最终事件返回结构化诊断 JSON。
10. 实现 `LearningTracker` 和 `LearningTrackerImpl`：
   - 记录每次提交的错误类型。
   - 更新用户薄弱点。
   - 创建错题卡片。

关键文件：

- `backend/src/main/java/com/interview/coach/agent/InterviewCoachAgent.java`
- `backend/src/main/java/com/interview/coach/agent/AgentState.java`
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

### 阶段 3：前端页面，Day 8-12

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
- `frontend/components/DiagnosisStream.tsx`

验收标准：

- 浏览器能打开题目列表。
- 能进入做题页。
- Monaco Editor 能输入 Java 代码。
- 点击提交后能展示测试结果。
- 测试失败后能展示 AI 诊断和分层提示。
- Dashboard 能展示弱点和训练计划。

### 阶段 4：训练计划与错题本，Day 13-16

目标：实现学习闭环，让项目不只是一次性 AI 分析。

任务：

1. 完善 `LearningTracker`。
2. 每次提交后更新用户弱点画像。
3. 实现 `TrainingPlanService`：
   - 根据弱点推荐题目。
   - 生成 3 天训练计划。
4. 实现错题卡片：
   - 题目。
   - 用户错误。
   - 错误类型。
   - 正确思路。
5. 实现用户学习接口：
   - `GET /api/users/{userId}/weaknesses`
   - `GET /api/users/{userId}/training-plans/latest`
   - `POST /api/users/{userId}/training-plans/generate`
   - `GET /api/users/{userId}/mistakes`
6. 完善 Dashboard 页面。

验收标准：

- 提交失败后能更新薄弱点。
- 能生成 3 天训练计划。
- 能查看错题卡片。
- Dashboard 能体现用户训练闭环。

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
POST /api/agent/review
POST /api/agent/hint
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
  "agentRunId": "run_1001",
  "errorType": "BOUNDARY_ERROR",
  "knowledgePoint": "HashMap",
  "specificError": "未处理重复元素导致查询失败",
  "hints": [
    {
      "level": 1,
      "content": "你可能遗漏了某些边界情况。"
    },
    {
      "level": 2,
      "content": "重点检查 HashMap 中 key 的判断逻辑。"
    },
    {
      "level": 3,
      "content": "遍历数组时，先判断 target - nums[i] 是否存在，再写入当前值。"
    }
  ],
  "confidence": 0.86,
  "steps": [
    {
      "stepName": "CODE_EXECUTION",
      "toolName": "CodeExecutionTool",
      "status": "SUCCESS",
      "durationMs": 1320
    }
  ]
}
```

SSE 示例：

```text
event: agent_step
data: {"step":"PLANNING","message":"正在判断需要调用哪些工具"}

event: agent_step
data: {"step":"CODE_EXECUTION","message":"正在执行 Java 测试用例"}

event: agent_step
data: {"step":"OBSERVATION","message":"检测到 2/5 个用例失败"}

event: agent_step
data: {"step":"ERROR_CLASSIFICATION","message":"正在分类错误类型"}

event: done
data: {"errorType":"BOUNDARY_ERROR","knowledgePoint":"HashMap","confidence":0.86}
```

### 5.4 用户学习接口

```http
GET /api/users/{userId}/weaknesses
GET /api/users/{userId}/training-plans/latest
POST /api/users/{userId}/training-plans/generate
GET /api/users/{userId}/mistakes
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
  "confidence": 0.86
}
```

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

### 本地 Phase 1 启动方式

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
MYSQL_USERNAME=root;MYSQL_PASSWORD=123456;REDIS_HOST=localhost;REDIS_PORT=6379;PISTON_BASE_URL=http://localhost:2000/api/v2
```

也可以用 PowerShell 启动：

```powershell
cd D:\code\ai-study\backend
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="123456"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
$env:PISTON_BASE_URL="http://localhost:2000/api/v2"
mvn spring-boot:run
```

### 后端验证

- 使用 Postman 或 curl 测试题目接口。
- 提交一段正确 Java 代码，确认判题通过。
- 提交一段错误 Java 代码，确认返回失败用例。
- 提交一段编译错误代码，确认返回编译错误。

Phase 1 已验证接口：

```http
GET http://localhost:8080/api/problems
GET http://localhost:8080/api/problems/101
GET http://localhost:8080/api/problems/101/template
POST http://localhost:8080/api/submissions
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

### 前端验证

- 打开首页，确认题目列表展示。
- 进入做题页，确认 Monaco Editor 可用。
- 提交代码，确认测试结果展示。
- 触发 Agent 诊断，确认 SSE 流式输出 Agent 步骤。
- 打开 Dashboard，确认弱点和训练计划展示。

### 演示验证

完整走一遍：

```text
选择 Two Sum -> 写 bug 代码 -> 提交失败 -> Agent 调用判题 Tool -> Observation -> 错误分类 -> 分层提示 -> 更新弱点 -> 生成训练计划
```

## 8. 关键风险和应对

| 风险 | 应对 |
| --- | --- |
| Piston 公共 API 需要授权 | 本地通过 Docker Desktop 自建 Piston，后端默认 `PISTON_BASE_URL=http://localhost:2000/api/v2` |
| 本地 Piston HTTP/2 请求返回 400 | `PistonClient` 使用 HTTP/1.1 请求工厂访问本地 Piston |
| Piston API 不稳定 | 后端保留 `JudgeService` 抽象，后期可替换为 Docker 沙箱或其他判题服务；演示前准备固定样例 |
| AI 响应慢 | 使用 SSE 流式输出 Agent 步骤，前端展示当前执行到哪个 Tool |
| AI 分类不准 | 准备 10 个固定错误样例调 Prompt |
| 前端做不完 | 优先完成做题页，Dashboard 用列表代替图表 |
| 题库太多拖慢进度 | MVP 先做 10 道题，README 写可扩展到 30 道 |
| Piston 测试用例封装复杂 | 第一版使用固定 Java Main 模板拼接测试输入 |

## 9. 演示脚本

推荐面试演示流程：

1. 打开首页，展示题目列表。
2. 选择 Two Sum。
3. 解释项目不是刷题平台，也不是 AI 聊天壳，而是 Java 代码诊断 Agent。
4. 在 Monaco Editor 中写一段有 bug 的 Java 代码。
5. 点击提交，展示 Piston 返回测试失败结果。
6. 打开 Agent 诊断面板，展示 SSE 步骤流。
7. 展示错误分类：例如 `BOUNDARY_ERROR`、`HashMap`。
8. 展示 Level 1、Level 2、Level 3 分层提示。
9. 修改代码后重新提交并通过。
10. 展示 AI Code Review。
11. 打开 Dashboard，展示薄弱点和 3 天训练计划。
12. 讲解后端设计：Piston 封装、Agent Tool、Observation、Memory、SSE、MySQL、Redis。

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
