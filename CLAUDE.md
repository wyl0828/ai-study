# AI Interview Coach Agent

## 项目概述

AI Interview Coach Agent 是一个基于 Agent Workflow 的 Java 代码诊断与面试训练系统。核心价值不是做通用刷题平台，而是围绕一次 Java 提交完成：

```text
题目预设提示 -> 提交代码 -> Piston 判题 -> Agent Observation -> AI 错误诊断 -> 弱点记忆 -> 训练计划
```

当前产品边界：

- 左侧题目区展示题目预设 Level 1/2/3 分层提示，不调用 AI。
- 右侧结果区只保留“测试结果”和“AI 诊断”。
- `AgentAnalyzeVO.hintLevel1/2/3` 后端仍返回并持久化，但当前做题页不再作为独立 tab 展示。

## 技术栈

- **后端**: Spring Boot 3 + Java 17 + MyBatis-Plus + Piston（代码执行）
- **前端**: Next.js 14 App Router + Tailwind CSS + Monaco Editor
- **AI**: Anthropic-compatible Messages API（Agent 诊断）

## 项目结构

```
ai-study/
├── backend/                           # Spring Boot 后端
├── frontend/                          # Next.js 前端
├── docs/
│   ├── API.md                         # 接口文档（权威）
│   ├── IMPLEMENTATION_PLAN.md         # 实现计划
│   └── PROJECT_STATUS.md              # 当前成果、风险、下一步大纲
├── stitch_front_end_interface_design/
│   └── mvp/                           # HTML 原型
│       ├── home.html
│       ├── problem.html
│       └── dashboard.html
└── data/                              # 数据库脚本
```

## 关键约定

### 前端

- **API 调用**: 使用 `lib/api.ts` 封装，当前 base URL 为 `http://localhost:8080`，fetch 使用 `cache: "no-store"`
- **状态管理**: React useState，不用 Redux/Zustand
- **userId**: 固定为 1（MVP 无登录）
- **语言**: 固定为 java
- **Dashboard**: 已使用真实 `/api/users/1/...` MySQL-backed 接口，不再使用 mock 数据
- **做题页模板**: `/problem/[id]` 由客户端 `ProblemWorkspace` 请求 `/api/problems/{id}/template`，Monaco Editor 使用返回的 `templateCode`
- **分层提示**: 题目预设提示放在 `frontend/lib/problemHints.ts`，由 `ProblemHintPanel` 显示在左侧，Level 1/2/3 默认收起
- **结果面板**: `ResultPanel` 只保留“测试结果”和“AI 诊断”，不要恢复右侧“分层提示”tab
- **草稿**: 通过 `frontend/lib/draft.ts` 使用 localStorage，仅保存临时代码、最近提交结果和最近诊断快照
- **SSE**: 后端已实现 SSE；当前主 demo 前端仍使用同步 `POST /api/agent/analyze`

### 后端

- **统一响应**: `ApiResponse<T>` 格式 `{ code: 0, message: "success", data: T }`
- **Agent 诊断**: `POST /api/agent/analyze` 返回 `AgentAnalyzeVO`（含 hintLevel1/2/3，但当前前端只展示诊断、改进建议、训练计划和步骤）
- **代码执行**: `SubmissionService -> JudgeService -> PistonClient`
- **Solution 模式**: `problemId=102/103/104` 提交非 `public` 的 `class Solution`；`SubmissionServiceImpl` 保存原始代码，只在送入 Piston 前通过 `CodeWrapper` 包装为 `Main.java`
- **ACM 模式**: `problemId=101/105/106/107/108` 仍提交完整 `public class Main`
- **不新增模式字段**: 当前不加 `code_mode`，不改 REST 请求结构

## 常用命令

```bash
# 前端
cd frontend && npm run dev        # 启动前端开发服务器 (localhost:3000)
cd frontend && npm run build      # 构建前端

# 后端
cd backend && mvn spring-boot:run     # 启动后端 (localhost:8080)
cd backend && mvn test                # 后端测试

# 前端轻量测试
cd frontend && node lib\draft.node-test.cjs
cd frontend && node lib\problem-hints-ui.node-test.cjs
cd frontend && node lib\template-loading.node-test.cjs
```

## 实现状态

- [x] 后端 Phase 1: 题目 + 提交 + 判题
- [x] 后端 Phase 2: Agent 诊断 + 分层提示 + 训练计划
- [x] 前端: 首页（题目列表）
- [x] 前端: 做题页（Monaco + 提交 + 题目预设提示 + AI 诊断）
- [x] 前端: Dashboard（真实 MySQL 学习数据）
- [x] 后端: Dashboard 查询接口 `UserController`
- [x] 后端: `102/103/104` Solution 提交模式试点
- [x] 前端: `102/103/104` 统一动态模板加载
- [x] 前端: 提示/诊断去重，右侧移除“分层提示”tab

## 下一步重点

优先参考 `docs/PROJECT_STATUS.md`：

1. 固定 3 个演示题和 bug 样例。
2. 编写根目录 README 和本地启动说明。
3. 跑通完整 demo 并准备截图。
4. 接入前端 SSE 展示 Agent 步骤。
5. 稳定后再把题目预设提示迁移到后端数据。

## 当前题目提交模式

| problemId | 题目 | 模式 | 用户提交 |
| --- | --- | --- | --- |
| 101 | 两数之和 | ACM | 完整 `public class Main` |
| 102 | 有效字母异位词 | Solution | `class Solution { public boolean isAnagram(String s, String t) { ... } }` |
| 103 | 反转链表 | Solution | `class Solution { public ListNode reverseList(ListNode head) { ... } }` |
| 104 | 合并两个有序链表 | Solution | `class Solution { public ListNode mergeTwoLists(ListNode list1, ListNode list2) { ... } }` |
| 105-108 | 树 / DP 题 | ACM | 完整 `public class Main` |

## 工作流命令

- 主要接口以 `docs/API.md` 为准。
- 项目约束以 `AGENTS.md` 为准。
- 当前成果和下一步大纲以 `docs/PROJECT_STATUS.md` 为准。
