# 知识训练模块设计文档

## 1. 模块定位

知识训练是 AI Interview Coach Agent 的独立训练入口，用于补齐 Java 后端面试中的表达型知识。

它不替代代码诊断，也不把算法错误强行映射成 MySQL、Redis、Spring 八股推荐。正确边界是：

```text
算法训练模块
  -> 代码提交
  -> Agent 诊断
  -> 算法弱点
  -> 算法题训练计划

知识训练模块
  -> Java 后端知识点自测
  -> 点评反馈模拟
  -> 标杆回答解析
  -> 高频追问复习

统一训练计划
  -> 同时展示算法题训练任务和知识卡片复习任务
```

## 2. 当前 V1 范围

当前 `/knowledge` 页面已锁定为前端 V1，目标是形成“先选专题、再自测、后看解析”的训练体验。

已实现能力：

- 顶部导航“知识训练”入口指向 `/knowledge`。
- 页面优先调用后端知识接口读取 `knowledge_card` 真实数据；后端不可用时回退本地示例数据。
- 支持搜索、难度筛选、训练状态筛选和专题过滤。
- 左侧知识体系大纲支持多级展开/收起，当前前端组织为 Java 核心、数据库、Spring、AI 工程。
- Java 核心下的集合框架按 List / Map / Set 展示，当前专题只展示匹配卡片；例如 Map 不展示 ArrayList / LinkedList。
- AI 工程下提供 Agent、RAG、LangChain 三个专题入口，并提供少量本地示例卡作为接口不可用时的兜底训练内容；不新增后端接口、不接独立 RAG 聊天、不调用真实 AI。
- 面包屑第一层为“知识训练”，左侧选中项、面包屑、专题标题和描述由同一个选择状态派生。
- 后端真实分类来自 `JAVA/JVM/SPRING/MYSQL/REDIS/AI`，前端知识树只是展示组织方式。
- 当前真实数据 120 条：侧边栏 24 个最终专题每个至少 5 张，覆盖 Java 基础、集合、JUC、JVM、MySQL、Redis、Spring 和 AI 工程。
- 卡片未展开时展示难度、分类、tags、标题、问题描述、训练状态、最近得分或“未自测”和主要动作按钮。
- 展开后默认只展示题目信息、模拟自测输入框、提交自测按钮和“跳过自测，直接查看解析”。
- 提交自测后展示点评反馈、评分、命中核心记忆点、标杆回答解析、核心记忆要点、面试官高频追问和“标记已掌握”。
- 点击“跳过自测，直接查看解析”时只显示解析区，不展示虚假的模拟评分。
- “标记已掌握”当前保存在 React state 中；自测提交会写入后端自测记录并可更新知识卡掌握度。

本次明确不做：

- 不新增数据库表。
- 不新增 REST 接口。
- 不把自测结果写入 localStorage；自测记录通过现有用户学习接口写入 MySQL。
- 不做真实 AI 调用。
- 不做独立 RAG 聊天或公开检索入口；RAG 只作为 Agent 内部 Tool，与 `/knowledge` 页面保持边界。
- 不做知识弱点画像或 `knowledge_weakness` 表。

## 3. 前端结构

当前 V1 相关文件：

```text
frontend/app/knowledge/page.tsx
frontend/components/KnowledgeTrainingPage.tsx
frontend/components/KnowledgeSidebar.tsx
frontend/components/KnowledgeTopicHeader.tsx
frontend/components/KnowledgeFilterBar.tsx
frontend/components/KnowledgeCard.tsx
frontend/components/KnowledgeSelfTest.tsx
frontend/components/KnowledgeFeedback.tsx
frontend/lib/knowledgeData.ts
```

职责划分：

