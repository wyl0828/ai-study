# AI Interview Coach Agent 项目设计文档

## 1. 项目定位

AI Interview Coach Agent 是一个基于 Agent Workflow 的 Java 代码诊断与面试训练系统。

它不是普通刷题平台，也不是简单的 AI 聊天助手，更不是 Spring Boot 包一层大模型 API。系统核心关注点是让 Agent 围绕一次代码提交完成工具调用、结果观察、错误归因、分层提示、弱点记忆更新和训练计划生成。

Agent-first 核心闭环：

```text
Agent 收到代码诊断任务
  -> Planner 判断需要执行判题工具
  -> CodeExecutionTool 执行 Java 代码和测试用例
  -> Observation 返回编译错误 / 运行错误 / 失败用例
  -> ErrorClassifierTool 分类错误类型和知识点
  -> WeaknessTrackerTool 更新长期弱点记忆（非核心，失败不阻塞）
  -> TrainingPlannerTool 生成后续训练建议（非核心，失败不阻塞）
  -> 输出完整诊断报告
```

该项目适合作为 Java 后端求职简历项目，重点展示：

- Spring Boot 后端业务建模能力
- 第三方代码执行服务封装能力
- MySQL 数据库表设计能力
- MyBatis-Plus Mapper 与 SQL 层设计能力
- Redis 缓存与临时状态管理能力
- SSE 流式输出能力
- Agent Workflow、Tool Calling、Observation 和 Memory 设计能力
- Agent Trace 与步骤状态记录能力
- 面向求职场景的完整产品闭环

## 2. 文档关系

本仓库建议保留以下核心文档：

| 文件 | 用途 |
| --- | --- |
| `docs/AI-Interview-Coach.md` | 项目设计、README、简历包装、面试讲解 |
| `docs/IMPLEMENTATION_PLAN.md` | 实际开发计划，按阶段推进实现 |
| `docs/API.md` | 当前已实现后端 REST / SSE 接口规范 |
| `docs/PROJECT_STATUS.md` | 当前成果、进度判断、主要风险和下一步开发大纲 |

当前文档用于说明项目是什么、为什么值得做、有哪些模块、数据库和接口如何设计，以及如何写进简历。

## 3. 技术栈

### 前端

- Next.js 14
- Tailwind CSS
- Monaco Editor

当前前端落地状态：

- 已实现 `/` 题库页、`/problem/[id]` 做题页、`/dashboard` 学习中心。
- 页面已按 `stitch_front_end_interface_design/mvp/` 下的 HTML 原型还原为紧凑 MVP 风格。
- 页面文案已完成中文化，风格参考国内技术学习产品和 LeetCode 中文站。
- 做题页已明确拆分“题目预设分层提示”和“AI 诊断”：左侧题目区域展示题目级 Level 1/2/3 静态提示，右侧只保留“测试结果”和“AI 诊断”两个 tab。
- 提交失败后通过 SSE 实时展示 Agent 执行步骤，完成后展示诊断结果；同步 `POST /api/agent/analyze` 保留作为 fallback。
- 做题页模板加载已统一为浏览器端请求 `/api/problems/{id}/template`：`102/103/104` 显示 `class Solution`，`101/105/106/107/108` 仍显示 ACM `public class Main`。
- Dashboard 已通过用户学习查询接口接入真实 MySQL 数据，展示弱点、错题卡、最近提交和训练计划。
- 如果本地 Dashboard 接口提示不存在，需要确认 Spring Boot 后端已重启到包含 `UserController` 的最新代码，并确认新增后端文件已纳入版本控制。

### 后端

- Spring Boot 3
- Java 17
- MySQL 8
- MyBatis-Plus
- Redis
- Server-Sent Events

### AI 能力

- Anthropic 兼容 API
- 默认可接 Claude
- 也可接 DeepSeek、Qwen 等兼容 Anthropic 协议或适配后的模型服务
- AI 输出使用结构化 JSON，方便后端落库和统计
- Agent Workflow 使用状态机和工具链实现，不把 LLM 当成单纯聊天接口
- LLM 主要负责错误归因、提示生成、代码点评和训练计划等语义判断节点
- 工具调用、状态流转、持久化和 SSE 过程输出由 Spring Boot 后端控制

### 代码执行

