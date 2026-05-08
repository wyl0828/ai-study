# Phase 4 实施计划：Dashboard 真实数据接入

> 状态：已实现。后端已新增 `UserController`、`UserLearningService` 和 Dashboard VO；前端 `/dashboard` 已切换到 `/api/users/1/...` 真实接口，并保留空状态引导文案。

## 1. 背景与目标

### 当前状态

Phase 1-3 已完成：

- 后端题目、提交、判题、Agent 诊断、弱点记忆、错题卡、训练计划持久化全部就绪。
- 前端首页、做题页、Dashboard 三个核心页面已完成，中文化完成，构建验证通过。
- 做题页提交失败后自动调用 `POST /api/agent/analyze`，诊断结果已落库。

**已解决的核心问题：Dashboard 原先使用 `frontend/lib/mock.ts` 硬编码数据。** 现在用户做完题、触发 Agent 诊断后，Dashboard 可以从真实查询接口读取学习数据。

### 目标

将 Dashboard 从 mock 数据切换为真实 MySQL 数据，打通"诊断结果 → 薄弱点 → 错题卡 → 训练计划 → Dashboard 展示"的完整闭环。

### 验收标准

1. 用户提交失败并触发 Agent 诊断后，Dashboard 能展示该次诊断产生的弱点、错题卡和训练计划。
2. 统计卡片（总提交、通过题目、薄弱知识点、错题数量）显示真实数据。
3. 无数据时显示空状态引导文案，不显示 mock 数据。
4. 前端 `npm run build` 通过。
5. 完整演示流：做错"两数之和" → Agent 诊断落库 → 打开 Dashboard 看到真实数据更新。

---

## 2. 现有资产盘点

### 2.1 后端已有的 Entity 和 Mapper

| Entity | Mapper | 说明 |
|--------|--------|------|
| `UserWeakness` | `UserWeaknessMapper` | 字段：`userId`, `knowledgePoint`, `errorType`, `wrongCount`, `submitCount`, `weaknessScore`, `lastWrongAt` |
| `MistakeCard` | `MistakeCardMapper` | 字段：`userId`, `problemId`, `submissionId`, `agentRunId`, `errorType`, `knowledgePoint`, `mistakeSummary`, `correctIdea` |
| `TrainingPlan` | `TrainingPlanMapper` | 字段：`userId`, `agentRunId`, `title`, `summary`, `startDate`, `endDate` |
| `TrainingPlanItem` | `TrainingPlanItemMapper` | 字段：`planId`, `dayIndex`, `knowledgePoint`, `problemTitle`, `reason`, `reviewFocus`, `status` |
| `Submission` | `SubmissionMapper` | 字段：`userId`, `problemId`, `status`, `passedCount`, `totalCount`, `createdAt` |
| `Problem` | `ProblemMapper` | 字段：`id`, `title`, `difficulty`, `category` |

所有 Mapper 均继承 `BaseMapper<T>`，支持 MyBatis-Plus 条件查询。

### 2.2 后端已有的 Service（仅写入）

| Service | 方法 | 说明 |
|---------|------|------|
| `LearningTracker` | `recordDiagnosis(AgentContext)` | 写入诊断、提示、弱点、错题卡 |
| `TrainingPlanService` | `savePlan(AgentContext, TrainingPlanResult)` | 写入训练计划和计划 item |

**没有查询方法。** 当前无法通过 REST API 读取用户的学习数据。

### 2.3 前端已有的 Dashboard 组件

| 组件 | Props 类型 | 说明 |
|------|-----------|------|
| `WeaknessList` | `weaknesses: UserWeakness[]` | 薄弱点排行 |
| `MistakeCards` | `mistakes: MistakeCard[]` | 错题卡片 |
| `TrainingPlan` | `plan: TrainingPlan` | 训练计划 |
| `SubmissionHistory` | `submissions: SubmissionHistoryItem[]` | 最近提交 |

组件已按 props 接收数据，**替换数据源即可，组件本身不需要改动**。

### 2.4 前端已有的类型定义

`frontend/lib/types.ts` 已定义 `UserWeakness`、`MistakeCard`、`TrainingPlan`、`TrainingPlanItem` 接口，但字段与后端 Entity 有差异（见下文映射表）。

---

## 3. 接口设计

### 3.1 接口总览

