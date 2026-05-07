# AI Interview Coach

## 项目概述

AI 面试教练 — 帮助用户刷算法题并获得 AI 诊断和分层提示。

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
│   └── IMPLEMENTATION_PLAN.md         # 实现计划
├── stitch_front_end_interface_design/
│   └── mvp/                           # HTML 原型
│       ├── home.html
│       ├── problem.html
│       └── dashboard.html
└── data/                              # 数据库脚本
```

## 关键约定

### 前端

- **API 调用**: 使用 `lib/api.ts` 封装，区分服务端/客户端 base URL
- **状态管理**: React useState，不用 Redux/Zustand
- **userId**: 固定为 1（MVP 无登录）
- **语言**: 固定为 java
- **Dashboard**: 使用 `lib/mock.ts` 数据（接口未暴露）
- **SSE**: P2 阶段实现，先用同步 `POST /api/agent/analyze`

### 后端

- **统一响应**: `ApiResponse<T>` 格式 `{ code: 0, message: "success", data: T }`
- **Agent 诊断**: `POST /api/agent/analyze` 返回 `AgentAnalyzeVO`（含 hintLevel1/2/3）
- **代码执行**: Piston 在线编译运行

## 常用命令

```bash
# 前端
cd frontend && npm run dev        # 启动前端开发服务器 (localhost:3000)
cd frontend && npm run build      # 构建前端

# 后端
cd backend && ./mvnw spring-boot:run  # 启动后端 (localhost:8080)
```

## 实现状态

- [x] 后端 Phase 1: 题目 + 提交 + 判题
- [x] 后端 Phase 2: Agent 诊断 + 分层提示 + 训练计划
- [ ] 前端: 首页（题目列表）
- [ ] 前端: 做题页（Monaco + 提交 + AI 诊断）
- [ ] 前端: 仪表盘（Mock 数据）

## 工作流命令

- `/implement-frontend` — 按计划逐步实现前端三页面
