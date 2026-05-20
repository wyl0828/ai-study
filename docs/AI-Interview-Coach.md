# AI Interview Coach Agent 项目设计文档

## 1. 项目定位

AI Interview Coach Agent 是一个基于 Agent Workflow 的 Java 代码诊断与后端面试训练系统。

它不是普通刷题平台，也不是简单的 AI 聊天助手，更不是 Spring Boot 包一层大模型 API。系统核心关注点是让 Agent 围绕一次代码提交完成工具调用、结果观察、错误归因或代码点评、弱点记忆更新和训练计划生成。后端知识训练是独立扩展模块，用于补足 Java 后端面试表达训练，不把算法错误强行映射为 MySQL / Redis / Spring 八股推荐。

Agent-first 核心闭环：

```text
Agent 收到代码诊断任务
  -> Planner 判断需要执行判题工具
  -> CodeExecutionTool 执行 Java 代码和测试用例
  -> Observation 返回编译错误 / 运行错误 / 失败用例
  -> RagRetrieveTool 检索题目知识 / 知识卡 / 当前用户历史学习记忆（非核心，失败不阻塞）
  -> 失败提交：ErrorClassifierTool 分类错误类型和知识点
  -> WeaknessTrackerTool 更新长期弱点记忆（非核心，失败不阻塞）
  -> TrainingPlannerTool 生成后续训练建议（非核心，失败不阻塞）
  -> AC 提交：CodeReviewTool 结合可用证据生成轻量代码点评（可降级，失败不阻塞 accepted 结果）
  -> 输出完整诊断报告或代码点评报告
```

该项目适合作为 Java 后端求职简历项目，重点展示：

- Spring Boot 后端业务建模能力
- 第三方代码执行服务封装能力
- MySQL 数据库表设计能力
- MyBatis-Plus Mapper 与 SQL 层设计能力
- Redis 配置预留与缓存扩展设计能力
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
| `docs/KNOWLEDGE_TRAINING_DESIGN.md` | 知识训练模块设计、前端 V1 交互、数据来源和训练计划接入边界 |

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
- 提交后通过 SSE 实时展示 Agent 执行步骤；失败提交完成后展示诊断结果，AC 提交完成后展示代码点评；同步 `POST /api/agent/analyze` 保留作为 fallback。
- 做题页模板加载已统一为浏览器端请求 `/api/problems/{id}/template`：当前 Hot100 精选 12 题全部显示 LeetCode 风格 `class Solution`，旧 `101-108` 前端兜底已移除。
- Dashboard 已通过用户学习查询接口接入真实 MySQL 数据，并重排为学习中心：顶部统计后优先展示“今日优先训练”和完整训练计划，再展示薄弱知识点排行、错误类型分布、最近提交、合并错题卡、后端知识训练入口和确定性 AI 教练建议。
- 已新增 `/knowledge` 知识训练页 V1，优先读取后端知识接口和 `knowledge_card` 真实数据，接口不可用时回退前端示例数据；页面完成可折叠知识体系大纲、统一面包屑/标题/左侧高亮、专题过滤、紧凑筛选、训练状态展示、模拟自测评分、点评反馈、标杆回答解析、高频追问和标记已掌握。AI 工程下的 Agent / RAG / LangChain 当前是前端专题入口，并有本地示例卡兜底；不新增后端接口、不调用真实 AI。
- 如果本地 Dashboard 接口提示不存在，需要确认 Spring Boot 后端已重启到包含 `UserController` 的最新代码，并确认新增后端文件已纳入版本控制。

### 后端

- Spring Boot 3
- Java 17
- MySQL 8
- MyBatis-Plus
- Redis（当前预留配置）
- Server-Sent Events

### AI 能力

- Anthropic 兼容 API
- 默认可接 Claude
- 也可接 DeepSeek、Qwen 等兼容 Anthropic 协议或适配后的模型服务
- AI 输出使用结构化 JSON，方便后端落库和统计
- Agent Workflow 使用状态机和工具链实现，不把 LLM 当成单纯聊天接口
- RAG V1 作为 Agent 内部 Tool 使用 MySQL 结构化检索，不做独立聊天入口或向量数据库
- LLM 主要负责错误归因和代码点评等语义判断节点；当前训练计划使用确定性 fallback，AI hint 生成已停用
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
Observation -> ErrorClassifierTool / CodeReviewTool -> Anthropic Compatible API
  ↓