| 方法 | 路径 | 说明 | 响应 data 类型 |
|------|------|------|---------------|
| `GET` | `/api/users/{userId}/dashboard/stats` | 学习统计概览 | `DashboardStatsVO` |
| `GET` | `/api/users/{userId}/weaknesses` | 薄弱点排行 | `UserWeaknessVO[]` |
| `GET` | `/api/users/{userId}/mistakes` | 错题卡片列表 | `MistakeCardVO[]` |
| `GET` | `/api/users/{userId}/training-plans/latest` | 最新训练计划 | `TrainingPlanVO` |
| `GET` | `/api/users/{userId}/submissions/recent` | 最近提交记录 | `SubmissionHistoryVO[]` |

所有接口统一返回 `ApiResponse<T>` 格式。

### 3.2 接口详情

#### 3.2.1 学习统计概览

```http
GET /api/users/{userId}/dashboard/stats
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "totalSubmissions": 12,
    "passedProblems": 2,
    "weakPointCount": 3,
    "mistakeCount": 4
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `totalSubmissions` | Integer | 该用户总提交次数 |
| `passedProblems` | Integer | 该用户通过的不同题目数（`status=ACCEPTED` 的 `COUNT(DISTINCT problem_id)`） |
| `weakPointCount` | Integer | 薄弱点数量（`user_weakness` 表该用户记录数） |
| `mistakeCount` | Integer | 错题卡数量（`mistake_card` 表该用户记录数） |

#### 3.2.2 薄弱点排行

```http
GET /api/users/{userId}/weaknesses
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "knowledgePoint": "HashMap Lookup in Array Traversal",
      "errorType": "LOGIC_ERROR",
      "wrongCount": 3,
      "weaknessScore": 15.0
    }
  ]
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `knowledgePoint` | String | 知识点名称 |
| `errorType` | String | 错误类型枚举 |
| `wrongCount` | Integer | 累计错误次数 |
| `weaknessScore` | BigDecimal | 弱点分（越高越薄弱） |

排序：按 `weaknessScore DESC`。

#### 3.2.3 错题卡片列表

```http
GET /api/users/{userId}/mistakes
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "problemId": 101,
      "problemTitle": "Two Sum",
      "errorType": "LOGIC_ERROR",
      "knowledgePoint": "HashMap Lookup in Array Traversal",
      "mistakeSummary": "Self-matching due to incorrect map operation order",
      "correctIdea": "Check complement before inserting current element"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 错题卡 ID |
| `problemId` | Long | 题目 ID |
| `problemTitle` | String | 题目标题（从 `problem` 表关联查询） |
| `errorType` | String | 错误类型 |
| `knowledgePoint` | String | 知识点 |
| `mistakeSummary` | String | 错误摘要 |
| `correctIdea` | String | 正确思路 |

排序：按 `created_at DESC`。

#### 3.2.4 最新训练计划

```http
GET /api/users/{userId}/training-plans/latest
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "title": "3-day recovery plan: HashMap Lookup in Array Traversal",
    "summary": "...",
    "items": [
      {
        "dayIndex": 1,
        "knowledgePoint": "HashMap",
        "problemTitle": "Two Sum",
        "reason": "Practice check-before-insert pattern",
        "reviewFocus": "Duplicate elements and boundary inputs",
        "status": "PENDING"
      }
    ]
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `title` | String | 计划标题 |
| `summary` | String | 计划摘要 |
| `items` | TrainingPlanItemVO[] | 训练计划条目 |

**TrainingPlanItemVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `dayIndex` | Integer | 第几天 |
| `knowledgePoint` | String | 知识点 |
| `problemTitle` | String | 题目标题 |
| `reason` | String | 推荐原因 |
| `reviewFocus` | String | 复习重点 |
| `status` | String | `PENDING` / `COMPLETED` |

取最新一条：`ORDER BY created_at DESC LIMIT 1`。无数据时返回 `null`。

#### 3.2.5 最近提交记录

```http
GET /api/users/{userId}/submissions/recent
```

**响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "problemId": 101,
      "problemTitle": "Two Sum",
      "status": "ACCEPTED",
      "passedCount": 3,
      "totalCount": 3,
      "createdAt": "2026-05-07T14:30:00"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `problemId` | Long | 题目 ID |
| `problemTitle` | String | 题目标题（从 `problem` 表关联查询） |
| `status` | String | 提交状态 |
| `passedCount` | Integer | 通过用例数 |
| `totalCount` | Integer | 总用例数 |
| `createdAt` | LocalDateTime | 提交时间 |

