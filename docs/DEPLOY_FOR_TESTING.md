# 简单部署给别人测试

本文档用于把 AI Interview Coach Agent 临时部署给少量测试者使用。目标是可访问、可登录、可提交、可看到学习记录隔离，不追求生产级运维。

## 推荐方案

第一版推荐部署在一台机器上：

```text
测试者浏览器 -> 前端 Next.js 端口
前端 /api rewrite -> 本机后端 8080
后端 -> MySQL / Piston / Redis / Qdrant / AI API
```

只把前端端口暴露给测试者。后端、MySQL、Piston、Redis、Qdrant 都留在部署机器本机或内网，不直接暴露给外部测试者。

## 一键启动本地测试环境

如果只是准备给局域网或内网穿透测试，可以先用辅助脚本拉起本地测试环境：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start_test_env.ps1
```

脚本会启动 Redis / Qdrant，检查 Piston，后台启动后端和前端，最后运行 `local_dependency_preflight.ps1`。它不会自动运行完整 `e2e_demo_smoke.ps1`，因为完整 smoke 会真实调用 AI / embedding 并写入提交、诊断和训练计划测试数据。

默认前端监听 `0.0.0.0:4000`，便于同局域网设备访问。后台日志写入 `.local-test-env`。如果想用生产构建启动前端：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start_test_env.ps1 -Production
```

脚本输出 `READY_FOR_E2E_SMOKE=True` 和局域网访问地址后，再把地址发给测试者。

## 方式一：本机局域网测试

适合先给同一 Wi-Fi 或同一局域网的人试用。

### 1. 启动运行时依赖

先启动 Docker Desktop，然后在项目根目录启动 Redis 和 Qdrant：

```powershell
docker compose up -d redis qdrant
```

启动 Piston。当前本机常用端口是 `2238 -> 2000`，确认接口可用：

```powershell
curl.exe --noproxy "*" http://127.0.0.1:2238/api/v2/runtimes
```

MySQL 需要已启动，并且数据库 `ai_interview_coach` 已导入项目脚本。

### 2. 配置环境变量

确认根目录 `.env` 至少包含：

```env
MYSQL_URL=jdbc:mysql://localhost:3306/ai_interview_coach?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
MYSQL_USERNAME=root
MYSQL_PASSWORD=your_password

AUTH_JWT_SECRET=replace_with_a_long_random_secret
AUTH_JWT_EXPIRE_HOURS=168

PISTON_BASE_URL=http://127.0.0.1:2238/api/v2

REDIS_HOST=127.0.0.1
REDIS_PORT=6379

QDRANT_HOST=127.0.0.1
QDRANT_PORT=6334
QDRANT_REST_URL=http://127.0.0.1:6333

AI_BASE_URL=your_anthropic_compatible_base_url
AI_API_KEY=your_ai_key
AI_MODEL=your_model

RAG_VECTOR_ENABLED=true
EMBEDDING_BASE_URL=your_embedding_base_url
EMBEDDING_API_KEY=your_embedding_key
EMBEDDING_MODEL=text-embedding-v4
EMBEDDING_DIMENSIONS=1536

NEXT_PUBLIC_API_BASE=
INTERNAL_API_BASE=http://localhost:8080
```

不要把 `.env` 发给测试者。

### 3. 启动后端

项目根目录执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File start-backend.ps1
```

确认后端可用：

```powershell
curl.exe --noproxy "*" http://127.0.0.1:8080/api/problems
```

### 4. 启动前端

如果只是局域网临时测试，可以用 dev server，但要监听 `0.0.0.0`，否则其他设备访问不到：

```powershell
cd frontend
npm install
npx next dev --hostname 0.0.0.0 --port 4000
```

如果想更接近部署环境，用生产构建：

```powershell
cd frontend
npm install
npm run build
npx next start -H 0.0.0.0 -p 4000
```

### 5. 获取访问地址

查看部署机器局域网 IP：

```powershell
ipconfig
```

假设 IP 是 `192.168.1.23`，测试者访问：

```text
http://192.168.1.23:4000
```

Windows 防火墙如果拦截，需要允许 Node.js 或开放 4000 端口。不要开放 MySQL、Redis、Qdrant、Piston 端口给测试者。

## 方式二：云服务器测试

适合让不在同一局域网的人测试。

### 1. 部署原则

- 前端和后端放同一台服务器最简单。
- 只开放前端端口，例如 `4000`，或用 Nginx 暴露 `80/443`。
- 后端 `8080` 只允许本机访问。
- MySQL、Redis、Qdrant、Piston 不对公网开放。
- `.env` 只放服务器本地。

### 2. 启动顺序

```powershell
docker compose up -d redis qdrant
```

启动 Piston，并确认：

```powershell
curl.exe --noproxy "*" http://127.0.0.1:2238/api/v2/runtimes
```

启动后端：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File start-backend.ps1
```