失败提交 -> WeaknessTrackerTool -> TrainingPlannerTool
  ↓
MySQL / Redis 预留配置 / SSE / Agent Trace
  ↓
前端展示题目预设提示、测试结果、AI 诊断、Agent 步骤、训练计划和知识卡片
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
| `agent.tool` | 代码执行、错误分类、代码点评、弱点更新、训练计划等 Tool |
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

- MVP 当前内置 Hot100 精选 12 道题，优先保证演示闭环
- 暂不追求完整 Hot100 或 30 题扩展，避免稀释 Agent 面试训练主线
- 题型覆盖数组、哈希表、链表、二叉树、动态规划、贪心
- 每道题包含面试式题目描述、难度、知识点、Java `class Solution` 模板代码、3 个测试用例、三层预设提示、参考思路和完整 Java 参考实现

当前 12 题分布：

| 类型 | 当前数量 | 代表题目 |
| --- | ---: | --- |
| 数组 / 贪心 | 2 | 两数之和、买卖股票的最佳时机 |
| 哈希表 | 2 | 字母异位词分组、最长连续序列 |
| 链表 | 3 | 反转链表、合并两个有序链表、环形链表 |
| 二叉树 | 3 | 层序遍历、最大深度、翻转二叉树 |
| 动态规划 | 2 | 爬楼梯、打家劫舍 |

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
- 判题底层仍使用 stdin/stdout：每条 `test_case.input_data` 作为 stdin，标准输出与 `expected_output` 归一化后比较。当前题库统一由用户提交非 `public` 的 `class Solution`，后端包装出 `public class Main` 测试 harness。
- 当前 Hot100 精选 12 题全部为 Solution 模式。后端通过 `CodeWrapper` 注册表按 `problem.id` 选择 adapter，在调用 Piston 前生成完整 `Main.java`；数据库 `submission.code` 仍保存用户原始 Solution 代码，便于 AI 诊断围绕用户代码而不是包装代码。
- `problem.code_mode` 是后端内部 DB 配置字段，用于声明当前题目的判题包装方式，不是 REST 请求参数；当前种子题统一为 `solution`，不保留旧 `101-108` 双题库兼容。

当前 Hot100 精选题：

| problemId | 题目 | 方法签名 |
| --- | --- | --- |
| 1 | 两数之和 | `public int[] twoSum(int[] nums, int target)` |
| 49 | 字母异位词分组 | `public List<List<String>> groupAnagrams(String[] strs)` |
| 128 | 最长连续序列 | `public int longestConsecutive(int[] nums)` |
| 206 | 反转链表 | `public ListNode reverseList(ListNode head)` |
| 21 | 合并两个有序链表 | `public ListNode mergeTwoLists(ListNode list1, ListNode list2)` |
| 141 | 环形链表 | `public boolean hasCycle(ListNode head)` |
| 102 | 二叉树的层序遍历 | `public List<List<Integer>> levelOrder(TreeNode root)` |
| 104 | 二叉树的最大深度 | `public int maxDepth(TreeNode root)` |
| 226 | 翻转二叉树 | `public TreeNode invertTree(TreeNode root)` |
| 70 | 爬楼梯 | `public int climbStairs(int n)` |
| 198 | 打家劫舍 | `public int rob(int[] nums)` |
| 121 | 买卖股票的最佳时机 | `public int maxProfit(int[] prices)` |

### 4.4 Agent Workflow 模块

系统通过 `InterviewCoachAgent` 编排一次代码诊断任务。Agent 不直接替用户写答案，而是按状态机顺序调用多个 Tool：

| Tool | 职责 |
| --- | --- |
| `CodeExecutionTool` | 调用 `JudgeService` 执行 Java 代码，生成判题 Observation |
| `RagRetrieveTool` | 在 Observation 后检索题目知识、知识卡和当前用户历史学习记忆，失败不阻塞后续诊断 |
| `ErrorClassifierTool` | 根据题目、代码、失败用例和历史弱点分类错误 |
| `CodeReviewTool` | 对 AC 提交做复杂度、代码风格和面试表达点评，不生成完整答案 |
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

AI 不直接替代后端业务流程，而是在 `ErrorClassifierTool` 和 `CodeReviewTool` 中负责语义判断。错误诊断 Tool 根据以下上下文生成结构化结果：

- 题目信息
- 用户代码
- 测试执行结果
- 失败用例
- 错误日志
- RAG 检索到的题目知识、知识卡和用户历史错题记忆

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

