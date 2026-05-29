# 下一阶段目标文档

## 总目标

把当前项目从“核心闭环已跑通”推进到“学习训练体系可持续运转”。

下一阶段不以面试包装、截图录屏或继续堆页面为主，而是围绕 6 个产品工程目标继续推进：

```text
20 题稳定训练集
-> 训练计划可追踪
-> RAG 内部可维护
-> 模拟面试历史和报告详情
-> 受控知识问答增强
-> Redis 题目 / 知识卡热点缓存
```

这些目标共同服务一个方向：用户的每次做题、诊断、自测、模拟面试和问答，都能沉淀为可追踪、可复盘、可继续训练的学习数据；Redis 缓存作为工程增强，只加速题目和知识卡只读热点接口，不改变学习数据事实源。

## 当前实施进度

截至 2026-05-29，当前工作树已推进到以下状态：

- 目标 1：20 题 seed、模板、三层提示、solution outline 和 20 个 `CodeWrapper` adapter 注册已有回归；新增 wrapper 编译运行测试覆盖三数之和、合并区间、岛屿数量和二叉树中序遍历的输出格式。固定演示样例已覆盖 `1`、`206`、`121`。
- 目标 2：训练项来源、训练计划历史、最近训练完成情况、训练计划追踪摘要、训练活动学习影响摘要和训练首次完成后的弱点改善事件已落地；重复提交 `COMPLETED` 不会重复降低薄弱点分数；`GET /api/users/{id}/training-plans/trace` 可一次返回最新计划完成率、已处理率、剩余天数 / 逾期状态、来源分布、下一条待练任务、下一步原因 / 优先级、最近训练动作和完成 / 跳过后的学习影响；Dashboard 训练追踪首屏失败会降级为空态，手动刷新时 trace 单点失败只影响训练追踪卡，不阻断最新计划、历史和最近动作刷新。
- 目标 3：MySQL RAG 健康检查、`GET /api/rag/health` 受控维护入口、失败向量重试 `POST /api/rag/vector/retry-failed`、系统索引重建保护、题目 / 知识卡过期系统文档检测、matchReason、用户隔离、Agent step 命中来源摘要与可选 Qdrant 混合检索代码级切片已落地；RAG health 已返回 `maintenancePriority / maintenanceReason`，能解释表缺失为何阻塞、系统索引为何优先于向量补偿、仅向量异常时为何只需重试失败 / 待补向量；Dashboard 可直接触发系统索引重建、失败向量重试、刷新 RAG 状态和空态重试，维护动作成功但 health 回读失败时保留维护结果并提示单独刷新；Qdrant 默认关闭，embedding / Qdrant 失败会降级 MySQL-only。本地 Qdrant REST smoke 已可通过 `scripts/qdrant_smoke.ps1` 验证；真实 embedding API 可在配置 `EMBEDDING_API_KEY` 后通过 `scripts/embedding_smoke.ps1` 验证。
- 目标 4：模拟面试最近列表、会话恢复、报告详情复盘、报告推荐进入训练计划和闭环追踪摘要已落地；报告详情已返回 `trainingPlanLinked / trainingPlanItemCount / reviewPathSummary`，可说明这份报告是否已沉淀为训练计划项，历史报告恢复时也能按 `MOCK_INTERVIEW_REPORT` 来源反查训练项；`GET /api/users/{id}/mock-interviews/trace` 可检查最近会话、报告、低分回答、弱点事件、训练计划推荐、结构化 `closureStatus` 和“报告 -> 推荐知识卡 -> 训练计划项 -> 下一步跳转”的复盘链路是否连上，并返回下一步原因 / 优先级解释为什么此刻继续会话、复盘推荐卡、回看报告或复测同类知识点；Dashboard 最近面试、闭环追踪和趋势首屏失败会降级为空态，手动刷新时单个 trace / trend 失败只影响对应卡片。
- 目标 5：`/rag-chat` 已加固为受控学习资料问答入口，覆盖学习记录查询、依据摘要、完整 AC 代码拒答和未提交代码诊断引导。
- 工程增强：Redis 已接入题目列表、题目详情、题目模板、知识卡分类、知识卡列表和知识卡详情热点缓存，key 为 `coach:problem:*:v1` / `coach:knowledge:*:v1`；`GET /api/cache/status` 可查看启用状态、Redis ping、key 模式、TTL、`checkedAt`、命中率、回源次数、最近降级原因、状态探测告警 `probeWarning` 和 `maintenanceAction`，统一状态会合并子缓存状态探测告警，并返回 `cacheBenefitSummary / fallbackRiskSummary / protectedDataSummary` 来说明缓存收益、MySQL 回源风险和 durable learning state 保护边界；`POST /api/cache/refresh` 以及题目 / 知识卡独立 refresh 可清理并从 MySQL 预热缓存且返回 `statusLabel`、`maintenanceAction`、`refreshedAt`；Dashboard 已展示缓存层统一状态、题目 / 知识卡子状态、key 数、命中率、回源次数、状态探测告警、维护动作和缓存边界，可直接刷新热点缓存、刷新缓存状态和空态重试；热点缓存刷新成功但状态回读失败时保留刷新结果并提示单独刷新状态；Redis 读写失败会降级 MySQL；训练数据、RAG 用户记忆、模拟面试和 Agent SSE 状态不放入 Redis 作为唯一存储。
- 最终联调：`scripts/e2e_demo_smoke.ps1` 已在真实 MySQL / Piston / Redis / Qdrant / Embedding / 后端 / 前端环境通过，并输出 `goal_coverage` 串起 training / rag / mockInterview / cache 四块证据；RAG 向量补偿已推进到 `HEALTHY`，`vectorPendingChunkCount=0`、`vectorFailedChunkCount=0`、`vectorIndexedChunkCount=573`；Redis 统一缓存状态已验证 `READY`、`cachedKeys=168`、refresh `failed=0`；历史训练活动会用 `LEGACY_TRAINING_PLAN` 和计划创建时间兜底来源与动作时间，避免旧数据破坏追踪解释。

