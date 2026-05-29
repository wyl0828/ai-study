# 最终项目验收清单

本文档用于演示前验收 AI Interview Coach Agent 是否处于可复现、可讲解、可录制状态。

## 1. 环境前置条件

先运行本地依赖预检，确认缺口集中在运行时还是应用代码：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\local_dependency_preflight.ps1
```

预检只读状态，不提交代码、不重建索引、不刷新缓存；它会检查 MySQL、Backend、Frontend、Piston、Redis、Qdrant 和 Docker，并输出 `READY_FOR_E2E_SMOKE` 与第一条 `NEXT_ACTION`。`READY_FOR_E2E_SMOKE=True` 后再进入完整端到端验收。

本机如遇 Windows 端口保留导致 `6379` / `6333` / `6334` 无法发布，可临时用高位端口做等价验收：

```powershell
docker run -d --name ai-study-redis-16379 -p 127.0.0.1:16379:6379 redis:7-alpine
docker run -d --name ai-study-qdrant-16333 -p 127.0.0.1:16333:6333 -p 127.0.0.1:16334:6334 qdrant/qdrant:latest
```

对应后端环境变量使用 `REDIS_PORT=16379`、`QDRANT_PORT=16334`，smoke 使用 `-QdrantUrl http://127.0.0.1:16333`。这只改变本机端口映射，不改变项目默认配置和接口语义。

- MySQL 8 已启动，数据库为 `ai_interview_coach`。
- Piston 已启动，并且 Java runtime 可用。
  - 本机常用地址：`http://127.0.0.1:2238/api/v2`
  - 验证：`curl.exe --noproxy "*" http://127.0.0.1:2238/api/v2/runtimes`
- Qdrant 已启动。
  - 启动：`docker compose up -d qdrant`
  - 验证：`curl.exe --noproxy "*" http://127.0.0.1:6333/healthz`
- Redis 可选但建议启动，用于题目 / 知识卡只读热点缓存。
  - 启动：`docker compose up -d redis`
  - 验证：请求 `/api/problems` 后执行 `docker exec ai-study-redis redis-cli --scan --pattern "coach:problem:*"`
- 后端已启动。
  - 验证：`curl.exe --noproxy "*" http://127.0.0.1:8080/api/problems`
- 前端已启动。
  - dev 模式默认：`http://127.0.0.1:4000`
  - production 验证可用：`npm run build` 后 `npx next start -H 127.0.0.1 -p 3000`
- Embedding 配置已存在。
  - `EMBEDDING_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode`
  - `EMBEDDING_MODEL=text-embedding-v4`
  - `EMBEDDING_DIMENSIONS=1536`
  - `EMBEDDING_API_KEY` 只放环境变量，不写入文档或提交。

## 2. 数据库迁移

旧库必须确认以下迁移已执行：

```powershell
cmd /c "mysql --default-character-set=utf8mb4 -u root -p ai_interview_coach < data\rag_mysql_migration.sql"
cmd /c "mysql --default-character-set=utf8mb4 -u root -p ai_interview_coach < data\rag_vector_migration.sql"
cmd /c "mysql --default-character-set=utf8mb4 -u root -p ai_interview_coach < data\training_plan_source_migration.sql"
cmd /c "mysql --default-character-set=utf8mb4 -u root -p ai_interview_coach < data\training_plan_activity_migration.sql"
cmd /c "mysql --default-character-set=utf8mb4 -u root -p ai_interview_coach < data\mock_interview_migration.sql"
```

最低验收字段：

- `rag_chunk.vector_status`
- `rag_chunk.embedding_model`
- `rag_chunk.embedding_dim`
- `training_plan_item.source_type`
- `training_plan_item.source_id`
- `training_plan_item.source_summary`
- `training_plan_item.status_updated_at`

## 3. 一键端到端验收

启动服务前，先跑离线回归门禁，确认 20 题 seed、模板、三层提示、题解和演示样例没有漂移：

```powershell
node frontend/lib/hot100-seed.node-test.cjs
node frontend/lib/demo-cases.node-test.cjs
node frontend/lib/core-loop-stability.node-test.cjs
```

推荐直接运行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\e2e_demo_smoke.ps1
```

如果只想验证主链路、不调用外部 embedding：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\e2e_demo_smoke.ps1 -SkipEmbedding
```