AC 提交不会写入错题卡或训练计划，而是通过 `CodeReviewTool` 返回轻量点评；如果 AI review 失败，Agent 记录失败 step 并降级返回 accepted 结果，不让点评失败拖垮已通过提交：

- 时间 / 空间复杂度表达
- 代码风格建议
- 面试表达建议
- 可优化点列表

### 4.6 分层提示模块

系统不直接给完整答案，而是模拟面试官引导过程，提供三级提示。当前产品上把提示分成两类：

- **题目预设分层提示**：属于题目内容，存储在后端 `problem` 表（`hint_level1/2/3`），通过 `GET /api/problems/{id}` 的 `presetHints` 字段返回，展示在左侧题目描述下方，不调用 AI。
- **Agent 诊断中的提示数据**：`HintGeneratorTool` 已从当前 Agent 工作流移除，代码中仅作为 `@Deprecated` 历史兼容类保留且不注册为 Spring Bean；`hint_record` 表和 `AgentAnalyzeVO.hintLevel1/2/3` 字段仅保留历史兼容与未来扩展入口，当前流程不写入新 hint 记录。前端右侧不再单独展示 AI 分层提示 tab，避免和“AI 诊断 / 改进建议”重复。

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
- 弱点事件：记录每次分数变化的来源、变化前后分数和触发原因
- 重复错误：错题卡按 fingerprint 合并同类错误，记录 `repeatCount` 和 `lastSeenAt`

薄弱点用于生成训练计划和错题复习卡片。这里的 Memory 不是把所有聊天记录塞进模型，而是把长期训练状态结构化存到 MySQL，必要时再由 Agent 读取。当前失败提交会写入 `user_weakness_event`，知识卡低分自测也会作为 `SELF_TEST` 来源进入弱点事件。

### 4.8 RAG V1 检索模块

RAG V1 不是通用知识库聊天，也不新增独立页面。它是 Agent Workflow 中 `OBSERVATION` 之后的可选 Tool：

```text
Observation -> RagRetrieveTool -> ErrorClassifierTool / CodeReviewTool
```

当前实现使用 MySQL 结构化检索，不引入 embedding、向量数据库、Elasticsearch 或 pgvector。检索来源包括：

- `problem`：题目描述、预设提示和 solution outline。
- `knowledge_card`：后端面试知识卡片。
- `ai_diagnosis`：用户历史失败诊断。
- `mistake_card`：用户历史错题卡。

检索规则是确定性打分：同用户、同题目、同知识点、同错误类型和关键词命中会提高排序；用户历史记忆必须通过 `user_id` 隔离，避免不同用户错题互相泄漏。RAG 失败只记录 `RAG_RETRIEVAL` failed step，不影响代码执行、错误诊断、AC 点评、弱点记录或训练计划。

### 4.9 训练计划模块

`TrainingPlannerTool` 根据用户薄弱点生成 3 天训练计划。当前实现保留 3 条算法训练任务，并可额外混入最多 1-2 条知识卡片复习任务；如果 RAG 命中 `KNOWLEDGE_CARD`，优先使用检索到的卡片，否则 fallback 到通用高优先级知识卡。

训练计划包含：

- 每天推荐题目
- 训练知识点
- 推荐原因
- 复习建议
- 完成状态
- 手动重新生成入口

训练计划 item 当前有两种类型：

| 类型 | 说明 |
| --- | --- |
| `PROBLEM` | 算法题训练，来源于 Agent 诊断后的算法弱点 |
| `KNOWLEDGE_CARD` | 后端知识卡片复习，来源于知识卡片模块的高优先级内容 |

注意：算法弱点不等于后端八股弱点。训练计划可以统一展示两类任务，但算法错误不会直接推荐 MySQL / Redis / Spring。

Dashboard 允许用户将训练项标记为 `COMPLETED` 或 `SKIPPED`；当计划内条目均结束时，计划状态变为 `COMPLETED`。手动重新生成仍使用确定性规则，不调用 LLM；旧 `ACTIVE` 计划会标记为 `REGENERATED`。

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

### 4.10 后端知识训练模块

后端知识训练是独立入口，用于补齐 Java 后端面试中的表达型知识。

第一版范围：