- MVP 默认接入 Piston API
- 第一版只支持 Java
- 当前后端使用 `JudgeService` 作为服务层判题抽象，Piston 调用封装在 `integration.piston.PistonClient`
- 后续可通过新增实现替换为 Docker 沙箱

### 推荐架构

```text
Next.js 前端
  ↓
Spring Boot API
  ↓
SubmissionService 创建提交记录
  ↓
Solution 题目通过 CodeWrapper 包装为 Main.java
  ↓
MyBatis-Plus Mapper -> MySQL
  ↓
InterviewCoachAgent
  ↓
Planner -> CodeExecutionTool -> Piston API
  ↓
Observation -> ErrorClassifierTool -> Anthropic Compatible API
  ↓
WeaknessTrackerTool -> TrainingPlannerTool
  ↓
MySQL / Redis / SSE / Agent Trace
  ↓
前端展示题目预设提示、测试结果、AI 诊断、Agent 步骤和训练计划
```

### Java 持久层方案

本项目默认使用 **MyBatis-Plus** 接入 MySQL 8，而不是 JPA。

推荐后端分层：

```text
Controller -> Service -> Mapper -> MySQL
```

选择 MyBatis-Plus 的原因：

- 更贴合国内 Java 后端岗位常见技术栈
- 面试时更容易讲 CRUD、Mapper、SQL、索引、分页、事务
- 简单表操作可以使用 `BaseMapper<T>`
- 复杂查询可以保留手写 SQL 的空间

### 后端分包设计

后端采用更贴近国内 Java 后端项目的分包结构：

```text
com.interview.coach
├── controller
├── service
│   └── impl
├── agent
│   └── tool
├── integration
│   ├── piston
│   └── ai
├── entity
├── mapper
├── dto
├── vo
├── enums
├── config
├── handler
└── CoachApplication
```

各包职责：

| 包 | 职责 |
| --- | --- |
| `controller` | 接收 HTTP 请求，调用 service，返回 VO |
| `service` | 定义业务接口 |
| `service.impl` | 实现业务流程编排 |
| `agent` | Agent 编排器、状态、上下文、步骤记录和 AI-native 逻辑 |
| `agent.tool` | 代码执行、错误分类、提示生成、弱点更新、训练计划等 Tool |
| `integration.piston` | 接入 Piston 代码执行 API |
| `integration.ai` | 接入 Anthropic 兼容模型 API |
| `entity` | MySQL 表映射实体 |
| `mapper` | MyBatis-Plus 数据访问接口 |
| `dto` | 请求参数和内部命令对象 |
| `vo` | 返回给前端的响应对象 |
| `enums` | 状态、难度、语言、错误类型、提示等级枚举 |
| `config` | 配置类 |
| `handler` | 全局异常处理和统一响应处理 |

这样拆分后，Agent Workflow 不会混在普通 controller 中，Piston 和模型调用也不会污染核心业务层。代码执行是 Tool，测试结果是 Observation，AI 只处理需要语义判断的节点。

## 4. 核心功能模块

### 4.1 用户训练模块

- 用户注册和登录
- 记录用户代码提交历史
- 记录用户错误类型
- 统计用户薄弱知识点
- 展示个人训练数据

MVP 阶段可以先实现简单账号密码登录，暂不引入复杂权限系统。

### 4.2 题目模块

- MVP 内置 10 道题，保证演示闭环
- 目标题库扩展到 30 道题
- 题型覆盖数组、哈希表、链表、二叉树、动态规划
- 每道题包含题目描述、难度、知识点、Java 模板代码、测试用例、参考思路

推荐题目分布：

| 类型 | MVP 数量 | 目标数量 | 代表题目 |
| --- | ---: | ---: | --- |
| 数组 | 2 | 6 | 两数之和、三数之和 |
| 哈希表 | 2 | 6 | 有效的字母异位词、最长无重复子串 |
| 链表 | 2 | 6 | 反转链表、合并有序链表 |
| 二叉树 | 2 | 6 | 最大深度、层序遍历 |
| 动态规划 | 2 | 6 | 爬楼梯、最长递增子序列 |

### 4.3 代码编辑与提交模块

