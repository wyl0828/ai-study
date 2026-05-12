# 后端知识训练功能设计文档

## 1. 功能定位

本功能用于把项目从“Java 算法题代码诊断系统”扩展为更完整的 **Java 后端面试训练系统**。

新增模块聚焦 Java 后端常见八股知识训练，例如 Java 集合、并发、JVM、Spring、MySQL、Redis。它与现有算法题诊断模块保持独立，不把算法错误强行映射到 MySQL、Redis、Spring 等后端知识点。

正确关系是：

```text
算法训练模块
  -> 代码提交
  -> Agent 诊断
  -> 算法弱点
  -> 算法题训练计划

后端知识训练模块
  -> 知识卡片浏览
  -> 后续可扩展自测 / 追问 / 掌握度
  -> 后端知识复习任务

统一训练计划
  -> 同时展示算法题训练任务和后端知识卡片复习任务
```

第一版目标是做出稳定、可展示、可解释的知识卡片模块，并轻量接入 Dashboard 和训练计划。

## 2. 设计原则

- 不做泛化教育平台，仍服务 Java 后端面试训练。
- 不把八股文塞进算法题页面，避免干扰当前做题和 Agent 诊断闭环。
- 不做第一版 RAG，先使用结构化 MySQL 知识卡片库。
- 不直接复制外部网站内容，只参考主题和分类，重新整理成项目自己的卡片内容。
- 保留 `source_name` 和 `source_url`，方便说明内容参考来源。
- 训练计划可以同时包含算法题和知识卡，但不表达二者之间的错误因果关系。
- 前端保持简单实用，新增一级页面 `/knowledge`。

## 3. 功能范围

### 3.1 本期实现

```text
1. 新增 /knowledge 知识训练页面
2. 新增 Java / MySQL / Redis / Spring / JVM 分类筛选
3. 新增知识卡片列表和详情展示
4. 新增 Dashboard 中“后端知识训练”入口
5. 扩展训练计划 item，支持 PROBLEM 和 KNOWLEDGE_CARD 两类任务
6. 准备第一批知识卡片种子数据
7. 更新 API 和 README 相关说明
```

### 3.2 本期不实现

```text
1. RAG 知识库
2. AI 自由问答
3. AI 模拟面试官追问
4. 知识掌握度评分
5. knowledge_weakness 表
6. 算法错误自动推荐 MySQL / Redis / Spring
7. 后台管理系统
```

这些能力可以作为二期或三期扩展。

## 4. 页面 UI 设计

### 4.1 导航结构

当前导航：

```text
题目
仪表盘
```

建议改为：

```text
题目
知识训练
学习中心
```

对应路由：

```text
/            算法题列表
/knowledge   后端知识训练
/dashboard   学习中心
```

不建议在 `/problem/[id]` 中新增“八股文”tab。算法做题页已经承担题目描述、代码编辑、测试结果、AI 诊断，如果再加入 Java/MySQL 八股内容，会破坏当前清晰的三栏结构。

### 4.2 /knowledge 页面布局

桌面端建议使用两栏布局：

```text
顶部：
后端知识训练标题
分类筛选 tab：全部 / Java / MySQL / Redis / Spring / JVM

左侧主区域：
知识卡片列表

右侧详情区域：
当前选中卡片的完整内容
```

移动端建议退化为单列：

```text
分类筛选
卡片列表
点击卡片后在当前页面展开详情
```

### 4.3 知识卡片列表信息

每张列表卡片展示：

```text
标题：HashMap 底层结构
分类：Java 集合
难度：MEDIUM
标签：哈希冲突 / 扩容 / 红黑树
问题预览：HashMap 在 JDK 1.8 中的底层结构是什么？
操作：查看详情
```

卡片应保持信息密度适中，避免做成营销式大卡片。它更像面试复习工具，不是内容官网首页。

### 4.4 知识卡详情信息

详情区展示：

```text
问题
标准回答
面试追问
记忆要点
参考来源
```

详情展示建议使用清晰分段：

```text
问题
HashMap 在 JDK 1.8 中的底层结构是什么？

回答
HashMap 底层主要由数组、链表和红黑树组成...

面试追问
为什么链表长度超过阈值后要转红黑树？

记忆要点
- 数组负责定位桶
- 链表处理哈希冲突
- 红黑树降低冲突严重时的查询复杂度
- 扩容会重新分布元素
```

### 4.5 Dashboard 入口