## 从当前状态继续的可执行目标

当前四个工程目标已完成，下一步进入最终演示前收口。后续不再优先扩功能，而是保持已落地功能的可维护性、可观测性和回归保护。

### 近期目标 A：缓存层可讲、可维护、可验收

目标：把 Redis 从“代码里已经接入”补成面试中能讲清楚的缓存层：缓存什么、不缓存什么、如何刷新、Redis 挂了怎么办、如何证明命中范围。

可执行小目标：

- 给题目缓存、知识卡缓存、统一缓存刷新结果补充可读 `summary`，展示本次预热尝试数量、失败数量和跳过原因。✓
- 给题目缓存、知识卡缓存、统一缓存刷新结果补充 `statusLabel` 和 `maintenanceAction`，让独立排查入口也能直接区分 READY / PARTIAL_FAILED / SKIPPED。✓
- 给题目缓存、知识卡缓存、统一缓存状态补充 `statusLabel` 和 `summary`，展示 Redis 是否 ready / degraded / disabled 以及当前 key 数和 TTL。✓
- 保持 Redis 只缓存题目 / 知识卡只读热点响应，不缓存提交、诊断、训练计划、知识卡自测 / 掌握度、RAG 用户记忆、模拟面试或 SSE 状态。✓
- 给题目缓存、知识卡缓存、统一缓存状态补充 `checkedAt` 和 `maintenanceAction`，刷新结果补充 `refreshedAt`，并在状态接口返回 list/detail/template 与 category/list/detail 分项 key 数量，让维护接口能直接说明“何时检查 / 何时刷新 / 哪一层缓存没预热 / 下一步怎么处理”。✓
- 给题目缓存、知识卡缓存、统一缓存状态补充 `probeWarning`，Redis ping 正常但 key 扫描 / hasKey 探测失败时返回 `PARTIAL_DEGRADED`，并把子缓存告警合并到统一状态。✓
- 给统一缓存状态补充 `cacheBenefitSummary / fallbackRiskSummary / protectedDataSummary`，让 Dashboard 和 smoke 能直接说明缓存收益、Redis 异常时的 MySQL 回源风险，以及提交、诊断、训练计划、RAG 用户记忆、模拟面试等数据不进入 Redis。✓
- Dashboard 接入 `GET /api/cache/status`，展示统一缓存状态、题目 / 知识卡子状态、缓存 key 数、维护动作和“只缓存只读热点、学习数据仍以 MySQL 为事实源”的边界；接口失败时不阻断训练闭环主数据。✓
- Dashboard 可直接触发 `POST /api/cache/refresh`，刷新后回读状态；状态暂不可用时保留重试缓存状态入口，刷新成功但状态回读失败时保留刷新摘要并提示单独刷新状态。✓
- 保持 `GET /api/cache/status` / `POST /api/cache/refresh` 作为统一维护入口，题目和知识卡独立入口作为排查入口；统一 refresh 摘要会带上 problem / knowledge 子缓存跳过或失败原因。✓
- 为 Redis 不可用、缓存关闭、刷新失败、MySQL 回源成功分别保留测试。✓
- 文档中明确缓存 key、TTL、降级策略和演示命令。✓