排序：按 `created_at DESC`，限制 10 条。

---

## 4. 后端实现计划

### 4.1 新建 VO 类（6 个文件）

路径：`backend/src/main/java/com/interview/coach/vo/`

| 文件名 | 说明 |
|--------|------|
| `DashboardStatsVO.java` | 学习统计概览 |
| `UserWeaknessVO.java` | 薄弱点排行 |
| `MistakeCardVO.java` | 错题卡片（含 problemTitle） |
| `TrainingPlanVO.java` | 训练计划（含 items 列表） |
| `TrainingPlanItemVO.java` | 训练计划条目 |
| `SubmissionHistoryVO.java` | 最近提交记录（含 problemTitle） |

### 4.2 新建 UserLearningService（2 个文件）

路径：`backend/src/main/java/com/interview/coach/service/`

**`UserLearningService.java`（接口）：**

```java
public interface UserLearningService {
    DashboardStatsVO getDashboardStats(Long userId);
    List<UserWeaknessVO> getWeaknesses(Long userId);
    List<MistakeCardVO> getMistakes(Long userId);
    TrainingPlanVO getLatestTrainingPlan(Long userId);
    List<SubmissionHistoryVO> getRecentSubmissions(Long userId);
}
```

**`service/impl/UserLearningServiceImpl.java`（实现）：**

实现要点：

- **getDashboardStats**：分别查询 submission 总数、accepted 的 distinct problemId 数、user_weakness 记录数、mistake_card 记录数。
- **getWeaknesses**：`UserWeaknessMapper.selectList` 按 `userId` 过滤，按 `weaknessScore DESC` 排序。
- **getMistakes**：`MistakeCardMapper.selectList` 按 `userId` 过滤，按 `createdAt DESC` 排序。实际实现中批量查询 `Problem` 表，并用 Map 组装 `problemTitle`。
- **getLatestTrainingPlan**：`TrainingPlanMapper.selectOne` 按 `userId` 过滤，按 `createdAt DESC` 排序，`LIMIT 1`。然后用 `planId` 查 `TrainingPlanItemMapper` 获取 items。
- **getRecentSubmissions**：`SubmissionMapper.selectList` 按 `userId` 过滤，按 `createdAt DESC` 排序，`last("LIMIT 10")`。实际实现中批量查询 `Problem` 表，并用 Map 组装 `problemTitle`。

### 4.3 新建 UserController（1 个文件）

路径：`backend/src/main/java/com/interview/coach/controller/UserController.java`

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserLearningService userLearningService;

    @GetMapping("/{userId}/dashboard/stats")
    public ApiResponse<DashboardStatsVO> getDashboardStats(@PathVariable Long userId) { ... }

    @GetMapping("/{userId}/weaknesses")
    public ApiResponse<List<UserWeaknessVO>> getWeaknesses(@PathVariable Long userId) { ... }

    @GetMapping("/{userId}/mistakes")
    public ApiResponse<List<MistakeCardVO>> getMistakes(@PathVariable Long userId) { ... }

    @GetMapping("/{userId}/training-plans/latest")
    public ApiResponse<TrainingPlanVO> getLatestTrainingPlan(@PathVariable Long userId) { ... }

    @GetMapping("/{userId}/submissions/recent")
    public ApiResponse<List<SubmissionHistoryVO>> getRecentSubmissions(@PathVariable Long userId) { ... }
}
```

### 4.4 后端文件清单

| 操作 | 文件路径 |
|------|----------|
| 新建 | `vo/DashboardStatsVO.java` |
| 新建 | `vo/UserWeaknessVO.java` |
| 新建 | `vo/MistakeCardVO.java` |
| 新建 | `vo/TrainingPlanVO.java` |
| 新建 | `vo/TrainingPlanItemVO.java` |
| 新建 | `vo/SubmissionHistoryVO.java` |
| 新建 | `service/UserLearningService.java` |
| 新建 | `service/impl/UserLearningServiceImpl.java` |
| 新建 | `controller/UserController.java` |

共 9 个新文件，不修改任何已有后端文件。

---

## 5. 前端实现计划

### 5.1 补充类型定义

修改 `frontend/lib/types.ts`，新增：

```typescript
export interface DashboardStatsVO {
  totalSubmissions: number;
  passedProblems: number;
  weakPointCount: number;
  mistakeCount: number;
}

