# AI Interview Coach Agent

## 项目概述

AI Interview Coach Agent 是一个基于 Agent Workflow 的 Java 代码诊断与面试训练系统。核心价值不是做通用刷题平台，而是围绕一次 Java 提交完成：

```text
题目预设提示 -> 提交代码 -> Piston 判题 -> Agent Observation -> RAG 检索 -> AI 错误诊断 -> 弱点记忆 -> 训练计划
```

产品边界：

- 左侧题目区展示题目预设 Level 1/2/3 分层提示，不调用 AI。
- 右侧结果区只保留"测试结果"和"AI 诊断"。
- `AgentAnalyzeVO.hintLevel1/2/3` 后端仍返回，但当前做题页不再作为独立 tab 展示，AI 诊断也不再生成这些字段。
- RAG V1 是 Agent 内部 Tool，在 Observation 后检索题目知识、知识卡、历史诊断和错题卡作为 AI 诊断证据；检索失败不阻塞核心闭环。
- `/rag-chat` 是受控学习资料问答入口，只回答题目、知识卡、历史诊断、错题卡和当前用户学习记录相关问题，不替代代码提交诊断主流程。
- `/mock-interview` 是模拟面试入口，复用知识卡作为面试题，AI 提问、评分、追问，低分写入弱点事件。
- `/knowledge` 是知识训练页，120 张知识卡支持自测评分、掌握度跟踪。

## 技术栈

- **后端**: Spring Boot 3 + Java 17 + MyBatis-Plus + Piston（代码执行）
- **前端**: Next.js 14 App Router + Tailwind CSS + Monaco Editor
- **AI**: Anthropic-compatible Messages API（Agent 诊断、知识问答、模拟面试评分）
- **RAG**: MySQL 结构化检索（V1），不依赖向量数据库

## 项目结构

```
ai-study/
├── backend/                           # Spring Boot 后端
├── frontend/                          # Next.js 前端
├── docs/
│   ├── API.md                         # 接口文档（权威）
│   ├── AI-Interview-Coach.md          # 项目设计、简历包装、面试讲解
│   ├── IMPLEMENTATION_PLAN.md         # 实现计划
│   ├── PROJECT_STATUS.md              # 当前成果、风险、下一步大纲
│   ├── KNOWLEDGE_TRAINING_DESIGN.md   # 知识训练模块设计
│   └── DEMO_CASES.md                  # 演示用例说明
├── data/
│   ├── schema.sql                     # 全量建表（含 mock_interview、rag 等）
│   ├── knowledge_cards.sql            # 120 张知识卡种子数据
│   ├── rag_mysql_migration.sql        # RAG 表迁移（旧库用）
│   └── mock_interview_migration.sql   # 模拟面试表迁移（旧库用）
└── scripts/
    ├── knowledge_card_profiles.cjs    # 知识卡内容源
    └── generate_knowledge_cards_sql.cjs # 生成 knowledge_cards.sql 和 knowledgeSeed.ts
```

## 关键约定

### 前端

- **API 调用**: 使用 `lib/api.ts` 封装，base URL 为 `http://localhost:8080`，fetch 使用 `cache: "no-store"`
- **状态管理**: React useState，不用 Redux/Zustand
- **userId**: 固定为 1（MVP 无登录）
- **语言**: 固定为 java
- **Dashboard**: 使用真实 `/api/users/1/...` MySQL-backed 接口，不使用 mock 数据
- **做题页模板**: `ProblemWorkspace` 在浏览器端请求 `/api/problems/{id}/template`，Monaco Editor 使用返回的 `templateCode`
- **分层提示**: 从后端 `GET /api/problems/{id}` 返回的 `presetHints` 读取，由 `ProblemHintPanel` 显示在左侧
- **结果面板**: `ResultPanel` 只保留"测试结果"和"AI 诊断"，不恢复右侧"分层提示"tab
- **草稿**: 通过 `frontend/lib/draft.ts` 使用 localStorage
- **SSE**: 前端通过 `fetch + ReadableStream` 接入 SSE，实时展示 Agent 步骤；同步 `POST /api/agent/analyze` 保留作为 fallback

### 后端

- **统一响应**: `ApiResponse<T>` 格式 `{ code: 0, message: "success", data: T }`
- **Agent 诊断**: `POST /api/agent/analyze` 和 SSE `/api/submissions/{id}/diagnosis/stream`；失败提交返回错误分类和诊断，AC 提交返回轻量代码点评
- **代码执行**: `SubmissionService -> JudgeService -> PistonClient`
- **Solution 模式**: 当前 Hot100 精选 12 题统一使用 LeetCode 风格 `class Solution`；后端通过 `CodeWrapper` 注册表包装送入 Piston 的代码，数据库保存用户原始代码
- **RAG 检索**: `RagService` 基于 MySQL 结构化检索 `rag_document`/`rag_chunk`；4 种来源：PROBLEM、KNOWLEDGE_CARD、AI_DIAGNOSIS、MISTAKE_CARD；系统级数据 userId=null 全局可见，用户级数据按 userId 隔离
- **模拟面试**: `MockInterviewService` 通过状态机管理会话（CREATED → ASKING_MAIN → ... → REPORTED），复用知识卡作为面试题
- **知识训练**: `KnowledgeCardService` 管理 120 张知识卡，`KnowledgeLearningService` 处理自测记录和掌握度