- `/knowledge` 页面
- 当前接口使用 `JAVA/JVM/SPRING/MYSQL/REDIS/AI` 后端分类；前端额外组织为 Java 核心、数据库、Spring、AI 工程四个知识体系入口
- 左侧知识体系支持多级展开/收起；面包屑、左侧高亮、专题标题和描述由同一个 `KnowledgeSelection` 派生
- 当前前端 V1 优先读取后端知识接口；接口失败时回退本地示例数据
- 集合框架专题按 List / Map / Set 做前端匹配过滤，避免 Map 专题混入 ArrayList / LinkedList 等明显不匹配卡片
- AI 工程包含 Agent、RAG、LangChain 三个前端专题入口，并提供少量本地示例卡作为接口不可用时的兜底训练内容；它不是独立 RAG 聊天入口，也不新增真实 AI 自测调用
- 每张卡片包含问题、模拟自测、训练状态、最近得分或未自测状态、点评反馈、标杆回答解析、核心记忆要点、高频追问、难度和 tags
- 展开后默认先自测，提交自测或跳过自测后才显示解析区
- 提交自测后写入 `self_test_record`，并更新 `user_knowledge_card_mastery`
- Dashboard 仅提供入口卡片，不在首页展开大量知识内容
- 训练计划轻量展示 `KNOWLEDGE_CARD` 类型 item

内容维护规则：

- 知识卡种子内容的唯一维护源是 `scripts/knowledge_card_profiles.cjs`，不要在前端组件或后端服务里硬编码答案。
- `scripts/generate_knowledge_cards_sql.cjs` 负责从 profile 生成 `data/knowledge_cards.sql` 和 `frontend/lib/knowledgeSeed.ts`，保证 MySQL 数据和接口不可用时的前端 fallback 一致。
- 知识卡是面试训练内容产品，不是运行时 AI 生成产物；问题、答案、keyPoints、followUps 都要显式维护。
- 已停用统一扩写逻辑，不再使用 `enrichAnswer` 或类似自动追加模板话术的方式补字数。
- 问题应是直白短问法，例如“什么是封装？”“ArrayList 是怎么扩容的？”“Spring Bean 的生命周期是什么？”；避免“你会如何结合后端项目说明”“从哪些维度说明”等套壳表达。
- 答案优先信息密度：基础概念题可以较短，机制题和复杂框架题可以更长，但必须直接回答本题，围绕定义、机制、边界和常见坑。
- `keyPoints` 必须是可命中的具体知识词或判断点，`followUps` 必须是围绕本题的自然追问。
- 每次改完内容都要运行 `node scripts/generate_knowledge_cards_sql.cjs` 和 `node frontend/lib/knowledge-tree-coverage.node-test.cjs`。导入新 SQL 后，如果本地库已有 RAG 系统索引，还要通过 `RagService.rebuildSystemIndex()` 或等价维护流程刷新系统 chunk。

暂不做：

- 独立 RAG 聊天 / 检索页面
- AI 模拟追问
- 后续可接入真实 AI 自测调用
- 知识卡收藏和浏览记录
- `knowledge_weakness` 表

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
    code_mode VARCHAR(32) NOT NULL DEFAULT 'acm',
    template_code TEXT,
    solution_outline TEXT,
    hint_level1 TEXT,
    hint_level2 TEXT,
    hint_level3 TEXT,
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

### 5.3.1 knowledge_card

