# API 接口文档

AI Interview Coach 当前后端接口规范。本文档按最新代码整理，覆盖当前已实现的 Controller：

- `ProblemController`
- `SubmissionController`
- `AgentController`
- `UserController`
- `KnowledgeController`

> 说明：Phase 4 已暴露 Dashboard 查询类 REST 接口，前端 `/dashboard` 已从 mock 数据切换为真实用户学习数据。
>
> 说明：当前题库为 Java-only Hot100 精选题，所有当前题目统一使用 LeetCode 风格 `class Solution` 提交模式。后端根据 `problem.code_mode='solution'` 和 `CodeWrapper` 注册表，在送入 Piston 前自动包装为 `Main.java`；数据库仍保存用户原始 `class Solution` 代码。
>
> 说明：后端知识训练为独立 `/knowledge` 模块；RAG V1 已作为 Agent 内部 Tool 接入，基于 MySQL 结构化检索 `problem`、`knowledge_card`、`ai_diagnosis` 和 `mistake_card`，不暴露独立聊天页或公开检索 REST 接口，也不根据算法错因强行推荐 MySQL / Redis / Spring。

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
| `RAG_RETRIEVAL` | 检索题目知识、知识卡和用户历史学习记忆（非核心，失败不阻塞） |
| `ERROR_CLASSIFICATION` | AI 错误分类 |
| `CODE_REVIEW` | AC 提交代码点评，可降级失败 |
| `MEMORY_UPDATE` | 持久化诊断、弱点和错题卡（非核心，失败不阻塞） |
| `TRAINING_PLAN` | 生成并保存训练计划（非核心，失败不阻塞） |
| `COMPLETED` | Agent 工作流完成 |
| `FAILED` | Agent 工作流失败 |

**AI 错误类型 `errorType`：**

`SYNTAX_ERROR` / `LOGIC_ERROR` / `BOUNDARY_ERROR` / `ALGORITHM_ERROR` / `TIMEOUT` / `RUNTIME_ERROR` / `SYSTEM_ERROR` / `ACCEPTED_REVIEW`

**训练计划条目类型 `itemType`：**

| 值 | 说明 |
|------|------|
| `PROBLEM` | 算法题训练项，兼容旧数据默认值 |
| `KNOWLEDGE_CARD` | 后端知识卡片复习项 |

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
      "id": 1,
      "title": "两数之和",
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
    "id": 1,
    "title": "两数之和",
    "description": "任务说明：\n给定一个整数数组 nums 和一个目标值 target，请在数组中找到两个不同位置的元素，使它们的和等于 target。\n\n返回要求：\n返回一个长度为 2 的 int[]，表示两个下标。\n\n约束与边界：\n同一个数组元素不能被使用两次；数组中可能出现重复数字。",
    "difficulty": "EASY",
    "category": "HashMap",
    "inputFormat": "Line 1: n. Line 2: n integers. Line 3: target.",
    "outputFormat": "返回 int[]，判题输出格式为 [i,j]。",
    "knowledgePoints": ["Array Traversal", "HashMap Lookup"],
    "sampleCases": [
      {
        "id": 1,
        "input": "4\n2 7 11 15\n9\n",
        "expectedOutput": "[0,1]",
        "sample": true
      }
    ],
    "solutionOutline": "解题思路：...\n\nJava 参考实现：\n```java\n...\n```",
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
| `description` | String | 题目描述；当前 Hot100 题库统一使用“任务说明 / 返回要求 / 约束与边界”的多段面试式题面 |
| `difficulty` | String | 难度 |
| `category` | String | 分类 |
| `inputFormat` | String | 输入格式说明 |
| `outputFormat` | String | 输出格式说明 |
| `knowledgePoints` | String[] | 知识点名称 |
| `sampleCases` | TestCaseVO[] | 示例测试用例 |
| `solutionOutline` | String / null | 题目预设参考题解，包含复盘文字和可选 Java 参考实现 |
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
    "problemId": 1,
    "language": "java",
    "templateCode": "class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        return new int[0];\n    }\n}"
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
| 当前 Hot100 精选 12 题 | Solution | 用户提交非 `public` 的 `class Solution`，不处理 stdin/stdout |

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
  "problemId": 1,
  "language": "java",
  "code": "class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        return new int[0];\n    }\n}"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userId` | Long | 是 | 用户 ID |
| `problemId` | Long | 是 | 题目 ID |
| `language` | String | 是 | v1 仅支持 `java` |
| `code` | String | 是 | 用户原始 Java 代码；当前题库提交非 `public` 的 `class Solution` |

提交模式说明：

- 前端提交用户原始 `class Solution`，`submission.code` 也保存原始代码。
- `SubmissionServiceImpl` 根据后端内部 DB 字段 `problem.code_mode = solution` 调用 `CodeWrapper.wrap(problem, code)`，只把送入 `JudgeService/Piston` 的代码包装成包含 `public class Main` 的 `Main.java`。
- `code_mode` 是后端内部 DB 配置字段，不是 REST 请求参数；提交接口不接收 `code_mode`。
- `CodeWrapper` 注册表会按题号注入数组、链表、二叉树等测试 harness。链表题会注入同级 `class ListNode`，二叉树题会注入同级 `class TreeNode`，用户模板中的 `Solution` 不能声明为 `public`。

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
        "expectedOutput": "[1,2]",
        "actualOutput": "[0,0]"
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

Agent 诊断会重新执行提交代码，然后按判题结果进入不同分支：

```text
失败提交：
PLANNING -> CODE_EXECUTION -> OBSERVATION -> ERROR_CLASSIFICATION
-> MEMORY_UPDATE -> TRAINING_PLAN -> COMPLETED

