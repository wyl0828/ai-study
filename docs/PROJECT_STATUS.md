# AI Interview Coach Agent 当前成果与下一步大纲

## 1. 当前成果

项目已经具备可演示的 MVP 闭环，不再只是题库页面或大模型接口包装。当前核心流程是：

```text
选择题目 -> 编写 Java 代码 -> 提交判题 -> 观察失败用例
-> 触发 Agent 检索题目知识 / 知识卡 / 历史学习记忆 -> AI 诊断
-> 记录错误类型和知识点
-> 更新弱点、错题卡和 3 天训练计划 -> Dashboard 展示学习数据
-> 可进入独立知识训练页复习 Java 后端知识卡片
```

已完成的主要成果：

- **题目与提交闭环**：支持题目列表、题目详情、Java 模板加载、代码提交、Piston 判题、失败用例返回和提交记录持久化。
- **Redis 只读热点缓存**：已接入题目列表、题目详情、题目模板、知识卡分类、知识卡列表和知识卡详情缓存，key 分别使用 `coach:problem:*:v1` 与 `coach:knowledge:*:v1` 前缀，TTL 可通过 `PROBLEM_CACHE_*` / `KNOWLEDGE_CACHE_*` 环境变量调整；`GET /api/cache/status`、`POST /api/cache/refresh` 提供统一缓存维护入口，题目和知识卡各自仍保留独立 status / refresh 接口；状态响应带 `statusLabel` / `summary` / `checkedAt` / `maintenanceAction`、list/detail/template、category/list/detail 分项 key 数量、`probeWarning` 状态探测告警，以及读路径命中率、命中次数、未命中次数、MySQL 回源次数和最近降级原因；统一状态还返回 `cacheBenefitSummary / fallbackRiskSummary / protectedDataSummary`，用于直接说明缓存收益、Redis 异常时的 MySQL 回源风险，以及提交、诊断、训练计划、RAG 用户记忆、模拟面试等 durable learning state 不进入 Redis；刷新响应带 `statusLabel` / `maintenanceAction` / `refreshedAt` / `message` / `totalWarmAttemptedCount` / `failedCount` / 预热摘要，便于排查最近检查、最近刷新、哪一层没预热、失败数量和下一步维护动作；统一状态会合并子缓存状态探测告警，Redis ping 正常但 key 扫描 / hasKey 探测失败时也能把顶层标记为 `PARTIAL_DEGRADED`；Dashboard 已展示“缓存层状态”，可直接看到统一状态、题目 / 知识卡子状态、缓存 key 数、缓存开关整体状态、Redis 整体可用性、最近检查时间、命中率、回源次数、最近降级原因、状态探测告警、维护动作和缓存边界；Dashboard 可直接触发热点缓存刷新并回读缓存状态，刷新结果显示最近刷新摘要；缓存状态暂不可用时 Dashboard 仍保留重试缓存状态入口；热点缓存刷新成功但状态回读失败时保留刷新结果并提示单独刷新状态；Redis 读写失败只记录 warning 并降级 MySQL，不缓存提交、知识卡自测/掌握度、训练计划、RAG 用户记忆、模拟面试或 Agent SSE 状态。
- **Hot100 Solution 模式统一**：当前题库升级为 Hot100 精选 20 题，全部使用 LeetCode 风格 `class Solution`；后端通过 `CodeWrapper` 注册表只包装送入 Piston 的代码，数据库仍保存用户原始代码。
- **题面与题解内容补齐**：20 题均已使用“任务说明 / 返回要求 / 约束与边界”的面试式题面，并补齐三层预设提示、solution outline 和完整 Java 参考实现；参考题解在前端左侧按小节折叠展示，避免长文本一次性铺开。
- **Agent Workflow 后端**：已实现 `InterviewCoachAgent`、`AgentContext`、`AgentStep` 和核心 Tool 链，支持同步分析接口和 SSE 流式诊断接口。`RAG_RETRIEVAL`、`MEMORY_UPDATE` 和 `TRAINING_PLAN` 为非核心步骤，失败不阻塞后续流程。
- **AI 诊断与学习数据**：失败提交后可生成结构化错误类型、知识点、具体错误、失败现象、根本原因、修改方向、面试提醒、改进建议和训练计划；失败现象优先来自 Piston failed case / 编译运行错误摘要，避免把原始 JVM 堆栈直接放进教练报告。诊断后会持久化 `ai_diagnosis`、`user_weakness`、`user_weakness_event`、`mistake_card`、`training_plan` 等数据；`hint_record` 保留为历史兼容表，当前 Agent 流程不写入新 AI hint。
- **AC 代码点评**：提交通过后 Agent 可进入 `CodeReviewTool` 分支，返回复杂度、代码风格、面试表达建议和可优化点，不生成完整答案；前端会在代码通过但点评仍在生成时实时展示 Agent 步骤。
- **前端做题页**：已实现三栏布局：左侧题目、题目预设分层提示和可主动查看的参考题解，中间 Monaco Editor，右侧测试结果和 AI 诊断。提交后通过 SSE 实时展示 Agent 执行步骤，失败时展示诊断结果，AC 时展示轻量代码点评；若用户在未重新提交的情况下修改代码，旧诊断或旧点评继续保留，并显示“基于上次提交，仅供参考”的 stale warning。
- **提示/题解/诊断边界已理顺**：题目通用 Level 1/2/3 提示和参考题解放在左侧，提示与完整 Java 参考实现默认不主动暴露且不调用 AI；右侧 AI 诊断只解释本次提交为什么错，并以教练报告展示失败现象、根本原因、修改方向、面试提醒和推荐训练。
- **Dashboard 真实数据接入**：学习中心已从 mock 数据切到后端真实接口，并从“数据看板”重排为“下一步学习指挥台”：统计卡片之后优先展示今日训练项、基于训练计划追踪和模拟面试闭环生成的下一步动作、训练计划追踪摘要和按天分页的完整训练计划，再展示薄弱排行、错误类型分布、最近提交、最近模拟面试、模拟面试闭环追踪、模拟面试趋势、合并错题卡、缓存层状态、RAG 索引状态和确定性 AI 教练建议；训练计划条目可完成/跳过，也支持手动重新生成；下一步动作会把训练计划项、进行中面试、报告推荐知识卡分别跳到题目 / 知识卡 / 模拟面试详情，并展示 trace 返回的原因和优先级；训练计划追踪、最近模拟面试、模拟面试闭环追踪和趋势首屏加载失败时会降级为空态并保留手动刷新入口，不阻断 Dashboard 主数据，手动刷新时辅助 trace / trend 单点失败也只影响对应卡片。
- **知识训练页 V1**：已新增 `/knowledge` 前端训练页，优先读取后端知识接口和 `knowledge_card` 真实数据；接口失败时回退本地示例数据。页面已从分类卡片列表打磨为“可折叠知识体系大纲 + 专题训练内容区”，支持 Java 核心、数据库、Spring、AI 工程入口，面包屑/左侧高亮/专题标题共用同一状态，Map/List/Set 等专题按前端规则过滤，卡片展示训练状态、最近得分或未自测状态；模拟自测评分、点评反馈、标杆回答解析、高频追问和标记已掌握均可用；自测记录已持久化到后端。
- **后端知识训练一期能力**：后端已有 `KnowledgeController`、`KnowledgeCardService` 和 `knowledge_card` 表；`data/knowledge_cards.sql` 提供 120 张原创整理的 Java 后端与 AI 工程面试知识卡，侧边栏 24 个最终专题均不少于 5 张。知识卡内容源已收口到 `scripts/knowledge_card_profiles.cjs`，由 `scripts/generate_knowledge_cards_sql.cjs` 同步生成 `data/knowledge_cards.sql` 和 `frontend/lib/knowledgeSeed.ts`。
- **知识卡内容质量整改**：120 张卡片已从批量模板问答整改为真实面试训练卡，问题改为直白短问法，答案围绕定义、机制、边界和常见坑；停用 `enrichAnswer` 自动扩写，不再用“结合后端项目”“从几个层面说明”等套话凑字数。`frontend/lib/knowledge-tree-coverage.node-test.cjs` 已加入问题模板、答案污染、空泛 keyPoints、followUps、SQL 与前端 fallback 一致性，以及 Spring Bean 生命周期、布隆过滤器、HashMap、ArrayList、Spring 事务、MySQL MVCC 等高风险卡关键词护栏。
- **RAG 内部检索层**：已新增 `rag_document` / `rag_chunk` MySQL 表、`RagService` 和 `RagRetrieveTool`，在 `OBSERVATION` 后检索题目、知识卡、AI 诊断和当前用户错题记忆；默认 MySQL-only，可选通过 `RAG_VECTOR_ENABLED=true` 启用 MySQL + Qdrant 混合检索。embedding 或 Qdrant 失败会降级为 MySQL-only，不阻塞核心闭环。`GET /api/rag/health` 已作为受控维护接口返回索引健康摘要、启用题目 / 知识卡与系统索引缺失计数、题目 / 知识卡过期系统文档计数、document / chunk 来源类型分布、向量状态计数、`maintenanceActions` 建议、`preferredMaintenanceAction` 和 `nextMaintenanceEndpoint`，并通过 `maintenancePriority / maintenanceReason` 解释为什么表缺失会阻塞、系统索引问题优先于向量补偿、仅向量失败时只需中优先级重试；Dashboard 已展示 RAG 索引状态、系统 chunk、用户记忆 chunk、向量 failed / pending、warning 数、下一维护接口、维护摘要、维护优先级、维护原因和首选动作；Dashboard 可直接触发系统索引重建和失败向量重试，执行后会刷新 RAG health；RAG health 暂不可用时 Dashboard 仍保留重试 RAG 状态入口；RAG 维护动作成功但 health 回读失败时保留维护结果并提示单独刷新状态；`POST /api/rag/system-index/rebuild` 可重建 `user_id IS NULL` 的系统题目 / 知识卡索引并保留用户记忆，`POST /api/rag/vector/retry-failed` 可在 embedding / Qdrant 恢复后重试 `vector_status=FAILED` 和空状态待索引 chunk；这些接口都不暴露 raw retrieval。
- **Qdrant 本地 smoke**：根目录 `docker-compose.yml` 提供 `qdrant/qdrant:latest`，`scripts/qdrant_smoke.ps1` 可启动 Qdrant 并验证 REST health、collection 创建、向量 upsert、query 和清理；脚本使用 `curl.exe --noproxy "*"` 避免本机代理导致 `localhost:6333` 误报 `502`。
- **Embedding smoke**：`scripts/embedding_smoke.ps1` 可在配置 `EMBEDDING_API_KEY` 后验证 OpenAI-compatible `/v1/embeddings` 返回真实向量；未配置 key 时不会伪造通过。
- **端到端演示 smoke**：`scripts/e2e_demo_smoke.ps1` 已固化真实联调流程，会检查 MySQL、Piston、Qdrant、Embedding、后端、前端，并自动跑 Two Sum 错误提交 SSE 诊断、AC 代码点评、Dashboard、RAG Chat 和向量落库检查；最终验收清单见 `docs/FINAL_ACCEPTANCE_CHECKLIST.md`。
- **知识库问答 V1**：已新增 `/rag-chat` 和 `POST /api/rag/chat` 作为受控学习资料问答入口；它只回答题目、知识卡、历史诊断、错题卡和当前用户学习记录相关问题，复用内部 RAG 与现有学习记忆数据，不接入联网搜索、不上传文档、不生成完整 AC 代码、不替代代码提交诊断主流程。
- **模拟面试 V1**：已新增 `/mock-interview` 和 `POST /api/mock-interviews` / `GET /api/mock-interviews/{sessionId}` / `POST /answers` / `POST /finish`，把知识卡升级为面试会话；后端通过显式状态机管理主问题、追问、评分、薄弱点事件和报告，前端以“模拟面试”为主导航入口，支持通过 `sessionId` 恢复会话，Dashboard 可展示最近面试并跳转继续或查看报告，也能按知识点展示多次面试的得分趋势。
- **模拟面试复盘闭环**：模拟面试报告生成后只会把低分、缺失要点或表达问题对应的推荐知识卡写入训练计划；强回答且无缺口的报告不强行生成推荐卡或训练计划，trace 摘要会说明无待复盘推荐卡、无需生成复盘训练计划，并直接进入同类面试复测。报告页会把推荐知识卡链接到 `/knowledge?cardId=...`，并给出“复习推荐卡 -> 回 Dashboard 看下一步动作 -> 再做同类面试复测”的复盘路径；报告详情现在返回 `trainingPlanLinked / trainingPlanItemCount / reviewPathSummary`，历史报告恢复时也会通过训练计划 item 的 `sourceType=MOCK_INTERVIEW_REPORT` 和 `sourceId=reportId` 反查这份报告是否已沉淀为训练项；Dashboard 下一步动作和模拟面试闭环卡也会承接最近报告推荐卡、报告详情或同类复测入口；如果最近报告没有推荐知识卡但存在低分回答，则 trace 会指向报告详情，提示先回看薄弱标签和缺失要点。“我忘了/不知道”等回答会走收窄追问的本地兜底评价，不升级为更难追问，保证面试训练可恢复。`GET /api/users/{id}/mock-interviews/trace` 已提供最近会话、报告、低分回答、弱点事件、报告推荐训练项、报告是否接入训练计划、`closureStatus` / `closureStatusLabel`、`nextAction`、`nextActionReason` 和 `nextActionPriority` 闭环建议；Dashboard 在最近面试、闭环追踪或趋势为空时仍可手动刷新面试闭环数据。
- **训练计划接入知识卡片**：`training_plan_item` 已支持 `PROBLEM` 和 `KNOWLEDGE_CARD` 两类条目；Agent 自动训练计划保留 3 条算法复盘项并最多选取 3 张 RAG 命中的知识卡，手动重新生成会创建 3 天计划且每天包含 1 个算法复盘任务和 1 个知识卡复习任务，不根据算法错因强行推荐八股。`GET /api/users/{id}/training-plans/trace` 已提供最新计划的完成率、已处理率、计划创建时间、已运行天数、剩余天数 / 逾期状态、来源分布、下一条待练任务、后端 `nextAction` / `nextActionReason` / `nextActionPriority` 建议、最近完成 / 跳过动作、最近推进时间和最近动作摘要，Dashboard 训练计划追踪卡会直接展示这些节奏证据，并可直接进入下一项训练；训练追踪暂不可用时 Dashboard 仍保留刷新训练追踪入口，便于演示训练闭环可追踪。
- **学习记忆连续化**：失败诊断会写入弱点事件，错题卡按 fingerprint 合并重复错误；知识卡自测写入 `self_test_record` 并更新 `user_knowledge_card_mastery`，低分自测也会进入弱点事件。
- **固定演示样例**：主线演示已切换为 `1 两数之和`、`206 反转链表` 和 `121 买卖股票的最佳时机`，使用说明见 `docs/DEMO_CASES.md`。
- **登录与用户隔离**：已新增最小化用户名/密码登录系统，每个测试者有独立的学习数据。后端使用 HMAC-SHA256 JWT 认证，前端通过 localStorage 存储 token；`AuthInterceptor` 拦截所有 `/api/**` 请求（公开接口除外），OPTIONS 预检请求自动放行；`SubmissionController`、`AgentController`、`UserController`、`RagChatController`、`MockInterviewController` 均已接入认证用户上下文，提交归属校验、用户数据隔离和 RAG 用户记忆隔离均已生效。登录是 demo 级测试账号系统，不是完整身份平台：邮箱验证、密码重置、OAuth、角色管理和权限控制不在 MVP 范围内。