```sql
CREATE TABLE knowledge_card (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category VARCHAR(32) NOT NULL,
    title VARCHAR(128) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    follow_up TEXT,
    key_points TEXT,
    difficulty VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    tags VARCHAR(255),
    source_name VARCHAR(64),
    source_url VARCHAR(255),
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

说明：

- 后端持久化分类为 `JAVA/JVM/SPRING/MYSQL/REDIS/AI`，保持接口和表结构稳定。
- Java 基础、集合、并发等细分方向主要写入 `tags`；前端 `/knowledge` 会再组织为 Java 核心 / 数据库 / Spring / AI 工程知识树。
- 当前数据由 `scripts/knowledge_card_profiles.cjs` 维护，并通过 `scripts/generate_knowledge_cards_sql.cjs` 生成 `data/knowledge_cards.sql` 和 `frontend/lib/knowledgeSeed.ts`；内容参考小林 coding 和 JavaGuide 选题覆盖后重新整理。
- 内容质量测试位于 `frontend/lib/knowledge-tree-coverage.node-test.cjs`，会校验最终专题覆盖、SQL 与前端 fallback 一致性、问题文案、答案模板污染、keyPoints/followUps 质量，以及 Spring Bean 生命周期、布隆过滤器、HashMap、ArrayList、Spring 事务和 MySQL MVCC 等重点卡核心关键词。

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
    knowledge_point VARCHAR(64) NOT NULL,
    error_type VARCHAR(64) NOT NULL,
    wrong_count INT NOT NULL DEFAULT 0,
    submit_count INT NOT NULL DEFAULT 0,
    weakness_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    last_wrong_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

### 5.10 user_weakness_event

```sql
CREATE TABLE user_weakness_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    knowledge_point VARCHAR(64) NOT NULL,
    error_type VARCHAR(64) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT,
    delta_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    before_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    after_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    reason TEXT,
    created_at DATETIME NOT NULL
);
```

### 5.11 training_plan

```sql
CREATE TABLE training_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    agent_run_id BIGINT,
    title VARCHAR(128) NOT NULL,
    summary TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL
);
```

### 5.12 training_plan_item

```sql
CREATE TABLE training_plan_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    item_type VARCHAR(32) NOT NULL DEFAULT 'PROBLEM',
    knowledge_card_id BIGINT,
    day_index INT NOT NULL,
    knowledge_point VARCHAR(64) NOT NULL,
    problem_title VARCHAR(128),
    knowledge_card_title VARCHAR(128),
    reason TEXT NOT NULL,
    review_focus TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING'
);
```

`item_type = PROBLEM` 兼容旧算法题训练项；`item_type = KNOWLEDGE_CARD` 表示后端知识卡片复习项。

### 5.13 mistake_card

```sql
CREATE TABLE mistake_card (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    submission_id BIGINT NOT NULL,
    agent_run_id BIGINT NOT NULL,
    error_type VARCHAR(64) NOT NULL,
    knowledge_point VARCHAR(64) NOT NULL,
    mistake_summary TEXT NOT NULL,
    correct_idea TEXT,
    fingerprint VARCHAR(255),
    repeat_count INT NOT NULL DEFAULT 1,
    last_seen_at DATETIME,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    created_at DATETIME NOT NULL
);
```

### 5.14 rag_document / rag_chunk

```sql
CREATE TABLE rag_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NOT NULL,
    user_id BIGINT,
    problem_id BIGINT,
    title VARCHAR(255) NOT NULL,
    knowledge_point VARCHAR(128),
    error_type VARCHAR(64),
    tags VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE rag_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NOT NULL,
    user_id BIGINT,
    problem_id BIGINT,
    chunk_index INT NOT NULL,
    chunk_text TEXT NOT NULL,
    knowledge_point VARCHAR(128),
    error_type VARCHAR(64),
    tags VARCHAR(512),
    metadata_json TEXT,
    created_at DATETIME NOT NULL
);
```

`source_type` 当前支持 `PROBLEM`、`KNOWLEDGE_CARD`、`AI_DIAGNOSIS` 和 `MISTAKE_CARD`。系统题目和知识卡 `user_id` 为空；用户诊断和错题卡必须带 `user_id`，检索时只允许返回 `user_id IS NULL OR user_id = 当前用户` 的 chunk。

### 5.15 self_test_record / user_knowledge_card_mastery

```sql
CREATE TABLE self_test_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    knowledge_card_id BIGINT NOT NULL,
    question_snapshot TEXT NOT NULL,
    user_answer TEXT NOT NULL,
    score INT NOT NULL,
    feedback TEXT,
    missing_key_points TEXT,
    created_at DATETIME NOT NULL
);

CREATE TABLE user_knowledge_card_mastery (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    knowledge_card_id BIGINT NOT NULL,
    mastery_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    self_test_count INT NOT NULL DEFAULT 0,
    last_score INT,
    last_practiced_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
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
  "problemId": 1,
  "language": "java",
  "code": "class Solution { ... }"
}
```

当前 Hot100 精选 12 题统一提交非 `public` 的 `class Solution`。前端不传 `code_mode`，后端只在送入 Piston 前按题号包装测试 harness。

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
data: {"errorType":"BOUNDARY_ERROR","knowledgePoint":"HashMap","confidence":0.86,"trainingPlanTitle":"3 天专项训练：HashMap"}
```

### 6.4 Agent 分析接口

```http
POST /api/agent/analyze
```

### 6.5 后端知识训练接口

```http
GET /api/knowledge/categories
GET /api/knowledge/cards
GET /api/knowledge/cards?category=JAVA
GET /api/knowledge/cards/{id}
```

接口规则：