AC 提交：
PLANNING -> CODE_EXECUTION -> OBSERVATION -> CODE_REVIEW -> COMPLETED
```

其中 `MEMORY_UPDATE` 和 `TRAINING_PLAN` 为失败诊断后的非核心步骤，失败不阻塞最终诊断结果。

诊断会产生并持久化：

- `agent_run`
- `agent_step`
- `ai_diagnosis`
- `user_weakness`
- `user_weakness_event`
- `mistake_card`
- `training_plan`
- `training_plan_item`

> 说明：`HINT_GENERATION` 步骤已移除。题目预设分层提示由后端 `problem` 表通过 `presetHints` 返回，前端仅保留静态 fallback。AI 诊断不再生成 `hintLevel1/2/3`，`hint_record` 表保留为历史兼容和未来扩展入口，当前流程不写入新数据。

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
| `codeReview` | CodeReviewResult / null | AC 提交的轻量代码点评；失败诊断时为 null |
| `hintLevel1` | String | 已废弃，不再生成，保留字段兼容 |
| `hintLevel2` | String | 已废弃，不再生成，保留字段兼容 |
| `hintLevel3` | String | 已废弃，不再生成，保留字段兼容 |
| `trainingPlanTitle` | String | 生成的训练计划标题 |
| `steps` | AgentStepVO[] | Agent 步骤记录 |

**CodeReviewResult：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `complexity` | String | 复杂度点评 |
| `codeStyle` | String | 代码风格点评 |
| `interviewSuggestion` | String | 面试表达建议 |
| `optimizationPoints` | String[] | 可优化点列表，不包含完整答案 |

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

> 前端已通过 `fetch + ReadableStream` 接入 SSE，实时展示 Agent 步骤。`done` 事件数据兼容 `ApiResponse` 包裹和裸 `AgentAnalyzeVO` 两种结构。`ProblemWorkspace` 使用递增 `streamId` 和 `AbortController` 保护多次提交、页面卸载和旧流回包；同步 `POST /api/agent/analyze` 仅在 SSE error、SSE 正常结束但没有 `done`、或 `done` 数据无效时作为 fallback。失败诊断和 AC 代码点评两个分支都应展示实时步骤。

**示例：**

```text
event:agent_step
data:{"stepName":"PLANNING","toolName":null,"status":"RUNNING","inputSummary":"Prepare agent context","outputSummary":null,"durationMs":null,"errorMessage":null}

event:agent_step
data:{"stepName":"RAG_RETRIEVAL","toolName":"RagRetrieveTool","status":"SUCCESS","inputSummary":"Retrieve problem knowledge and user learning memory","outputSummary":"RAG evidence ready","durationMs":18,"errorMessage":null}

event:agent_step
data:{"stepName":"ERROR_CLASSIFICATION","toolName":"ErrorClassifierTool","status":"SUCCESS","inputSummary":"Classify execution observation","outputSummary":"Diagnosis ready","durationMs":23548,"errorMessage":null}