验收方式：

- `CacheMaintenanceControllerTest`
- `ProblemServiceImplTest`
- `KnowledgeCardServiceImplTest`
- `RedisProblemCacheServiceTest`
- `RedisKnowledgeCardCacheServiceTest`
- `frontend/lib/core-loop-stability.node-test.cjs`
- `scripts/e2e_demo_smoke.ps1` 中 `cache=status=READY, allRedisAvailable=True, cachedKeys=168, failedRefresh=0`

### 近期目标 B：训练闭环追踪继续收口

目标：让 Dashboard 的“下一步学习指挥台”不仅能展示计划，还能说明为什么推荐、做到了哪一步、下一步去哪里。

可执行小目标：

- 训练计划条目继续稳定返回 `sourceType`、推荐原因、条目类型和跳转目标，前端优先使用后端 `targetHref` / `targetLabel`。✓
- `GET /api/users/{id}/training-plans/trace` 继续作为训练计划追踪总入口，覆盖完成率、已处理率、剩余天数 / 逾期状态、最近动作、下一条待练任务、下一步原因 / 优先级、进度摘要、来源摘要和跳转目标。✓
- Dashboard 下一步动作继续优先承接进行中模拟面试、训练计划待办、报告推荐知识卡和低分知识点复测；进行中模拟面试优先于训练计划待办展示。✓
- Dashboard 下一步动作复用 trace 的 `nextActionReason` / `nextActionPriority`，展示“为什么此刻优先做这个”，避免只给跳转链接。✓
- 补足空状态和无计划状态，避免前端回退 mock 数据。✓
- 训练追踪首屏失败降级为空态；手动刷新训练状态时 trace 单点失败只影响追踪卡，不阻断最新计划、历史和最近动作刷新。✓
- 训练完成 / 跳过后继续返回可解释 `learningImpactSummary`；完成项在有同知识点薄弱记录且首次从非完成状态转为完成时写入弱点改善事件，重复完成不重复写事件，跳过项只记录训练节奏。✓
- 弱点趋势继续区分“最近加重”“最近改善”“持续薄弱”：最近事件 `deltaScore > 0` 显示加重，训练完成等 `deltaScore < 0` 显示改善；没有近期事件但弱点分仍高时显示持续薄弱。✓

验收方式：

- `UserLearningServiceImplTest`
- `UserControllerTest`
- `npx tsc --noEmit`
- `frontend/lib/core-loop-stability.node-test.cjs`

### 近期目标 C：RAG 维护入口变成日常排查工具