在 `/dashboard` 右侧或统计区附近新增入口模块：

```text
后端知识训练
已收录 Java / MySQL / Redis / Spring / JVM 高频知识卡
开始复习
```

这个入口只负责跳转 `/knowledge`，不在 Dashboard 内展开大量内容。

### 4.6 训练计划展示

训练计划 item 支持两类：

```text
PROBLEM：算法题训练
KNOWLEDGE_CARD：知识卡片复习
```

展示示例：

```text
第 1 天
- 算法题：两数之和
- 算法题：反转链表
- 知识卡片：HashMap 底层结构
- 知识卡片：MySQL 索引为什么能加速查询
```

文案上避免暗示“算法题错了所以推荐 MySQL”。更准确的表达是“今天的训练任务同时包含算法题和后端知识复习”。

## 5. 内容来源策略

### 5.1 参考来源

第一版卡片主题可以参考公开 Java 后端面试资料，例如：

- 小林 coding 面试题目录：https://xiaolincoding.com/interview/

使用方式：

```text
参考分类和选题
-> 重新组织问题
-> 原创整理标准回答
-> 增加追问和记忆点
-> 保存 source_name 和 source_url
```

不直接复制外部网站长文内容，不整站抓取，不把外部文章原文作为项目数据入库。

### 5.2 第一批分类

建议第一批只覆盖 Java 后端岗位最常见方向：

```text
Java 基础
Java 集合
Java 并发
JVM
Spring
MySQL
Redis
```

如果为了页面 tab 更简洁，可以在 UI 上合并为：

```text
Java
JVM
Spring
MySQL
Redis
```

其中 Java 分类内部用 tags 区分：

```text
基础 / 集合 / 并发
```

### 5.3 第一批数量

建议第一版 35 到 50 张即可：

```text
Java 基础：5 张
Java 集合：6 张
Java 并发：6 张
JVM：5 张
Spring：5 张
MySQL：5 张
Redis：5 张
```

数量足够展示产品价值，不要一开始追求大而全。

## 6. 数据库设计

### 6.1 新增 knowledge_card 表

```sql
CREATE TABLE knowledge_card (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    follow_up TEXT,
    key_points TEXT,
    difficulty VARCHAR(32) NOT NULL,
    tags VARCHAR(255),
    source_name VARCHAR(128),
    source_url VARCHAR(512),
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_knowledge_card_category (category, enabled, sort_order),
    INDEX idx_knowledge_card_enabled (enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

字段说明：

| 字段 | 说明 |
| --- | --- |
| `category` | 一级分类，例如 JAVA、MYSQL、REDIS、SPRING、JVM |
| `title` | 卡片标题，例如 HashMap 底层结构 |
| `question` | 面试题问题 |
| `answer` | 项目内原创整理答案 |
| `follow_up` | 面试官可能追问 |
| `key_points` | 记忆要点，可用换行或 JSON 字符串 |
| `difficulty` | EASY / MEDIUM / HARD |
| `tags` | 标签，例如 集合,哈希表,扩容 |
| `source_name` | 来源名称，例如 小林 coding |
| `source_url` | 参考链接 |
| `enabled` | 是否启用 |
| `sort_order` | 页面排序 |

### 6.2 扩展 training_plan_item 表

当前 `training_plan_item` 已有：

```text
id
plan_id
day_index
knowledge_point
problem_title
reason
review_focus
status
```

建议新增：

```sql
ALTER TABLE training_plan_item
    ADD COLUMN item_type VARCHAR(32) NOT NULL DEFAULT 'PROBLEM' AFTER plan_id,
    ADD COLUMN knowledge_card_id BIGINT NULL AFTER item_type,
    ADD COLUMN knowledge_card_title VARCHAR(128) NULL AFTER problem_title,
    ADD INDEX idx_training_plan_item_type (plan_id, item_type);