- 前端使用 Monaco Editor 编辑 Java 代码
- 用户提交代码后，后端创建提交记录
- 后端调用 Piston API 执行代码
- 返回编译错误、运行错误、测试通过数和失败用例

第一版只支持 Java，避免多语言判题拖慢开发进度。

Phase 1 本地开发说明：

- Piston 公共 API 已改为白名单授权模式，本地开发和演示优先使用 Docker 自建 Piston。
- 当前本机 Piston API 地址为 `http://localhost:2000/api/v2`。
- 当前安装的 Piston Java runtime 为 `java 15.0.2`，题库模板避免使用 Java 17 专属语法。
- 后端工程仍使用 Java 17 和 Spring Boot 3；Piston runtime 只影响用户提交代码的执行环境。
- 本地 Piston 对 Java HTTP 客户端默认 HTTP/2 请求不兼容，`PistonClient` 强制使用 HTTP/1.1。
- 判题底层仍使用 stdin/stdout：每条 `test_case.input_data` 作为 stdin，标准输出与 `expected_output` 归一化后比较。ACM 题由用户提交完整 `public class Main`，Solution 题由后端包装出 `public class Main`。
- 当前已引入小范围 Solution 模式试点：`problemId=102/103/104` 的 `template_code` 和用户提交为非 `public` 的 `class Solution`，后端通过 `CodeWrapper` 在调用 Piston 前生成完整 `Main.java`。数据库 `submission.code` 仍保存用户原始 Solution 代码，便于 AI 诊断围绕用户代码而不是包装代码。
- `101/105/106/107/108` 暂保留 ACM 模式；不新增 `code_mode` 字段，不改变 REST 请求结构。

当前 Solution 签名：

| problemId | 题目 | 方法签名 |
| --- | --- | --- |
| 102 | 有效字母异位词 | `public boolean isAnagram(String s, String t)` |
| 103 | 反转链表 | `public ListNode reverseList(ListNode head)` |
| 104 | 合并两个有序链表 | `public ListNode mergeTwoLists(ListNode list1, ListNode list2)` |

### 4.4 Agent Workflow 模块

系统通过 `InterviewCoachAgent` 编排一次代码诊断任务。Agent 不直接替用户写答案，而是按状态机顺序调用多个 Tool：

| Tool | 职责 |
| --- | --- |
| `CodeExecutionTool` | 调用 `JudgeService` 执行 Java 代码，生成判题 Observation |
| `ErrorClassifierTool` | 根据题目、代码、失败用例和历史弱点分类错误 |
| `WeaknessTrackerTool` | 更新用户薄弱知识点和错题卡片（非核心，失败不阻塞） |
| `TrainingPlannerTool` | 根据弱点记忆生成下一步训练建议（非核心，失败不阻塞） |

Agent 执行过程会产生步骤记录：

- `stepName`
- `toolName`
- `status`
- `inputSummary`
- `outputSummary`
- `durationMs`
- `errorMessage`

这些步骤既可以通过 SSE 推送给前端，也可以作为 Agent Trace 持久化，方便面试时解释“Agent 每一步做了什么、慢在哪里、失败在哪里”。

### 4.5 AI 错误诊断模块

AI 不直接替代后端业务流程，而是在 `ErrorClassifierTool` 和 `TrainingPlannerTool` 中负责语义判断。错误诊断 Tool 根据以下上下文生成结构化结果：

- 题目信息
- 用户代码
- 测试执行结果
- 失败用例
- 错误日志
- 用户历史薄弱点

诊断结果包括：

- 错误类型
- 关联知识点
- 具体错误原因
- 改进建议
- 置信度评分

常见错误类型：

- `SYNTAX_ERROR`：语法错误
- `LOGIC_ERROR`：逻辑错误
- `BOUNDARY_ERROR`：边界条件遗漏
- `ALGORITHM_ERROR`：算法选择不当
- `TIMEOUT`：时间复杂度过高
- `RUNTIME_ERROR`：运行时异常

### 4.6 分层提示模块

系统不直接给完整答案，而是模拟面试官引导过程，提供三级提示。当前产品上把提示分成两类：

