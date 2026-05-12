# API 接口文档

AI Interview Coach 当前后端接口规范。本文档按最新代码整理，覆盖当前已实现的 Controller：

- `ProblemController`
- `SubmissionController`
- `AgentController`
- `UserController`
- `KnowledgeController`

> 说明：Phase 4 已暴露 Dashboard 查询类 REST 接口，前端 `/dashboard` 已从 mock 数据切换为真实用户学习数据。
>
> 说明：当前题库为 Java-only。`problemId=101/105/106/107/108` 暂保留 ACM `public class Main` 提交模式；`problemId=102/103/104` 已切换为 LeetCode 风格 `class Solution` 提交模式，后端在送入 Piston 前自动包装为 `Main.java`。

---

## 1. 通用约定

### 1.1 基础路径

所有业务接口前缀为 `/api`。

### 1.2 统一响应格式

除 SSE 流式接口外，接口返回 `ApiResponse<T>`：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | Integer | `0` 表示成功，非 `0` 表示错误 |
| `message` | String | 成功时为 `success`，失败时为错误描述 |
| `data` | Object / Array / null | 业务数据，失败时为 `null` |

### 1.3 错误码

| code | 含义 |
|------|------|
| `0` | 成功 |
| `400` | 请求参数错误 |
| `404` | 资源不存在 |
| `500` | 服务器内部错误或 Agent 分析失败 |

### 1.4 枚举值

**语言 `language`：**

| 值 | 说明 |
|------|------|
| `java` | v1 唯一支持语言 |

**提交状态 `status`：**

| 值 | 说明 |
|------|------|
| `RUNNING` | 执行中 |
| `ACCEPTED` | 全部通过 |
| `WRONG_ANSWER` | 答案错误 |
| `COMPILE_ERROR` | 编译错误 |
| `RUNTIME_ERROR` | 运行时错误 |
| `TIME_LIMIT_EXCEEDED` | 超时 |
| `SYSTEM_ERROR` | 系统错误 |
| `UNSUPPORTED_LANGUAGE` | 不支持的语言 |

**Agent 步骤状态：**

| 值 | 说明 |
|------|------|
| `RUNNING` | 步骤执行中 |
| `SUCCESS` | 步骤成功 |
| `FAILED` | 步骤失败 |

**Agent 步骤名称 `stepName`：**

| 值 | 说明 |
|------|------|
| `PLANNING` | 准备 Agent 上下文 |
| `CODE_EXECUTION` | 重新执行提交代码 |
| `OBSERVATION` | 观察判题结果 |
| `ERROR_CLASSIFICATION` | AI 错误分类 |
| `MEMORY_UPDATE` | 持久化诊断、弱点和错题卡（非核心，失败不阻塞） |
| `TRAINING_PLAN` | 生成并保存训练计划（非核心，失败不阻塞） |
| `COMPLETED` | Agent 工作流完成 |
| `FAILED` | Agent 工作流失败 |

**AI 错误类型 `errorType`：**

`SYNTAX_ERROR` / `LOGIC_ERROR` / `BOUNDARY_ERROR` / `ALGORITHM_ERROR` / `TIMEOUT` / `RUNTIME_ERROR` / `SYSTEM_ERROR` / `ACCEPTED_REVIEW`

---

## 2. 题目接口

### 2.1 获取题目列表

```http
GET /api/problems
```

**响应 `data`：** `ProblemListItemVO[]`

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 101,
      "title": "Two Sum",
      "difficulty": "EASY",
      "category": "HashMap"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 题目 ID |
| `title` | String | 题目标题 |
| `difficulty` | String | 难度 |
| `category` | String | 分类 |

### 2.2 获取题目详情

```http
GET /api/problems/{id}
```

**路径参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | Long | 是 | 题目 ID |

