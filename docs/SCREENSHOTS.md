# 功能截图索引

本文档整理 README 和项目展示可直接引用的截图素材。当前 README 主图直接使用 `docs/demo-cases` 下的 5 张用户提供截图。

## 已有截图

| 场景 | 文件 | 说明 |
|------|------|------|
| 题库首页 | `docs/demo-cases/屏幕截图 2026-05-29 134145.png` | 展示 20 题训练集、难度筛选、知识点筛选和搜索入口。 |
| AI 诊断 | `docs/demo-cases/屏幕截图 2026-05-29 134238.png` | 展示反转链表错误诊断、失败现象、根本原因、修改方向、面试提醒和 Agent timeline。 |
| 知识训练 | `docs/demo-cases/屏幕截图 2026-05-29 134420.png` | 展示 Redis 知识卡、自测输入、跳过自测看解析和标杆回答解析。 |
| 模拟面试 | `docs/demo-cases/屏幕截图 2026-05-29 134435.png` | 展示 Spring 模拟面试会话、当前问题、实时反馈和回答输入区。 |
| Dashboard / 学习中心 | `docs/demo-cases/屏幕截图 2026-05-29 134445.png` | 展示学习统计、今日优先训练、完整训练计划和下一步动作。 |

README 当前优先引用：

- `docs/demo-cases/屏幕截图 2026-05-29 134145.png`
- `docs/demo-cases/屏幕截图 2026-05-29 134238.png`
- `docs/demo-cases/屏幕截图 2026-05-29 134420.png`
- `docs/demo-cases/屏幕截图 2026-05-29 134435.png`
- `docs/demo-cases/屏幕截图 2026-05-29 134445.png`

## 可选补采清单

当前 5 张截图已经覆盖题库、AI 诊断、知识训练、模拟面试和学习中心。若后续还要强化工程维护亮点，可再补采以下局部截图：

| 优先级 | 场景 | 目标页面 / 区域 | 验收重点 |
|--------|------|-----------------|----------|
| P1 | Dashboard RAG health | `/dashboard` RAG 索引状态卡片 | `statusLabel`、系统 chunk、用户记忆 chunk、向量 failed / pending、维护动作。 |
| P1 | Dashboard 缓存状态 | `/dashboard` 缓存层状态卡片 | Redis ready / degraded、cached keys、hit rate、protected data boundary。 |
| P1 | 模拟面试报告 | `/mock-interview?sessionId=...` 报告区域 | 平均分、强弱项、推荐知识卡、训练计划联动、复盘路径。 |
| P1 | RAG Chat | `/rag-chat` | 受控学习资料问答、sources，不生成完整 AC 代码。 |
| P1 | AC 代码点评 | `/problem/121` 或 `/problem/1` AC 后 | `CODE_REVIEW` 分支、复杂度、代码风格、面试表达建议。 |

## 建议采集命名

```text
docs/demo-cases/dashboard-rag-health.png
docs/demo-cases/dashboard-cache-status.png
docs/demo-cases/mock-interview-report.png
docs/demo-cases/rag-chat-sources.png
docs/demo-cases/problem-ac-review.png
```

## 采集前检查

先确认本地演示环境和数据处于可复现状态：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\local_dependency_preflight.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\e2e_demo_smoke.ps1
```

如果只采前端静态画面，可以先跑离线护栏：

```powershell
node frontend/lib/hot100-seed.node-test.cjs
node frontend/lib/demo-cases.node-test.cjs
node frontend/lib/core-loop-stability.node-test.cjs
```
