# AI Interview Coach Agent 当前成果与下一步大纲

## 1. 当前成果

项目已经具备可演示的 MVP 闭环，不再只是题库页面或大模型接口包装。当前核心流程是：

```text
选择题目 -> 编写 Java 代码 -> 提交判题 -> 观察失败用例
-> 触发 Agent 诊断 -> 记录错误类型和知识点
-> 更新弱点、错题卡和 3 天训练计划 -> Dashboard 展示学习数据
-> 可进入独立知识训练页复习 Java 后端知识卡片
```

已完成的主要成果：

- **题目与提交闭环**：支持题目列表、题目详情、Java 模板加载、代码提交、Piston 判题、失败用例返回和提交记录持久化。
- **Hot100 Solution 模式统一**：当前题库升级为 Hot100 精选 12 题，全部使用 LeetCode 风格 `class Solution`；后端通过 `CodeWrapper` 注册表只包装送入 Piston 的代码，数据库仍保存用户原始代码。
- **题面与题解内容补齐**：12 题均已使用“任务说明 / 返回要求 / 约束与边界”的面试式题面，并补齐三层预设提示、solution outline 和完整 Java 参考实现。
- **Agent Workflow 后端**：已实现 `InterviewCoachAgent`、`AgentContext`、`AgentStep` 和核心 Tool 链，支持同步分析接口和 SSE 流式诊断接口。`MEMORY_UPDATE` 和 `TRAINING_PLAN` 为非核心步骤，失败不阻塞后续流程。
- **AI 诊断与学习数据**：失败提交后可生成结构化错误类型、知识点、具体错误、改进建议和训练计划，并持久化 `ai_diagnosis`、`user_weakness`、`user_weakness_event`、`mistake_card`、`training_plan` 等数据；`hint_record` 保留为历史兼容表，当前 Agent 流程不写入新 AI hint。
- **AC 代码点评**：提交通过后 Agent 可进入 `CodeReviewTool` 分支，返回复杂度、代码风格、面试表达建议和可优化点，不生成完整答案；前端会在代码通过但点评仍在生成时实时展示 Agent 步骤。
- **前端做题页**：已实现三栏布局：左侧题目、题目预设分层提示和可主动查看的参考题解，中间 Monaco Editor，右侧测试结果和 AI 诊断。提交后通过 SSE 实时展示 Agent 执行步骤，失败时展示诊断结果，AC 时展示轻量代码点评；若用户在未重新提交的情况下修改代码，旧诊断或旧点评继续保留，并显示“基于上次提交，仅供参考”的 stale warning。
- **提示/题解/诊断边界已理顺**：题目通用 Level 1/2/3 提示和参考题解放在左侧，提示与完整 Java 参考实现默认不主动暴露且不调用 AI；右侧 AI 诊断只解释本次提交为什么错，并展示改进建议和推荐训练。
- **Dashboard 真实数据接入**：学习中心已从 mock 数据切到后端真实接口，展示统计、薄弱点、弱点趋势、错题卡、最近提交和最新训练计划；训练计划条目可完成/跳过，也支持手动重新生成。
- **知识训练页 V1**：已新增 `/knowledge` 前端训练页，优先读取后端知识接口和 `knowledge_card` 真实数据；接口失败时回退 3 条本地示例数据，搜索、难度筛选、分类筛选、卡片展开、模拟自测评分、点评反馈、标杆回答解析、高频追问和标记已掌握均可用；自测记录已持久化到后端。
- **后端知识训练一期能力**：后端已有 `KnowledgeController`、`KnowledgeCardService` 和 `knowledge_card` 表；`data/knowledge_cards.sql` 提供 15 张原创整理的 Java 后端面试知识卡。
- **训练计划轻接入知识卡片**：`training_plan_item` 已支持 `PROBLEM` 和 `KNOWLEDGE_CARD` 两类条目；`TrainingPlannerTool` 保留原有 3 条算法训练项，只额外混入最多 1-2 条高优先级知识卡，不根据算法错因强行推荐八股。
- **学习记忆连续化**：失败诊断会写入弱点事件，错题卡按 fingerprint 合并重复错误；知识卡自测写入 `self_test_record` 并更新 `user_knowledge_card_mastery`，低分自测也会进入弱点事件。
- **固定演示样例**：主线演示已切换为 `1 两数之和`、`206 反转链表` 和 `121 买卖股票的最佳时机`，使用说明见 `docs/DEMO_CASES.md`。