- 只返回 `enabled = true` 的知识卡片。
- 分类顺序固定为 `JAVA/JVM/SPRING/MYSQL/REDIS/AI`。
- 列表按 `sort_order ASC, id ASC` 排序。
- 当前列表接口会返回完整 `answer/followUp/keyPoints/sourceUrl`，支撑 `/knowledge` 页面直接展示解析；详情接口仍保留用于单卡读取。
- `/knowledge` 页面使用 MySQL 结构化数据，不作为开放 RAG 查询入口；这些知识卡会被 RAG V1 索引，供 Agent 诊断和训练计划内部使用。

`/api/agent/analyze` 用于同步执行 Agent 分析，主要作为 SSE fallback 和 API 层演示入口。失败提交返回错误诊断、训练计划标题和步骤；当前 `AgentAnalyzeVO` 还会返回面向教练报告的可选字段：`failurePhenomenon`、`rootCause`、`repairDirection`、`interviewReminder`。其中失败现象优先由后端根据 failed case、编译错误、运行错误或 errorMessage 生成；运行时堆栈会摘要化展示，完整堆栈仍留在测试结果中。AC 提交返回 `codeReview` 和步骤。逐级获取 hint 与单独的 code review REST 端点暂未暴露。

### 6.6 用户学习接口

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

Dashboard 当前通过以上接口读取真实 MySQL 学习数据，包括统计、薄弱点、弱点事件、错题、最新训练计划、错误类型分布和最近提交。前端展示层已把最新训练计划拆成“今日优先训练”和“完整训练计划”：今日任务优先选择 `PENDING` 条目，再选择 `NEEDS_REVIEW` / `RETRY`，全部完成时提示复盘最近错题。错题卡复用后端 `repeatCount` 和前端同类聚合展示“出现 N 次 / 反复出现”；AI 教练建议由最高薄弱点、今日训练项和最近错题确定性生成，不调用 AI、不新增接口。训练计划条目状态和手动重新生成也已暴露；知识训练页通过用户学习接口保存自测记录。

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
RagRetrieveTool 检索题目知识、知识卡和用户历史学习记忆（非核心，失败不阻塞）
  ↓
失败提交 -> ErrorClassifierTool 诊断错误类型和知识点
  ↓
WeaknessTrackerTool 更新弱点记忆和错题卡片（非核心，失败不阻塞）
  ↓
TrainingPlannerTool 生成训练计划
  ↓
AC 提交 -> CodeReviewTool 生成复杂度、风格和面试表达点评
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
    ├── RagRetrieveTool.java
    ├── ErrorClassifierTool.java
    ├── CodeReviewTool.java
    ├── WeaknessTrackerTool.java
    └── TrainingPlannerTool.java