event:done
data:{"agentRunId":1,"submissionId":1,"errorType":"LOGIC_ERROR","knowledgePoint":"HashMap Lookup in Array Traversal","specificError":"Self-matching due to incorrect map operation order","diagnosis":"Code adds current element before checking, allowing same-element pairing.","codeReview":null,"hintLevel1":null,"hintLevel2":null,"hintLevel3":null,"trainingPlanTitle":"3-day recovery plan: HashMap Lookup in Array Traversal","steps":[...]}
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
| `trendLabel` | String | 趋势标签：如 `新暴露问题`、`最近加重`、`持续薄弱`、`最近改善` |
| `lastDeltaScore` | BigDecimal / null | 最近一次弱点分变化 |
| `lastEventAt` | DateTime / null | 最近一次弱点事件时间 |

排序：按 `weaknessScore DESC`。

### 6.3 最近弱点事件

```http
GET /api/users/{userId}/weakness-events/recent?limit=20
```

**响应 `data`：** `UserWeaknessEventVO[]`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 弱点事件 ID |
| `knowledgePoint` | String | 知识点 |
| `errorType` | String | 错误类型 |
| `sourceType` | String | 来源：`SUBMISSION_FAILED` / `SELF_TEST` |
| `sourceId` | Long / null | 来源记录 ID，提交失败时为 submissionId，自测低分时为 selfTestRecordId |
| `deltaScore` | BigDecimal | 本次变化分 |
| `beforeScore` | BigDecimal | 变化前分数 |
| `afterScore` | BigDecimal | 变化后分数 |
| `reason` | String / null | 触发原因摘要 |
| `createdAt` | DateTime | 事件时间 |

### 6.4 错题卡片列表

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
| `repeatCount` | Integer | 相同 fingerprint 的重复出现次数 |
| `lastSeenAt` | DateTime / null | 最近一次出现时间 |
| `status` | String | 错题状态，当前默认 `OPEN` |

排序：按 `created_at DESC`。失败诊断会按 `userId + knowledgePoint + errorType + normalizedSpecificError` 生成 fingerprint；同一用户的同类未关闭错题会更新 `repeatCount`，不再无限插入重复卡片。

### 6.5 最新训练计划

```http
GET /api/users/{userId}/training-plans/latest
```

**响应 `data`：** `TrainingPlanVO / null`

无训练计划时 `data` 返回 `null`。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 训练计划 ID |
| `title` | String | 计划标题 |
| `summary` | String | 计划摘要 |
| `status` | String | 计划状态：`ACTIVE` / `COMPLETED` / `REGENERATED` |
| `items` | TrainingPlanItemVO[] | 计划条目 |

**TrainingPlanItemVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 训练计划条目 ID |
| `itemType` | String | 条目类型：`PROBLEM` / `KNOWLEDGE_CARD`，为空时前端按 `PROBLEM` 兼容 |
| `dayIndex` | Integer | 第几天 |
| `knowledgePoint` | String | 知识点 |
| `problemId` | Long / null | 算法题 ID，仅算法题任务使用 |
| `problemTitle` | String / null | 算法题标题，仅算法题任务使用 |
| `knowledgeCardId` | Long / null | 知识卡片 ID，仅知识卡任务使用 |
| `knowledgeCardTitle` | String / null | 知识卡片标题，仅知识卡任务使用 |
| `reason` | String | 推荐原因 |
| `reviewFocus` | String | 复习重点 |
| `status` | String | 条目状态：`PENDING` / `COMPLETED` / `SKIPPED` |

### 6.6 更新训练计划条目状态

```http
PATCH /api/users/{userId}/training-plans/items/{itemId}/status
Content-Type: application/json

{
  "status": "COMPLETED"
}
```

支持状态：`PENDING`、`COMPLETED`、`SKIPPED`。当一个计划下所有条目均为 `COMPLETED` 或 `SKIPPED` 时，后端会将计划状态更新为 `COMPLETED`。

### 6.7 手动重新生成训练计划

```http
POST /api/users/{userId}/training-plans/regenerate
Content-Type: application/json

{
  "replaceCurrentPlan": true,
  "reason": "USER_REQUEST"
}
```

当前仍使用确定性计划生成逻辑，不调用 LLM。`replaceCurrentPlan=true` 时，用户已有 `ACTIVE` 计划会标记为 `REGENERATED`，再创建新计划。