export interface SubmissionHistoryVO {
  problemId: number;
  problemTitle: string;
  status: string;
  passedCount: number;
  totalCount: number;
  createdAt: string;
}
```

同时调整已有类型以匹配后端 VO 字段：

- `UserWeakness`：去掉 `category`、`relatedProblemCount`，字段改为 `errorType`、`wrongCount`、`weaknessScore`（BigDecimal）。
- `MistakeCard`：字段改为 `mistakeSummary`、`correctIdea`（去掉 `userError`、`correctApproach`）。
- `TrainingPlanItem`：新增 `status` 字段。

### 5.2 新增 API 调用

修改 `frontend/lib/api.ts`，新增：

```typescript
export const userApi = {
  stats: (userId: number) =>
    request<ApiResponse<DashboardStatsVO>>(`/api/users/${userId}/dashboard/stats`),
  weaknesses: (userId: number) =>
    request<ApiResponse<UserWeakness[]>>(`/api/users/${userId}/weaknesses`),
  mistakes: (userId: number) =>
    request<ApiResponse<MistakeCard[]>>(`/api/users/${userId}/mistakes`),
  latestPlan: (userId: number) =>
    request<ApiResponse<TrainingPlan | null>>(`/api/users/${userId}/training-plans/latest`),
  recentSubmissions: (userId: number) =>
    request<ApiResponse<SubmissionHistoryVO[]>>(`/api/users/${userId}/submissions/recent`),
};
```

### 5.3 改造 Dashboard 页面

修改 `frontend/app/dashboard/page.tsx`：

**改造要点：**

1. 删除所有 `import { mock... }` 引用。
2. 使用 `useState` + `useEffect` 在组件挂载时并发请求 5 个接口（`userId` 固定为 1）。
3. 加载中状态：统计卡片显示 `--`，列表区域显示 loading 文案。
4. 空数据状态：
   - 无弱点/错题/训练计划时显示引导文案："还没有学习数据，去做第一道题并触发 AI 诊断吧"。
   - 无提交记录时显示："还没有提交记录，去题库看看"。
5. `TrainingPlan` 组件和 AI 教练建议区域：无训练计划时隐藏或显示占位文案。

### 5.4 前端文件清单

| 操作 | 文件路径 |
|------|----------|
| 修改 | `frontend/lib/types.ts` — 新增 `DashboardStatsVO`、`SubmissionHistoryVO`，调整 `UserWeakness`、`MistakeCard`、`TrainingPlanItem` 字段 |
| 修改 | `frontend/lib/api.ts` — 新增 `userApi` |
| 修改 | `frontend/app/dashboard/page.tsx` — 替换 mock 为真实 API |
| 修改 | `frontend/components/WeaknessList.tsx` — 适配字段名变更（如有） |
| 修改 | `frontend/components/MistakeCards.tsx` — 适配字段名变更（如有） |
| 修改 | `frontend/components/SubmissionHistory.tsx` — 适配 `SubmissionHistoryVO` 类型 |

共 0 个新文件，修改 5-6 个已有文件。

---

## 6. 前后端字段映射

### 6.1 UserWeakness

| 后端 Entity 字段 | 后端 VO 字段 | 前端类型字段 | 说明 |
|-----------------|-------------|-------------|------|
| `knowledgePoint` | `knowledgePoint` | `knowledgePoint` | 知识点名称 |
| `errorType` | `errorType` | `errorType` | 错误类型（新增） |
| `wrongCount` | `wrongCount` | `wrongCount` | 累计错误次数（原 `errorCount`） |
| `weaknessScore` | `weaknessScore` | `weaknessScore` | 弱点分 |
| — | — | ~~`category`~~ | 前端去掉，后端不提供 |
| — | — | ~~`relatedProblemCount`~~ | 前端去掉，后端不提供 |

### 6.2 MistakeCard

| 后端 Entity 字段 | 后端 VO 字段 | 前端类型字段 | 说明 |
|-----------------|-------------|-------------|------|
| `id` | `id` | `id` | 错题卡 ID |
| `problemId` | `problemId` | `problemId` | 题目 ID |
| — (join) | `problemTitle` | `problemTitle` | 题目标题 |
| `errorType` | `errorType` | `errorType` | 错误类型 |
| `knowledgePoint` | `knowledgePoint` | `knowledgePoint` | 知识点 |
| `mistakeSummary` | `mistakeSummary` | `mistakeSummary` | 错误摘要（原 `userError`） |
| `correctIdea` | `correctIdea` | `correctIdea` | 正确思路（原 `correctApproach`） |

### 6.3 TrainingPlan / TrainingPlanItem

| 后端 Entity 字段 | 后端 VO 字段 | 前端类型字段 | 说明 |
|-----------------|-------------|-------------|------|
| `title` | `title` | `title` | 计划标题 |
| `summary` | `summary` | `summary` | 计划摘要 |
| `items[].dayIndex` | `items[].dayIndex` | `items[].dayIndex` | 第几天 |
| `items[].knowledgePoint` | `items[].knowledgePoint` | `items[].knowledgePoint` | 知识点 |
| `items[].problemTitle` | `items[].problemTitle` | `items[].problemTitle` | 题目标题 |
| `items[].reason` | `items[].reason` | `items[].reason` | 推荐原因 |
| `items[].reviewFocus` | `items[].reviewFocus` | `items[].reviewFocus` | 复习重点 |
| `items[].status` | `items[].status` | `items[].status` | 状态（新增） |

### 6.4 SubmissionHistory

| 后端来源 | VO 字段 | 前端类型字段 | 说明 |
|---------|---------|-------------|------|
| `submission.problemId` | `problemId` | `problemId` | 题目 ID |
| `problem.title` (join) | `problemTitle` | `problemTitle` | 题目标题 |
| `submission.status` | `status` | `status` | 提交状态 |
| `submission.passedCount` | `passedCount` | `passedCount` | 通过数 |
| `submission.totalCount` | `totalCount` | `totalCount` | 总数 |
| `submission.createdAt` | `createdAt` | `createdAt` | 提交时间 |

---

## 7. 实施顺序

```
Step 1  后端：新建 6 个 VO 类
Step 2  后端：新建 UserLearningService 接口 + UserLearningServiceImpl 实现
Step 3  后端：新建 UserController
Step 4  后端：启动验证 5 个接口（curl / Postman）
Step 5  前端：修改 types.ts 补充和调整类型
Step 6  前端：修改 api.ts 新增 userApi
Step 7  前端：修改 Dashboard 页面组件，替换 mock 数据
Step 8  前端：适配 WeaknessList / MistakeCards / SubmissionHistory 组件字段变更
Step 9  前端：npm run build 验证
Step 10 端到端：做错题 → Agent 诊断 → Dashboard 真实数据验证
```

实际验证：

- 已运行 `backend` 全量 `mvn test`，新增 `UserLearningServiceImplTest` 覆盖统计聚合、无训练计划、错题卡补题目标题和最近提交补题目标题。
- 已运行 `frontend` 的 `npm run build`，Next.js 编译和类型检查通过。

---

## 8. 风险与应对

| 风险 | 影响 | 应对 |
|------|------|------|
| `mistake_card` 表没有 `problemTitle`，需要 join `problem` 表 | 查询稍复杂 | 在 `UserLearningServiceImpl` 中批量查 problem 表，用 Map 组装 |
| 训练计划无数据时前端 TrainingPlan 组件可能报错 | 页面白屏 | 后端返回 `null`，前端做空值判断，无数据时隐藏该区域 |
| `UserWeakness` 字段变更影响前端组件 | 组件 props 不匹配 | 同步修改前端类型和组件，Step 8 统一处理 |
| 首次使用时 Dashboard 全空 | 用户困惑 | 空状态引导文案引导用户去做题 |

---

## 9. 演示脚本（更新）

完整演示流程：

1. 打开首页，展示题目列表。
2. 选择"两数之和"，写一段有 bug 的 Java 代码。
3. 点击提交，展示 Piston 返回测试失败结果。
4. 自动触发 Agent 诊断，展示 AI 错误分类和三层提示。
5. **打开 Dashboard，展示真实的薄弱知识点、错题卡片、训练计划和最近提交记录。**
6. 说明 Dashboard 数据来自 `user_weakness`、`mistake_card`、`training_plan`、`submission` 表，不是 mock。
7. 讲解后端设计：Agent Tool 链、Observation、Memory 持久化、Dashboard 查询接口。
8. 说明下一步可以接入 SSE 流式诊断和训练计划完成状态更新。
