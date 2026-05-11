# AI Interview Coach Agent

基于 Agent Workflow 的 Java 代码诊断与面试训练系统。

## 项目定位

不是通用刷题平台，不是简单 AI 聊天助手，不是 Spring Boot 包一层大模型 API。

核心价值：围绕一次 Java 代码提交，完成完整的 Agent 驱动训练闭环：

```text
题目预设提示 → 提交代码 → Piston 判题 → Agent Observation
→ AI 错误诊断 → 弱点记忆 → 错题卡 → 训练计划
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Next.js 14 + Tailwind CSS + Monaco Editor |
| 后端 | Spring Boot 3 + Java 17 |
| 持久层 | MySQL 8 + MyBatis-Plus |
| 缓存 | Redis |
| 代码执行 | Piston API（可替换为 Docker 沙箱） |
| AI | Anthropic-compatible Messages API |
| 流式通信 | Server-Sent Events |

## 项目结构

```text
ai-study/
├── backend/                           # Spring Boot 后端
├── frontend/                          # Next.js 前端
├── data/                              # 数据库脚本
│   ├── schema.sql                     # 建表语句
│   └── problems.sql                   # 题目数据
├── docs/                              # 项目文档
│   ├── API.md                         # 接口文档
│   ├── IMPLEMENTATION_PLAN.md         # 实现计划
│   ├── PROJECT_STATUS.md              # 当前成果与下一步
│   ├── DEMO_CASES.md                  # 演示用例
│   └── demo-cases/                    # 演示 bug/fixed 代码
├── .env                               # 环境变量（不提交）
├── start-backend.bat                  # Windows 启动脚本
└── start-backend.ps1                  # PowerShell 启动脚本
```

## 快速启动

### 前置依赖

- Java 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8
- Redis
- Piston（代码执行服务）

### 1. 数据库初始化

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE ai_interview_coach CHARACTER SET utf8mb4;"

# 导入表结构和数据
mysql -u root -p ai_interview_coach < data/schema.sql
mysql -u root -p ai_interview_coach < data/problems.sql
```

### 2. 配置环境变量

复制 `.env.example` 为 `.env`，或直接创建 `.env`：

```bash
# MySQL
MYSQL_URL=jdbc:mysql://localhost:3306/ai_interview_coach?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
MYSQL_USERNAME=root
MYSQL_PASSWORD=your_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Piston（代码执行）
PISTON_BASE_URL=http://localhost:2000/api/v2

# AI（Anthropic-compatible）
AI_BASE_URL=https://api.anthropic.com
AI_API_KEY=your_api_key
AI_MODEL=claude-3-5-sonnet-latest
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:3000`。

## 核心功能

### 1. 题目与提交

- 内置 8 道 Java 算法题（数组、哈希表、链表、二叉树、动态规划）
- 支持 ACM 模式（完整 `public class Main`）和 Solution 模式（`class Solution`）
- Monaco Editor 代码编辑器
- Piston 判题，返回编译错误、运行错误、失败用例

### 2. Agent Workflow 诊断

提交失败后触发 Agent 工作流：

| 步骤 | 工具 | 说明 |
|------|------|------|
| PLANNING | - | 准备 Agent 上下文 |
| CODE_EXECUTION | CodeExecutionTool | 重新执行代码 |
| OBSERVATION | - | 观察判题结果 |
| ERROR_CLASSIFICATION | ErrorClassifierTool | AI 分类错误类型 |
| MEMORY_UPDATE | WeaknessTrackerTool | 更新弱点记忆和错题卡（非核心，失败不阻塞） |
| TRAINING_PLAN | TrainingPlannerTool | 生成 3 天训练计划（非核心，失败不阻塞） |

### 3. 学习数据持久化

- **弱点记忆**：按知识点统计错误次数和薄弱分数
- **错题卡片**：记录每次错误的原因和正确思路
- **训练计划**：根据弱点生成 3 天针对性训练
- **Dashboard**：展示统计、薄弱点、错题卡、最近提交和训练计划

### 4. 分层提示机制

- **题目预设提示**：存储在后端 `problem` 表，通过 API 返回，Level 1/2/3 展示在左侧题目区，不调用 AI
- **AI 诊断**：针对本次提交解释错误原因，展示在右侧结果区，通过 SSE 实时展示 Agent 步骤

## 演示流程

推荐演示顺序：

1. **101 两数之和**（ACM 模式）：HashMap 查询/写入顺序 bug
2. **104 合并两个有序链表**（Solution 模式）：忘记连接剩余链表
3. **103 反转链表**（Solution 模式）：返回原始 head

演示步骤：

```text
复制 bug 代码 → 提交失败 → 观察 failedCases
→ 展示 AI 诊断 → 展示 Dashboard 更新
→ 复制 fixed 代码 → 重新提交通过
```

详细演示用例见 `docs/DEMO_CASES.md`。

## 后端架构

```text
com.interview.coach
├── controller          # 接收 HTTP 请求，返回 VO
├── service             # 业务接口
│   └── impl            # 业务实现
├── agent               # Agent 编排器、状态、上下文
│   └── tool            # 代码执行、错误分类、提示生成等 Tool
├── integration
│   ├── piston          # Piston 代码执行客户端
│   └── ai              # Anthropic-compatible AI 客户端
├── entity              # MySQL 表映射
├── mapper              # MyBatis-Plus Mapper
├── dto                 # 请求参数
├── vo                  # 响应对象
├── enums               # 枚举定义
├── config              # 配置类
└── handler             # 全局异常处理
```

## 接口总览

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/problems` | 获取题目列表 |
| GET | `/api/problems/{id}` | 获取题目详情 |
| GET | `/api/problems/{id}/template` | 获取代码模板 |
| POST | `/api/submissions` | 提交代码并判题 |
| POST | `/api/agent/analyze` | 同步 Agent 诊断 |
| GET | `/api/submissions/{id}/diagnosis/stream` | SSE 流式诊断 |
| GET | `/api/users/{id}/dashboard/stats` | Dashboard 统计 |
| GET | `/api/users/{id}/weaknesses` | 薄弱点排行 |
| GET | `/api/users/{id}/mistakes` | 错题卡片 |
| GET | `/api/users/{id}/training-plans/latest` | 最新训练计划 |
| GET | `/api/users/{id}/submissions/recent` | 最近提交记录 |

完整接口文档见 `docs/API.md`。

## 简历亮点

### 简洁版

```text
AI Interview Coach：基于 Spring Boot、Next.js 和 LLM 构建面向 Java 后端求职者的 AI 面试训练系统，
实现算法题在线提交、Piston 代码执行、测试结果解析、AI 错误诊断、分层提示、薄弱知识点统计和个性化训练计划生成。
```

### Agent 重点版

```text
设计基于状态机的 Agent Workflow，将代码执行、错误分类、提示生成、弱点追踪、训练规划封装为独立 Tool，
通过 Agent 编排器串联执行。代码执行结果作为 Observation 输入后续推理节点，每步记录 Agent Step，
并通过 SSE 流式输出执行过程。
```

## 文档索引

| 文档 | 用途 |
|------|------|
| [AI-Interview-Coach.md](docs/AI-Interview-Coach.md) | 项目设计、数据库表、API 设计、简历包装 |
| [API.md](docs/API.md) | 当前后端接口规范 |
| [IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md) | 实现计划与阶段划分 |
| [PROJECT_STATUS.md](docs/PROJECT_STATUS.md) | 当前成果、风险、下一步大纲 |
| [DEMO_CASES.md](docs/DEMO_CASES.md) | 演示用例与 bug 样例 |

## License

MIT