### 6.8 错误统计

```http
GET /api/users/{userId}/dashboard/error-stats
```

**响应 `data`：** `ErrorStatsVO`

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "errorTypeDistribution": [
      {
        "errorType": "LOGIC_ERROR",
        "count": 3
      }
    ],
    "topWeakPoints": [
      {
        "knowledgePoint": "HashMap 基础查找",
        "errorType": "LOGIC_ERROR",
        "wrongCount": 2,
        "weaknessScore": 15.0
      }
    ]
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `errorTypeDistribution` | ErrorTypeCount[] | 按错误类型聚合的错题次数 |
| `topWeakPoints` | KnowledgeWeakness[] | 按薄弱分数排序的前 5 个薄弱点 |

**ErrorTypeCount：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `errorType` | String | 错误类型 |
| `count` | Integer | 该错误类型累计错误次数 |

**KnowledgeWeakness：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `knowledgePoint` | String | 知识点 |
| `errorType` | String | 错误类型 |
| `wrongCount` | Integer | 累计错误次数 |
| `weaknessScore` | Double | 薄弱分数 |

### 6.9 最近提交记录

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

## 7. 后端知识训练接口

知识训练接口读取 `knowledge_card` 表，只返回 `enabled = true` 的卡片。分类固定为 `JAVA/JVM/SPRING/MYSQL/REDIS/AI`，前端展示为 Java / JVM / Spring / MySQL / Redis / AI；Java 基础、集合、并发、AI 工程等细分通过 `tags` 和前端知识树组织展示。

### 7.1 获取知识分类

```http
GET /api/knowledge/categories
```

**响应 `data`：** `KnowledgeCategoryVO[]`

```json
[
  {
    "code": "JAVA",
    "label": "Java",
    "cardCount": 12
  }
]
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | String | 分类编码：`JAVA` / `JVM` / `SPRING` / `MYSQL` / `REDIS` |
| `label` | String | 前端展示名称 |
| `cardCount` | Integer | 当前启用卡片数量 |

### 7.2 获取知识卡片列表

```http
GET /api/knowledge/cards
GET /api/knowledge/cards?category=JAVA
```

列表按 `sort_order ASC, id ASC` 排序。列表接口不返回完整答案，只返回用于列表展示的摘要字段。

**响应 `data`：** `KnowledgeCardVO[]`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 卡片 ID |
| `category` | String | 一级分类 |
| `title` | String | 卡片标题 |
| `question` | String | 面试问题 |
| `difficulty` | String | 难度 |
| `tags` | String | 标签，逗号分隔 |
| `sourceName` | String | 来源名称 |

### 7.3 获取知识卡片详情

```http
GET /api/knowledge/cards/{id}
```

详情接口返回完整答案、追问、记忆点和来源链接；未知 ID 或禁用卡片返回 `404`。

| 字段 | 类型 | 说明 |
|------|------|------|
| `answer` | String | 结构化整理后的答案 |
| `followUp` | String | 高频追问 |
| `keyPoints` | String | 记忆点 |
| `sourceUrl` | String | 来源链接，当前为 `https://xiaolincoding.com/interview/` |

### 7.4 提交知识卡自测

```http
POST /api/users/{userId}/knowledge/cards/{cardId}/self-tests
Content-Type: application/json

{
  "userAnswer": "HashMap 底层是数组、链表和红黑树...",
  "score": 70,
  "feedback": "覆盖了部分要点，但缺少树化条件。",
  "missingKeyPoints": ["链表长度达到 8 且数组长度达到 64 时树化"]
}
```

自测评分仍由前端根据 `keyPoints` / `answerKeywords` 轻量计算，后端负责持久化 `self_test_record` 并更新 `user_knowledge_card_mastery`。低分自测会额外写入 `user_weakness_event`，来源为 `SELF_TEST`。

**响应 `data`：** `SelfTestRecordVO`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 自测记录 ID |
| `knowledgeCardId` | Long | 知识卡 ID |
| `score` | Integer | 自测得分 |
| `feedback` | String / null | 反馈文案 |
| `missingKeyPoints` | String[] | 缺失要点 |
| `createdAt` | DateTime | 提交时间 |

### 7.5 获取最近自测记录

```http
GET /api/users/{userId}/knowledge/cards/{cardId}/self-tests/recent?limit=5
```