```

兼容策略：

- 老数据默认 `item_type = 'PROBLEM'`。
- 算法题任务继续使用 `problem_title`。
- 知识卡片任务使用 `knowledge_card_id` 和 `knowledge_card_title`。
- 前端展示时根据 `item_type` 区分图标和文案。

### 6.3 暂不新增 knowledge_weakness

第一版不记录掌握度，所以不需要新增 `knowledge_weakness` 表。

二期如果做知识自测，可以再新增：

```text
knowledge_progress
knowledge_weakness
knowledge_card_review_log
```

## 7. 后端设计

### 7.1 新增包内类

沿用当前 Spring Boot 分层结构：

```text
backend/src/main/java/com/interview/coach/
├── controller/KnowledgeController.java
├── service/KnowledgeCardService.java
├── service/impl/KnowledgeCardServiceImpl.java
├── entity/KnowledgeCard.java
├── mapper/KnowledgeCardMapper.java
├── vo/KnowledgeCardVO.java
└── vo/KnowledgeCategoryVO.java
```

### 7.2 Controller 职责

`KnowledgeController` 只负责：

- 接收分类筛选参数
- 调用 service
- 返回统一 `ApiResponse<T>`

不在 Controller 中写 SQL，不拼装复杂业务。

### 7.3 Service 职责

`KnowledgeCardService` 负责：

- 查询启用的知识卡片
- 按分类过滤
- 查询卡片详情
- 返回分类统计
- entity 转 VO

### 7.4 Mapper 职责

`KnowledgeCardMapper` 继承 MyBatis-Plus `BaseMapper<KnowledgeCard>`。

第一版使用 Wrapper 查询即可，不需要 XML。

## 8. API 设计

### 8.1 获取知识卡片列表

```http
GET /api/knowledge/cards?category=JAVA
```

参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `category` | String | 否 | JAVA / MYSQL / REDIS / SPRING / JVM |

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "category": "JAVA",
      "title": "HashMap 底层结构",
      "question": "HashMap 在 JDK 1.8 中的底层结构是什么？",
      "difficulty": "MEDIUM",
      "tags": ["集合", "哈希表", "红黑树"],
      "sourceName": "小林 coding",
      "sourceUrl": "https://xiaolincoding.com/interview/"
    }
  ]
}
```

列表接口可以不返回完整 `answer`，减少页面首屏数据量。详情接口再返回完整内容。

### 8.2 获取知识卡片详情

```http
GET /api/knowledge/cards/{id}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "category": "JAVA",
    "title": "HashMap 底层结构",
    "question": "HashMap 在 JDK 1.8 中的底层结构是什么？",
    "answer": "HashMap 底层主要由数组、链表和红黑树组成...",
    "followUp": "为什么链表长度超过阈值后要转红黑树？",
    "keyPoints": [
      "数组负责定位桶",
      "链表处理哈希冲突",
      "红黑树降低冲突严重时的查询复杂度",
      "扩容会重新计算元素位置"
    ],
    "difficulty": "MEDIUM",
    "tags": ["集合", "哈希表", "红黑树"],
    "sourceName": "小林 coding",
    "sourceUrl": "https://xiaolincoding.com/interview/"
  }
}
```

### 8.3 获取知识分类

```http
GET /api/knowledge/categories
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "category": "JAVA",
      "label": "Java",
      "count": 17
    },
    {
      "category": "MYSQL",
      "label": "MySQL",
      "count": 5
    }
  ]
}
```

## 9. 前端设计

### 9.1 新增路由

```text
frontend/app/knowledge/page.tsx
```

页面为客户端组件或服务端组件均可。考虑分类切换和卡片选择交互，推荐页面内部使用 client component。

### 9.2 新增组件

建议拆分：

```text
frontend/components/KnowledgeTrainingClient.tsx
frontend/components/KnowledgeCategoryTabs.tsx
frontend/components/KnowledgeCardList.tsx
frontend/components/KnowledgeCardDetail.tsx
frontend/components/KnowledgeTrainingEntry.tsx
```

其中：

- `KnowledgeTrainingClient` 负责页面状态。
- `KnowledgeCategoryTabs` 负责分类筛选。
- `KnowledgeCardList` 负责卡片列表。
- `KnowledgeCardDetail` 负责详情展示。
- `KnowledgeTrainingEntry` 可复用于 Dashboard 入口。

### 9.3 lib/api.ts 扩展

新增：

```ts
export const knowledgeApi = {
  categories: () => request<ApiResponse<KnowledgeCategory[]>>("/api/knowledge/categories"),
  cards: (category?: string) =>
    request<ApiResponse<KnowledgeCardListItem[]>>(
      category ? `/api/knowledge/cards?category=${category}` : "/api/knowledge/cards"
    ),
  detail: (id: number) =>
    request<ApiResponse<KnowledgeCardDetail>>(`/api/knowledge/cards/${id}`),
};
```

### 9.4 types.ts 扩展

新增：