目标：RAG 继续保持内部 Agent Tool，不变成公开 raw retrieval；但维护入口要能告诉开发者索引是否健康、缺什么、下一步该执行哪个维护动作。

可执行小目标：

- `GET /api/rag/health` 继续扩展健康摘要，不返回 chunk 原文；返回 `statusLabel` 和 `maintenanceSummary` 作为一眼可读的维护状态。✓
- `GET /api/rag/health` 返回 `preferredMaintenanceAction` 和 `nextMaintenanceEndpoint`，让维护台或 smoke 脚本可直接拿到首选下一步动作。✓
- `GET /api/rag/health` 返回 `maintenancePriority / maintenanceReason`，让维护台能解释为什么系统索引问题优先于向量补偿，以及表缺失为什么阻塞维护端点。✓
- `RAG_RETRIEVAL` Agent step 输出命中数量、题目 / 知识卡 / 用户记忆来源分布，以及 MySQL-only / MySQL+Qdrant hybrid / MySQL-only fallback 模式，便于排查但不暴露 raw chunk。✓
- `maintenanceActions` 要能覆盖空索引、重复系统文档、缺失系统文档、过期题目 / 知识卡索引、失败向量和待补向量；系统索引和向量索引同时异常时，首选动作仍先 rebuild，摘要会提示后续 retry vector。✓
- `POST /api/rag/system-index/rebuild` 继续只重建系统题目 / 知识卡索引，不删除用户记忆，并返回系统索引前后变化与用户记忆保留摘要。✓
- `POST /api/rag/vector/retry-failed` 继续重试失败和空状态向量 chunk，向量关闭时返回 no-op，并返回 matched / attempted / indexed / failed / skipped 摘要。✓
- Dashboard 可直接触发系统索引重建和失败向量重试；RAG health 暂不可用时保留重试 RAG 状态入口，维护动作成功但 health 回读失败时保留维护摘要并提示单独刷新状态。✓
- 维护脚本继续把 RAG rebuild 作为可选步骤，不影响普通 MySQL-only demo；`scripts/e2e_demo_smoke.ps1` 默认只记录 skipped evidence，只有显式 `-RunRagRebuild` 才调用 rebuild。✓

验收方式：

- `RagServiceImplTest`
- `RagChatControllerTest`
- `scripts/e2e_demo_smoke.ps1 -RunRagRebuild` 可作为后续手动验收。
- 当前真实向量补偿验收：`GET /api/rag/health` 返回 `HEALTHY`、`vectorPendingChunkCount=0`、`vectorFailedChunkCount=0`。

### 近期目标 D：模拟面试复盘闭环继续细化

目标：模拟面试不只是单次问答，而是能恢复、能看报告、能进入训练计划、能复测同一知识点。

可执行小目标：

- 继续保留会话状态机，前端刷新后只通过后端状态恢复，不自行推断流程。
- 报告详情继续展示命中要点、缺失要点、表达问题和推荐知识卡。
- 报告详情继续返回 `trainingPlanLinked / trainingPlanItemCount / reviewPathSummary`，说明报告是否已经沉淀到训练计划，历史报告恢复时也能反查训练项。✓
- 推荐知识卡只来自低分、缺失要点或表达问题；强回答且无缺口的报告不强行生成复盘训练计划。
- 推荐知识卡继续链接 `/knowledge?cardId=...`，复盘路径回到 Dashboard 下一步动作。
- `GET /api/users/{id}/mock-interviews/trace` 继续作为闭环摘要入口，说明最近报告是否写入训练计划，并返回 `closureStatus`、`closureStatusLabel`、`closureSummary`、`reviewPathSummary`、`nextActionReason`、`nextActionPriority`、`nextTargetHref`、`nextTargetLabel` 作为可执行下一步。✓
- 最近报告没有推荐知识卡但存在低分回答时，trace 下一步指向报告详情，说明应先回看薄弱标签和缺失要点，不构造不存在的知识卡复盘。✓
- 趋势视图继续按知识卡聚合，区分分数变好、变差和无变化；最近报告没有推荐卡且没有低分回答时，trace 摘要说明无待复盘推荐卡、无需生成复盘训练计划，并直接跳到同类分类复测入口。✓
- 趋势视图继续区分最近卡点是 `KNOWLEDGE_GAP`（知识点不会）还是 `EXPRESSION_GAP`（表达不完整），Dashboard 卡片展示对应类型。✓
- 最近面试、闭环追踪和趋势首屏失败降级为空态；手动刷新面试闭环时单个 trace / trend 失败只影响对应卡片。✓