## 2. 当前进度判断

整体进度处于 **Phase 5 产品打磨阶段**，并完成了知识训练页 V1、4.1 小范围产品增强和 4.2 稳定核心闭环回归护栏。SSE 流式诊断已接入前端，Agent 步骤实时展示；完整 demo 复盘、截图录屏和面试 Q&A 归入最终阶段。

从简历项目角度看，当前已经能讲清楚这些亮点：

- Spring Boot 分层设计：Controller、Service、Mapper、Agent、Tool、Integration 分包清晰。
- MyBatis-Plus + MySQL：题目、提交、诊断、弱点、弱点事件、错题卡、训练计划、后端知识卡、自测记录和知识卡掌握度均有持久化模型。
- Piston 执行服务封装：Controller 不直接调用外部判题服务，代码执行通过 `JudgeService` 抽象。
- Agent 工程化：代码执行是 Tool，判题结果是 Observation，错误诊断和学习更新由 Agent Workflow 串联。
- SSE 能力：前端已通过 `fetch + ReadableStream` 接入 SSE，实时展示 Agent 每一步执行过程。
- 学习闭环：一次失败提交能影响弱点趋势、错题卡和训练计划，不是一次性 AI 文本回答。
- 训练计划统一管理：算法题训练和后端知识卡片复习能在同一个计划中展示，但算法诊断和八股知识保持来源边界。

当前还不适合继续扩大的方向：

- 不急着扩到 30 道题，先保证 Hot100 精选 12 题中的主 demo 题能稳定演示。
- 不急着做多语言、Docker 沙箱、复杂登录权限、语音/视频面试。
- 不急着把前端做成完整 IDE，当前 Monaco + 提交 + 诊断已经够支撑简历演示。

## 3. 主要风险

- **AI 诊断稳定性**：模型可能输出不稳定 JSON 或分类不准，需要准备固定 bug 样例和验证脚本。
- **本地依赖较多**：MySQL、Piston、后端、前端都要启动；Redis 当前只有配置预留，热点题目/题目详情缓存待接入，演示前需要一键化或清晰启动脚本。
- **SSE 稳定性**：SSE 已接入前端，并补充了 `agentStreamState` 状态决策和源码级回归测试；AC 代码点评分支也已展示实时 Agent 步骤。正式演示前仍建议用连续提交、用户中断和后端不可达场景做一次手动压测。
- **题目内容已迁移到后端**：`problem` 表存储 `hint_level1/2/3` 和 `solution_outline`，通过 `ProblemDetailVO.presetHints` 与 `solutionOutline` 返回。
- **知识数据导入依赖**：新库可直接执行 `data/schema.sql` 和 `data/knowledge_cards.sql`；旧库需要先执行 `data/knowledge_training_migration.sql` 和 `data/learning_memory_continuity_migration.sql`，再执行 `data/knowledge_cards.sql`。
- **知识卡内容边界**：当前知识卡参考小林 coding 和 JavaGuide 选题覆盖并重新整理，不是 RAG，也不应直接复制外部文章长文本。
- **文档与代码容易漂移**：提示/诊断边界已调整，后续修改接口或页面时要同步更新 `docs/API.md` 和设计文档。

## 4. 下一步大纲

### 4.1 第一优先级：小范围产品增强（已完成）

- 优化知识训练反馈：
  - 在点评反馈区补充“缺失要点”，让用户知道还差哪些核心记忆点。
  - 低分反馈改成更接近面试官评价的表达，指出回答过短、缺少机制、触发条件或优化目的。
  - 自测评分仍由前端轻量计算；后端已持久化自测记录和知识卡掌握度，低分自测会写入弱点事件。