- **题目预设分层提示**：属于题目内容，存储在后端 `problem` 表（`hint_level1/2/3`），通过 `GET /api/problems/{id}` 的 `presetHints` 字段返回，展示在左侧题目描述下方，不调用 AI。
- **Agent 诊断中的提示数据**：`HintGeneratorTool` 已从 Agent 工作流移除，`hint_record` 表保留但不再写入新数据。当前前端右侧不再单独展示 AI 分层提示 tab，避免和”AI 诊断 / 改进建议”重复。

题目预设分层提示的展示规则：

| 提示等级 | 说明 | 示例 |
| --- | --- | --- |
| Level 1 | 只提示方向 | 你可能遗漏了某些边界情况 |
| Level 2 | 指出知识点 | 重点检查 HashMap 中 key 的判断逻辑 |
| Level 3 | 给伪代码或关键思路 | 遍历数组时，先判断 target - nums[i] 是否存在，再写入当前值 |

这个设计让通用解题引导和本次提交诊断边界更清楚：左侧提示回答“这道题可以怎么想”，右侧 AI 诊断回答“你这次为什么错”。

### 4.7 薄弱点记忆模块

每次 Agent 诊断后，`WeaknessTrackerTool` 根据错误类型和知识点更新用户长期弱点记忆。

记录维度：

- 知识点
- 错误次数
- 提交次数
- 错误率
- 最近错误时间
- 薄弱分数

薄弱点用于生成训练计划和错题复习卡片。这里的 Memory 不是把所有聊天记录塞进模型，而是把长期训练状态结构化存到 MySQL，必要时再由 Agent 读取。

### 4.8 训练计划模块

`TrainingPlannerTool` 根据用户薄弱点生成 3 天训练计划。

训练计划包含：

- 每天推荐题目
- 训练知识点
- 推荐原因
- 复习建议
- 完成状态

示例：

```text
第 1 天：哈希表专项
- 两数之和
- 有效的字母异位词
- 重点复习 key 判断和重复元素处理

第 2 天：二叉树递归专项
- Maximum Depth of Binary Tree
- Path Sum
- 重点复习递归终止条件和空节点处理

第 3 天：动态规划入门
- Climbing Stairs
- House Robber
- 重点复习状态定义和初始化
```

## 5. 数据库表设计

### 5.1 user