```ts
export interface KnowledgeCategory {
  category: string;
  label: string;
  count: number;
}

export interface KnowledgeCardListItem {
  id: number;
  category: string;
  title: string;
  question: string;
  difficulty: string;
  tags: string[];
  sourceName: string | null;
  sourceUrl: string | null;
}

export interface KnowledgeCardDetail extends KnowledgeCardListItem {
  answer: string;
  followUp: string | null;
  keyPoints: string[];
}
```

### 9.5 导航栏

`Navbar.tsx` links 改为：

```ts
const links = [
  { href: "/", label: "题目" },
  { href: "/knowledge", label: "知识训练" },
  { href: "/dashboard", label: "学习中心" },
];
```

### 9.6 Dashboard 接入

在 Dashboard 右侧 `AI 教练建议` 附近加入入口卡片：

```text
后端知识训练
Java / MySQL / Redis / Spring / JVM 高频面试知识卡
开始复习
```

点击跳转 `/knowledge`。

## 10. 训练计划接入

### 10.1 VO 扩展

后端 `TrainingPlanItemVO` 增加：

```text
itemType
knowledgeCardId
knowledgeCardTitle
```

前端 `TrainingPlanItem` 同步增加：

```ts
itemType: "PROBLEM" | "KNOWLEDGE_CARD";
knowledgeCardId?: number | null;
knowledgeCardTitle?: string | null;
```

### 10.2 展示规则

如果 `itemType = PROBLEM`：

```text
算法题：{problemTitle}
复习重点：{reviewFocus}
```

如果 `itemType = KNOWLEDGE_CARD`：

```text
知识卡片：{knowledgeCardTitle}
复习重点：{reviewFocus}
```

### 10.3 生成策略

第一版不让算法错误自动映射八股知识。训练计划里知识卡片可以先通过简单规则加入：

```text
每个 3 天计划中加入 1 到 2 张启用的高频知识卡
优先选 sort_order 靠前的卡片
不同天分散展示
```

这只是“统一训练计划”的展示能力，不表达算法错因和后端知识之间的因果关系。

后续如果新增知识自测，再根据知识弱点生成更个性化的知识卡复习任务。

## 11. 种子数据示例

### 11.1 HashMap 底层结构

```text
category: JAVA
title: HashMap 底层结构
question: HashMap 在 JDK 1.8 中的底层结构是什么？
answer: HashMap 底层主要由数组、链表和红黑树组成。数组用于定位桶，链表用于处理哈希冲突。当同一个桶中的链表长度达到阈值，并且数组容量满足条件时，链表会转为红黑树以提升查询效率。
follow_up: 为什么链表长度超过阈值后不是一定立即转红黑树？
key_points:
  - 数组定位桶
  - 链表处理冲突
  - 红黑树优化极端冲突
  - 扩容会重新分布元素
difficulty: MEDIUM
tags: 集合,哈希表,扩容,红黑树
source_name: 小林 coding
source_url: https://xiaolincoding.com/interview/
```

### 11.2 MySQL 索引

```text
category: MYSQL
title: MySQL 索引为什么能加速查询
question: MySQL 索引为什么可以提升查询速度？
answer: 索引通过额外的数据结构减少扫描数据量。InnoDB 常用 B+ 树索引，查询时可以从根节点逐层定位到叶子节点，而不是全表扫描。索引适合高选择性的查询条件，但也会带来额外存储和写入维护成本。
follow_up: 为什么 InnoDB 常用 B+ 树而不是普通二叉树？
key_points:
  - 减少扫描范围
  - B+ 树层高低
  - 叶子节点有序
  - 写入需要维护索引
difficulty: EASY
tags: 索引,B+树,InnoDB
source_name: 小林 coding
source_url: https://xiaolincoding.com/interview/
```

### 11.3 Redis 缓存穿透

```text
category: REDIS
title: Redis 缓存穿透
question: 什么是缓存穿透，常见解决方案有哪些？
answer: 缓存穿透指查询的数据在缓存和数据库中都不存在，导致请求每次都绕过缓存打到数据库。常见解决方案包括缓存空值、布隆过滤器、参数校验和限流。
follow_up: 缓存空值会带来什么问题？
key_points:
  - 请求命中不存在数据
  - 数据库压力增大
  - 缓存空值
  - 布隆过滤器
difficulty: EASY
tags: 缓存,穿透,布隆过滤器
source_name: 小林 coding
source_url: https://xiaolincoding.com/interview/
```

## 12. RAG 边界

