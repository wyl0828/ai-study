# API 接口文档

AI Interview Coach 当前后端接口规范。本文档按最新代码整理，覆盖当前已实现的 Controller：

- `ProblemController`
- `SubmissionController`
- `AgentController`

> 说明：Phase 2 已实现 Agent 诊断、分层提示、弱点记忆和训练计划的后端工作流，但 Dashboard 查询类接口尚未暴露为 REST Controller。

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
| `HINT_GENERATION` | AI 生成三层提示 |
| `MEMORY_UPDATE` | 持久化诊断、提示、弱点和错题卡 |
| `TRAINING_PLAN` | 生成并保存训练计划 |
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
    ]
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
| `code` | String | 是 | 完整 Java 源码，入口类为 `Main` |

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
-> HINT_GENERATION -> MEMORY_UPDATE -> TRAINING_PLAN -> COMPLETED
```

诊断会产生并持久化：

- `agent_run`
- `agent_step`
- `ai_diagnosis`
- `hint_record`
- `user_weakness`
- `mistake_card`
- `training_plan`
- `training_plan_item`

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
| `hintLevel1` | String | Level 1 提示：方向 |
| `hintLevel2` | String | Level 2 提示：知识点和可能问题 |
| `hintLevel3` | String | Level 3 提示：伪代码或关键思路，不给完整 Java 答案 |
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

## 6. 当前接口总览

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/problems` | 获取题目列表 |
| `GET` | `/api/problems/{id}` | 获取题目详情 |
| `GET` | `/api/problems/{id}/template` | 获取 Java 代码模板 |
| `POST` | `/api/submissions` | 提交 Java 代码并判题 |
| `POST` | `/api/agent/analyze` | 同步执行 Agent 诊断 |
| `GET` | `/api/submissions/{submissionId}/diagnosis/stream` | SSE 流式 Agent 诊断 |

---

## 7. 当前前端调用时序

### 7.1 做题与诊断流程

```text
1. GET  /api/problems
2. GET  /api/problems/{id}
3. GET  /api/problems/{id}/template
4. POST /api/submissions
5. 如果提交失败：
   GET /api/submissions/{submissionId}/diagnosis/stream
   或 POST /api/agent/analyze
6. 前端从 AgentAnalyzeVO 中展示 diagnosis、hintLevel1、hintLevel2、hintLevel3、trainingPlanTitle
```

### 7.2 尚未暴露的接口

以下能力已有部分持久化表或服务基础，但当前代码还没有 REST Controller：

- 获取用户薄弱点列表
- 获取错题卡片列表
- 获取最新训练计划详情
- 单独获取某一级 hint
- 对已通过提交做代码 review