验收方式：

- `UserLearningServiceImplTest`
- `npx tsc --noEmit`
- `frontend/lib/core-loop-stability.node-test.cjs`

### 近期目标 E：20 题训练集只做稳定化，不继续扩题

目标：不继续扩到完整 Hot100，先让精选 20 题在 seed、wrapper、模板、提示、题解和错误样例上可回归。

可执行小目标：

- 保持 20 题 `code_mode='solution'`、Java 模板、三层提示、solution outline 和知识点绑定一致。
- 为核心演示题继续沉淀固定 bug 样例，先保证 `1`、`206`、`121` 稳。
- 后续再按知识类别补充典型 WA / 编译错误 / 边界错误样例。
- 保持 `CodeWrapper` 只包装送入 Piston 的代码，数据库保存用户原始 `class Solution`。

验收方式：

- `CodeWrapperTest`
- `frontend/lib/hot100-seed.node-test.cjs`
- `docs/DEMO_CASES.md` 固定样例。

## 目标 1：20 题稳定训练集

目标：把现有 Hot100 精选 20 题从“能跑”变成“可回归、可诊断、可长期维护”的训练集。

### 小目标 1.1：题目数据一致性

完成标准：

- `data/problems.sql` 中 20 道题全部为 `code_mode='solution'`。
- 每题都有：
  - Java `class Solution` 模板
  - 3 个测试用例
  - 三层预设提示
  - `solution_outline`
  - 对应知识点绑定
- 题目 ID 与 `CodeWrapper.supportedProblemIds()` 完全一致。

验收方式：

- `frontend/lib/hot100-seed.node-test.cjs` 覆盖 20 题 seed。
- `CodeWrapperTest` 覆盖 20 题 adapter 注册。

### 小目标 1.2：每题 wrapper 可验证

完成标准：

- 数组、字符串、链表、二叉树、区间、网格等输入都由 `CodeWrapper` 统一包装。
- 用户提交代码仍保存原始 `class Solution`。
- 只有送入 Piston 的代码被包装成 `Main.java`。
- wrapper 输出格式与 `test_case.expected_output` 对齐。

验收方式：

- 每类题至少有一个 wrapper 单测。
- 重点覆盖：
  - 三数之和结果规范化
  - 合并区间二维数组输出
  - 岛屿数量字符网格输入
  - 二叉树中序遍历列表输出

### 小目标 1.3：典型错误样例沉淀

完成标准：

- 为核心演示题和高频题准备固定 WA / 编译错误 / 边界错误样例。
- 每个样例明确：
  - 触发题目
  - 错误代码
  - 预期失败用例
  - 预期错误类型
  - 预期知识点
- 样例不作为完整答案生成入口。

验收方式：

- 至少覆盖 `1`、`206`、`121`。
- 后续可扩展到 20 题中每个知识类别至少 1 个错误样例。

### 小目标 1.4：题解展示稳定

完成标准：

- 左侧参考题解按小节折叠展示。
- 完整 Java 参考实现默认不主动暴露。
- 展开 preset hints 不触发 AI。
- 右侧仍只展示测试结果和 AI 诊断 / AC 点评。

验收方式：

- `frontend/lib/problem-hints-ui.node-test.cjs` 覆盖题解分节和提示边界。
- `frontend/lib/core-loop-stability.node-test.cjs` 继续覆盖右侧 tab 边界。

