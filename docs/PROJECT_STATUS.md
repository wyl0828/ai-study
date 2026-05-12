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
- **混合提交模式**：`101/105/106/107/108` 保持 ACM `public class Main` 模式；`102/103/104` 支持 LeetCode 风格 `class Solution`，后端通过 `CodeWrapper` 只包装送入 Piston 的代码，数据库仍保存用户原始代码。
- **Agent Workflow 后端**：已实现 `InterviewCoachAgent`、`AgentContext`、`AgentStep` 和核心 Tool 链，支持同步分析接口和 SSE 流式诊断接口。`MEMORY_UPDATE` 和 `TRAINING_PLAN` 为非核心步骤，失败不阻塞后续流程。
- **AI 诊断与学习数据**：失败提交后可生成结构化错误类型、知识点、具体错误、改进建议和训练计划，并持久化 `ai_diagnosis`、`hint_record`、`user_weakness`、`mistake_card`、`training_plan` 等数据。
- **前端做题页**：已实现三栏布局：左侧题目描述和题目预设分层提示，中间 Monaco Editor，右侧测试结果和 AI 诊断。提交失败后通过 SSE 实时展示 Agent 执行步骤，完成后展示诊断结果。
- **提示/诊断边界已理顺**：题目通用 Level 1/2/3 提示放在左侧，默认收起，不调用 AI；右侧 AI 诊断只解释本次提交为什么错，并展示改进建议和推荐训练。
- **Dashboard 真实数据接入**：学习中心已从 mock 数据切到后端真实接口，展示统计、薄弱点、错题卡、最近提交和最新训练计划。
- **后端知识训练一期**：已新增 `/knowledge` 页面、`KnowledgeController`、`KnowledgeCardService` 和 `knowledge_card` 表；当前内置约 35 张 Java / JVM / Spring / MySQL / Redis 知识卡片，内容为结构化种子数据。
- **训练计划轻接入知识卡片**：`training_plan_item` 已支持 `PROBLEM` 和 `KNOWLEDGE_CARD` 两类条目；`TrainingPlannerTool` 保留原有 3 条算法训练项，只额外混入最多 1-2 条高优先级知识卡，不根据算法错因强行推荐八股。
- **固定演示样例**：`101`、`103`、`104` 的 bug / fixed 代码已沉淀到 `docs/demo-cases/`，使用说明见 `docs/DEMO_CASES.md`。

## 2. 当前进度判断

整体进度处于 **Phase 5 打磨与演示准备阶段**，并完成了“后端知识训练一期”扩展。SSE 流式诊断已接入前端，Agent 步骤实时展示。

从简历项目角度看，当前已经能讲清楚这些亮点：

- Spring Boot 分层设计：Controller、Service、Mapper、Agent、Tool、Integration 分包清晰。
- MyBatis-Plus + MySQL：题目、提交、诊断、弱点、错题卡、训练计划和后端知识卡均有持久化模型。
- Piston 执行服务封装：Controller 不直接调用外部判题服务，代码执行通过 `JudgeService` 抽象。
- Agent 工程化：代码执行是 Tool，判题结果是 Observation，错误诊断和学习更新由 Agent Workflow 串联。
- SSE 能力：前端已通过 `fetch + ReadableStream` 接入 SSE，实时展示 Agent 每一步执行过程。
- 学习闭环：一次失败提交能影响弱点、错题卡和训练计划，不是一次性 AI 文本回答。
- 训练计划统一管理：算法题训练和后端知识卡片复习能在同一个计划中展示，但算法诊断和八股知识保持来源边界。

当前还不适合继续扩大的方向：

- 不急着扩到 30 道题，先保证 8 道 MVP 题都能稳定演示。
- 不急着做多语言、Docker 沙箱、复杂登录权限、语音/视频面试。
- 不急着把前端做成完整 IDE，当前 Monaco + 提交 + 诊断已经够支撑简历演示。

## 3. 主要风险

- **AI 诊断稳定性**：模型可能输出不稳定 JSON 或分类不准，需要准备固定 bug 样例和验证脚本。
- **本地依赖较多**：MySQL、Redis、Piston、后端、前端都要启动，演示前需要一键化或清晰启动脚本。
- **SSE 稳定性**：SSE 已接入前端，但客户端断开、多请求并发等边界情况仍需观察。
- **题目提示已迁移到后端**：`problem` 表存储 `hint_level1/2/3`，通过 `ProblemDetailVO.presetHints` 返回。
- **已有本地数据库需要升级**：旧库访问 `/knowledge` 前需要执行 `data/knowledge_training_migration.sql` 和 `data/knowledge_cards.sql`；PowerShell 下使用 `cmd /c "mysql ... < file.sql"`。
- **知识卡内容边界**：当前知识卡参考小林 coding 选题并重新整理，不是 RAG，也不应直接复制外部文章长文本。
- **文档与代码容易漂移**：提示/诊断边界已调整，后续修改接口或页面时要同步更新 `docs/API.md` 和设计文档。

## 4. 下一步大纲

### 4.1 第一优先级：演示稳定性

- 准备 3 个固定演示题：
  - `101 两数之和`：HashMap 查询/写入顺序 bug。
  - `103 反转链表`：指针断链或返回错误头节点。
  - `104 合并两个有序链表`：直接返回 `null` 或漏接剩余链表。
- 为每个演示题准备一份“错误代码”和“修正后代码”。
- 写清楚本地启动顺序：MySQL、Redis、Piston、Spring Boot、Next.js。
- 确认 Dashboard 在触发一次诊断后能看到新增弱点、错题卡和训练计划。
- 确认训练计划中算法题显示为“算法题：xxx”，知识卡片显示为“知识卡片：xxx”。
- 确认 `/knowledge` 能展示五个一级分类，Java 基础/集合/并发只作为 tags 出现。

### 4.2 第二优先级：README 与面试材料

- 继续维护根目录 `README.md`，包含项目定位、技术栈、架构图、启动方式、核心流程和截图位置。
- 准备一页“面试讲解稿”，重点讲：
  - 为什么不是普通刷题平台。
  - Piston 为什么要封装成 `JudgeService`。
  - Agent Workflow 每一步怎么记录。
  - MySQL 如何承载长期学习记忆和结构化知识卡片。
  - 为什么提示是题目预设，AI 诊断是本次提交动态分析。
- 准备 3 到 4 张截图：题目页、AI 诊断、Dashboard、知识训练页。

### 4.3 第三优先级：小范围产品增强

- 给知识卡片增加轻量自测状态：
  - 只记录是否看过、是否答错、是否收藏。
  - 暂不新增复杂 `knowledge_weakness` 体系。
- 增加 Accepted 提交后的轻量代码点评：
  - 不生成完整答案。
  - 只点评复杂度、代码风格和可优化点。

### 4.4 暂缓事项

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
3. 跑一遍完整 demo 并记录截图
4. 前端 SSE 步骤展示 ✓
5. 将题目预设提示迁移到后端数据 ✓
6. 后端知识训练一期 ✓
7. 准备简历描述和面试问答
```

当前最应该保护的是演示闭环稳定性，而不是继续堆功能。只要能稳定展示“代码执行 Tool -> Observation -> AI 诊断 -> Memory -> Training Plan”，这个项目已经具备清晰的 Java 后端 + Agent 工程化简历价值。