按 `created_at DESC` 返回最近自测记录，默认限制 5 条。

---

## 8. 当前接口总览

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/problems` | 获取题目列表 |
| `GET` | `/api/problems/{id}` | 获取题目详情 |
| `GET` | `/api/problems/{id}/template` | 获取 Java 代码模板 |
| `GET` | `/api/knowledge/categories` | 获取后端知识卡片分类 |
| `GET` | `/api/knowledge/cards` | 获取后端知识卡片列表 |
| `GET` | `/api/knowledge/cards/{id}` | 获取后端知识卡片详情 |
| `POST` | `/api/users/{userId}/knowledge/cards/{cardId}/self-tests` | 提交知识卡自测 |
| `GET` | `/api/users/{userId}/knowledge/cards/{cardId}/self-tests/recent` | 获取最近知识卡自测记录 |
| `POST` | `/api/submissions` | 提交 Java 代码并判题 |
| `POST` | `/api/agent/analyze` | 同步执行 Agent 诊断 |
| `GET` | `/api/submissions/{submissionId}/diagnosis/stream` | SSE 流式 Agent 诊断 |
| `GET` | `/api/users/{userId}/dashboard/stats` | 获取 Dashboard 学习统计 |
| `GET` | `/api/users/{userId}/weaknesses` | 获取薄弱点排行 |
| `GET` | `/api/users/{userId}/weakness-events/recent` | 获取最近弱点事件 |
| `GET` | `/api/users/{userId}/mistakes` | 获取错题卡片列表 |
| `GET` | `/api/users/{userId}/training-plans/latest` | 获取最新训练计划 |
| `PATCH` | `/api/users/{userId}/training-plans/items/{itemId}/status` | 更新训练计划条目状态 |
| `POST` | `/api/users/{userId}/training-plans/regenerate` | 手动重新生成训练计划 |
| `GET` | `/api/users/{userId}/dashboard/error-stats` | 获取错误类型分布和 Top 薄弱点 |
| `GET` | `/api/users/{userId}/submissions/recent` | 获取最近提交记录 |

---

## 9. 当前前端调用时序

### 9.1 做题与诊断流程

```text
1. GET  /api/problems
2. GET  /api/problems/{id}
3. 浏览器端 ProblemWorkspace 请求 GET /api/problems/{id}/template
4. POST /api/submissions
5. 提交后前端调用 GET /api/submissions/{submissionId}/diagnosis/stream（SSE）
   实时接收 agent_step 事件展示 Agent 执行步骤
   收到 done 事件后展示最终诊断结果或 AC 代码点评
   同步接口 POST /api/agent/analyze 仅作为 fallback