## 目标 2：训练计划可追踪

目标：训练计划不只是“生成出来”，而是能说明来源、跟踪执行、反馈到学习状态。

### 小目标 2.1：训练项来源可解释

完成标准：

- 每个训练项能说明来源：
  - failed submission
  - mistake card
  - AI diagnosis
  - knowledge self-test
  - mock interview report
  - RAG 命中知识卡
- 前端展示“为什么推荐我做这个”。
- 不把算法错因强行解释成后端八股知识。

验收方式：

- 训练计划 VO 能返回足够的来源摘要。
- Dashboard 训练项展示推荐原因和复习重点。

### 小目标 2.2：训练计划历史可查看

完成标准：

- 保留历史训练计划。
- 用户能查看当前计划和历史计划。
- 旧计划状态区分：
  - `ACTIVE`
  - `COMPLETED`
  - `REGENERATED`
- 手动重新生成不会删除旧计划。

验收方式：

- 新增训练计划历史查询能力。
- Dashboard 或独立训练计划页面可查看历史摘要。

### 小目标 2.3：训练项完成后可沉淀结果

完成标准：

- 用户完成或跳过训练项后，记录状态变化。
- 最近训练活动返回 `learningImpactSummary`，说明完成会形成改善趋势，跳过只记录节奏。
- 训练项完成事件保持幂等：重复提交 `COMPLETED` 不重复写 `TRAINING_PLAN_COMPLETED` 弱点改善事件。
- 后续可扩展记录：
  - 复习得分
  - 复习备注
  - 是否仍需重练
- 训练项完成情况能影响 Dashboard 的学习状态展示。

验收方式：

- 当前 `COMPLETED` / `SKIPPED` 状态稳定保存。
- Dashboard 能展示最近训练完成情况。
- Dashboard 能展示最近训练活动的学习影响摘要。

### 小目标 2.4：训练计划和弱点趋势联动

完成标准：

- 训练计划完成情况可用于解释弱点变化。
- 自测低分、模拟面试低分、提交失败继续写入弱点事件。
- 后续弱点趋势不仅看错题，也看训练完成情况。✓

验收方式：

- `user_weakness_event` 来源继续保持清晰。
- Dashboard 能区分“最近加重”“最近改善”“持续薄弱”等趋势。✓

## 目标 3：RAG 内部可维护

目标：RAG 不只是 Agent 里的黑盒工具，而是可检查、可重建、可降级、可扩展的内部检索系统。

### 小目标 3.1：MySQL RAG 健康检查

完成标准：

- 能检查 `rag_document` / `rag_chunk` 是否存在系统索引。
- `GET /api/rag/health` 返回健康摘要，不暴露 raw retrieval 或 chunk 原文。
- 能发现空索引、重复索引、过期知识卡 chunk。
- RAG index 为空时返回空结果，不抛异常。

验收方式：

- 空 RAG index 测试通过。
- 系统索引重建后 problem / knowledge_card chunk 可检索。

### 小目标 3.2：失败向量可重试

完成标准：

- `POST /api/rag/vector/retry-failed?limit=50` 重试 `rag_chunk.vector_status='FAILED'` 和空状态的待补向量索引。
- 返回 enabled / attempted / indexed / failed / skipped 计数，不返回 chunk 原文。
- `RAG_VECTOR_ENABLED=false` 时返回 no-op，保持 MySQL-only 行为。

验收方式：

- embedding / Qdrant 恢复后，失败 chunk 可被重新标记为 `INDEXED`。
- 向量关闭时不调用 embedding 或 Qdrant。
- 接口仍不构成公开 raw RAG retrieval。

### 小目标 3.3：系统索引重建可控

完成标准：

- `RagService.rebuildSystemIndex()` 只重建系统 chunk。
- 不删除用户历史 `AI_DIAGNOSIS` / `MISTAKE_CARD` 记忆。
- 重建失败不影响已有用户记忆。

