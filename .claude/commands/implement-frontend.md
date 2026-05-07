# AI Interview Coach 前端实现与完善工作流

## 项目状态

前端三页面已基本实现，`npm run build` 通过。当前任务是**完善细节、修复问题、对齐原型**。

## 工作目录

- **前端代码**: `D:\code\ai-study\frontend`
- **HTML 原型**: `D:\code\ai-study\stitch_front_end_interface_design\mvp\`
- **API 文档**: `D:\code\ai-study\docs\API.md`

## 核心约束

1. **不要实现**模拟面试、职业信号、工程等级等超出 MVP 的功能
2. **Dashboard 用 mock 数据**（接口未暴露）
3. **SSE 暂不实现**，先用同步 `POST /api/agent/analyze`
4. **userId 固定为 1**，**语言固定为 java**

## 实现检查清单

按优先级逐项检查，发现问题立即修复：

### P0：核心链路

#### 1. 配置文件

- [ ] `tailwind.config.ts` — design tokens 完整（primary #005a90、surface 系列）
- [ ] `next.config.mjs` — API 代理 `/api/:path*` → `http://localhost:8080/api/:path*`
- [ ] `globals.css` — Google Fonts（Inter + JetBrains Mono）、Material Symbols、scrollbar

#### 2. 类型与 API

- [ ] `lib/types.ts` — 与 `docs/API.md` 完全对齐
- [ ] `lib/api.ts` — request() 处理业务 code（code !== 0 抛错）
- [ ] `lib/mock.ts` — Dashboard mock 数据完整

#### 3. 做题页（核心）

- [ ] `app/problem/[id]/page.tsx` — 服务端获取 problem + template
- [ ] `components/ProblemDescription.tsx` — 左栏题目描述，参考 `problem.html` 原型
- [ ] `components/CodeEditor.tsx` — Monaco Editor，顶栏有文件标签 + 重置 + 提交
- [ ] `components/ProblemWorkspace.tsx` — 提交流程正确：
  - 提交 → 切到测试结果 Tab
  - 未通过 → 自动触发 AI 诊断 → 切到 AI 诊断 Tab
  - 通过 → AI 诊断 Tab 显示"已通过"
- [ ] `components/ResultPanel.tsx` — Tab 切换容器
- [ ] `components/TestResult.tsx` — 状态标签 + 通过率 + 失败用例
- [ ] `components/AiDiagnosis.tsx` — 错误类型 + 知识点 + 诊断 + 训练计划标题
- [ ] `components/HintPanel.tsx` — Level 1/2/3 分层展示，L1 默认展开
- [ ] `components/DifficultyBadge.tsx` — EASY/MEDIUM/HARD 颜色

#### 4. 导航栏

- [ ] `components/Navbar.tsx` — 三个页面链接，usePathname() 高亮
- [ ] 高度 56px，做题页 `h-[calc(100vh-56px)]` 正确

### P1：补全页面

#### 5. 首页

- [ ] `app/page.tsx` — 服务端获取题目列表
- [ ] `components/HomeClient.tsx` — 筛选（难度、分类）+ 搜索 + 卡片网格
- [ ] `components/ProblemCard.tsx` — 卡片设计，Link 到做题页
- [ ] 参考 `home.html` 原型还原设计

#### 6. 仪表盘

- [ ] `app/dashboard/page.tsx` — 统计概览 + 两栏布局
- [ ] `components/WeaknessList.tsx` — 薄弱点排行
- [ ] `components/SubmissionHistory.tsx` — 提交历史
- [ ] `components/MistakeCards.tsx` — 错题卡片
- [ ] `components/TrainingPlan.tsx` — 训练计划
- [ ] 参考 `dashboard.html` 原型还原设计

### P2：体验优化

- [ ] SSE 流式步骤展示
- [ ] loading 状态（骨架屏）
- [ ] error 边界处理
- [ ] 空状态 UI
- [ ] 响应式适配

## 执行方式

当用户说以下关键词时触发对应操作：

### "检查前端" / "检查状态"

运行 `npm run build`，报告编译错误和警告。

### "修复 + 具体问题"

根据用户描述的问题定位文件并修复。

### "对齐原型"

读取 `stitch_front_end_interface_design/mvp/` 下的 HTML 原型，对比当前组件，修复差异。

### "完善 + 页面名"

针对指定页面（首页/做题页/仪表盘）进行细节完善。

### "实现 P2"

开始实现 P2 阶段的体验优化功能。

## 关键代码模式

### 提交流程（ProblemWorkspace.tsx）

```tsx
async function handleSubmit() {
  setIsSubmitting(true)
  setError(null)
  try {
    const { data: result } = await submissionApi.submit({
      userId: 1, problemId, language: "java", code
    })
    setSubmissionResult(result)
    setActiveTab("test")  // 先展示测试结果

    if (result.status !== "ACCEPTED") {
      setIsAnalyzing(true)
      try {
        const { data: diag } = await agentApi.analyze(result.submissionId)
        setDiagnosis(diag)
        setActiveTab("diagnosis")  // 诊断完成后再切
      } catch {
        setError("AI 诊断失败")
      } finally {
        setIsAnalyzing(false)
      }
    }
  } catch (e) {
    setError((e as Error).message)
  } finally {
    setIsSubmitting(false)
  }
}
```

### API 请求（lib/api.ts）

```ts
async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${url}`, init)
  if (!res.ok) throw new Error(`请求失败：${res.status}`)
  const json = await res.json()
  if (typeof json.code !== "undefined" && json.code !== 0) {
    throw new Error(json.message || "接口返回异常")
  }
  return json
}
```

## 验证命令

```bash
cd D:\code\ai-study\frontend
npm run build    # 编译检查
npm run dev      # 启动开发服务器 → http://localhost:3000
```

浏览器验证顺序：
1. 首页加载题目列表
2. 点击题目 → 做题页 Monaco 加载
3. 提交代码 → 测试结果展示
4. 提交失败 → AI 诊断自动触发
5. Dashboard mock 数据展示
6. 导航栏跳转正常