**响应 `data`：** `ProblemDetailVO`

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 101,
    "title": "Two Sum",
    "description": "Given n integers and a target, return the indices of two numbers whose sum is target.",
    "difficulty": "EASY",
    "category": "HashMap",
    "inputFormat": "Line 1: n. Line 2: n integers. Line 3: target.",
    "outputFormat": "Print two indices separated by one space, or -1 -1.",
    "knowledgePoints": ["Array Traversal", "HashMap Lookup"],
    "sampleCases": [
      {
        "id": 1,
        "input": "4\n2 7 11 15\n9\n",
        "expectedOutput": "0 1",
        "sample": true
      }
    ],
    "presetHints": {
      "level1": "思考每个数需要寻找的另一个数是什么，不要只靠双重循环枚举。",
      "level2": "可以用 HashMap 记录已经遍历过的数值和下标，重点注意查询和写入的顺序。",
      "level3": "遍历 nums 时，先检查 target - nums[i] 是否已经出现；如果出现就输出两个下标，否则再记录当前 nums[i]。"
    }
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 题目 ID |
| `title` | String | 题目标题 |
| `description` | String | 题目描述 |
| `difficulty` | String | 难度 |
| `category` | String | 分类 |
| `inputFormat` | String | 输入格式说明 |
| `outputFormat` | String | 输出格式说明 |
| `knowledgePoints` | String[] | 知识点名称 |
| `sampleCases` | TestCaseVO[] | 示例测试用例 |
| `presetHints` | PresetHintsVO / null | 题目预设分层提示，无数据时为 null |

**PresetHintsVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `level1` | String | 方向提示 |
| `level2` | String | 知识点提示 |
| `level3` | String | 伪代码提示 |

**TestCaseVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 测试用例 ID |
| `input` | String | 输入数据 |
| `expectedOutput` | String | 期望输出 |
| `sample` | Boolean | 是否为示例 |

### 2.3 获取代码模板

```http
GET /api/problems/{id}/template
```

**路径参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | Long | 是 | 题目 ID |

**响应 `data`：** `ProblemTemplateVO`

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "problemId": 101,
    "language": "java",
    "templateCode": "import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        ...\n    }\n}"
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `problemId` | Long | 题目 ID |
| `language` | String | 固定为 `java` |
| `templateCode` | String | Java 模板代码 |

当前模板模式：

| problemId | 模式 | 模板/提交形态 |
|------|------|------|
| `101` | ACM | 用户提交完整 `public class Main`，自行处理 stdin/stdout |
| `102` | Solution | `class Solution { public boolean isAnagram(String s, String t) { ... } }` |
| `103` | Solution | `class Solution { public ListNode reverseList(ListNode head) { ... } }` |
| `104` | Solution | `class Solution { public ListNode mergeTwoLists(ListNode list1, ListNode list2) { ... } }` |
| `105`-`108` | ACM | 暂保留完整 `public class Main` |

---

## 3. 提交与判题接口

### 3.1 提交 Java 代码

```http
POST /api/submissions
Content-Type: application/json
```

**请求体：** `SubmitCodeRequest`

```json
{
  "userId": 1,
  "problemId": 101,
  "language": "java",
  "code": "import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        ...\n    }\n}"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userId` | Long | 是 | 用户 ID |
| `problemId` | Long | 是 | 题目 ID |
| `language` | String | 是 | v1 仅支持 `java` |
| `code` | String | 是 | 用户原始 Java 代码；ACM 题提交完整 `Main`，Solution 题提交非 `public` 的 `class Solution` |

提交模式说明：

- `problemId=102/103/104`：前端提交用户原始 `class Solution`，`submission.code` 也保存原始代码。`SubmissionServiceImpl` 调用 `CodeWrapper.wrap(problemId, code)`，只把送入 `JudgeService/Piston` 的代码包装成包含 `public class Main` 的 `Main.java`。
- `problemId=101/105/106/107/108`：后端不包装，仍按完整 `public class Main` 判题。
- 不存在 `code_mode` 字段，也没有新增 REST 参数；模式由后端当前白名单题号控制。
- `CodeWrapper` 会为链表题注入同级 `class ListNode`，用户模板中的 `Solution` 不能声明为 `public`。

**响应 `data`：** `SubmissionResultVO`

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "submissionId": 1,
    "status": "WRONG_ANSWER",
    "passedCount": 1,
    "totalCount": 3,
    "runtime": null,
    "memory": null,
    "errorMessage": "failed 2 test case(s)",
    "failedCases": [
      {
        "caseId": 2,
        "input": "3\n3 2 4\n6\n",
        "expectedOutput": "1 2",
        "actualOutput": "0 0\n"
      }
    ]
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `submissionId` | Long | 提交记录 ID，后续用于 Agent 诊断 |
| `status` | String | 提交状态 |
| `passedCount` | Integer | 通过用例数 |
| `totalCount` | Integer | 总用例数 |
| `runtime` | Integer / null | 运行时间，单位毫秒；当前 Piston 适配可能返回 null |
| `memory` | Integer / null | 内存占用；当前 Piston 适配可能返回 null |
| `errorMessage` | String / null | 错误信息 |
| `failedCases` | FailedCaseVO[] | 失败用例列表 |

**FailedCaseVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `caseId` | Long | 测试用例 ID |
| `input` | String | 输入数据 |
| `expectedOutput` | String | 期望输出 |
| `actualOutput` | String | 实际输出 |

---

## 4. Agent 诊断接口

Agent 诊断会重新执行提交代码，然后按以下工作流处理：

```text
PLANNING -> CODE_EXECUTION -> OBSERVATION -> ERROR_CLASSIFICATION
-> MEMORY_UPDATE -> TRAINING_PLAN -> COMPLETED
```

其中 `MEMORY_UPDATE` 和 `TRAINING_PLAN` 为非核心步骤，失败不阻塞后续流程。

诊断会产生并持久化：

- `agent_run`
- `agent_step`
- `ai_diagnosis`
- `user_weakness`
- `mistake_card`
- `training_plan`
- `training_plan_item`

> 说明：`HINT_GENERATION` 步骤已移除。题目预设分层提示由前端静态配置提供，AI 诊断不再生成 hintLevel1/2/3。`hint_record` 表保留但不再写入新数据。

### 4.1 触发 Agent 分析（同步）

```http
POST /api/agent/analyze
Content-Type: application/json
```

**请求体：** `AgentAnalyzeRequest`

```json
{
  "submissionId": 1
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `submissionId` | Long | 是 | 已保存的提交 ID |

**响应 `data`：** `AgentAnalyzeVO`

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "agentRunId": 1,
    "submissionId": 1,
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
}
```

**AgentAnalyzeVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `agentRunId` | Long | AgentRun ID |
| `submissionId` | Long | 提交 ID |
| `errorType` | String | AI 分类错误类型 |
| `knowledgePoint` | String | 关联知识点 |
| `specificError` | String | 具体错误 |
| `diagnosis` | String | 诊断说明 |
| `hintLevel1` | String | 已废弃，不再生成，保留字段兼容 |
| `hintLevel2` | String | 已废弃，不再生成，保留字段兼容 |
| `hintLevel3` | String | 已废弃，不再生成，保留字段兼容 |
| `trainingPlanTitle` | String | 生成的训练计划标题 |
| `steps` | AgentStepVO[] | Agent 步骤记录 |

**AgentStepVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `stepName` | String | 步骤名称 |
| `toolName` | String / null | 工具名称 |
| `status` | String | `RUNNING` / `SUCCESS` / `FAILED` |
| `inputSummary` | String | 输入摘要 |
| `outputSummary` | String / null | 输出摘要 |
| `durationMs` | Long / null | 步骤耗时，毫秒 |
| `errorMessage` | String / null | 错误信息 |

### 4.2 SSE 流式 Agent 诊断

```http
GET /api/submissions/{submissionId}/diagnosis/stream
Accept: text/event-stream
```

**路径参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `submissionId` | Long | 是 | 已保存的提交 ID |

**响应：** `text/event-stream`

事件类型：

| event | data | 说明 |
|------|------|------|
| `agent_step` | `AgentStepVO` JSON | 每个步骤开始和结束时都会推送一次 |
| `done` | `AgentAnalyzeVO` JSON | Agent 工作流成功完成 |
| `error` | `ApiResponse<Void>` JSON | Agent 工作流失败 |

> 前端已通过 `fetch + ReadableStream` 接入 SSE，实时展示 Agent 步骤。`done` 事件数据兼容 `ApiResponse` 包裹和裸 `AgentAnalyzeVO` 两种结构。

**示例：**

```text
event:agent_step
data:{"stepName":"PLANNING","toolName":null,"status":"RUNNING","inputSummary":"Prepare agent context","outputSummary":null,"durationMs":null,"errorMessage":null}

event:agent_step
data:{"stepName":"ERROR_CLASSIFICATION","toolName":"ErrorClassifierTool","status":"SUCCESS","inputSummary":"Classify execution observation","outputSummary":"Diagnosis ready","durationMs":23548,"errorMessage":null}

event:done
data:{"agentRunId":1,"submissionId":1,"errorType":"LOGIC_ERROR","knowledgePoint":"HashMap Lookup in Array Traversal","specificError":"Self-matching due to incorrect map operation order","diagnosis":"Code adds current element before checking, allowing same-element pairing.","hintLevel1":"...","hintLevel2":"...","hintLevel3":"...","trainingPlanTitle":"3-day recovery plan: HashMap Lookup in Array Traversal","steps":[...]}
```