```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(128),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

### 5.2 problem

```sql
CREATE TABLE problem (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(128) NOT NULL,
    description TEXT NOT NULL,
    difficulty VARCHAR(32) NOT NULL,
    category VARCHAR(64) NOT NULL,
    input_format TEXT,
    output_format TEXT,
    template_code TEXT,
    solution_outline TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

### 5.3 knowledge_point

```sql
CREATE TABLE knowledge_point (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    category VARCHAR(64) NOT NULL,
    description TEXT
);
```

### 5.4 problem_knowledge_point

```sql
CREATE TABLE problem_knowledge_point (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    problem_id BIGINT NOT NULL,
    knowledge_point_id BIGINT NOT NULL
);
```

### 5.5 test_case

```sql
CREATE TABLE test_case (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    problem_id BIGINT NOT NULL,
    input_data TEXT NOT NULL,
    expected_output TEXT NOT NULL,
    is_sample TINYINT NOT NULL DEFAULT 0,
    weight INT NOT NULL DEFAULT 1
);
```

### 5.6 submission

```sql
CREATE TABLE submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    language VARCHAR(32) NOT NULL,
    code LONGTEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    passed_count INT NOT NULL DEFAULT 0,
    total_count INT NOT NULL DEFAULT 0,
    execution_time INT,
    memory_usage INT,
    error_message TEXT,
    created_at DATETIME NOT NULL
);
```

### 5.7 ai_diagnosis

```sql
CREATE TABLE ai_diagnosis (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    submission_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    error_type VARCHAR(64),
    knowledge_point_id BIGINT,
    specific_error TEXT,
    diagnosis_summary TEXT,
    suggestion TEXT,
    confidence_score DECIMAL(5, 2),
    created_at DATETIME NOT NULL
);
```

### 5.8 hint_record

```sql
CREATE TABLE hint_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    submission_id BIGINT,
    hint_level INT NOT NULL,
    hint_content TEXT NOT NULL,
    created_at DATETIME NOT NULL
);
```

### 5.9 user_weakness

```sql
CREATE TABLE user_weakness (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    knowledge_point_id BIGINT NOT NULL,
    wrong_count INT NOT NULL DEFAULT 0,
    submit_count INT NOT NULL DEFAULT 0,
    weakness_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    last_wrong_at DATETIME,
    updated_at DATETIME NOT NULL
);
```

### 5.10 training_plan

```sql
CREATE TABLE training_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    summary TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at DATETIME NOT NULL
);
```

### 5.11 training_plan_item

```sql
CREATE TABLE training_plan_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    problem_id BIGINT,
    knowledge_point_id BIGINT,
    reason TEXT,
    day_index INT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'TODO'
);
```

### 5.12 mistake_card

```sql
CREATE TABLE mistake_card (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    submission_id BIGINT NOT NULL,
    error_type VARCHAR(64),
    knowledge_point_id BIGINT,
    mistake_summary TEXT,
    correct_idea TEXT,
    created_at DATETIME NOT NULL
);
```

## 6. 后端接口设计

### 6.1 题目接口

```http
GET /api/problems
GET /api/problems/{id}
GET /api/problems/{id}/template
```

### 6.2 代码提交接口

```http
POST /api/submissions
```

请求示例：

```json
{
  "userId": 1,
  "problemId": 101,
  "language": "java",
  "code": "public class Main { ... }"
}
```

对于 `problemId=102/103/104`，`code` 可以是 `class Solution { ... }`；对于其他当前题目，仍提交完整 `public class Main`。

响应示例：

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

### 6.3 Agent 诊断 SSE 接口

```http
GET /api/submissions/{submissionId}/diagnosis/stream
```

SSE 返回示例：

```text
event: agent_step
data: {"step":"PLANNING","message":"正在判断本次提交需要调用哪些工具"}

event: agent_step
data: {"step":"CODE_EXECUTION","message":"正在运行 Java 测试用例"}

event: agent_step
data: {"step":"OBSERVATION","message":"检测到 2/5 个测试用例失败"}

event: agent_step
data: {"step":"ERROR_CLASSIFICATION","message":"正在分类错误类型和关联知识点"}

event: done
data: {"errorType":"BOUNDARY_ERROR","knowledgePoint":"HashMap","confidence":0.86,"hintLevel1":"..."}
```

### 6.4 Agent 分析接口

```http
POST /api/agent/analyze
```

`/api/agent/analyze` 用于同步分析失败提交。代码通过后的 Code Review 和逐级获取提示暂未暴露为 REST Controller。

### 6.5 用户学习接口

```http
GET /api/users/{userId}/dashboard/stats
GET /api/users/{userId}/weaknesses
GET /api/users/{userId}/training-plans/latest
GET /api/users/{userId}/mistakes
GET /api/users/{userId}/submissions/recent
```

Dashboard 当前通过以上接口读取真实 MySQL 学习数据；手动重新生成训练计划暂未暴露为接口。

## 7. Agent Workflow 设计

### 7.1 整体流程

```text
用户提交代码
  ↓
Submission Service 保存提交记录
  ↓
InterviewCoachAgent 创建 AgentRun
  ↓
Planner 判断需要调用 CodeExecutionTool
  ↓
CodeExecutionTool 调用 JudgeService / Piston API
  ↓
Observation 记录编译错误、运行错误、失败用例
  ↓
ErrorClassifierTool 诊断错误类型和知识点
  ↓
WeaknessTrackerTool 更新弱点记忆和错题卡片（非核心，失败不阻塞）
  ↓
TrainingPlannerTool 生成训练计划
  ↓
SSE 流式返回 Agent 步骤和最终诊断结果
```

### 7.2 Agent 核心类设计

```text
agent/
├── InterviewCoachAgent.java
├── AgentState.java
├── AgentContext.java
├── AgentStep.java
└── tool/
    ├── Tool.java
    ├── CodeExecutionTool.java
    ├── ErrorClassifierTool.java
    ├── WeaknessTrackerTool.java
    └── TrainingPlannerTool.java
```

`InterviewCoachAgent` 是核心编排器，不直接访问 Controller，也不直接拼 HTTP 请求。它接收 `AgentContext`，按状态机执行 Tool，并把每一步写入 `AgentStep`。Tool 内部可以调用现有 service，例如 `CodeExecutionTool` 调用 `JudgeService`，`WeaknessTrackerTool` 调用 `LearningTracker`。

### 7.3 Agent 输入上下文

```json
{
  "problem": {
    "title": "Two Sum",
    "difficulty": "Easy",
    "category": "HashMap",
    "knowledgePoints": ["HashMap", "Array"]
  },
  "submission": {
    "language": "java",
    "code": "public class Main { ... }",
    "status": "WRONG_ANSWER"
  },
  "executionResult": {
    "passedCount": 7,
    "totalCount": 10,
    "failedCases": [
      {
        "input": "[3,3], target=6",
        "expected": "[0,1]",
        "actual": "[]"
      }
    ]
  },
  "userWeaknesses": [
    {
      "knowledgePoint": "HashMap",
      "weaknessScore": 72.5
    }
  ]
}
```

### 7.4 Agent 输出结构

```json
{
  "agentRunId": "run_1001",
  "errorType": "BOUNDARY_ERROR",
  "knowledgePoint": "HashMap",
  "specificError": "未处理重复元素导致查询失败",
  "diagnosis": "代码没有正确处理重复元素场景。",
  "hintLevel1": "考虑是否所有边界输入都被覆盖。",
  "hintLevel2": "重点检查 map 中 key 的判断逻辑。",
  "hintLevel3": "遍历数组时，先判断 target - nums[i] 是否存在，再写入当前值。",
  "review": "整体思路正确，但边界处理不完整。",
  "weaknessScoreDelta": 8,
  "confidence": 0.86,
  "steps": [
    {
      "stepName": "CODE_EXECUTION",
      "toolName": "CodeExecutionTool",
      "status": "SUCCESS",
      "durationMs": 1240
    }
  ]
}
```

## 8. MVP 范围控制

### 必须完成

- 已完成：题目列表和详情
- 已完成：Java 代码编辑器
- 已完成：Java 代码提交和 Piston 执行
- 已完成：测试结果解析
- 已完成：AI 错误诊断
- 已完成：Agent Workflow 编排
- 已完成：SSE 流式返回 Agent 步骤的后端接口
- 已完成：Agent Step / Trace 记录
- 已完成：题目预设分层提示展示（数据来自后端 problem 表），后端 AI hint 生成已移除
- 已完成：Dashboard 查询接口和真实数据接入
- 已完成：薄弱点统计、错题卡和 3 天训练计划从 MySQL 持久化数据读取
- 已完成：无数据时 Dashboard 显示空状态引导文案

### 暂不实现

- 多语言判题
- 完整 LeetCode 题库
- 本地 Docker 沙箱
- 视频或语音面试
- 多 Agent 协作
- 企业级权限系统
- 复杂 UI 动画

## 9. 简历亮点写法

### 简洁版

```text
AI Interview Coach：基于 Spring Boot、Next.js 和 LLM 构建面向 Java 后端求职者的 AI 面试训练系统，实现算法题在线提交、Piston 代码执行、测试结果解析、AI 错误诊断、分层提示、薄弱知识点统计和个性化训练计划生成。
```

### 后端重点版

```text
使用 Spring Boot 设计题目、提交、诊断、提示、训练计划等核心模块；基于 MyBatis-Plus 接入 MySQL 8，完成题目、提交记录、AI 诊断、薄弱点和训练计划等数据持久化；封装 Piston 代码执行服务，支持 Java 代码编译运行、测试用例判定和错误日志解析，并使用 Redis 缓存热门题目与用户训练状态。
```

### AI Agent 重点版

```text
设计基于状态机的 Agent Workflow，将代码执行、错误分类、提示生成、弱点追踪、训练规划封装为独立 Tool，通过 Agent 编排器串联执行。代码执行结果作为 Observation 输入后续推理节点，每步记录 Agent Step，并通过 SSE 流式输出执行过程。
```

### 扩展性重点版

```text
采用策略模式抽象代码执行服务，MVP 阶段接入 Piston API，后续可平滑替换为 Docker 沙箱，提升系统可控性与安全性。
```

## 10. 面试可讲技术点

### 10.1 代码执行服务如何设计？

- 第一版使用 Piston API。
- 后端定义 `JudgeService` 接口作为代码执行与测试用例判定抽象。
- 当前实现为 `JudgeServiceImpl`，内部调用 `integration.piston.PistonClient`。
- PistonClient 只负责外部 HTTP 调用，Controller 不直接依赖 PistonClient。
- 后续可以新增 Docker 沙箱实现，并保持 Controller 与 SubmissionService 基本不变。
- 这样可以体现扩展性和安全意识。

### 10.2 为什么使用 SSE？

- Agent 诊断过程不是瞬时完成。
- SSE 适合服务端向浏览器单向推送。
- 比轮询更实时。
- 比 WebSocket 更轻量。
- 很适合流式展示 Agent 步骤，例如运行测试、观察失败用例、分类错误、生成提示、更新弱点。

### 10.3 如何判断用户错误类型？

系统结合三类信息：

- 测试执行结果
- 失败用例
- 用户代码

再让 LLM 输出结构化错误分类，最后落库到 `ai_diagnosis` 和 `user_weakness`。

### 10.4 如何避免 AI 直接给答案？

- 使用题目预设分层提示机制。
- Level 1 只给方向。
- Level 2 给知识点。
- Level 3 给伪代码。
- 前端把通用提示放在题目区，不让它看起来像每次都要调用模型生成。
- AI 诊断只针对本次提交解释错误原因和改进建议。
- 默认不直接返回完整代码。

### 10.5 如何生成训练计划？

- 根据 `user_weakness` 的错误次数、提交次数、错误率和最近错误时间计算薄弱分数。
- 根据薄弱知识点匹配相关题目。
- 生成 3 天训练计划。

### 10.6 Redis 用在哪里？

- 缓存热门题目列表。
- 缓存题目详情。
- 缓存用户最近训练状态。
- 缓存 AI 诊断过程中的临时上下文。

### 10.7 为什么使用 MyBatis-Plus？

- 国内 Java 后端岗位更常见。
- 简单 CRUD 可以直接使用 `BaseMapper<T>`。
- 复杂查询可以通过 XML 或注解 SQL 保持可控。
- 面试时可以展开讲 Mapper、分页、索引、事务和 SQL 优化。

### 10.8 项目最大亮点是什么？

项目不是简单调用 AI API，而是形成完整 Agent Workflow 训练闭环：

```text
任务输入 -> Planner -> Tool 调用 -> Observation -> 错误归因 -> 分层提示 -> 长期弱点记忆 -> 训练计划
```

这个闭环能体现项目的业务完整性、后端建模能力和 AI Agent 工程化能力。LLM 不是聊天入口，而是 Agent 工作流中的语义判断节点。

### 10.9 Agent 和普通工作流有什么区别？

普通工作流通常是固定的 service 串联；本项目的 Agent Workflow 会维护 `AgentContext`、`AgentState` 和 `AgentStep`，每一步都有明确的工具输入、Observation 输出和状态记录。

MVP 阶段不追求复杂自主规划，而是采用可解释的状态机式 Agent。这样既能体现 Tool Calling、Observation、Memory 和 Trace，也能避免黑盒 Agent 难以调试。

## 11. 推荐演示流程

1. 进入题目列表。
2. 选择“两数之和”演示 ACM 模式，或选择“反转链表 / 有效字母异位词 / 合并两个有序链表”演示 Solution 模式。
3. 在左侧题目区逐层展开预设 Level 1/2/3 提示，说明这些提示属于题目内容，不调用 AI。
4. 在 Monaco Editor 中写一段存在 bug 的 Java 代码。
5. 提交代码并展示测试失败结果。
6. 右侧 AI 诊断面板实时展示 Agent 执行步骤，完成后显示错误类型、关联知识点、错误原因、改进建议和推荐训练。
7. 展示学习中心中的真实薄弱点统计、错题卡片、最近提交和 3 天训练计划。
8. 说明 Dashboard 数据来自 `user_weakness`、`mistake_card`、`training_plan`、`training_plan_item` 和 `submission` 表。
9. 解释后端如何封装 Piston 代码执行服务、Agent Tool、Observation、Memory、Dashboard 查询接口和 SSE 步骤流。

## 12. README 标题建议

```text
AI Interview Coach Agent
基于 Agent Workflow 的 Java 代码诊断与面试训练系统
```

一句话介绍：

```text
一个结合代码执行 Tool、错误诊断 Agent、分层提示、弱点记忆和训练计划的 Java 面试训练系统。
```