第一版不做 RAG，原因：

```text
1. 当前需求是稳定展示知识卡片，不是开放式问答
2. 项目重点仍是面试训练闭环，不是知识库检索系统
3. RAG 会引入文档切分、向量存储、召回质量、引用展示等额外复杂度
4. 结构化卡片更容易进入训练计划和 Dashboard
```

未来可以扩展 RAG 的场景：

```text
1. 用户对知识卡进行自由追问
2. AI 模拟面试官连续追问
3. 答案需要带资料引用
4. 从多份资料中自动生成或补充卡片
5. 大规模知识库超过人工维护范围
```

## 13. 验收标准

### 13.1 后端验收

```text
GET /api/knowledge/categories 返回分类和数量
GET /api/knowledge/cards 返回启用卡片列表
GET /api/knowledge/cards?category=JAVA 正确过滤
GET /api/knowledge/cards/{id} 返回完整卡片详情
training_plan_item 老数据仍能正常展示
训练计划 itemType 为 KNOWLEDGE_CARD 时能返回知识卡片信息
```

### 13.2 前端验收

```text
Navbar 显示“知识训练”
/knowledge 页面能加载知识卡片
分类 tab 可以筛选
点击卡片可以查看详情
详情区展示问题、回答、追问、记忆点、来源
Dashboard 有后端知识训练入口
训练计划能区分算法题和知识卡片任务
移动端没有明显布局溢出
```

### 13.3 产品验收

```text
算法题页面不新增八股文 tab
算法诊断仍只解释当前代码错误
后端知识卡片有独立入口
训练计划可以统一展示算法题和知识卡
文案不表达算法错误和 MySQL/Redis/Spring 的强因果关系
```

## 14. 实施阶段

### 阶段 1：数据层

```text
1. 新增 knowledge_card 表 SQL
2. 扩展 training_plan_item 表 SQL
3. 新增 data/knowledge_cards.sql
4. 准备 35 到 50 张原创整理种子卡片
```

### 阶段 2：后端接口

```text
1. 新增 KnowledgeCard entity
2. 新增 KnowledgeCardMapper
3. 新增 KnowledgeCardService
4. 新增 KnowledgeCardServiceImpl
5. 新增 KnowledgeController
6. 新增 KnowledgeCardVO / KnowledgeCategoryVO
7. 补充后端单元测试或接口手动验证
```

### 阶段 3：前端页面

```text
1. 新增 /knowledge 路由
2. 新增 knowledgeApi
3. 新增 types
4. 新增分类 tab、卡片列表、详情组件
5. Navbar 增加“知识训练”
6. Dashboard 增加入口卡片
```

### 阶段 4：训练计划轻接入

```text
1. 扩展 TrainingPlanItem entity / VO
2. 扩展 UserLearningServiceImpl 查询映射
3. 扩展前端 TrainingPlan 组件
4. 可选：让训练计划中混入少量固定知识卡片任务
```

### 阶段 5：文档和验证

```text
1. 更新 docs/API.md
2. 更新 README 功能介绍
3. 验证 /knowledge 页面
4. 验证 Dashboard 入口
5. 验证训练计划老数据兼容
```

## 15. 风险与控制

### 15.1 内容版权风险

风险：直接复制外部网站内容。

控制：

```text
只参考题目和分类
答案重新组织
保留来源链接
不批量抓取整站
```

### 15.2 产品边界风险

风险：项目变成普通八股文网站。

控制：

```text
知识训练服务 Java 后端面试
训练计划轻接入
不做大而全内容平台
不影响算法 Agent 诊断闭环
```

### 15.3 技术复杂度风险

风险：过早引入 RAG、AI 追问、掌握度评分。

控制：

```text
第一版只做结构化卡片
RAG 放到后续增强
自测和知识弱点放到二期
```

## 16. 面试表达

可以这样介绍这个功能：

```text
项目原本支持 Java 算法题提交、代码执行、AI 诊断、弱点记忆和训练计划。为了更贴近 Java 后端求职场景，我扩展了后端知识训练模块，将 Java、JVM、Spring、MySQL、Redis 高频面试知识整理成结构化知识卡片，并通过 Dashboard 和训练计划统一管理。

算法错误和后端八股知识没有被强行绑定。算法弱点仍来自代码提交后的 Agent 诊断，后端知识训练则通过独立知识卡片入口完成。训练计划作为统一学习入口，可以同时安排算法题训练和后端知识复习任务。
```