如果当前没有启动前端：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\e2e_demo_smoke.ps1 -SkipFrontend
```

通过时必须看到：

```text
== E2E demo smoke passed ==
failed_submission: ... status=WRONG_ANSWER ...
accepted_submission: ... status=ACCEPTED ...
latest_plan: ... status=ACTIVE, items=6
mysql: indexedVectorChunks=...
qdrant_collection: ai_study_rag_chunks => 200
```

## 4. 脚本覆盖的验收项

`scripts/e2e_demo_smoke.ps1` 会验证：

- 后端 `/api/problems` 可访问。
- `/api/problems` 至少返回 20 道稳定训练题，并包含核心演示题 `1` / `206` / `121`。
- Piston `/runtimes` 可访问。
- Qdrant `/healthz` 和 `ai_study_rag_chunks` collection 可访问。
- Embedding smoke 返回真实 1536 维向量。
- 前端 `/problem/1` 和 `/dashboard` 可访问。
- `GET /api/problems/1` 返回题目详情和 `presetHints`。
- `GET /api/problems/1/template` 返回 `class Solution` 模板。
- Two Sum bug 代码提交后得到 `WRONG_ANSWER`。
- 错误提交 SSE 包含并成功完成：

```text
PLANNING -> CODE_EXECUTION -> OBSERVATION -> RAG_RETRIEVAL
-> ERROR_CLASSIFICATION -> MEMORY_UPDATE -> TRAINING_PLAN -> COMPLETED
```

- Two Sum fixed 代码提交后得到 `ACCEPTED`。
- AC 提交 SSE 包含并成功完成：

```text
PLANNING -> CODE_EXECUTION -> OBSERVATION -> RAG_RETRIEVAL -> CODE_REVIEW -> COMPLETED
```

- 最新训练计划为 `ACTIVE`，且包含训练项；`/api/users/{id}/training-plans/latest` 返回 `id` / `title` / `summary` / `status` / `statusLabel`，首个 item 带 `id` / `itemType` / `dayIndex` / `knowledgePoint` / `reason` / `reviewFocus` / `sourceType` / `sourceSummary` / `targetHref` / `targetLabel` / `status`，用于证明今日训练项可解释、可跳转、可追踪；旧训练计划缺少来源时服务端以 `LEGACY_TRAINING_PLAN` 和历史计划摘要兜底，避免历史数据让今日任务失去解释。
- Dashboard stats 返回真实学习统计。
- Dashboard 辅助卡片首屏失败时降级为空态，不影响 stats、薄弱点、最近提交、完整训练计划和错题卡主数据展示。
- 训练计划 trace 单点失败只影响训练追踪卡，手动刷新训练计划时仍应保留最新计划、历史计划和最近训练动作的可用结果。
- 模拟面试 trace / trend 单点失败只影响对应卡片，最近模拟面试、闭环追踪和趋势可分别降级、分别重试。
- `/api/rag/health` 返回 RAG 索引健康摘要，`tablesAvailable=true`、系统 chunk 不为空，并包含 `statusLabel` / `maintenanceSummary` / `maintenancePriority` / `maintenanceReason` / `checkedAt` / `userMemoryDocumentCount` / `userMemoryChunkCount` / `duplicateSystemDocumentCount` / `vectorEnabled` / `vectorIndexedChunkCount` / `vectorFailedChunkCount` / `vectorPendingChunkCount` / `warnings` / `maintenanceActions` / `preferredMaintenanceAction` / `nextMaintenanceEndpoint`。
- `POST /api/rag/system-index/rebuild` 返回 `success` / `vectorEnabled`、系统文档与 chunk 重建前后计数、用户记忆文档与 chunk 重建前后计数、`warnings` / `message`、`boundary` 和可读 `summary`，并确认用户记忆计数未被清空；`boundary` 必须说明只重建系统 problem / knowledge_card 索引、不删除用户 AI 诊断或错题卡记忆；`scripts/e2e_demo_smoke.ps1` 默认跳过重建，需显式传 `-RunRagRebuild` 避免向量 RAG 场景下误触发大量 embedding 调用。
- `POST /api/rag/vector/retry-failed?limit=50` 返回 enabled / requested / effective / attempted / matched / indexed / failed / skipped 计数、`message` 和可读 `summary`；如启用向量 RAG，可重试失败向量索引，且只返回计数摘要。
- RAG 维护成功但 health 回读失败时保留维护摘要，Dashboard 提供“刷新 RAG 状态”继续确认最新 health。
- `/api/rag/chat` 返回受控知识问答答案和 sources。
- `/api/users/{id}/training-plans/trace` 返回最新训练计划窗口 `startDate` / `endDate`、创建时间、计划状态中文文案 `statusLabel`、剩余天数、完成 / 跳过 / 已处理计数、`completionRate` / `handledRate`、来源分布、进度摘要 `progressSummary`、来源摘要 `sourceTypeSummary`、最近动作摘要 `latestActivitySummary` / `latestActivityAt`、下一条待练任务、`nextAction` / `nextActionReason` / `nextActionPriority`、`nextTargetHref` / `nextTargetLabel` 和最近训练动作；`nextItem` 也应带 `targetHref` / `targetLabel` / `sourceType` / `reason`。
- Dashboard 顶部“下一步动作”卡片应展示动作来源 `sourceLabel`，能区分训练计划、模拟面试、模拟面试报告、面试趋势和默认启动项，避免只看到优先级却无法解释推荐来源。
- `/api/users/{id}/training-plans/history` 返回历史计划身份 `id` / `title` / `status` / `statusLabel`、计划窗口 `startDate` / `endDate`、`itemCount` / `completedCount` / `skippedCount` / `pendingCount` / `handledCount`、`completionRate` / `handledRate` 和 `createdAt`，用于说明训练计划不是一次性结果，而是可回看、可追踪的历史闭环。
- `/api/users/{id}/training-plans/activities/recent` 如存在完成 / 跳过记录，返回 `itemId` / `planId` / `planTitle` / `itemType` / `taskTitle` / `knowledgePoint`、`learningImpactSummary`、`sourceType` / `sourceSummary`、`status` / `statusLabel` 和 `statusUpdatedAt`，说明完成会形成改善趋势、跳过只记录训练节奏，并能解释最近训练动作属于哪个计划项、来自哪个来源、何时发生，前端不需要自行猜测状态文案。
- `/api/users/{id}/mock-interviews/trace` 返回最近模拟面试会话、报告、回答轮次、低分回答、弱点事件、训练计划推荐摘要、最近面试时间、最近会话状态文案 `latestSessionStatusLabel`、最近弱点标签、`recommendedCardIds`、`reportTrainingPlanLinked`、报告回看入口 `reportReviewHref` / `reportReviewLabel`，以及 `nextActionReason` / `nextActionPriority` / `nextTargetHref` / `nextTargetLabel`；其中 `reportedSessionCount` / `answeredTurnCount` / `lowScoreTurnCount` / `weaknessEventCount` / `trainingPlanItemCount` 必须可用于解释闭环进度，存在最近会话时还应返回 `latestCategory`，用于同类复测入口。
- `/api/mock-interviews/{sessionId}` 的报告详情如存在 report，应返回 `trainingPlanLinked` / `trainingPlanItemCount` / `reviewPathSummary`，说明这份模拟面试报告是否已经沉淀到训练计划；历史报告恢复时同样要能通过 `sourceType=MOCK_INTERVIEW_REPORT` / `sourceId=reportId` 反查训练项。
- `/api/users/{id}/mock-interviews/trends` 如存在趋势数据，返回知识卡 `knowledgeCardId`、知识点、分类、最近会话 `latestSessionId`、最近得分 `latestScore`、上次得分 `previousScore`、分数变化 `deltaScore`、变化方向 `trendLabel`、面试次数 `interviewCount`、最近面试时间 `lastInterviewAt`、最近卡点，以及 `latestIssueType` / `latestIssueTypeLabel`，用于区分“知识点不会”和“表达不完整”。
- MySQL 中存在 `vector_status='INDEXED'` 的 RAG chunk。
- `GET /api/problems/cache/status` 返回 Redis 题目缓存摘要、`enabled` / `redisAvailable` / `statusLabel`、总 `cachedKeyCount`、列表 / 详情 / 模板 TTL 和 MySQL fallback 边界说明。
- `GET /api/knowledge/cache/status` 返回 Redis 知识卡缓存摘要、`enabled` / `redisAvailable` / `statusLabel`、总 `cachedKeyCount`、分类 / 列表 / 详情 TTL 和 MySQL fallback 边界说明。
- `GET /api/problems/cache/status`、`GET /api/knowledge/cache/status` 和 `GET /api/cache/status` 均返回 `checkedAt` 与 `maintenanceAction`，能说明最近检查时间和下一步维护动作。
- `GET /api/cache/status` 返回统一 `probeWarning`；当 Redis ping 正常但题目或知识卡 key 扫描 / hasKey 探测失败时，统一状态应为 `PARTIAL_DEGRADED`，并提示检查 Redis key scan 权限。
- `GET /api/cache/status` 返回题目 / 知识卡统一缓存摘要，包含 `allEnabled` / `allRedisAvailable` / `cachedKeyCount` / `statusLabel`，`summary` 中包含 `problem=...` 与 `knowledge=...` 子状态，并返回 `cacheBenefitSummary` / `fallbackRiskSummary` / `protectedDataSummary`，能说明只读热点缓存收益、Redis 异常时的 MySQL 回源风险，以及提交、诊断、训练计划、RAG 用户记忆、模拟面试等 durable learning state 不进入 Redis；`POST /api/cache/refresh` 可统一预热只读热点缓存，统一响应返回 `message`、`totalWarmAttemptedCount`、`failedCount`、`refreshScopeSummary`、`warmupResultSummary`、`protectedDataSummary` 和 `boundary`，且 `summary` 中包含 `problem:` / `knowledge:` 子刷新结果，明确刷新只影响 Redis 只读热点响应，不把学习状态放入 Redis。
- smoke 最终输出包含 `goal_coverage`，一行串起 `training=...`、`rag=...`、`mockInterview=...`、`cache=...` 四块证据，方便最终验收时快速确认四个大目标都有当前状态摘要。
- 缓存刷新成功但状态回读失败时保留刷新摘要，Dashboard 提供“刷新缓存状态”继续确认最新缓存状态。
- `POST /api/problems/cache/refresh` 可清理并从 MySQL 预热题目列表、详情和模板缓存，返回 `refreshedAt`、`message`、`totalWarmAttemptedCount`、`failedCount` 和可读 `summary`。
- `POST /api/knowledge/cache/refresh` 可清理并从 MySQL 预热知识卡分类、列表和详情缓存，返回 `refreshedAt`、`message`、`totalWarmAttemptedCount`、`failedCount` 和可读 `summary`。
- Redis 启用时，题目接口会写入 `coach:problem:*` 缓存 key；Redis 不可用时题目接口应降级 MySQL。
- Redis 启用时，知识卡接口会写入 `coach:knowledge:*` 缓存 key；Redis 不可用时知识卡分类、列表和详情接口应降级 MySQL，不影响自测、掌握度或训练计划事实源。

## 4.1 最新通过证据

截至 2026-05-29，本地完整 smoke 已在备用端口环境通过：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\e2e_demo_smoke.ps1 `
  -BackendUrl http://127.0.0.1:8081 `
  -PistonBaseUrl http://127.0.0.1:2238/api/v2 `
  -QdrantUrl http://127.0.0.1:16333
