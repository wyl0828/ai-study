# Demo Cases

本文档固定 3 个可演示题目和对应 bug 样例，用于稳定展示 AI Interview Coach Agent 的完整闭环：

```text
选择题目 -> 提交 bug 代码 -> Piston 判题失败 -> AI 诊断
-> 弱点记忆 / 错题卡 / 训练计划更新 -> 提交 fixed 代码通过
```

这些样例只服务于演示，不修改业务代码、不修改接口、不修改 `CodeWrapper`。

## 使用方式

1. 启动 MySQL、Redis、Piston、Spring Boot 后端和 Next.js 前端。
2. 打开对应题目页，例如 `/problem/101`。
3. 将 bug 文件内容复制到 Monaco Editor。
4. 点击“提交代码”，观察测试失败和失败用例。
5. 等待右侧“AI 诊断”展示错误类型、知识点、错误原因、改进建议和推荐训练。
6. 打开 `/dashboard`，观察弱点、错题卡、最近提交和训练计划变化。
7. 回到题目页，将 fixed 文件内容复制到 Monaco Editor。
8. 再次提交，确认修正后代码通过测试。

## Case 1: 101 两数之和

- 题目 ID：`101`
- 题目名称：两数之和
- 提交模式：ACM
- Bug 类型：HashMap 查询和写入顺序错误
- Bug 文件：`docs/demo-cases/101-two-sum-bug.java`
- Fixed 文件：`docs/demo-cases/101-two-sum-fixed.java`

### 演示用 bug 代码

```java
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] nums = new int[n];
        for (int i = 0; i < n; i++) {
            nums[i] = sc.nextInt();
        }
        int target = sc.nextInt();

        Map<Integer, Integer> indexByValue = new HashMap<>();
        for (int i = 0; i < n; i++) {
            indexByValue.put(nums[i], i);
            int need = target - nums[i];
            if (indexByValue.containsKey(need)) {
                System.out.println(indexByValue.get(need) + " " + i);
                return;
            }
        }

        System.out.println("-1 -1");
    }
}
```

### 修正后代码

```java
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] nums = new int[n];
        for (int i = 0; i < n; i++) {
            nums[i] = sc.nextInt();
        }
        int target = sc.nextInt();

        Map<Integer, Integer> indexByValue = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int need = target - nums[i];
            if (indexByValue.containsKey(need)) {
                System.out.println(indexByValue.get(need) + " " + i);
                return;
            }
            indexByValue.put(nums[i], i);
        }

        System.out.println("-1 -1");
    }
}
```

### 预期失败现象

Bug 代码会在当前元素刚写入 HashMap 后立即检查 complement，导致当前元素可以和自己匹配。例如：

```text
输入：
2
3 3
6

期望输出：
0 1

实际输出：
0 0
```

### 预期 AI 诊断方向

- 错误类型：`LOGIC_ERROR` 或 `BOUNDARY_ERROR`
- 关联知识点：HashMap 基础查找、数组遍历
- 具体问题：查询和写入顺序反了，导致自匹配或重复元素处理错误
- 改进建议：每轮先检查 `target - nums[i]` 是否已经出现，再写入当前 `nums[i]`

### Dashboard 预期更新内容

- HashMap / 数组遍历相关弱点分增加
- 新增一张两数之和错题卡
- 最新训练计划包含 HashMap 查询顺序、重复元素处理相关训练建议

## Case 2: 103 反转链表

- 题目 ID：`103`
- 题目名称：反转链表
- 提交模式：Solution
- Bug 类型：链表反转后返回错误头节点
- Bug 文件：`docs/demo-cases/103-reverse-list-bug.java`
- Fixed 文件：`docs/demo-cases/103-reverse-list-fixed.java`

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

Bug 代码虽然完成了指针反转，但最后返回的是原始 `head`。反转后原始头节点已经变成尾节点，因此输出会不完整。例如：

```text
输入：
5
1 2 3 4 5

期望输出：
5 4 3 2 1

实际输出：
1
```

### 预期 AI 诊断方向

- 错误类型：`LOGIC_ERROR`
- 关联知识点：链表指针、反转链表
- 具体问题：反转完成后新头节点是 `prev`，不是原始 `head`
- 改进建议：循环中保存 `next`，反转 `cur.next`，移动 `prev/cur`，最后返回 `prev`

### Dashboard 预期更新内容

- 链表指针相关弱点分增加
- 新增一张反转链表错题卡
- 最新训练计划包含链表指针移动、返回新头节点相关训练建议

## Case 3: 104 合并两个有序链表

- 题目 ID：`104`
- 题目名称：合并两个有序链表
- 提交模式：Solution
- Bug 类型：循环结束后忘记连接剩余链表
- Bug 文件：`docs/demo-cases/104-merge-two-lists-bug.java`
- Fixed 文件：`docs/demo-cases/104-merge-two-lists-fixed.java`

### 演示用 bug 代码

```java
class Solution {
    public ListNode mergeTwoLists(ListNode list1, ListNode list2) {
        ListNode dummy = new ListNode(0);
        ListNode tail = dummy;

        while (list1 != null && list2 != null) {
            if (list1.val <= list2.val) {
                tail.next = list1;
                list1 = list1.next;
            } else {
                tail.next = list2;
                list2 = list2.next;
            }
            tail = tail.next;
        }

        return dummy.next;
    }
}
```

### 修正后代码

```java
class Solution {
    public ListNode mergeTwoLists(ListNode list1, ListNode list2) {
        ListNode dummy = new ListNode(0);
        ListNode tail = dummy;

        while (list1 != null && list2 != null) {
            if (list1.val <= list2.val) {
                tail.next = list1;
                list1 = list1.next;
            } else {
                tail.next = list2;
                list2 = list2.next;
            }
            tail = tail.next;
        }

        tail.next = list1 != null ? list1 : list2;
        return dummy.next;
    }
}
```

### 预期失败现象

Bug 代码只处理两个链表都非空的部分，任意一条链表先走完后，另一条链表剩余节点没有接入结果。例如：

```text
输入：
3
1 2 4
3
1 3 4

期望输出：
1 1 2 3 4 4

实际输出：
1 1 2 3 4
```

### 预期 AI 诊断方向

- 错误类型：`LOGIC_ERROR` 或 `BOUNDARY_ERROR`
- 关联知识点：链表指针、链表合并、dummy 节点
- 具体问题：主循环结束后没有连接 `list1` 或 `list2` 的剩余部分
- 改进建议：使用 `dummy` 和 `tail` 合并主体后，执行 `tail.next = list1 != null ? list1 : list2`

### Dashboard 预期更新内容

- 链表合并 / 链表指针相关弱点分增加
- 新增一张合并两个有序链表错题卡
- 最新训练计划包含 dummy 节点、尾指针、剩余链表连接相关训练建议

## 演示顺序建议

推荐按以下顺序演示：

1. `101 两数之和`：最容易解释 HashMap 查询/写入顺序，适合作为 Agent Workflow 开场。
2. `104 合并两个有序链表`：能展示 Solution 模式和链表 dummy 节点。
3. `103 反转链表`：用于补充链表指针移动和返回值错误。

每个 case 都遵循同一套讲解节奏：

```text
复制 bug 代码 -> 提交失败 -> 观察 failedCases
-> 展示 AI 诊断 -> 展示 Dashboard 更新
-> 复制 fixed 代码 -> 重新提交通过
```
