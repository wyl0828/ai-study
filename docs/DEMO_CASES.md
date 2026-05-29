# Demo Cases

本文档固定 3 个 Hot100 精选演示题，用于稳定展示 AI Interview Coach Agent 的完整闭环：

```text
选择题目 -> 提交 bug 代码 -> Piston 判题失败 -> AI 诊断
-> 弱点记忆 / 错题卡 / 训练计划更新 -> 提交 fixed 代码通过
```

当前题库统一为 Java `class Solution` 模式。用户不需要处理标准输入输出；后端 `CodeWrapper` 注册表会在送入 Piston 前包装测试 harness。

题目页左侧内容来自后端 `problem` 表：题面按“任务说明 / 返回要求 / 约束与边界”组织，提示为预设 Level 1/2/3，题解包含解题思路、易错点、复杂度和完整 Java 参考实现；这些内容不触发 AI 调用。

结构化样例清单维护在 `docs/demo-cases/demo-cases.json`，其中记录当前活跃演示题的 bug/fixed 文件、预期失败用例、预期错误类型和知识点。新增或替换演示样例时，以该 manifest 为准，并运行 `node frontend/lib/demo-cases.node-test.cjs`。

## 使用方式

1. 启动 MySQL、Piston、Spring Boot 后端和 Next.js 前端；建议同时启动 Redis，用于题目列表、题目详情和模板热点缓存。Redis 不可用时题目接口会降级 MySQL，核心 demo 不依赖 Redis 作为事实源。
2. 打开对应题目页，例如 `/problem/1`。
3. 先展示左侧完整题面、预设提示和题解区域，说明题目内容与 AI 诊断边界。
4. 将 bug 代码复制到 Monaco Editor。
5. 点击“提交代码”，观察测试失败和失败用例。
6. 观察右侧“AI 诊断”中的 SSE Agent 时间线：失败提交应出现 `PLANNING -> CODE_EXECUTION -> OBSERVATION -> RAG_RETRIEVAL -> ERROR_CLASSIFICATION -> MEMORY_UPDATE -> TRAINING_PLAN -> COMPLETED`。
7. 等待诊断完成，确认右侧展示错误类型、知识点和教练报告：失败现象、根本原因、修改方向、面试提醒和推荐训练。
8. 打开 `/dashboard`，观察今日优先训练、完整训练计划、弱点排行、错误类型分布、合并错题卡和最近提交变化。
9. 回到题目页，将 fixed 代码复制到 Monaco Editor。
10. 再次提交，确认修正后代码通过测试，并查看 AC 代码点评；AC 分支应出现 `PLANNING -> CODE_EXECUTION -> OBSERVATION -> RAG_RETRIEVAL -> CODE_REVIEW -> COMPLETED`。

### Windows 前端端口注意

Windows 更新、Docker/WSL 或 Hyper-V 可能会保留 `3000` / `3001` 附近端口，导致 Next.js 启动时报 `EACCES: permission denied`。本项目的 `npm run dev` 已默认绑定 `127.0.0.1:4000`，演示时直接使用：

```powershell
cd D:\code\ai-study\frontend
npm run dev
```

如果需要确认 Windows 保留端口范围，可以执行：

```powershell
netsh interface ipv4 show excludedportrange protocol=tcp
netsh interface ipv6 show excludedportrange protocol=tcp
```

并访问 `http://127.0.0.1:4000/problem/1`。

## Case 1: 1 两数之和

- 题目 ID：`1`
- 题目名称：两数之和
- 提交模式：Solution
- Bug 类型：HashMap 查询和写入顺序错误

### 演示用 bug 代码

```java
import java.util.HashMap;
import java.util.Map;

class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> indexByValue = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            indexByValue.put(nums[i], i);
            int need = target - nums[i];
            if (indexByValue.containsKey(need)) {
                return new int[] {indexByValue.get(need), i};
            }
        }
        return new int[0];
    }
}
```

