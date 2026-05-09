# 做题草稿自动保存方案

## 1. 设计原则

```text
当前阶段：localStorage 草稿缓存（零后端改动）
代码结构：draft.ts 抽象隔离，页面不直接操作 localStorage
升级路径：登录完成后 draft.ts 接后端接口，页面不用改
正式数据：submission/ai_diagnosis/hint_record/user_weakness/training_plan 仍由后端 MySQL 保存
```

## 2. 数据结构

```typescript
// frontend/lib/draft.ts

export interface ProblemDraft {
  userId: number;
  problemId: number;
  code: string;
  language: "java";
  updatedAt: string;  // ISO 时间戳

  lastResult?: {
    submissionId: number;
    status: string;
    passedCount: number;
    totalCount: number;
    errorMessage: string | null;
    submittedAt: string;
    codeSnapshot: string;  // 提交时的代码快照
  };

  lastDiagnosis?: {
    submissionId: number;
    agentRunId?: number;
    errorType: string;
    knowledgePoint: string;
    diagnosis: string;
    hintLevel1: string;
    hintLevel2: string;
    hintLevel3: string;
    codeSnapshot: string;  // 诊断时的代码快照
  };
}
```

存储 key：`interview_coach_draft_${userId}_${problemId}`

## 3. API 抽象层

页面只调用这些函数，不关心底层是 localStorage 还是后端：

```typescript
saveDraft(userId: number, problemId: number, draft: Partial<ProblemDraft>): void
loadDraft(userId: number, problemId: number): ProblemDraft | null
clearDraft(userId: number, problemId: number): void
formatDraftTime(isoString: string): string  // → "05-08 09:06"
```

核心字段校验（缺失则返回 null）：

- userId
- problemId
- code
- language
- updatedAt

非核心字段缺失时仍恢复：

- lastResult（可选）
- lastDiagnosis（可选）

## 4. 页面加载流程

```text
进入 /problem/[id]
    │
    ├─ page.tsx 服务端请求题目详情
    │
    ├─ ProblemWorkspace 浏览器端请求 /api/problems/{id}/template
    │
    ├─ loadDraft(1, problemId)
    │     ├─ 解析失败 → 返回 null，静默忽略
    │     └─ 核心字段缺失 → 返回 null
    │
    ├─ 草稿存在？
    │     ├─ 是 → 恢复 code/lastResult/lastDiagnosis
    │     │       显示提示条："草稿已自动保存于 05-08 09:06"
    │     └─ 否 → 使用后端返回的 templateCode
    │
    └─ 渲染页面
```

## 5. UI 交互设计

### 5.1 草稿恢复提示条

位置：代码编辑器顶部，工具栏下方

```text
┌─────────────────────────────────────────────────┐
│ [Java] 解题代码.java              [重置代码]     │
├─────────────────────────────────────────────────┤
│ ℹ 草稿已自动保存于 05-08 09:06         [关闭]    │
├─────────────────────────────────────────────────┤
│                                                 │
│  (Monaco Editor)                                │
│                                                 │
└─────────────────────────────────────────────────┘
```

- 点击 [关闭] 隐藏提示条（本次会话内不再显示）
- 切换题目后重新判断

### 5.2 重置代码按钮

位置：编辑器工具栏，"提交代码"按钮旁边

确认弹窗："确定要重置代码吗？将恢复为默认模板并清除运行结果。"

确认后执行：

1. clearDraft(1, problemId)
2. 重新请求 /api/problems/{id}/template
3. code 恢复为后端返回的 templateCode
4. submissionResult 清空
5. diagnosis 清空
6. activeTab 重置为 "test"
7. 草稿提示条隐藏

### 5.3 通过状态逻辑

```typescript
const isCurrentCodeAccepted =
  lastResult?.status === "ACCEPTED" &&
  lastResult.codeSnapshot === code;
```

展示逻辑：

- 代码未修改 → 显示 "已通过" 标签（不可点击提交）
- 代码已修改 → 显示 "重新提交" 按钮（可点击）

### 5.4 诊断过期提示

```typescript
const isDiagnosisStale =
  lastDiagnosis &&
  lastDiagnosis.codeSnapshot !== code;
```

当诊断过期时，在 AI 诊断面板顶部显示：

```text
⚠ 该诊断基于上次提交，当前代码已修改，仅供参考。
```

## 6. 保存触发时机

| 事件 | 保存内容 | 防抖 |
|------|----------|------|
| 编辑代码 | code + updatedAt | 1 秒防抖 |
| 提交成功 | lastResult（含 codeSnapshot）+ updatedAt | 立即 |
| AI 诊断完成 | lastDiagnosis（含 codeSnapshot）+ updatedAt | 立即 |
| 重置代码 | 清除草稿 | 立即 |

## 7. 异常处理

| 场景 | 处理 |
|------|------|
| localStorage 读取失败 | 静默忽略，使用默认模板 |
| JSON 解析失败 | 静默忽略，使用默认模板 |
| 核心字段缺失 | 返回 null，使用默认模板 |
| 非核心字段缺失 | 缺失字段忽略，仍恢复其余内容 |
| localStorage 写满 | 捕获异常，静默失败，不影响使用 |

## 8. 代码改动清单

| 文件 | 改动类型 | 说明 |
|------|----------|------|
| `lib/draft.ts` | 新建 | 草稿读写抽象层，页面不直接操作 localStorage |
| `components/ProblemWorkspace.tsx` | 修改 | 客户端读取模板、初始化读草稿、提交/诊断后存草稿、重置功能、通过状态判断 |
| `components/CodeEditor.tsx` | 修改 | 新增草稿提示条、重置按钮、模板加载状态、通过/重新提交状态、诊断过期提示 |

## 9. 强制约束

```text
ProblemWorkspace.tsx 和 CodeEditor.tsx 不允许直接操作 localStorage，
只能调用 frontend/lib/draft.ts。
```

原因：后续接后端时，只需替换 draft.ts 内部实现，页面不用大改。

## 10. 后续升级路径

### 第一步：新增后端表

```sql
CREATE TABLE user_problem_draft (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  problem_id BIGINT NOT NULL,
  language VARCHAR(32) NOT NULL DEFAULT 'java',
  code TEXT NOT NULL,
  last_submission_id BIGINT NULL,
  updated_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL,
  UNIQUE KEY uk_user_problem (user_id, problem_id)
);
```

### 第二步：新增后端接口

```http
GET    /api/users/{userId}/drafts/{problemId}
PUT    /api/users/{userId}/drafts/{problemId}
DELETE /api/users/{userId}/drafts/{problemId}
```

请求：

```json
{
  "language": "java",
  "code": "class Solution { ... } 或 public class Main { ... }",
  "lastSubmissionId": 1001
}
```

响应：

```json
{
  "problemId": 101,
  "userId": 1,
  "language": "java",
  "code": "class Solution { ... } 或 public class Main { ... }",
  "lastSubmissionId": 1001,
  "updatedAt": "2026-05-08 12:30:00"
}
```

### 第三步：升级 draft.ts 内部实现

```text
进入页面 → 先请求后端草稿
本地编辑 → 先写 localStorage + 1-2 秒防抖同步到后端
冲突处理 → 比较 updatedAt，保留更新的
```

页面逻辑不用改，只改 draft.ts 内部实现。