失败示例：

```text
event:error
data:{"code":500,"message":"agent analysis failed","data":null}
```

---

## 5. AI 运行配置

Agent AI 调用通过 Anthropic-compatible Messages API。配置项来自环境变量或 `application.yml` 默认值：

| 环境变量 | 配置项 | 说明 |
|------|------|------|
| `AI_BASE_URL` | `coach.ai.base-url` | Anthropic-compatible base URL，例如 `https://example.com/anthropic` |
| `AI_API_KEY` | `coach.ai.api-key` | API 密钥，不要提交到仓库 |
| `AI_MODEL` | `coach.ai.model` | 模型名 |
| `AI_MAX_TOKENS` | `coach.ai.max-tokens` | 最大输出 token，当前默认 `3000` |
| `AI_ANTHROPIC_VERSION` | `coach.ai.anthropic-version` | Anthropic API version，默认 `2023-06-01` |

PowerShell 示例：

```powershell
$env:AI_BASE_URL="https://example.com/anthropic"
$env:AI_MODEL="mimo-v2.5-pro"
$env:AI_API_KEY="<your-api-key>"
$env:AI_MAX_TOKENS="3000"
```

---

## 6. 用户学习与 Dashboard 接口

Dashboard 查询接口读取 Agent 诊断后持久化的学习数据，用于展示统计卡片、薄弱点、错题卡、训练计划和最近提交记录。当前 demo 前端固定使用 `userId=1`。

### 6.1 学习统计概览

```http
GET /api/users/{userId}/dashboard/stats
```

**响应 `data`：** `DashboardStatsVO`

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "totalSubmissions": 12,
    "passedProblems": 2,
    "weakPointCount": 3,
    "mistakeCount": 4
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `totalSubmissions` | Integer | 用户总提交次数 |
| `passedProblems` | Integer | `ACCEPTED` 状态下通过的不同题目数 |
| `weakPointCount` | Integer | 薄弱点记录数量 |
| `mistakeCount` | Integer | 错题卡数量 |

### 6.2 薄弱点排行

```http
GET /api/users/{userId}/weaknesses
```

**响应 `data`：** `UserWeaknessVO[]`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 薄弱点记录 ID |
| `knowledgePoint` | String | 知识点 |
| `errorType` | String | 错误类型 |
| `wrongCount` | Integer | 累计错误次数 |
| `weaknessScore` | BigDecimal | 薄弱分数，越高越薄弱 |

排序：按 `weaknessScore DESC`。

### 6.3 错题卡片列表

```http
GET /api/users/{userId}/mistakes
```

**响应 `data`：** `MistakeCardVO[]`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 错题卡 ID |
| `problemId` | Long | 题目 ID |
| `problemTitle` | String | 题目标题 |
| `errorType` | String | 错误类型 |
| `knowledgePoint` | String | 知识点 |
| `mistakeSummary` | String | 错误摘要 |
| `correctIdea` | String | 正确思路 |

排序：按 `created_at DESC`。

### 6.4 最新训练计划

```http
GET /api/users/{userId}/training-plans/latest
```

**响应 `data`：** `TrainingPlanVO / null`

无训练计划时 `data` 返回 `null`。

| 字段 | 类型 | 说明 |
|------|------|------|
| `title` | String | 计划标题 |
| `summary` | String | 计划摘要 |
| `items` | TrainingPlanItemVO[] | 计划条目 |

**TrainingPlanItemVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `itemType` | String | `PROBLEM` / `KNOWLEDGE_CARD`，老数据默认为 `PROBLEM` |
| `knowledgeCardId` | Long / null | 知识卡片 ID，仅知识卡任务使用 |
| `dayIndex` | Integer | 第几天 |
| `knowledgePoint` | String | 知识点 |
| `problemTitle` | String / null | 算法题标题，仅算法题任务使用 |
| `knowledgeCardTitle` | String / null | 知识卡片标题，仅知识卡任务使用 |
| `reason` | String | 推荐原因 |
| `reviewFocus` | String | 复习重点 |
| `status` | String | 训练状态 |