```

`InterviewCoachAgent` 是核心编排器，不直接访问 Controller，也不直接拼 HTTP 请求。它接收 `AgentContext`，按状态机执行 Tool，并把每一步写入 `AgentStep`。Tool 内部可以调用现有 service，例如 `CodeExecutionTool` 调用 `JudgeService`，`RagRetrieveTool` 调用 `RagService`，`WeaknessTrackerTool` 调用 `LearningTracker`。

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
    "code": "class Solution { ... }",
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
  ],
  "ragRetrieveResult": {
    "hits": [
      {
        "sourceType": "KNOWLEDGE_CARD",
        "sourceId": 1,
        "title": "HashMap 在 JDK 1.8 中的底层结构",
        "score": 120
      }
    ]
  }
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
  "codeReview": null,
  "hintLevel1": null,
  "hintLevel2": null,
  "hintLevel3": null,
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

### MVP 已落地能力

- 已完成：题目列表和详情
- 已完成：Java 代码编辑器
- 已完成：Java 代码提交和 Piston 执行
- 已完成：测试结果解析
- 已完成：AI 错误诊断
- 已完成：AC 提交轻量代码点评分支
- 已完成：Agent Workflow 编排
- 已完成：SSE 流式返回 Agent 步骤，前端已使用 fetch + ReadableStream 接入
- 已完成：Agent Step / Trace 记录
- 已完成：题目预设分层提示展示（数据来自后端 problem 表），后端 AI hint 生成已移除
- 已完成：Dashboard 查询接口和真实数据接入
- 已完成：薄弱点统计、错题卡和 3 天训练计划从 MySQL 持久化数据读取
- 已完成：Dashboard 教练化重排，今日优先训练前置，完整训练计划降级为详细展开区，错误统计去除重复薄弱点展示，错题卡按同类错误聚合表达，AI 教练建议基于现有学习数据确定性生成
- 已完成：弱点事件、重复错题合并、训练计划条目状态、手动重新生成训练计划、自测记录和知识卡掌握度持久化
- 已完成：无数据时 Dashboard 显示空状态引导文案
- 已完成：知识训练页 V1，`/knowledge` 优先使用后端知识卡 API 和 `knowledge_card` 真实数据，接口不可用时回退本地示例数据；页面形成可折叠知识体系大纲、专题过滤、训练状态、模拟自测、点评反馈、标杆回答解析、高频追问和标记已掌握闭环
- 已完成：训练计划支持 `PROBLEM` / `KNOWLEDGE_CARD` 两类 item，知识卡片最多轻量混入 1-2 条
- 已完成：RAG V1 内部检索层，使用 MySQL 结构化检索题目、知识卡、AI 诊断和用户错题卡，并作为 `RAG_RETRIEVAL` Agent Step 进入 SSE 时间线
- 已完成：Phase 5 小范围产品增强，知识训练反馈展示缺失要点，AC 代码点评有兜底文案和 stale warning；AC 点评生成中展示实时 Agent 步骤，编辑器代码变化但未重新提交时不隐藏上次点评；本地服务 / AI / SSE 错误提示更具体
- 已完成：核心闭环回归护栏，`ProblemWorkspace` 以 SSE 为主路径、同步 analyze 仅作 fallback；`agentStreamState` 与前端 Node 测试覆盖旧流拦截、abort、Dashboard no mock 和页面 tab 边界

### 当前不纳入 MVP

- 多语言判题
- 完整 LeetCode 题库
- 本地 Docker 沙箱
- 向量数据库 / embedding 版 RAG 和复杂知识掌握度模型
- 单独 hint 查询接口
- 单独 accepted-code review REST 接口
- 知识卡收藏
- 独立 RAG 聊天 / 检索 REST 接口
- Redis 热点缓存真正接入
- 视频或语音面试
- 多 Agent 协作
- 企业级权限系统
- 复杂 UI 动画

### 近期产品增强优先级

第一优先级小范围产品增强已落地：知识训练反馈补“缺失要点”并优化低分点评口吻；AC 提交复用已有 `CodeReviewTool` / `codeReview` 分支，把复杂度、代码风格、面试表达建议展示清楚；AC 点评生成中展示实时 Agent 步骤，用户编辑代码但未重新提交时保留上次点评并标记 stale；后端、Piston、AI、SSE 任一服务不可用时，前端给出更明确的排查提示。

第二优先级稳定核心闭环已补回归护栏：问题页继续保持左侧题目预设提示、右侧测试结果和 AI 诊断 / AC 点评；SSE 通过 streamId、AbortController 和 `agentStreamState` 保护多次提交、旧流覆盖、用户中断和同步 fallback；Dashboard 统计、薄弱点、错题卡、错误分布和训练计划继续来自 MySQL，不回退 mock。

第三优先级当前不纳入 MVP 但保留设计入口：单独 hint 查询、单独 accepted-code review REST 接口、知识卡收藏、独立 RAG 聊天 / 检索 REST 接口、Redis 热点缓存真正接入。

## 9. 简历亮点写法

### 简洁版

```text
AI Interview Coach：基于 Spring Boot、Next.js 和 LLM 构建面向 Java 后端求职者的 AI 面试训练系统，实现算法题在线提交、Piston 代码执行、测试结果解析、RAG 辅助 AI 错误诊断、AC 代码点评、分层提示、后端知识卡片训练、薄弱知识点统计和统一训练计划生成。
```

### 后端重点版

```text
使用 Spring Boot 设计题目、提交、诊断、提示、训练计划、知识卡片和 RAG 检索等核心模块；基于 MyBatis-Plus 接入 MySQL 8，完成题目、提交记录、AI 诊断、薄弱点、训练计划、后端知识卡片和 RAG chunk 等数据持久化；封装 Piston 代码执行服务，支持 Java 代码编译运行、测试用例判定和错误日志解析；当前预留 Redis 配置，热点题目和题目详情缓存待后续接入。
```

### AI Agent 重点版

```text
设计基于状态机的 Agent Workflow，将代码执行、RAG 检索、错误分类、代码点评、弱点追踪、训练规划封装为独立 Tool，通过 Agent 编排器串联执行。代码执行结果作为 Observation 输入 RAG 和后续推理节点，每步记录 Agent Step，并通过 SSE 流式输出执行过程。
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
- 很适合流式展示 Agent 步骤，例如运行测试、观察失败用例、分类错误、代码点评、更新弱点。

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
- AI 诊断只针对本次提交生成教练报告，重点展示失败现象、根本原因、修改方向和面试提醒。
- 默认不直接返回完整代码。

