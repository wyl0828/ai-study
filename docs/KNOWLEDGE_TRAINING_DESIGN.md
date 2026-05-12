# 后端知识训练模块设计文档

## 1. 模块定位

后端知识训练是 AI Interview Coach Agent 的独立训练入口，用于补齐 Java 后端面试中的表达型知识。

它和算法诊断模块的关系是：

```text
算法诊断模块
+
后端知识卡片模块
+
统一训练计划模块
```

算法错误不会强行推荐 MySQL / Redis / Spring 八股。两类训练任务只是在训练计划中统一展示。

## 2. 第一版范围

已实现能力：

- `/knowledge` 页面
- `GET /api/knowledge/categories`
- `GET /api/knowledge/cards`
- `GET /api/knowledge/cards?category=JAVA`
- `GET /api/knowledge/cards/{id}`
- `knowledge_card` 表
- `data/knowledge_cards.sql` 种子数据
- `training_plan_item` 支持 `PROBLEM` / `KNOWLEDGE_CARD`
- Dashboard 增加“后端知识训练”入口

暂不实现：

- RAG 知识库
- AI 模拟追问
- 知识掌握度评分
- 收藏、浏览记录、自测记录
- `knowledge_weakness` 表
- 算法错因到八股知识的自动映射

## 3. 分类与内容来源

一级分类固定为：

```text
JAVA
JVM
SPRING
MYSQL
REDIS
```

Java 基础、集合、并发等细分只作为 `tags` 展示，不做独立 tab。

当前知识卡片内容参考小林 coding 面试专题的选题方向，并重新整理为结构化问答。数据中保留：

```text
source_name = 小林 coding
source_url = https://xiaolincoding.com/interview/
```

## 4. 数据表

核心表：

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

训练计划扩展字段：

```text
training_plan_item.item_type
training_plan_item.knowledge_card_id
training_plan_item.knowledge_card_title
```

`item_type` 默认 `PROBLEM`，兼容旧训练计划数据。

## 5. 前端交互

Navbar 当前为：

```text
题目 / 知识训练 / 学习中心
```

`/knowledge` 页面：

- 顶部展示标题和模块说明
- 分类 tab：全部 / Java / JVM / Spring / MySQL / Redis
- 桌面端：左侧卡片列表，右侧详情
- 移动端：单列列表，点击卡片后展开详情

Dashboard 只展示入口卡片，不展开大量知识内容，避免学习中心过重。

## 6. 训练计划接入规则

训练计划 item 有两种类型：

| 类型 | 展示 | 来源 |
| --- | --- | --- |
| `PROBLEM` | 算法题：{problemTitle} | Agent 诊断后的算法弱点 |
| `KNOWLEDGE_CARD` | 知识卡片：{knowledgeCardTitle} | 高优先级知识卡片 |

`TrainingPlannerTool` 规则：

- 保留原有 3 条算法训练任务
- 最多额外混入 1-2 条知识卡片
- 按 `sort_order` 选择高优先级知识卡
- 不覆盖、不减少算法训练项
- 不根据算法错因推荐八股

## 7. 数据库升级

新库初始化：

```bash
mysql -u root -p ai_interview_coach < data/schema.sql
mysql -u root -p ai_interview_coach < data/problems.sql
mysql -u root -p ai_interview_coach < data/knowledge_cards.sql
```

已有本地库升级：

```powershell
cmd /c "mysql --default-character-set=utf8mb4 -u root -p ai_interview_coach < data\knowledge_training_migration.sql"
cmd /c "mysql --default-character-set=utf8mb4 -u root -p ai_interview_coach < data\knowledge_cards.sql"
```

PowerShell 不支持直接使用 Bash 风格 `<` 输入重定向，所以 Windows 下使用 `cmd /c`。

## 8. 验证清单

- `/api/knowledge/categories` 返回五个一级分类
- `/api/knowledge/cards?category=JAVA` 只返回 Java 卡片
- `/api/knowledge/cards/{id}` 返回答案、追问、记忆点和来源链接
- `/knowledge` 页面能加载卡片并切换分类
- Dashboard 入口能跳转 `/knowledge`
- 最新训练计划能区分 `PROBLEM` 和 `KNOWLEDGE_CARD`
- 算法诊断结果不出现强行推荐 MySQL / Redis / Spring 的内容