验收方式：

- 单测覆盖系统重建不删除用户记忆。
- 手动重建后，题目和知识卡可重新检索。

### 小目标 3.4：RAG 检索质量可测试

完成标准：

- 同题目、同知识点、同错误类型 chunk 排名高于无关 chunk。
- 当前用户记忆排名高于其他用户记忆。
- 用户 A 的记忆不会被用户 B 检索。✓
- 向量 only 命中的 chunk 合并回 MySQL 后仍继续执行用户隔离，避免 Qdrant 返回其他用户记忆时泄漏。✓

验收方式：

- `RagServiceImplTest` 覆盖排序、用户隔离、向量 only hit 二次隔离和向量相似度提升排序。✓
- `QdrantRagVectorStoreTest` 覆盖 Qdrant payload filter 包含系统 chunk 或当前用户 chunk 条件。✓
- 检索结果中保留 sourceType、sourceId、score、reason 方便排查。✓

### 小目标 3.5：升级为 MySQL + Qdrant 混合检索

完成标准：

- MySQL 继续作为 RAG chunk 事实源。
- Qdrant 只保存向量点和 payload。
- 默认 `RAG_VECTOR_ENABLED=false`。
- 开启后支持 OpenAI-compatible embeddings。
- Qdrant / embedding 失败时降级 MySQL-only。
- 用户隔离在 MySQL 和 Qdrant payload filter 双层生效。

验收方式：

- 向量关闭时行为与当前 MySQL RAG 一致。
- 开启向量后可融合规则分和向量相似度。
- 停掉 Qdrant 后 Agent 诊断仍能返回。

## 目标 4：模拟面试历史和报告详情

目标：模拟面试不只是单场问答，而是能沉淀为可回看、可复盘、可继续训练的学习模块。

### 小目标 4.1：模拟面试历史列表

完成标准：

- 用户能看到历史模拟面试列表。
- 列表展示：
  - 分类
  - 状态
  - 平均分
  - 已答题数
  - 弱点标签
  - 创建 / 完成时间
- 进行中会话可继续。
- 已报告会话可查看报告。

验收方式：

- `GET /api/users/{userId}/mock-interviews/recent` 支撑 Dashboard 最近面试。
- 后续可扩展为完整历史分页接口。

### 小目标 4.2：报告详情页

完成标准：

- 单场报告能独立查看。
- 报告展示：
  - 平均分
  - 主要弱点
  - 推荐知识卡
  - 每轮主问题和追问
  - 用户回答
  - 命中要点
  - 缺失要点
  - 表达问题
- 报告不生成完整 Java AC 代码。

验收方式：

- `/mock-interview?sessionId=...` 能恢复已报告会话。
- 后续可新增 `/mock-interview/report/{sessionId}` 或页面内报告详情视图。

### 小目标 4.3：模拟面试推荐进入训练计划

完成标准：

- 低分、缺失要点或表达问题对应的 `recommendedCardIds` 可以写入训练计划；强回答且无缺口时不生成复盘训练计划。✓
- 训练计划项能标记来源为 mock interview。✓
- 保存训练计划失败不影响报告生成。✓

验收方式：

- `MockInterviewServiceImplTest` 覆盖报告生成后保存训练计划、训练计划保存失败不阻塞报告生成，以及强回答且无缺口时不生成推荐卡或训练计划。
- Dashboard 最新训练计划能看到模拟面试推荐项。

### 小目标 4.4：同知识点多次面试趋势

完成标准：

- 同一知识点多次模拟面试后，能看到分数变化。✓
- 能区分“知识点不会”和“表达不完整”。✓
- 低分或缺失点继续写入 `user_weakness_event`。✓

验收方式：

- 弱点事件来源包含 `MOCK_INTERVIEW`；低分追问和“分数合格但仍有缺失点”的主回答都有回归测试。✓
- `GET /api/users/{userId}/mock-interviews/trends` 按知识点展示最近得分、上次得分、分数变化和最近卡点。✓
- Dashboard 展示“模拟面试趋势”。✓

