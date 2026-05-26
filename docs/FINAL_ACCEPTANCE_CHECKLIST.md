# 最终项目验收清单

本文档用于演示前验收 AI Interview Coach Agent 是否处于可复现、可讲解、可录制状态。

## 1. 环境前置条件

- MySQL 8 已启动，数据库为 `ai_interview_coach`。
- Piston 已启动，并且 Java runtime 可用。
  - 本机常用地址：`http://127.0.0.1:2238/api/v2`
  - 验证：`curl.exe --noproxy "*" http://127.0.0.1:2238/api/v2/runtimes`
- Qdrant 已启动。
  - 启动：`docker compose up -d qdrant`
  - 验证：`curl.exe --noproxy "*" http://127.0.0.1:6333/healthz`
- Redis 可选但建议启动，用于题目热点缓存。
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

- 最新训练计划为 `ACTIVE`，且包含训练项。
- Dashboard stats 返回真实学习统计。
- `/api/rag/chat` 返回受控知识问答答案和 sources。
- MySQL 中存在 `vector_status='INDEXED'` 的 RAG chunk。
- Redis 启用时，题目接口会写入 `coach:problem:*` 缓存 key；Redis 不可用时题目接口应降级 MySQL。

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