### RAG

- V1 使用 MySQL 结构化检索，不依赖向量数据库或 embedding
- 数据来源：`problem`、`knowledge_card`、`ai_diagnosis`、`mistake_card`
- `RagRetrieveTool` 是 Agent 内部 Tool，在 OBSERVATION 后执行
- `/api/rag/chat` 是受控学习资料问答入口，二次重排后调用 AI 生成回答
- 用户记忆 chunk 按 `user_id` 隔离，不泄露给其他用户
- 检索失败只记录 failed step，不阻塞后续诊断或 AC 点评

## 常用命令

```bash
# 前端
cd frontend && npm run dev        # 启动前端开发服务器 (localhost:3000)
cd frontend && npm run build      # 构建前端

# 后端
cd backend && mvn spring-boot:run     # 启动后端 (localhost:8080)
cd backend && mvn test                # 后端测试

# 前端轻量测试
cd frontend && node lib/draft.node-test.cjs
cd frontend && node lib/problem-hints-ui.node-test.cjs
cd frontend && node lib/template-loading.node-test.cjs
cd frontend && node lib/knowledge-tree-coverage.node-test.cjs
cd frontend && node lib/core-loop-stability.node-test.cjs
cd frontend && node lib/rag-chat.node-test.cjs
cd frontend && node lib/mock-interview.node-test.cjs
```

## 实现状态

- [x] 后端 Phase 1: 题目 + 提交 + 判题
- [x] 后端 Phase 2: Agent 诊断 + 分层提示 + 训练计划
- [x] 前端: 首页（题目列表）
- [x] 前端: 做题页（Monaco + 提交 + 题目预设提示 + AI 诊断 + SSE 实时步骤）
- [x] 前端: Dashboard（真实 MySQL 学习数据）
- [x] 后端: Dashboard 查询接口 `UserController`
- [x] 前端: SSE 流式展示 Agent 执行步骤 + fallback 同步分析
- [x] 后端: 题目预设提示迁移到 problem 表 + ProblemDetailVO.presetHints
- [x] 前端: ProblemNavigator 上一题/下一题切换
- [x] 后端: AC 代码点评（CodeReviewTool）
- [x] 后端: RAG V1 内部检索层（RagService + RagRetrieveTool）
- [x] 后端: 知识库问答 V1（RagChatController + RagChatService）
- [x] 前端: 知识库问答页 `/rag-chat`
- [x] 后端: 模拟面试 V1（MockInterviewController + MockInterviewService）
- [x] 前端: 模拟面试页 `/mock-interview`
- [x] 后端: 知识训练一期（KnowledgeController + KnowledgeCardService + 120 张知识卡）
- [x] 前端: 知识训练页 `/knowledge`（知识树、自测评分、掌握度跟踪）
- [x] 后端: 训练计划接入知识卡片（PROBLEM + KNOWLEDGE_CARD 两类条目）
- [x] 后端: 学习记忆连续化（弱点事件 + 错题卡合并 + 自测记录 + 知识卡掌握度）
- [x] 后端: 知识卡内容质量整改（120 张卡片从模板问答整改为真实面试训练卡）

## 当前题目提交模式

当前题库为 Hot100 精选 12 题，统一使用 LeetCode 风格 `class Solution` 提交。后端根据 `problem.code_mode='solution'` 和 `CodeWrapper` 注册表，在送入 Piston 前自动包装为 `Main.java`；数据库仍保存用户原始 `class Solution` 代码。

## 当前已实现 Controller

- `ProblemController` — 题目列表、详情、模板
- `SubmissionController` — 代码提交、判题
- `AgentController` — 同步 Agent 分析、SSE 流式诊断
- `UserController` — Dashboard 统计、薄弱点、错题卡、训练计划、最近提交
- `KnowledgeController` — 知识分类、知识卡列表/详情、自测记录
- `RagChatController` — 受控知识库问答
- `MockInterviewController` — 模拟面试会话管理

## 工作流命令

- 主要接口以 `docs/API.md` 为准。
- 项目约束以 `AGENTS.md` 为准。
- 当前成果和下一步大纲以 `docs/PROJECT_STATUS.md` 为准。
- 项目设计和简历包装以 `docs/AI-Interview-Coach.md` 为准。