### 修正后代码

```java
import java.util.HashMap;
import java.util.Map;

class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> indexByValue = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int need = target - nums[i];
            if (indexByValue.containsKey(need)) {
                return new int[] {indexByValue.get(need), i};
            }
            indexByValue.put(nums[i], i);
        }
        return new int[0];
    }
}
```

### 预期失败现象

Bug 代码会在当前元素刚写入 HashMap 后立即检查 complement，导致当前元素可以和自己匹配。例如 `[3,3]`、`target=6` 会返回 `[0,0]`，期望为 `[0,1]`。

### 预期 AI 诊断方向

- 错误类型：`LOGIC_ERROR` 或 `BOUNDARY_ERROR`
- 关联知识点：HashMap 查找、数组遍历
- 具体问题：查询和写入顺序反了，导致自匹配或重复元素处理错误
- 改进建议：每轮先检查 `target - nums[i]` 是否已经出现，再写入当前 `nums[i]`

## Case 2: 206 反转链表

- 题目 ID：`206`
- 题目名称：反转链表
- 提交模式：Solution
- Bug 类型：链表反转后返回错误头节点

### 演示用 bug 代码

```java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode prev = null;
        ListNode cur = head;

        while (cur != null) {
            ListNode next = cur.next;
            cur.next = prev;
            prev = cur;
            cur = next;
        }

        return head;
    }
}
```

### 修正后代码

```java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode prev = null;
        ListNode cur = head;

        while (cur != null) {
            ListNode next = cur.next;
            cur.next = prev;
            prev = cur;
            cur = next;
        }

        return prev;
    }
}
```

### 预期失败现象

Bug 代码完成了指针反转，但最后返回的是原始 `head`。反转后原始头节点已经变成尾节点，因此 `[1,2,3,4,5]` 会输出 `[1]`，期望为 `[5,4,3,2,1]`。

### 预期 AI 诊断方向

- 错误类型：`LOGIC_ERROR`
- 关联知识点：链表指针、反转链表
- 具体问题：反转完成后新头节点是 `prev`，不是原始 `head`
- 改进建议：循环中保存 `next`，反转 `cur.next`，移动 `prev/cur`，最后返回 `prev`

## Case 3: 121 买卖股票的最佳时机

- 题目 ID：`121`
- 题目名称：买卖股票的最佳时机
- 提交模式：Solution
- 推荐用途：低门槛 AC 点评演示

### 推荐 AC 代码

```java
class Solution {
    public int maxProfit(int[] prices) {
        int minPrice = Integer.MAX_VALUE;
        int best = 0;

        for (int price : prices) {
            best = Math.max(best, price - minPrice);
            minPrice = Math.min(minPrice, price);
        }

        return best;
    }
}
```

### 预期 AC 点评方向

- 复杂度：时间 O(n)，空间 O(1)
- 代码风格：变量语义清楚，但可在面试表达中强调“最低买入价只来自当前天之前”
- 面试建议：说明为什么不能用未来最低价计算过去利润
- 可优化点：当前写法已足够简洁，可补充空数组或极短数组的解释

## 演示顺序建议

1. `1 两数之和`：最容易解释 HashMap 查询/写入顺序，适合作为 Agent Workflow 开场。
2. `206 反转链表`：展示 Solution 模式、链表结构注入和返回值错误诊断。
3. `121 买卖股票的最佳时机`：提交正确代码，展示 AC 后的轻量 codeReview 分支。

每个失败 case 都遵循同一套讲解节奏：

```text
复制 bug 代码 -> 提交失败 -> 观察 failedCases
-> 展示 SSE Agent 步骤和 RAG_RETRIEVAL -> 展示 AI 诊断 -> 展示 Dashboard 更新
-> 学习中心优先展示今日该练什么，并把重复错题合并为可复盘资产
-> 复制 fixed 代码 -> 重新提交通过
```