6. `OBSERVATION` 后 Agent 会执行可选 `RAG_RETRIEVAL`，检索题目、知识卡和当前用户历史学习记忆；检索失败只记录 failed step，不阻塞后续诊断或 AC 点评
7. 失败提交展示 errorType、knowledgePoint、diagnosis、specificError、trainingPlanTitle 和 steps
8. AC 提交优先展示 codeReview、steps，不生成错题卡和训练计划；如果 AI code review 失败，后端记录 `CODE_REVIEW` failed step，并降级返回 accepted 分支结果
9. 用户修改编辑器代码但未重新提交时，前端保留上次诊断或 codeReview，并展示 stale warning，提示结果基于上次提交
```

模板加载说明：

- `/problem/[id]/page.tsx` 只在服务端读取题目详情，不再读取模板。
- `ProblemWorkspace.tsx` 在浏览器端按当前 `problemId` 请求 `/api/problems/{id}/template`，因此 Network 面板能看到 Hot100 题目的 `class Solution` 模板请求。
- `CodeEditor.tsx` 使用 `ProblemWorkspace` 状态中的 `code` 作为 Monaco `value`。
- “重置代码”会清除该题草稿，并重新请求后端模板，不再恢复写死的默认模板。
- `frontend/lib/api.ts` 对 fetch 使用 `cache: “no-store”`，避免 Next.js 或浏览器复用旧模板响应。
- 做题页左侧”分层提示”从 `GET /api/problems/{id}` 返回的 `presetHints` 读取；旧 `101-108` 前端静态 fallback 已移除。右侧结果区只保留”测试结果”和”AI 诊断”。
- 提交失败后，右侧”AI 诊断”通过 SSE 实时展示 Agent 步骤（Planning → CodeExecution → Observation → RagRetrieval → ErrorClassification → MemoryUpdate → TrainingPlan → Completed），完成后展示诊断结果。
- AC 提交后，右侧”AI 诊断”在代码点评生成中展示 Agent 步骤（Planning → CodeExecution → Observation → RagRetrieval → CodeReview → Completed），完成后展示 `codeReview`。如果用户随后编辑代码但不重新提交，前端仍保留上次点评，只显示“基于上次提交，仅供参考”的 stale warning。

### 9.2 Dashboard 当前状态

当前 `/dashboard` 页面展示以下模块，数据来自 `/api/users/1/...` 用户学习查询接口：

- 薄弱知识点排行
- 最近提交记录
- 错题卡片
- 训练计划，包括完成、跳过和重新生成入口
- 后端知识训练入口
- 错误类型分布和 Top 薄弱点
- AI 教练建议

前端加载时会并发请求统计、薄弱点、错题、最新训练计划、最近提交记录和错误统计。无数据时显示空状态引导文案，不回退 mock 数据。训练计划操作通过 `PATCH /training-plans/items/{itemId}/status` 和 `POST /training-plans/regenerate` 写回后端，再刷新最新计划。

### 9.3 知识训练页面当前状态

当前 `/knowledge` 页面为前端 V1 真实数据接入：页面优先调用后端知识接口读取 `knowledge_card` 表；如果后端未启动或接口失败，则回退到前端本地示例数据，并在页面顶部显示浅提示。它展示以下模块：

- 搜索框
- 难度筛选：全部 / 简单 / 中等 / 困难
- 分类筛选：全部分类 / Java / MySQL / Redis / Spring / JVM
- 知识点卡片列表
- 展开卡片后的模拟自测输入框和提交按钮
- 提交自测后的点评反馈、评分、命中记忆点、标杆回答解析、核心记忆要点、面试官高频追问和“标记已掌握”
- 最近自测记录展示；提交自测后写入 `self_test_record`，并更新知识卡掌握度
- “跳过自测，直接查看解析”路径；该路径只显示解析区，不显示虚假模拟评分

本地自测评分仍在前端根据 `keyPoints` / `answerKeywords` 简单计算；后端负责保存自测记录、更新 `user_knowledge_card_mastery`，低分自测会写入 `user_weakness_event` 作为学习趋势来源。顶部“标记已掌握”仍是页面内轻量状态，不作为 durable mastery 的唯一来源。

真实数据由 `data/knowledge_cards.sql` 提供 120 张结构化知识卡，侧边栏 24 个最终专题每个至少 5 张。内容参考小林 coding 和 JavaGuide 的公开面试知识目录做选题覆盖后重新整理，AI 工程内容按本项目 Agent/RAG 设计原创组织，不复制原文。`/knowledge` 页面本身不是 RAG 查询入口；这些知识卡会被 RAG V1 索引为 Agent 诊断和训练计划的内部证据来源。

### 9.4 Dashboard 联调排查

如果访问 `/api/users/{userId}/...` 返回类似 `No static resource api/users/...`，通常不是前端路径写错，而是当前运行的 `localhost:8080` 后端进程不是包含 `UserController` 的最新代码。处理顺序：

1. 确认当前工作区存在 `backend/src/main/java/com/interview/coach/controller/UserController.java`。
2. 确认 `backend/src/main/java/com/interview/coach/service/impl/UserLearningServiceImpl.java` 存在并实现 6 个查询方法。
3. 重启 Spring Boot 后端，让新 Controller 被重新扫描注册。
4. 如果别人从 git 或远程仓库检查代码，确认新增的 `UserController`、`UserLearningService`、Dashboard VO 和测试文件已经被加入版本控制，而不是停留在 untracked 状态。
5. 重新请求 `GET http://localhost:8080/api/users/1/dashboard/stats`，成功时应返回统一 `ApiResponse` JSON。

### 9.5 尚未暴露的接口

以下增强能力当前尚未暴露为 REST Controller：

- 单独获取某一级 hint
- 单独暴露 accepted-code review REST 接口（当前通过 `/api/agent/analyze` 和 SSE 的 AC 分支返回 `codeReview`）
- 知识卡片收藏
- 独立 RAG 聊天 / 检索 REST 接口（当前 RAG 仅作为 Agent 内部 Tool 使用）