### 6.5 最近提交记录

```http
GET /api/users/{userId}/submissions/recent
```

**响应 `data`：** `SubmissionHistoryVO[]`

| 字段 | 类型 | 说明 |
|------|------|------|
| `problemId` | Long | 题目 ID |
| `problemTitle` | String | 题目标题 |
| `status` | String | 提交状态 |
| `passedCount` | Integer | 通过用例数 |
| `totalCount` | Integer | 总用例数 |
| `createdAt` | LocalDateTime | 提交时间 |

排序：按 `created_at DESC`，限制 10 条。

---

## 7. 当前接口总览

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/problems` | 获取题目列表 |
| `GET` | `/api/problems/{id}` | 获取题目详情 |
| `GET` | `/api/problems/{id}/template` | 获取 Java 代码模板 |
| `GET` | `/api/knowledge/categories` | 获取后端知识卡片分类 |
| `GET` | `/api/knowledge/cards` | 获取后端知识卡片列表 |
| `GET` | `/api/knowledge/cards/{id}` | 获取后端知识卡片详情 |
| `POST` | `/api/submissions` | 提交 Java 代码并判题 |
| `POST` | `/api/agent/analyze` | 同步执行 Agent 诊断 |
| `GET` | `/api/submissions/{submissionId}/diagnosis/stream` | SSE 流式 Agent 诊断 |
| `GET` | `/api/users/{userId}/dashboard/stats` | 获取 Dashboard 学习统计 |
| `GET` | `/api/users/{userId}/weaknesses` | 获取薄弱点排行 |
| `GET` | `/api/users/{userId}/mistakes` | 获取错题卡片列表 |
| `GET` | `/api/users/{userId}/training-plans/latest` | 获取最新训练计划 |
| `GET` | `/api/users/{userId}/submissions/recent` | 获取最近提交记录 |

---

## 8. 当前前端调用时序

## 8. 后端知识训练接口

后端知识训练接口读取 `knowledge_card` 表，用于 `/knowledge` 独立页面。第一版使用结构化 MySQL 知识卡片，不接 RAG，不把算法错误强行映射到 MySQL / Redis / Spring 等八股知识。

### 8.1 获取知识分类

```http
GET /api/knowledge/categories
```

**响应 `data`：** `KnowledgeCategoryVO[]`

```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "category": "JAVA", "label": "Java", "count": 12 },
    { "category": "JVM", "label": "JVM", "count": 5 },
    { "category": "SPRING", "label": "Spring", "count": 6 },
    { "category": "MYSQL", "label": "MySQL", "count": 6 },
    { "category": "REDIS", "label": "Redis", "count": 6 }
  ]
}
```

分类固定顺序：`JAVA`、`JVM`、`SPRING`、`MYSQL`、`REDIS`。Java 基础、集合、并发不作为一级分类，而是放在卡片 `tags` 中。

### 8.2 获取知识卡片列表

```http
GET /api/knowledge/cards
GET /api/knowledge/cards?category=JAVA
```

**响应 `data`：** `KnowledgeCardVO[]`

列表接口不返回完整 `answer` 和 `followUp`，用于页面列表展示。

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "category": "JAVA",
      "label": "Java",
      "title": "HashMap 底层结构",
      "question": "HashMap 在 JDK 1.8 中的底层结构是什么？",
      "answer": null,
      "followUp": null,
      "keyPoints": ["数组定位桶", "链表处理哈希冲突"],
      "difficulty": "MEDIUM",
      "tags": ["基础", "集合", "HashMap"],
      "sourceName": "小林 coding",
      "sourceUrl": "https://xiaolincoding.com/interview/"
    }
  ]
}
```

### 8.3 获取知识卡片详情

```http
GET /api/knowledge/cards/{id}
```

**响应 `data`：** `KnowledgeCardVO`

