# 更新维护流程

本文档记录 AI Interview Coach Agent 测试部署环境的日常更新、重启、验证和回滚流程。适用于当前单机部署方式：前端 Next.js、后端 Spring Boot、MySQL、Redis、Piston 同机运行，只对外暴露前端端口。

## 基本原则

本地改代码并提交到 Git，服务器只负责拉取、构建、重启和验证。不要在服务器上直接改业务代码，否则后续容易和 Git 版本冲突。

## 1. 本地提交并推送

在本地项目根目录确认变更、运行必要测试，然后推送到 GitHub：

```powershell
git status
git add .
git commit -m "更新说明"
git push origin main
```

如果本地开发分支还没有合并到 `main`，先在本地完成合并和验证，再推送 `main`。

## 2. 服务器拉取最新代码

SSH 登录服务器后：

```bash
cd /opt/ai-study
git status
git pull origin main
```

如果 `git status` 显示服务器上有未提交改动，先停下来确认来源，不要直接覆盖。正常测试部署环境中，服务器工作区应保持干净。

## 3. 后端更新

如果改动涉及 `backend/`、`data/`、根目录 `.env` 相关配置或 Java 后端逻辑：

```bash
cd /opt/ai-study/backend
mvn -q test
systemctl restart ai-study-backend
sleep 8
curl -I http://127.0.0.1:8080/api/problems
```

期望看到后端接口返回 `HTTP/1.1 200`。如果失败，查看日志：

```bash
journalctl -u ai-study-backend -n 120 --no-pager
tail -120 /opt/ai-study/backend.log
```

## 4. 前端更新

如果改动涉及 `frontend/`：

```bash
cd /opt/ai-study/frontend
npm install
npm run build
systemctl restart ai-study-frontend
sleep 3
curl -I http://127.0.0.1:4000/login
```

期望看到前端页面返回 `HTTP/1.1 200 OK`。如果失败，查看日志：

```bash
journalctl -u ai-study-frontend -n 120 --no-pager
tail -120 /opt/ai-study/frontend.log
ss -lntp | grep ':4000'
```

## 5. 前后端都更新

如果一次更新同时改了前后端，按后端、前端的顺序执行：

```bash
cd /opt/ai-study
git pull origin main

cd /opt/ai-study/backend
mvn -q test
systemctl restart ai-study-backend

cd /opt/ai-study/frontend
npm install
npm run build
systemctl restart ai-study-frontend

curl -I http://127.0.0.1:8080/api/problems
curl -I http://127.0.0.1:4000/login
```

浏览器再访问外部地址，例如：

```text
http://116.62.243.66:4000
```

## 6. 数据库迁移

如果新增了数据库迁移 SQL，不要重复执行所有历史 SQL。先备份，再执行新增迁移：

```bash
mkdir -p /opt/ai-study-backups
mysqldump -uroot -p123456 ai_interview_coach > /opt/ai-study-backups/ai_interview_coach-$(date +%F-%H%M).sql

cd /opt/ai-study
mysql --default-character-set=utf8mb4 -uroot -p123456 ai_interview_coach < data/your_new_migration.sql
```

执行后检查关键表或字段是否存在。不要把测试库直接清空，除非明确决定重置所有测试账号和学习记录。

## 7. systemd 常用命令

当前测试部署推荐用 systemd 托管前后端：

```bash
systemctl status ai-study-backend --no-pager
systemctl status ai-study-frontend --no-pager

systemctl restart ai-study-backend
systemctl restart ai-study-frontend

journalctl -u ai-study-backend -f
journalctl -u ai-study-frontend -f
```

Docker 里的 Piston 也需要保持运行：

```bash
docker ps
curl http://127.0.0.1:2238/api/v2/runtimes
```

期望能看到 `piston_api` 容器和 `java 15.0.2` runtime。

## 8. 更新后最小验收

每次更新后至少做一次小闭环：

1. 打开首页和登录页。
2. 登录一个测试账号。
3. 进入 `/problem/1`。
4. 提交一份错误 Java 代码，确认显示失败用例和 AI 诊断。
5. 回到 `/dashboard`，确认提交、错题和训练计划更新。
6. 如涉及登录或隔离改动，再切换第二个账号确认数据没有串号。

## 9. 快速回滚

如果新版本出问题，可以先回滚到上一个 Git 提交：

```bash
cd /opt/ai-study
git log --oneline -5
git reset --hard HEAD~1
```

然后按改动范围重新构建并重启服务。这个操作会丢弃服务器工作区未提交改动，所以只在确认服务器没有需要保留的手工修改时使用。