### 10.5 如何生成训练计划？

- 根据 `user_weakness` 的错误次数、提交次数、错误率和最近错误时间计算薄弱分数。
- 根据薄弱知识点匹配相关题目。
- 生成 3 天训练计划。
- 计划 item 分为 `PROBLEM` 和 `KNOWLEDGE_CARD`，知识卡片最多混入 1-2 条，用于统一管理复习任务。
- 算法错误只推荐算法相关训练，不把 Two Sum 之类的错误强行映射到 MySQL / Redis / Spring。

### 10.6 Redis 用在哪里？

- 当前代码只预留 Redis 配置，还没有实际缓存读写逻辑。
- 后续适合接入热点题目列表和题目详情缓存。
- 用户最近训练状态和 Agent 临时上下文可以作为后续扩展方向，但不作为当前 MVP 的已落地能力描述。

### 10.7 为什么使用 MyBatis-Plus？

- 国内 Java 后端岗位更常见。
- 简单 CRUD 可以直接使用 `BaseMapper<T>`。
- 复杂查询可以通过 XML 或注解 SQL 保持可控。
- 面试时可以展开讲 Mapper、分页、索引、事务和 SQL 优化。

### 10.8 项目最大亮点是什么？

项目不是简单调用 AI API，而是形成完整 Agent Workflow 训练闭环：

```text
任务输入 -> Planner -> Tool 调用 -> Observation -> 错误归因 / 代码点评 -> 长期弱点记忆 -> 训练计划
```

这个闭环能体现项目的业务完整性、后端建模能力和 AI Agent 工程化能力。LLM 不是聊天入口，而是 Agent 工作流中的语义判断节点。

### 10.9 Agent 和普通工作流有什么区别？

普通工作流通常是固定的 service 串联；本项目的 Agent Workflow 会维护 `AgentContext`、`AgentState` 和 `AgentStep`，每一步都有明确的工具输入、Observation 输出和状态记录。

MVP 阶段不追求复杂自主规划，而是采用可解释的状态机式 Agent。这样既能体现 Tool Calling、Observation、Memory 和 Trace，也能避免黑盒 Agent 难以调试。

## 11. 推荐演示流程

完整演示复盘、截图录屏和面试 Q&A 归入最终阶段；当前阶段只保留以下流程作为参考。

1. 进入题目列表。
2. 选择 `1 两数之和` 作为主线失败诊断演示，或选择 `206 反转链表` / `121 买卖股票的最佳时机` 展示结构题和 AC 点评分支。
3. 在左侧题目区逐层展开预设 Level 1/2/3 提示，说明这些提示属于题目内容，不调用 AI。
4. 在 Monaco Editor 中写一段存在 bug 的 Java 代码。
5. 提交代码并展示测试失败结果。
6. 右侧 AI 诊断面板实时展示 Agent 执行步骤，完成后显示错误类型、关联知识点和教练报告（失败现象、根本原因、修改方向、面试提醒、推荐训练）。
7. 展示学习中心中的今日优先训练、完整训练计划、真实薄弱点统计、错误类型分布、合并错题卡和最近提交。
8. 说明 Dashboard 数据来自 `user_weakness`、`mistake_card`、`training_plan`、`training_plan_item` 和 `submission` 表。
9. 打开 `/knowledge`，展示 Java 核心 / 数据库 / Spring / AI 工程知识体系大纲，说明 Agent / RAG / LangChain 只是知识训练专题入口，不是新增 RAG 聊天或真实 AI 调用；演示先自测、再查看标杆回答解析和高频追问。
10. 解释后端如何封装 Piston 代码执行服务、Agent Tool、Observation、Memory、Dashboard 查询接口、知识卡 API 和 SSE 步骤流。

## 12. README 标题建议

```text
AI Interview Coach Agent
基于 Agent Workflow 的 Java 代码诊断与后端面试训练系统
```

一句话介绍：

```text
一个结合代码执行 Tool、错误诊断 Agent、AC 代码点评、分层提示、弱点记忆、后端知识卡片和统一训练计划的 Java 面试训练系统。
```