详情接口返回完整回答、追问和记忆要点。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "category": "JAVA",
    "label": "Java",
    "title": "HashMap 底层结构",
    "question": "HashMap 在 JDK 1.8 中的底层结构是什么？",
    "answer": "HashMap 底层主要由数组、链表和红黑树组成。",
    "followUp": "为什么链表长度超过阈值后不是一定立即转红黑树？",
    "keyPoints": ["数组定位桶", "链表处理哈希冲突", "红黑树优化极端冲突"],
    "difficulty": "MEDIUM",
    "tags": ["基础", "集合", "HashMap"],
    "sourceName": "小林 coding",
    "sourceUrl": "https://xiaolincoding.com/interview/"
  }
}
```

---

## 9. 当前前端调用时序

### 9.1 做题与诊断流程

```text
1. GET  /api/problems
2. GET  /api/problems/{id}
3. 浏览器端 ProblemWorkspace 请求 GET /api/problems/{id}/template
4. POST /api/submissions
5. 如果提交失败：
   前端调用 GET /api/submissions/{submissionId}/diagnosis/stream（SSE）
   实时接收 agent_step 事件展示 Agent 执行步骤
   收到 done 事件后展示最终诊断结果
   同步接口 POST /api/agent/analyze 保留作为 fallback
6. 前端从 AgentAnalyzeVO 中展示 errorType、knowledgePoint、diagnosis、specificError、trainingPlanTitle 和 steps
```

模板加载说明：

- `/problem/[id]/page.tsx` 只在服务端读取题目详情，不再读取模板。
- `ProblemWorkspace.tsx` 在浏览器端按当前 `problemId` 请求 `/api/problems/{id}/template`，因此 Network 面板能看到 `102/103/104` 的模板请求。
- `CodeEditor.tsx` 使用 `ProblemWorkspace` 状态中的 `code` 作为 Monaco `value`。
- “重置代码”会清除该题草稿，并重新请求后端模板，不再恢复写死的默认 `Main` 模板。
- `frontend/lib/api.ts` 对 fetch 使用 `cache: “no-store”`，避免 Next.js 或浏览器复用旧模板响应。
- 做题页左侧”分层提示”优先从 `GET /api/problems/{id}` 返回的 `presetHints` 读取，如果后端未返回则 fallback 到前端 `problemHints.ts` 静态映射；右侧结果区只保留”测试结果”和”AI 诊断”。
- 提交失败后，右侧”AI 诊断”通过 SSE 实时展示 Agent 步骤（Planning → CodeExecution → Observation → ErrorClassification → MemoryUpdate → TrainingPlan → Completed），完成后展示诊断结果。

### 9.2 Dashboard 当前状态

当前 `/dashboard` 页面展示以下模块，数据来自 `/api/users/1/...` 用户学习查询接口：

- 薄弱知识点排行
- 最近提交记录
- 错题卡片
- 训练计划
- AI 教练建议
- 后端知识训练入口

前端加载时会并发请求统计、薄弱点、错题、最新训练计划和最近提交记录。无数据时显示空状态引导文案，不回退 mock 数据。

### 9.3 知识训练页面当前状态

当前 `/knowledge` 页面展示以下模块：

- 分类筛选：全部 / Java / JVM / Spring / MySQL / Redis
- 知识卡片列表
- 卡片详情：问题、标准回答、面试追问、记忆要点和来源链接

页面调用：

```text
GET /api/knowledge/categories
GET /api/knowledge/cards
GET /api/knowledge/cards/{id}
```

### 9.4 Dashboard 联调排查

如果访问 `/api/users/{userId}/...` 返回类似 `No static resource api/users/...`，通常不是前端路径写错，而是当前运行的 `localhost:8080` 后端进程不是包含 `UserController` 的最新代码。处理顺序：

1. 确认当前工作区存在 `backend/src/main/java/com/interview/coach/controller/UserController.java`。
2. 确认 `backend/src/main/java/com/interview/coach/service/impl/UserLearningServiceImpl.java` 存在并实现 5 个查询方法。
3. 重启 Spring Boot 后端，让新 Controller 被重新扫描注册。
4. 如果别人从 git 或远程仓库检查代码，确认新增的 `UserController`、`UserLearningService`、Dashboard VO 和测试文件已经被加入版本控制，而不是停留在 untracked 状态。
5. 重新请求 `GET http://localhost:8080/api/users/1/dashboard/stats`，成功时应返回统一 `ApiResponse` JSON。

### 9.5 尚未暴露的接口

以下增强能力当前尚未暴露为 REST Controller：

- 单独获取某一级 hint
- 对已通过提交做代码 review
- 手动重新生成训练计划
