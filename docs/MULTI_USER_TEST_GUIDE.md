# 多人测试说明

本文档用于把当前 AI Interview Coach Agent 发给同学、朋友或面试官做轻量测试。测试目标不是压测，也不是公开运营，而是验证登录后的学习记录隔离和核心训练闭环是否顺畅。

## 测试地址

- 访问地址：由部署者提供前端地址。
- 推荐浏览器：Chrome 或 Edge。
- 测试账号：测试者可以自行注册一个临时账号。

请不要使用真实常用密码。当前账号体系用于项目测试，不作为正式生产账号系统。

## 推荐测试流程

1. 打开测试地址，注册并登录。
2. 进入首页，选择“两数之和”。
3. 展开左侧预设提示，确认提示不会自动生成完整答案。
4. 提交一份有 bug 的 Java `class Solution` 代码。
5. 等待右侧 Agent timeline 完成，重点观察：
   - 测试结果是否展示失败用例。
   - AI 诊断是否解释当前错误。
   - timeline 是否包含 RAG 检索、错误分类、弱点更新、训练计划等步骤。
6. 打开 Dashboard，确认最新提交、错题、薄弱点和训练计划是否出现。
7. 再提交一份正确代码，确认状态为 `ACCEPTED`，并观察轻量代码 Review。
8. 进入知识训练、自测、RAG Chat、模拟面试页面，做一次简单操作，确认这些记录都挂在当前登录用户下。

## 重点反馈

请测试者优先反馈这些问题：

- 登录、注册、刷新页面后登录态是否异常。
- 提交代码后是否长时间卡住，或 Agent timeline 不动。
- AI 诊断是否和失败用例明显不相关。
- Dashboard 是否看不到刚才产生的提交、错题或训练计划。
- RAG Chat 或模拟面试是否出现和自己无关的历史记录。
- 页面上是否有明显的 401 / 403 / 500 报错。

反馈时最好带上：

- 测试账号用户名。
- 测试页面路径，例如 `/problem/1`、`/dashboard`。
- 大概操作时间。
- 截图或报错文本。
- 如果是代码提交问题，附上提交的 Java 代码。

## 测试边界

当前版本用于项目演示和小范围测试，以下行为暂不作为重点：

- 不测试高并发。
- 不测试多语言提交，当前只支持 Java。
- 不要求题库规模很大，优先看核心闭环是否稳定。
- 不要求 AI 每次回答完全一致，只要求围绕当前提交错误和学习记录给出合理诊断。
- 不使用真实隐私信息、真实常用密码或敏感代码。

## 部署者检查清单

发给别人测试前，部署者先确认：

- 前端页面可以通过外部地址访问。
- 浏览器请求使用前端同源 `/api` 或正确的外部 API 地址，不是测试者电脑上的 `localhost:8080`。
- 后端、MySQL、Piston、Redis、Qdrant 已启动。
- 只暴露必要入口，通常只需要暴露前端端口。
- MySQL、Redis、Qdrant、Piston 不直接暴露给外部测试者。
- `.env` 中已配置 AI 和 embedding 所需环境变量。
- 已运行本地预检和 smoke：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\local_dependency_preflight.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\e2e_demo_smoke.ps1
```

预检看到 `READY_FOR_E2E_SMOKE=True`，完整 smoke 看到 `E2E demo smoke passed` 后，再发给测试者。