## 1.1 最新验收状态

截至 2026-05-29，本轮四个工程目标已经完成并通过真实联调：

- **训练闭环质量增强**：完整 smoke 已跑通 Two Sum 错误提交，返回 `WRONG_ANSWER`，SSE Agent 步骤完成 `PLANNING -> CODE_EXECUTION -> OBSERVATION -> RAG_RETRIEVAL -> ERROR_CLASSIFICATION -> MEMORY_UPDATE -> TRAINING_PLAN -> COMPLETED`，并生成可追踪训练计划。
- **RAG 内部可维护性增强**：`GET /api/rag/health` 经向量补偿后返回 `healthy=true`、`statusLabel=HEALTHY`、`vectorPendingChunkCount=0`、`vectorFailedChunkCount=0`、`vectorIndexedChunkCount=573`，维护摘要为无需后续动作。
- **模拟面试闭环增强**：完整 smoke 的 `goal_coverage` 已包含 `mockInterview=...`，Dashboard 可读取最近会话、闭环状态、报告回看入口、弱点标签和趋势摘要。
- **缓存层补完整**：真实 Redis 已验证，`GET /api/cache/status` 返回 `status=READY`、`allRedisAvailable=True`、`cachedKeys=168`；统一 refresh 从 MySQL 预热题目和知识卡只读热点 key，`failedRefresh=0`；缓存命中或刷新成功后会清空已恢复的 `lastFallbackReason`，只保留累计 fallback 计数作为历史观测。