| 文件 | 职责 |
| --- | --- |
| `page.tsx` | `/knowledge` 路由入口，渲染知识训练页面 |
| `KnowledgeTrainingPage.tsx` | 页面级状态：接口加载、fallback、统一专题选择、搜索、筛选、展开卡片、已掌握统计 |
| `KnowledgeSidebar.tsx` | 可折叠知识体系大纲，负责左侧选中和展开/收起交互 |
| `KnowledgeTopicHeader.tsx` | 面包屑、当前专题标题和描述 |
| `KnowledgeFilterBar.tsx` | 紧凑筛选条：难度、训练状态、数量和搜索 |
| `KnowledgeCard.tsx` | 单张知识卡片、展开状态、解析区显示时机 |
| `KnowledgeSelfTest.tsx` | 自测输入、空答案校验、提交自测、跳过自测 |
| `KnowledgeFeedback.tsx` | 自测评分、点评、命中记忆点展示 |
| `knowledgeData.ts` | 本地示例数据、接口数据映射、类型定义、本地评分函数 |

## 4. 数据结构

每个知识点包含：

```ts
interface KnowledgeTopic {
  id: number;
  title: string;
  category: "Java" | "MySQL" | "Redis" | "Spring" | "JVM";
  difficulty: "简单" | "中等" | "困难";
  tags: string[];
  question: string;
  answerKeywords: string[][];
  referenceAnswer: string;
  keyPoints: string[];
  followUpQuestions: string[];
  mastered: boolean;
}
```

`answerKeywords` 用于本地模拟自测评分。它不代表最终 AI Prompt，也不落库。

## 5. 自测交互

当前页面的核心体验是“先自测，再看答案”：

```text
展开卡片
-> 输入自己的回答
-> 提交自测
-> 本地计算评分
-> 展示点评反馈和命中记忆点
-> 展示标杆回答解析
-> 展示核心记忆要点和高频追问
-> 标记已掌握
```

空回答提交时提示：

```text
请先输入你的回答
```

评分为本地模拟：

- 低分示例：45/100，回答过于简略，没有覆盖核心机制。
- 中分示例：70/100，覆盖了部分要点，但缺少触发条件或优化目的。
- 高分示例：85/100，回答较完整，基本覆盖面试核心点。

## 6. 与后端知识接口的关系

仓库中已有后端知识训练设计和接口：

```http
GET /api/knowledge/categories
GET /api/knowledge/cards
GET /api/knowledge/cards?category=JAVA
GET /api/knowledge/cards/{id}
```

当前 `/knowledge` 前端 V1 已按以下策略接入：

- 页面加载时并发请求分类和卡片列表。
- 展开卡片时按需请求详情，补齐标杆回答、核心记忆点和高频追问。
- 接口失败或后端未启动时回退本地示例数据，并显示浅提示。
- 自测评分继续在前端本地模拟，不调用真实 AI。
- 掌握状态继续只保存在 React state，不写入后端。

后续继续迭代时应保持以下边界：

- 后端接口只提供结构化知识卡片，不做 RAG。
- 当前优先增强前端反馈质量，例如补充“缺失要点”和更像面试官的低分点评。
- 自测记录和知识卡掌握度已通过 `self_test_record`、`user_knowledge_card_mastery` 和用户学习 REST 接口持久化；低分自测会写入 `user_weakness_event`。
- 算法 Agent 诊断仍不根据算法错因强行推荐后端八股知识。

## 7. 验收标准

当前 V1 验收标准：

- `/knowledge` 页面可以打开。
- 搜索、难度筛选、分类筛选有效。
- 点击知识点可以展开和收起。
- 展开后默认不直接展示答案区。
- 点击“提交自测”后出现评分、点评反馈、命中记忆点和解析区。
- 点击“跳过自测，直接查看解析”后显示解析区，但不显示模拟评分。
- 点击“标记已掌握”后卡片显示“已掌握”，顶部计数增加。
- 提交自测后后端保存 `self_test_record`，刷新后可读取最近自测记录。
- 不影响 `/`、`/problem/[id]`、`/dashboard`。
- 不新增后端依赖；新增的学习记忆表通过 `data/learning_memory_continuity_migration.sql` 管理。
- `npm run build` 通过。

## 8. 后续增强

V1 定稿后可以小步增强：

- 点评反馈区补充“缺失要点”。
- 低分点评文案更接近面试官反馈。
- 知识卡掌握状态和自测记录已持久化；后续可继续做收藏、复习历史聚合和 RAG 检索。

当前近期目标仍保持知识训练为独立入口，不让算法诊断强行推荐八股知识。
