# 后续 Issues 草案

本文档用于把后续计划拆成 GitHub Issues。当前项目已经完成可演示闭环，下面条目以稳定性、安全性、可观测性、测试覆盖和部署为主，不把产品方向扩成通用刷题站或泛聊天平台。

## P0: Codex Security / 安全基线

**目标：** 梳理本地演示项目中最容易被问到的安全边界，让项目在简历评审和开源展示时更可信。

**范围：**

- 检查 `.env`、日志、README、docs 中是否泄露 API key、数据库密码、embedding key。
- 给 AI、Piston、RAG 维护接口补充安全说明：本地演示默认无登录，生产化需要认证和权限。
- 限制维护型接口的产品定位：`/api/rag/system-index/rebuild`、`/api/rag/vector/retry-failed`、`/api/cache/refresh` 是受控维护入口，不是普通用户入口。
- 审查 SSE 诊断输出，避免把完整运行时敏感堆栈、系统 prompt、RAG raw chunk 原文直接暴露给前端。
- 给 README 增加生产部署安全注意事项：密钥管理、CORS 白名单、限流、接口鉴权、Piston 沙箱隔离。

**验收：**

- `rg -n "sk-|api_key|password|secret|token|EMBEDDING_API_KEY|AI_API_KEY" .` 无真实密钥泄露。
- README / docs 明确本项目当前是本地演示安全边界。
- 维护接口在文档中标注后续生产化需要 admin 权限。

## P0: 三题最终 Demo Replay

**目标：** 固定 `1` / `206` / `121` 三道题的演示输入、失败现象、诊断分支和 AC 点评分支。

**范围：**

- 重新跑 `docs/demo-cases/1-two-sum-bug.java` 和 fixed 代码。
- 重新跑 `206` 反转链表 bug / fixed 代码。
- 重新跑 `121` 买卖股票 bug / fixed 代码。
- 记录每题预期状态、错误类型、知识点、失败用例和 AC 点评摘要。
- 更新 `docs/DEMO_CASES.md` 中的最新验收证据。

**验收：**

- 三题 bug 样例均能进入失败诊断。
- 三题 fixed 样例均能通过并可返回 AC code review。
- 失败提交 SSE 包含 `RAG_RETRIEVAL`，且顺序在 `OBSERVATION` 之后。

## P1: RAG 优化与维护体验

**目标：** 保持 RAG 是 Agent 内部 Tool，同时让索引健康、向量补偿和用户记忆隔离更容易排查。

**范围：**

- 给 `RAG_RETRIEVAL` step 输出更稳定的来源分布摘要：problem / knowledge_card / ai_diagnosis / mistake_card。
- 为用户记忆隔离补充更多自动化测试：用户 A 的诊断和错题不能被用户 B 检索。
- 继续完善 `GET /api/rag/health` 的维护建议文案，避免过度技术化。
- 可选优化 MySQL rule score 和 Qdrant similarity 融合权重，但不引入新的向量数据库。
- 补充 RAG health 和 vector retry 的截图。

**验收：**

- `RagServiceImplTest` 覆盖同题目、同知识点、用户隔离、空索引和向量 fallback。
- `scripts\e2e_demo_smoke.ps1` 的 `goal_coverage` 包含 `rag=...`。
- Dashboard RAG 状态卡能解释下一步维护动作。

## P1: 测试覆盖补强

**目标：** 把核心边界变成回归护栏，减少后续改 UI 或接口时破坏演示闭环。

**范围：**

- 后端补 Controller 层测试：cache、rag maintenance、mock interview、training plan trace。
- 后端补 Agent 分支测试：失败诊断、AC code review、RAG 失败不阻塞。
- 前端继续维护 `frontend/lib/core-loop-stability.node-test.cjs`：右侧 tab 边界、SSE fallback、旧流拦截、Dashboard no mock。
- 增加截图采集前的 smoke checklist。

**验收：**

```powershell
mvn test
npx tsc --noEmit
node frontend/lib/hot100-seed.node-test.cjs
node frontend/lib/demo-cases.node-test.cjs
node frontend/lib/core-loop-stability.node-test.cjs
```

## P1: Docker 部署与本地一键启动

**目标：** 降低演示环境搭建成本，把 MySQL、Redis、Qdrant、Piston、后端、前端的启动顺序写清楚。

**范围：**

- 扩展 `docker-compose.yml`，明确 Redis、Qdrant、Piston 的默认端口和 Windows 备用端口策略。
- 评估是否加入 MySQL compose 服务；如果保留本机 MySQL，也要写清楚原因。
- 新增 `docs/DEPLOYMENT.md`：本地开发、演示模式、生产化差距。
- 补一个 `scripts/start-demo.ps1`，按依赖顺序启动服务或打印下一步动作。

**验收：**

- 新机器按 README 和 `docs/DEPLOYMENT.md` 能完成本地启动。
- `scripts\local_dependency_preflight.ps1` 能准确指出缺失服务。
- `scripts\e2e_demo_smoke.ps1 -SkipEmbedding` 能完成主链路验收。

## P2: README 截图与短视频素材

**目标：** 让仓库首页对评审者和面试官更友好。

**范围：**

- 补采 AI 诊断、Agent timeline、RAG health、缓存状态、模拟面试报告截图。
- 更新 [SCREENSHOTS.md](SCREENSHOTS.md) 中的已完成状态。
- 可选补 60 到 90 秒演示视频，覆盖从失败提交到 Dashboard 学习闭环。

**验收：**

- README 首屏能看到架构图和主要页面截图。
- 截图能体现项目不是普通 LeetCode clone，而是 Agent-driven training loop。

## P2: 文档一致性巡检

**目标：** 防止 `README.md`、`docs/API.md`、`docs/IMPLEMENTATION_PLAN.md`、`docs/PROJECT_STATUS.md` 与代码漂移。

**范围：**

- 对照当前 Controller 更新 API 表。
- 对照当前数据表更新设计文档。
- 删除或标注已经过期的“未来阶段”文字。
- 保持 AGENTS.md 中的 MVP 边界：不新增通用 RAG chat、公开 raw retrieval、多语言判题或完整答案生成。

**验收：**

- 文档中的已完成 / 暂不做 / 后续计划与当前代码一致。
- README 的快速启动、Demo 流程和验收命令可直接执行。