构建并启动前端：

```powershell
cd frontend
npm install
npm run build
npx next start -H 0.0.0.0 -p 4000
```

### 3. 反向代理可选

如果你用 Nginx，可以只对外暴露域名，并把请求转给前端：

```nginx
server {
    listen 80;
    server_name your-domain.example;

    location / {
        proxy_pass http://127.0.0.1:4000;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

浏览器访问 `/api/...` 时会先到前端 Next.js，再由 `frontend/next.config.mjs` rewrite 到同机后端 `http://localhost:8080/api/...`。

如果后端和前端不在同一台机器，当前第一版需要同步调整 rewrite 或在 Nginx 层单独代理 `/api`。为了少踩坑，测试阶段建议先保持前后端同机。

## 方式三：内网穿透

适合你暂时没有云服务器，但想给外部同学快速试用。

推荐只穿透前端端口：

```text
公网临时地址 -> 本机 4000
```

不要穿透这些端口：

```text
8080  MySQL  Redis  Qdrant  Piston
```

原因是浏览器只需要访问前端，前端服务会把 `/api` 转给本机后端。后端和依赖服务不需要直接暴露。

## 部署前检查

部署机器上先跑：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\local_dependency_preflight.ps1
```

看到：

```text
READY_FOR_E2E_SMOKE=True
```

再跑完整 smoke：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\e2e_demo_smoke.ps1
```

看到：

```text
== E2E demo smoke passed ==
```

再把访问地址发给测试者。

## 发给测试者的话术

可以直接发：

```text
这是 AI Interview Coach Agent 的临时测试地址：

http://你的地址

请注册一个临时账号，不要使用真实常用密码。
推荐先测：
1. 登录 / 注册
2. 两数之和提交一份有 bug 的 Java Solution
3. 等 AI 诊断和 Agent timeline 完成
4. 看 Dashboard 是否出现提交、错题和训练计划
5. 再提交正确代码，看 AC Review

如果遇到卡住、401/403/500、诊断明显不相关、看到别人的记录，请把账号名、页面路径、时间、截图或报错发给我。
```

更完整的测试说明见 `docs/MULTI_USER_TEST_GUIDE.md`。

## 日常更新维护

部署完成后的代码同步、构建重启、数据库迁移、systemd 命令和回滚流程见 `docs/MAINTENANCE.md`。

## 常见问题

### 别人打不开页面

检查：

- 前端是否用 `0.0.0.0` 启动。
- 测试者访问的是部署机器 IP 或域名，不是 `127.0.0.1`。
- Windows 防火墙或云服务器安全组是否放行前端端口。

### 页面能打开，但提交后失败

检查：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\local_dependency_preflight.ps1
```

重点看 Backend、Piston、AI 环境变量。

### Agent timeline 卡住

常见原因：

- 后端没有加载 `.env`，导致 `AI_API_KEY` 缺失。
- Piston 没启动或端口不对。
- SSE 请求没有走到同一个后端。

先看后端日志，再跑完整 smoke 复现。

### 登录后刷新丢失

确认浏览器没有禁用 localStorage，并检查 `/api/auth/me` 是否返回 200。

### 测试者看到别人的记录

这是高优先级问题。记录测试账号、页面、时间和截图，停止继续扩大测试范围，然后优先排查后端是否有接口仍信任前端传入的 `userId`。

## 测试结束后

如果只是临时测试，可以停止前端和后端进程。Docker 依赖按需停止：

```powershell
docker compose stop redis qdrant
docker stop piston_api
```

如果测试产生了大量 smoke 数据，可以后续单独清理测试账号和关联记录；不要直接清空生产演示库。