- 完善 AC 代码点评展示：
  - 后端已有 `CodeReviewTool` / `codeReview` 分支，前端优先把 AC 后的复杂度、代码风格、面试表达建议和可优化点展示清楚。
  - 点评仍然不生成完整答案，不新增单独 REST 接口。
  - 点评结果不再被当前编辑器代码是否等于上次 AC 快照控制；用户改动代码但未重新提交时，保留上次 AC 点评并显示 stale warning。
  - AC 点评生成中继续展示 SSE Agent 步骤，避免“已通过后没有实时流程”的观感。
- 改善错误状态：
  - 后端、Piston、AI、SSE 任一服务未启动时，前端给出更明确的排查提示。
  - 不再只暴露笼统的“请求失败”，优先提示用户检查 Spring Boot、Piston、AI 配置或 SSE fallback 状态。

### 4.2 第二优先级：稳定核心闭环

- 保持问题页边界：
  - 左侧只放题目描述和题目预设 Level 1/2/3 提示。
  - 右侧只放测试结果和 AI 诊断 / AC 点评，不重新引入右侧“分层提示”tab。
- 稳住 SSE 流程：
  - 已用 streamId、AbortController 和回归测试覆盖多次提交、旧流覆盖、用户中断和 fallback 同步分析这些边界。
  - 保持 SSE 为前端主路径，同步 `POST /api/agent/analyze` 只作为 fallback。
- Dashboard 数据继续收口：
  - 统计、薄弱点、错题卡、错误分布、训练计划继续来自 MySQL。
  - 无数据时显示空状态，不回退 mock 数据。

当前落地状态：

- `ProblemWorkspace` 继续以 `agentApi.streamDiagnosis()` 为主路径；同步 `POST /api/agent/analyze` 只在 SSE error、SSE 正常结束但无 done、或 done 数据无效时作为 fallback。
- `frontend/lib/agentStreamState.ts` 收口 streamId 新鲜度判断和 fallback 触发条件；再次提交和组件卸载会中断当前流，旧流的 step / done / error / end 不覆盖新提交状态。
- `frontend/lib/core-loop-stability.node-test.cjs` 覆盖右侧 tab 边界、左侧题目预设提示、SSE fallback 条件、旧流拦截、abort 行为、Dashboard 不导入 mock 和空状态文案。

### 4.3 第三优先级：暂不做但保留

- 单独 hint 查询接口。
- 单独 accepted-code review REST 接口。
- 知识卡收藏和 RAG 检索。
- Redis 热点缓存真正接入。

### 4.4 最终阶段再考虑

- 跑通 `1/206/121` 三题完整 demo 并记录失败点。
- 对齐 `docs/API.md`、`docs/IMPLEMENTATION_PLAN.md`、`docs/AI-Interview-Coach.md` 中细节漂移的接口和流程描述。
- 针对 `1/206/121` 的失败诊断和 AC 点评各录一次短流程：验证 AC 点评生成中有实时步骤，修改代码但不提交时旧点评仍保留并提示仅供参考。
- 明确 `hint_record` 的最终策略。
- 采集题目页、AI 诊断、Dashboard、知识训练页截图，必要时录制 60-90 秒演示视频。
- 准备一页 Agent Workflow 面试 Q&A。

### 4.5 暂缓事项

- 多语言支持。
- Docker 沙箱替换 Piston。
- 大规模题库扩展。
- 用户登录、权限和多租户。
- 复杂图表和 UI 动画。
- 多 Agent 协作。

## 5. 推荐近期任务顺序

```text
1. 固定 3 个演示题和 bug 样例 ✓
2. 写 README 和启动文档 ✓
3. 前端 SSE 步骤展示 ✓
4. 将题目预设提示迁移到后端数据 ✓
5. 知识训练页 V1 ✓
6. 小范围优化知识训练反馈、AC 点评展示和错误排查提示 ✓
7. 稳住 SSE 边界与 Dashboard MySQL 数据闭环 ✓
8. 已通过代码的 AC 点评实时步骤和 stale warning 回归 ✓
9. 最终阶段：完整 demo 复盘、截图/录屏、文档细节对齐和面试 Q&A
```

当前最应该保护的是核心闭环边界，而不是继续堆功能。最终演示阶段再集中处理完整 demo 复盘、截图录屏、文档细节对齐和面试 Q&A。