## 目标 5：受控知识问答增强

目标：`/rag-chat` 成为受控学习资料问答入口，而不是通用聊天或答案生成器。

### 小目标 5.1：问题类型识别

完成标准：

- 问题进入后先识别类型：
  - 题目问题
  - 知识卡问题
  - 历史诊断问题
  - 错题卡问题
  - 当前用户学习记录问题
  - 越界问题
- 学习记录类问题优先走结构化查询。
- 普通知识问题走 MySQL RAG。

验收方式：

- “我最近哪里薄弱”查询用户学习记录。
- “两数之和为什么要先查再放”走题目 / 知识卡 RAG。
- 越界问题稳定拒答。

### 小目标 5.2：回答附带依据摘要

完成标准：

- 回答可以说明依据来自：
  - problem
  - knowledge_card
  - ai_diagnosis
  - mistake_card
  - user learning records
- 不暴露 raw retrieval endpoint。
- 不直接粘贴长篇原文。

验收方式：

- 响应中有可读来源摘要。
- 前端能展示“参考依据”。

### 小目标 5.3：禁止完整 AC 代码

完成标准：

- `/rag-chat` 不生成完整 Java AC 代码。
- 可以解释思路、复杂度、易错点和伪代码。
- 对“直接给完整答案”类请求稳定拒绝或改为提示式回答。

验收方式：

- 回归测试覆盖完整 AC 代码请求。
- 回答不包含完整 `class Solution` 解法。

### 小目标 5.4：知识问答不替代提交诊断

完成标准：

- 代码错误诊断仍以提交 -> Piston -> Agent 为主流程。
- `/rag-chat` 可以解释历史诊断，但不直接诊断未提交代码。
- 不上传文档、不联网搜索、不变成通用聊天。

验收方式：

- 文档明确边界。
- 前端入口文案保持“学习资料问答”，不包装成万能助手。

## 推荐执行顺序

```text
1. 20 题稳定训练集
2. 训练计划可追踪
3. RAG 内部可维护
4. 模拟面试历史和报告详情
5. 受控知识问答增强
6. Redis 题目 / 知识卡热点缓存
```

原因：

- 20 题训练集是所有学习数据的入口，先稳定它，后面的诊断、计划和 RAG 才有可靠输入。
- 训练计划承接提交诊断、自测和模拟面试，是学习闭环的中枢。
- RAG 可维护性决定 Agent 诊断和知识问答能不能长期稳定。
- 模拟面试历史依赖已有报告和训练计划来源字段。
- 受控知识问答最后增强，避免过早变成通用聊天。
- Redis 缓存只服务题目和知识卡只读热点接口，作为工程增强而不是新的学习数据事实源。

## 当前不做

- 不继续扩到完整 Hot100。
- 不做多语言判题。
- 不做 Docker 沙箱替换 Piston。
- 不做公开 raw RAG retrieval 接口。
- 不把 `/api/rag/chat` 做成通用聊天。
- 不做语音 / 视频模拟面试。
- 不做复杂权限系统。

## 下一阶段完成定义

当以下条件都满足时，可以认为下一阶段完成：

- 20 题训练集有稳定 seed / wrapper / template / hint / solution outline 回归保护。
- 训练计划能说明来源、保存历史、展示完成情况。
- RAG 能检查、重建、排错，并可选升级到 Qdrant 混合检索。
- 模拟面试能查看历史和报告详情，报告推荐能进入训练计划，并能解释下一步复盘动作的原因和优先级。
- `/rag-chat` 能回答受控学习问题，并稳定拒绝通用聊天和完整 AC 代码请求。
- Redis 缓存能加速题目列表、详情、模板、知识卡分类、知识卡列表和知识卡详情，并在 Redis 故障时降级 MySQL；Dashboard 可查看、刷新和重试缓存状态，维护成功但状态回读失败时保留维护结果。
