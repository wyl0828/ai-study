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

当前 `/knowledge` 页面已锁定为前端 V1，目标是先形成“先答后看解析”的训练体验。

已实现能力：

- 顶部导航“知识训练”入口指向 `/knowledge`。
- 页面优先调用后端知识接口读取 `knowledge_card` 真实数据；后端不可用时回退 3 条本地示例数据。
- 支持搜索、难度筛选和分类筛选。
- 分类固定为 Java / MySQL / Redis / Spring / JVM。
- 首批真实数据 15 条：Java 4、JVM 2、Spring 3、MySQL 3、Redis 3。
- 卡片未展开时展示难度、分类、tags、标题、问题描述和“查看解析，或在此之前完成模拟自测”。
- 展开后默认只展示题目信息、模拟自测输入框、提交自测按钮和“跳过自测，直接查看解析”。
- 提交自测后展示点评反馈、评分、命中核心记忆点、标杆回答解析、核心记忆要点、面试官高频追问和“标记已掌握”。
- 点击“跳过自测，直接查看解析”时只显示解析区，不展示虚假的模拟评分。
- “标记已掌握”只保存在 React state 中，顶部已掌握数量同步更新。

本次明确不做：

- 不新增数据库表。
- 不新增 REST 接口。
- 不把自测结果写入 MySQL 或 localStorage。
- 不做真实 AI 调用。
- 不做 RAG。
- 不做知识弱点画像或 `knowledge_weakness` 表。

## 3. 前端结构

当前 V1 相关文件：

```text
frontend/app/knowledge/page.tsx
frontend/components/KnowledgeTrainingPage.tsx
frontend/components/KnowledgeCard.tsx
frontend/components/KnowledgeSelfTest.tsx
frontend/components/KnowledgeFeedback.tsx
frontend/lib/knowledgeData.ts
```

职责划分：

| 文件 | 职责 |
| --- | --- |
| `page.tsx` | `/knowledge` 路由入口，渲染知识训练页面 |
| `KnowledgeTrainingPage.tsx` | 页面级状态：接口加载、mock fallback、搜索、筛选、展开卡片、已掌握统计 |
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
- 自测记录和掌握状态暂不持久化；如需落库，应新增清晰的数据表和 REST 接口。
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
- 不影响 `/`、`/problem/[id]`、`/dashboard`。
- 不新增后端依赖，不新增表或字段。
- `npm run build` 通过。

## 8. 后续增强

V1 定稿后可以小步增强：

- 点评反馈区补充“缺失要点”。
- 低分点评文案更接近面试官反馈。
- 暂不增加知识卡掌握状态持久化。
- 暂不增加知识自测记录和复习历史。

当前近期目标只做前端体验增强，不改变后端接口、数据库 schema 或训练计划持久化策略。
