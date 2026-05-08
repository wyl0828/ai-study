# 力扣式代码提交体验方案

## 1. 设计目标

将部分题目从 ACM 完整代码模式改为 LeetCode Solution 函数模式：

```text
当前：用户写完整 Main（Scanner、main、链表构造、输出）
目标：用户只写 Solution 类，后端自动包装成完整 Main
```

首批试点：problemId=103（反转链表）

## 2. 数据流

```text
用户提交 Solution 代码
    ↓
Submission 表保存原始代码（用于 AI 诊断）
    ↓
CodeWrapper.wrap(problemId, code)
    ↓
包装成完整 Main 程序
    ↓
Piston 执行包装后的代码
    ↓
返回测试结果
```

## 3. 改动文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/.../judge/CodeWrapper.java` | 新建 | 代码包装器 |
| `backend/.../service/impl/JudgeServiceImpl.java` | 修改 | 调用 Piston 前包装代码 |
| `data/problems.sql` | 修改 | problemId=103 的 template_code |

## 4. CodeWrapper.java 设计

位置：`backend/src/main/java/com/interview/coach/judge/CodeWrapper.java`

```java
package com.interview.coach.judge;

public class CodeWrapper {

    /**
     * 将用户 Solution 代码包装成完整 Main 程序
     * 当前只支持 problemId=103（反转链表）
     * 其他题目原样返回
     */
    public static String wrap(Long problemId, String userCode) {
        if (problemId == null || userCode == null) {
            return userCode;
        }

        if (Long.valueOf(103L).equals(problemId)) {
            return wrapReverseList(userCode);
        }

        return userCode;
    }

    private static String wrapReverseList(String solutionCode) {
        return """
                import java.util.*;

                class ListNode {
                    int val;
                    ListNode next;
                    ListNode(int val) { this.val = val; }
                }

                public class Main {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);

                        int n = sc.nextInt();

                        ListNode dummy = new ListNode(0);
                        ListNode tail = dummy;

                        for (int i = 0; i < n; i++) {
                            tail.next = new ListNode(sc.nextInt());
                            tail = tail.next;
                        }

                        Solution solution = new Solution();
                        ListNode result = solution.reverseList(dummy.next);

                        while (result != null) {
                            System.out.print(result.val);
                            if (result.next != null) {
                                System.out.print(" ");
                            }
                            result = result.next;
                        }
                    }
                }

                """ + solutionCode;
    }
}
```

### 关键约束

- 只能有一个 `public class Main`
- `Solution` 不能是 `public`
- `ListNode` 必须和 `Main`、`Solution` 同级

## 5. JudgeServiceImpl.java 修改

修改位置：`judge` 方法中调用 `sendToPiston` 之前

```java
// 当前代码
PistonResponse response = sendToPiston(language, code, stdin);

// 修改后
String wrappedCode = CodeWrapper.wrap(submission.getProblemId(), code);
PistonResponse response = sendToPiston(language, wrappedCode, stdin);
```

注意：
- `submission` 表仍然保存原始 `code`
- 只有发送给 Piston 的代码是包装后的

## 6. 前端模板修改

### 6.1 修改 SQL 文件

修改 `data/problems.sql` 中 problemId=103 的 `template_code`：

```sql
UPDATE problem SET template_code = 'class Solution {
    public ListNode reverseList(ListNode head) {
        // 请在这里实现反转链表逻辑
        return null;
    }
}' WHERE id = 103;
```

### 6.2 手动更新当前数据库

**重要**：如果数据库已经初始化过，只改 `data/problems.sql` 不会自动生效。

需要手动执行以下 SQL：

```sql
UPDATE problem
SET template_code = 'class Solution {
    public ListNode reverseList(ListNode head) {
        // 请在这里实现反转链表逻辑
        return null;
    }
}'
WHERE id = 103;
```

验证更新：

```sql
SELECT id, title, template_code FROM problem WHERE id = 103;
```

## 7. 测试用例

problemId=103 的测试用例（已存在于 test_case 表）：

| case_id | input | expected_output |
|---------|-------|-----------------|
| 1 | `5\n1 2 3 4 5` | `5 4 3 2 1` |
| 2 | `1\n42` | `42` |
| 3 | `0` | （空） |
| 4 | `3\n1 1 1` | `1 1 1` |

## 8. 验证方式

```bash
# 提交正确的 reverseList Solution
curl -s -X POST "http://localhost:8080/api/submissions" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "problemId": 103,
    "language": "java",
    "code": "class Solution {\n    public ListNode reverseList(ListNode head) {\n        ListNode prev = null;\n        ListNode curr = head;\n        while (curr != null) {\n            ListNode next = curr.next;\n            curr.next = prev;\n            prev = curr;\n            curr = next;\n        }\n        return prev;\n    }\n}"
  }'
```

预期返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "status": "ACCEPTED",
    "passedCount": 4,
    "totalCount": 4
  }
}
```

## 9. 验证清单

| 验证项 | 预期结果 |
|--------|----------|
| 正确 Solution 通过所有测试 | status=ACCEPTED, passedCount=4 |
| Submission 表保存原始 Solution | 不含 ListNode、Main 等包装代码 |
| AI 诊断使用原始 Solution | 不分析 Scanner、main 等包装代码 |
| problemId=101 不受影响 | 仍使用完整 Main 模式 |

## 10. 实现约束

本次实现**只改三个地方**：

1. 新建 `CodeWrapper.java`
2. 修改 `JudgeServiceImpl.java`
3. 修改 problemId=103 的 template_code

**不要做**：

- 不要实现草稿持久化
- 不要新增 codeMode 字段
- 不要改其他题目
- 不要改 Submission 表结构

## 11. 风险点

| 风险 | 应对 |
|------|------|
| ListNode 定义冲突 | 包装代码中 ListNode 在 Main 外部定义 |
| Solution 不能是 public | 包装代码中 Solution 不加 public |
| 测试用例输入格式不匹配 | 确保包装代码读取逻辑与测试用例一致 |
| 其他题目受影响 | 只对 problemId=103 生效，其他题目原样返回 |

## 11. 后续扩展

功能稳定后，可以：

1. 新增 `code_mode` 字段到 problem 表（`acm` / `solution`）
2. 将 CodeWrapper 改为模板驱动（从数据库读取包装模板）
3. 逐步将其他链表题、树题改为 Solution 模式：
   - problemId=104：合并两个有序链表
   - problemId=105：二叉树的最大深度
   - problemId=106：二叉树层序遍历
