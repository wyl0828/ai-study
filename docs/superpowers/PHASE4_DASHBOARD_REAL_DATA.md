# Phase 4 实施记录：Dashboard 真实数据接入

> 状态：已实现。Dashboard 已从 `frontend/lib/mock.ts` 切换到 `/api/users/1/...` 真实用户学习接口。

## 目标

打通以下闭环：

```text
失败提交 -> Agent 诊断落库 -> 弱点/错题卡/训练计划/提交记录 -> Dashboard 展示
```

Dashboard 展示的数据来源：

- `submission`
- `user_weakness`
- `mistake_card`
- `training_plan`
- `training_plan_item`
- `problem`

## 已实现接口

后端新增 `UserController`，路径前缀为 `/api/users`：

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/users/{userId}/dashboard/stats` | 学习统计概览 |
| `GET` | `/api/users/{userId}/weaknesses` | 薄弱点排行 |
| `GET` | `/api/users/{userId}/mistakes` | 错题卡片列表 |
| `GET` | `/api/users/{userId}/training-plans/latest` | 最新训练计划 |
| `GET` | `/api/users/{userId}/submissions/recent` | 最近提交记录 |

后端新增读模型服务：

- `UserLearningService`
- `UserLearningServiceImpl`

实现要点：

- 使用 MyBatis-Plus `LambdaQueryWrapper`。
- `passedProblems` 按 `ACCEPTED` 提交的 distinct `problemId` 统计。
- 错题卡和最近提交通过批量查询 `Problem` 表补齐 `problemTitle`。
- 最新训练计划按 `createdAt DESC LIMIT 1` 查询；无数据返回 `null`。
- 最近提交按 `createdAt DESC LIMIT 10` 查询。

## 前端改造

Dashboard 当前固定 demo 用户 `userId=1`，页面加载时并发请求 5 个接口。

已改造文件：

- `frontend/lib/types.ts`
- `frontend/lib/api.ts`
- `frontend/app/dashboard/page.tsx`
- `frontend/components/WeaknessList.tsx`
- `frontend/components/MistakeCards.tsx`
- `frontend/components/SubmissionHistory.tsx`
- `frontend/components/TrainingPlan.tsx`

前端行为：

- 加载中统计卡片显示 `--`。
- 请求失败显示错误提示，不回退 mock。
- 无数据时显示“还没有学习数据，去做第一道题并触发 AI 诊断吧”。
- `TrainingPlan` 支持 `null`，不再依赖 `problemId`。

## 验证结果

已验证：

```powershell
cd backend
mvn test
```

结果：9 tests, 0 failures。

```powershell
cd frontend
npm run build
```

结果：Next.js build 通过。

## 联调排查

如果访问：

```http
GET http://localhost:8080/api/users/1/dashboard/stats
```

返回类似：

```text
No static resource api/users/1/dashboard/stats
```

优先检查：

1. 当前运行的 Spring Boot 后端是否已经重启到最新代码。
2. `backend/src/main/java/com/interview/coach/controller/UserController.java` 是否存在。
3. `backend/src/main/java/com/interview/coach/service/impl/UserLearningServiceImpl.java` 是否存在。
4. 新增后端文件是否已纳入版本控制；如果仍是 untracked，其他工具查看旧提交时会误判“没有 UserController”。

## 后续增强

暂不实现：

- 手动重新生成训练计划接口
- 单独获取某一级 hint
- accepted-code review
- Dashboard 图表和复杂可视化