```

关键证据：

- `embedding: scripts/embedding_smoke.ps1 passed`
- `problem_list: count=20, demoIds=1/206/121`
- `failed_submission: ... status=WRONG_ANSWER`
- `accepted_submission: ... status=ACCEPTED`
- `cache: status=READY, allRedisAvailable=True, cachedKeys=168, failedRefresh=0`
- `rag_health` 经向量补偿后为 `HEALTHY`，`vectorPendingChunkCount=0`，`vectorFailedChunkCount=0`
- `qdrant_collection: ai_study_rag_chunks => 200`
- `goal_coverage` 同时包含 `training=...; rag=...; mockInterview=...; cache=...`

## 5. 手动演示顺序

推荐正式演示时按这个顺序：

1. 打开 `/problem/1`，展示题目、预设提示和 Java Solution 模板。
2. 复制 `docs/demo-cases/1-two-sum-bug.java`，提交，展示 failed cases。
3. 观察 Agent timeline，重点讲 `OBSERVATION -> RAG_RETRIEVAL -> ERROR_CLASSIFICATION`。
4. 展示 AI 诊断、弱点记忆和训练计划标题。
5. 打开 `/dashboard`，展示最新训练计划和错题数据已经更新。
6. 复制 `docs/demo-cases/1-two-sum-fixed.java`，提交，展示 AC code review。
7. 打开 `/rag-chat`，提问“两数之和为什么要先查 HashMap 再放当前元素”，展示 sources 来自历史诊断/错题。

## 6. 常见失败处理

- `Piston is not ready`：检查 Piston 容器端口。本机常用 `2238 -> 2000`，后端要设置 `PISTON_BASE_URL=http://localhost:2238/api/v2`。
- `Qdrant is not ready`：运行 `docker compose up -d qdrant`，再跑 `scripts/qdrant_smoke.ps1`。
- `Embedding smoke timed out`：先单独运行 `scripts/embedding_smoke.ps1 -TimeoutSeconds 90`；确认 key、base url、网络和代理。
- `Unknown column vector_status/source_type`：旧库没有执行最新迁移，按本文档第 2 节补迁移。
- `Frontend is not ready`：先跑 `npm run build`；若 dev 端口被 Windows 拒绝，使用 `npx next start -H 127.0.0.1 -p 3000` 验证生产构建。
- PowerShell 本地请求遇到 `502`：优先使用 `curl.exe --noproxy "*"` 访问 localhost。