本轮最终通过命令：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\e2e_demo_smoke.ps1 `
  -BackendUrl http://127.0.0.1:8081 `
  -PistonBaseUrl http://127.0.0.1:2238/api/v2 `
  -QdrantUrl http://127.0.0.1:16333
```

同时通过的回归包括：

- `scripts/embedding_smoke.ps1`
- `mvn -q "-Dtest=RedisProblemCacheServiceTest,RedisKnowledgeCardCacheServiceTest,UserLearningServiceImplTest,RagServiceImplTest" test`
- `npx tsc --noEmit`
- `frontend/lib/local-dependency-preflight.node-test.cjs`
- `frontend/lib/core-loop-stability.node-test.cjs`
- `frontend/lib/learning-view.node-test.cjs`

## 2. 当前进度判断

整体进度处于 **Phase 5 产品打磨收口完成、进入最终演示准备前状态**。知识训练页 V1、核心闭环回归护栏、RAG 内部检索与 Qdrant 混合检索、模拟面试闭环、训练计划追踪和 Redis 热点缓存层均已有真实接口、Dashboard 展示、维护入口和 smoke 证据。SSE 流式诊断已接入前端，Agent 步骤实时展示；完整 demo 复盘已有 `scripts\e2e_demo_smoke.ps1`、`scripts\local_dependency_preflight.ps1` 和 `docs\FINAL_ACCEPTANCE_CHECKLIST.md` 作为演示前验收入口。

从简历项目角度看，当前已经能讲清楚这些亮点：

- Spring Boot 分层设计：Controller、Service、Mapper、Agent、Tool、Integration 分包清晰。
- MyBatis-Plus + MySQL：题目、提交、诊断、弱点、弱点事件、错题卡、训练计划、后端知识卡、自测记录、知识卡掌握度和 RAG 文档 / chunk 均有持久化模型。
- Piston 执行服务封装：Controller 不直接调用外部判题服务，代码执行通过 `JudgeService` 抽象。
- Agent 工程化：代码执行和 RAG 检索都是 Tool，判题结果是 Observation，错误诊断、学习更新和训练计划由 Agent Workflow 串联。
- SSE 能力：前端已通过 `fetch + ReadableStream` 接入 SSE，实时展示 Agent 每一步执行过程。
- 学习闭环：一次失败提交能影响弱点趋势、合并错题卡、今日训练项和完整训练计划，不是一次性 AI 文本回答。
- 训练计划统一管理：算法题训练和后端知识卡片复习能在同一个计划中展示，但算法诊断和八股知识保持来源边界。

当前不建议继续扩大的方向：

- 不急着扩到完整 Hot100，先保证 Hot100 精选 20 题中的主 demo 题能稳定演示。
- 不急着做多语言、Docker 沙箱、复杂登录权限、语音/视频面试。
- 不急着把前端做成完整 IDE，当前 Monaco + 提交 + 诊断已经够支撑简历演示。

## 3. 主要风险

- **AI 诊断稳定性**：模型可能输出不稳定 JSON 或分类不准，需要准备固定 bug 样例和验证脚本。
- **本地依赖较多**：MySQL、Piston、后端、前端都要启动；Redis 已用于题目和知识卡热点缓存，但不是核心事实源，Redis 不可用时题目和知识训练接口会降级 MySQL。
- **SSE 稳定性**：SSE 已接入前端，并补充了 `agentStreamState` 状态决策和源码级回归测试；AC 代码点评分支也已展示实时 Agent 步骤。正式演示前仍建议用连续提交、用户中断和后端不可达场景做一次手动压测。
- **题目内容已迁移到后端**：`problem` 表存储 `hint_level1/2/3` 和 `solution_outline`，通过 `ProblemDetailVO.presetHints` 与 `solutionOutline` 返回。
- **知识数据导入依赖**：新库可直接执行 `data/schema.sql` 和 `data/knowledge_cards.sql`；旧库需要先执行 `data/knowledge_training_migration.sql`、`data/learning_memory_continuity_migration.sql`、`data/rag_mysql_migration.sql`、`data/rag_vector_migration.sql` 和 `data/mock_interview_migration.sql`，再执行 `data/knowledge_cards.sql`。导入新知识卡后要通过 `RagService.rebuildSystemIndex()` 或等价维护流程重建系统 RAG 索引，避免 Agent 检索到旧知识卡 chunk；如启用向量 RAG，还要先启动 `docker compose up -d qdrant` 并配置 `EMBEDDING_*`。
- **知识卡内容边界**：当前知识卡参考小林 coding 和 JavaGuide 选题覆盖并重新整理，可作为 RAG V1 的系统知识来源；`/knowledge` 页面是知识训练入口，`/rag-chat` 是受控学习资料问答入口，两者都不是通用聊天或公开 raw retrieval。后续维护时不要恢复自动扩写或模板化问题，问题、答案、keyPoints、followUps 都应在内容源里显式维护。
- **文档与代码容易漂移**：提示/诊断边界已调整，后续修改接口或页面时要同步更新 `docs/API.md` 和设计文档。

## 4. 下一步大纲

### 4.1 第一优先级：最终演示前收口

当前四个工程目标已经完成，下一步不再继续扩功能，优先做最终演示前收口：

- 固定最终演示环境启动顺序：`local_dependency_preflight` -> 后端 / 前端 -> `e2e_demo_smoke`。
- 录制前再跑一次完整 smoke，确认 `goal_coverage` 中 training / rag / mockInterview / cache 四块都有最新证据。
- 针对 `1 / 206 / 121` 各准备一个失败样例和一个 AC 样例，避免演示时临场写错。
- 采集题目页、AI 诊断、Dashboard、RAG health、缓存状态和模拟面试报告截图。
- 准备一页面试讲解稿：Agent Workflow、RAG 可维护、Redis 缓存边界、训练计划追踪、模拟面试闭环。

以下条目保留为已完成工程能力的维护口径：

- Redis 缓存层继续补完整：
  - 题目和知识卡缓存已经接入，refresh 结果已返回可读 `statusLabel`、`maintenanceAction`、`summary`、`refreshedAt`、预热尝试数量、失败数量和 Redis 不可用时的跳过原因。
  - 状态接口已返回 `statusLabel`、`summary`、`checkedAt`、`maintenanceAction`、命中率、回源次数和最近降级原因，能直接说明 ready / degraded / disabled、最近检查时间、读路径效果和下一步维护动作。
  - Dashboard 已接入 `GET /api/cache/status`，展示统一缓存状态、题目 / 知识卡子状态、缓存 key 数、缓存开关整体状态、Redis 整体可用性、最近检查时间、状态探测告警、维护动作和“只缓存只读热点、学习数据仍以 MySQL 为事实源”的边界。
  - 保持统一维护入口 `GET /api/cache/status`、`POST /api/cache/refresh`，题目 / 知识卡独立入口作为排查入口。
  - 文档继续明确 key、TTL、缓存边界、MySQL 回源和 Redis 故障降级。
- RAG 维护入口继续增强：
  - `GET /api/rag/health` 继续作为内部排查入口，只返回索引健康摘要和 `maintenanceActions`，覆盖题目 / 知识卡系统文档缺失、过期、重复和向量待补，不暴露 raw retrieval。
  - Dashboard 已接入 `GET /api/rag/health`，展示 RAG 索引状态、系统 chunk、用户记忆 chunk、向量 failed / pending、warning 数、下一维护接口、维护摘要和首选动作，并可直接触发系统索引重建和失败向量重试；接口失败时不阻断训练闭环主数据，空状态下也保留重试 RAG 状态入口。
  - 系统索引重建和失败 / 待补向量重试继续保持可控维护接口。
  - `RagServiceImplTest` 已覆盖同题目 / 同知识点排序、用户记忆隔离、向量相似度提升排序和向量 only hit 合并后的 MySQL 二次隔离；`QdrantRagVectorStoreTest` 已覆盖 Qdrant payload filter 的系统 chunk / 当前用户 chunk 条件。
- 训练闭环继续可追踪：
  - `GET /api/users/{id}/training-plans/trace` 继续承接完成率、已处理率、剩余天数 / 逾期状态、最近动作、下一条待练任务、下一步原因和优先级。
  - Dashboard 训练计划追踪卡已展示计划已运行天数、创建时间和最近推进时间，并可直接进入下一项训练；trace 空状态下也保留刷新训练追踪入口，便于判断训练节奏是否持续。
  - Dashboard 训练计划追踪卡可直接进入下一项训练，避免 trace 只展示目标但还要用户手动找入口。
  - Dashboard 继续从训练计划和模拟面试 trace 生成下一步动作，不回退 mock 数据，并展示为什么优先该动作。
  - Dashboard 对无训练计划、无最近训练活动等状态保留明确空状态文案，并由前端稳定性 guard 防止回退 mock 数据或空白面板。
- 模拟面试闭环继续细化：
  - 保持会话恢复、报告详情、问题回答才推荐知识卡进入训练计划、同知识点趋势，以及 trace 里的结构化闭环状态、下一步原因 / 优先级。
  - Dashboard 模拟面试闭环卡已展示最近方向和最近面试时间，让报告复盘、训练计划推荐和同类复测能从“最后一次面试”串起来看；最近面试、闭环追踪和趋势空状态都可手动刷新面试闭环数据。
  - Dashboard 模拟面试闭环卡可直接进入面试复盘或同类复测，避免 trace 只展示目标但还要用户手动找入口。
  - 低分回答和“分数合格但仍有缺失点”的回答都会写入 `user_weakness_event(sourceType=MOCK_INTERVIEW)`，保证面试训练继续沉淀到弱点事件。
  - 报告页继续引导“复习推荐知识卡 -> 回 Dashboard 看下一步动作 -> 再复测同类面试”。

### 4.2 第二优先级：稳定核心闭环继续作为回归底线

- 保持问题页边界：
  - 左侧只放题目描述和题目预设 Level 1/2/3 提示。
  - 右侧只放测试结果和 AI 诊断 / AC 点评，不重新引入右侧“分层提示”tab。
- 稳住 SSE 流程：
  - 已用 streamId、AbortController 和回归测试覆盖多次提交、旧流覆盖、用户中断和 fallback 同步分析这些边界。
  - 保持 SSE 为前端主路径，同步 `POST /api/agent/analyze` 只作为 fallback。
- Dashboard 数据继续收口：
  - 统计、薄弱点、错题卡、错误分布、训练计划继续来自 MySQL。
  - 最近模拟面试列表来自 `GET /api/users/{userId}/mock-interviews/recent`，展示会话摘要和报告弱点标签，不回退 mock 数据。
  - 模拟面试趋势来自 `GET /api/users/{userId}/mock-interviews/trends`，按知识卡展示最近得分、上次得分、变化方向、最近卡点，以及最近卡点属于知识点不会还是表达不完整。
  - 无数据时显示空状态，不回退 mock 数据。
  - 当前前端已把训练计划前置为“今日优先训练 + 下一步动作 + 完整训练计划”，下一步动作由训练计划追踪和模拟面试闭环确定性生成；完整计划按第 1/2/3 天分页展示；错误统计只保留错误类型分布，错题卡按同类错误模式聚合展示本质问题、修复动作和复盘口令，AI 教练建议由现有弱点、训练计划和错题数据确定性生成，不新增后端接口。

当前落地状态：

- `ProblemWorkspace` 继续以 `agentApi.streamDiagnosis()` 为主路径；同步 `POST /api/agent/analyze` 只在 SSE error、SSE 正常结束但无 done、或 done 数据无效时作为 fallback。
- `frontend/lib/agentStreamState.ts` 收口 streamId 新鲜度判断和 fallback 触发条件；再次提交和组件卸载会中断当前流，旧流的 step / done / error / end 不覆盖新提交状态。
- `frontend/lib/core-loop-stability.node-test.cjs` 覆盖右侧 tab 边界、左侧题目预设提示、SSE fallback 条件、旧流拦截、abort 行为、Dashboard 不导入 mock、最近模拟面试、模拟面试趋势、训练计划跳转、今日优先训练和下一步动作前置、空状态文案。

### 4.3 第三优先级：20 题训练集只做稳定化

- 不继续扩到完整 Hot100，先保持精选 20 题 seed / wrapper / template / hint / solution outline 一致。
- 固定演示题继续以 `1`、`206`、`121` 为主，后续只补高价值典型错误样例。
- `CodeWrapper` 继续只包装送入 Piston 的代码，`submission.code` 仍保存用户原始 `class Solution`。

### 4.4 暂不做但保留

- 单独 hint 查询接口。
- 单独 accepted-code review REST 接口。
- 知识卡收藏。
- 通用 RAG 聊天 / 公开 raw RAG 检索接口。
- 用户训练状态、知识卡自测 / 掌握度、RAG 用户记忆、模拟面试和 Agent SSE 状态不放入 Redis 作为唯一存储。

### 4.5 最终阶段再考虑

- 跑通 `1/206/121` 三题完整 demo 并记录失败点。
- 对齐 `docs/API.md`、`docs/IMPLEMENTATION_PLAN.md`、`docs/AI-Interview-Coach.md` 中细节漂移的接口和流程描述。
- 针对 `1/206/121` 的失败诊断和 AC 点评各录一次短流程：验证 AC 点评生成中有实时步骤，修改代码但不提交时旧点评仍保留并提示仅供参考。
- 明确 `hint_record` 的最终策略。
- 采集题目页、AI 诊断、Dashboard、知识训练页截图，必要时录制 60-90 秒演示视频。
- 准备一页 Agent Workflow 面试 Q&A。

### 4.6 暂缓事项

- 多语言支持。
- Docker 沙箱替换 Piston。
- 大规模题库扩展。
- 用户登录、权限和多租户。
- 复杂图表和 UI 动画。
- 多 Agent 协作。

## 5. 推荐近期任务顺序

```text
1. Redis 缓存状态 / refresh 结果继续补维护可观测性，并保持测试 / API 文档同步
2. 训练计划 trace 和 Dashboard 下一步动作继续回归，确保无 mock 回退
3. RAG health / rebuild / retry-failed 维护入口继续补排查口径和测试
4. 模拟面试 trace / report / 推荐知识卡 / 训练计划联动继续补闭环验证
5. 精选 20 题只补稳定性和典型错误样例，不扩完整 Hot100
6. 最终阶段才做完整 demo 复盘、截图录屏、文档大范围润色和面试 Q&A
```

当前最应该保护的是核心闭环边界，而不是继续堆功能。最终演示阶段再集中处理完整 demo 复盘、截图录屏、文档细节对齐和面试 Q&A。
