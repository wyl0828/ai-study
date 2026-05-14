USE ai_interview_coach;

INSERT INTO `user` (id, username, password_hash, email, created_at, updated_at) VALUES
(1, 'demo', 'demo-only-not-for-production', 'demo@example.com', NOW(), NOW())
ON DUPLICATE KEY UPDATE username = VALUES(username), email = VALUES(email), updated_at = NOW();

DELETE FROM test_case;
DELETE FROM problem_knowledge_point;
DELETE FROM problem;
DELETE FROM knowledge_point;

INSERT INTO knowledge_point (id, name, category, description) VALUES
(1, '数组遍历', 'Array', '遍历数组并处理下标边界。'),
(2, 'HashMap 查找', 'HashMap', '使用 HashMap 或 HashSet 完成快速查询。'),
(3, '分组计数', 'HashMap', '用计数特征或排序特征完成分组。'),
(4, '链表指针', 'LinkedList', '安全移动、重连链表指针并处理空链表。'),
(5, '快慢指针', 'LinkedList', '用快慢指针判断链表结构。'),
(6, '二叉树递归', 'Tree', '使用递归处理二叉树，并写清楚终止条件。'),
(7, '二叉树层序遍历', 'Tree', '使用队列完成二叉树按层遍历。'),
(8, '动态规划状态', 'DynamicProgramming', '定义状态、初始化和状态转移。'),
(9, '贪心维护最优值', 'Greedy', '遍历时维护当前最优候选。');

INSERT INTO problem
(id, title, description, difficulty, category, input_format, output_format, code_mode, template_code, solution_outline, hint_level1, hint_level2, hint_level3, enabled, created_at, updated_at)
VALUES
(1, '两数之和',
'任务说明：
给定一个整数数组 nums 和一个目标值 target，请在数组中找到两个不同位置的元素，使它们的和等于 target。你需要返回这两个元素在原数组中的下标，而不是返回元素值。

返回要求：
返回一个长度为 2 的 int[]，表示两个下标。任意一个合法顺序都可以通过判题。

约束与边界：
测试数据保证存在且只需要返回一个答案。同一个数组元素不能被使用两次；数组中可能出现重复数字，重复值场景要特别注意查询和写入 HashMap 的顺序。',
'EASY', 'HashMap',
'第 1 行为数组长度 n，第 2 行为 n 个整数，第 3 行为 target。',
'返回 int[]，判题输出格式为 [i,j]。',
'solution',
'class Solution {
    public int[] twoSum(int[] nums, int target) {
        // 请返回两个不同下标
        return new int[0];
    }
}',
'方法一：哈希表

解题思路：
遍历 nums 时，把已经看过的数字和下标存入 HashMap。对当前 nums[i]，先查 target - nums[i] 是否已经出现，出现就返回两个下标；否则再记录当前数字。

易错点：
查询必须发生在写入当前数字之前，否则重复值场景可能错误地使用同一个元素两次。

复杂度：
时间复杂度 O(n)，空间复杂度 O(n)。

Java 参考实现：
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
```',
'不要先写双重循环，先想如何快速知道另一个数是否出现过。',
'用 HashMap 保存已经遍历过的数值到下标。',
'每轮先查 target - nums[i]，没有找到时再 put 当前 nums[i]。', 1, NOW(), NOW()),
(49, '字母异位词分组',
'任务说明：
给定一个字符串数组 strs，请把互为字母异位词的字符串归到同一组。字母异位词表示两个字符串包含相同字符及相同出现次数，但字符顺序可以不同。

返回要求：
返回 List<List<String>>，每个内部列表是一组异位词。组内顺序和组间顺序不作为核心考点，判题会做规范化比较。

约束与边界：
字符串可能为空串，也可能存在多个互不相关的分组。请为每个字符串构造稳定的分组 key，例如排序后的字符串或字符计数签名。',
'MEDIUM', 'HashMap',
'第 1 行为字符串数量 n，第 2 行起为 n 个小写字符串。',
'返回 List<List<String>>，判题会对组内和组间排序后比较。',
'solution',
'class Solution {
    public List<List<String>> groupAnagrams(String[] strs) {
        // 请返回异位词分组
        return new ArrayList<>();
    }
}',
'方法一：排序签名

解题思路：
互为异位词的字符串排序后结果相同。用排序后的字符串作为 key，把原字符串加入同一个列表。

易错点：
不要只比较长度；输出顺序不用依赖插入顺序，判题会规范化。

复杂度：
设最长字符串长度为 k，总字符串数为 n，时间复杂度 O(n * k log k)。

Java 参考实现：
```java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Solution {
    public List<List<String>> groupAnagrams(String[] strs) {
        Map<String, List<String>> groups = new HashMap<>();
        for (String str : strs) {
            char[] chars = str.toCharArray();
            Arrays.sort(chars);
            String key = new String(chars);
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(str);
        }
        return new ArrayList<>(groups.values());
    }
}
```',
'异位词共享同一种字符组成。',
'可以把每个字符串排序后作为分组 key。',
'遍历 strs，计算 key，map.computeIfAbsent(key, ...).add(str)。', 1, NOW(), NOW()),
(128, '最长连续序列',
'任务说明：
给定一个未排序整数数组 nums，请找出数组中最长连续数字序列的长度。连续序列指若干整数可以组成 x、x + 1、x + 2 这样的连续关系，这些数字在原数组中不要求相邻，出现顺序也不重要。

返回要求：
返回最长连续序列的长度；如果数组为空，返回 0。

约束与边界：
数组可能包含重复数字、负数和已经打乱顺序的数字。面试中通常期望接近 O(n) 的做法，重点是避免从每个数字都重复扩展同一段连续序列。',
'MEDIUM', 'HashMap',
'第 1 行为数组长度 n，第 2 行为 n 个整数。',
'返回整数。',
'solution',
'class Solution {
    public int longestConsecutive(int[] nums) {
        // 请返回最长连续序列长度
        return 0;
    }
}',
'方法一：HashSet 起点扩展

解题思路：
把所有数字放入 HashSet。只有当 x - 1 不存在时，x 才是一段连续序列的起点，然后不断查 x + 1、x + 2。

易错点：
如果从每个数都向后扩展，最坏会退化；只从起点扩展才能保持线性。

复杂度：
时间复杂度 O(n)，空间复杂度 O(n)。

Java 参考实现：
```java
import java.util.HashSet;
import java.util.Set;

class Solution {
    public int longestConsecutive(int[] nums) {
        Set<Integer> values = new HashSet<>();
        for (int num : nums) {
            values.add(num);
        }

        int best = 0;
        for (int num : values) {
            if (values.contains(num - 1)) {
                continue;
            }
            int current = num;
            int length = 1;
            while (values.contains(current + 1)) {
                current++;
                length++;
            }
            best = Math.max(best, length);
        }
        return best;
    }
}
```',
'先判断一个数字是不是连续段的起点。',
'用 HashSet 支持 O(1) 查询 x - 1 和 x + 1。',
'只有 !set.contains(x - 1) 时才向后 while 扩展并更新答案。', 1, NOW(), NOW()),
(206, '反转链表',
'任务说明：
给定一个单链表的头节点 head，请将整个链表原地反转。反转后，原来的尾节点会成为新的头节点，所有节点的 next 指针方向都需要被重新连接。

返回要求：
返回反转后链表的新头节点；如果链表为空，返回 null。

约束与边界：
链表长度可能为 0 或 1。修改 next 指针前必须保存后继节点，否则容易丢失未处理的后半段链表。推荐使用 O(1) 额外空间的迭代写法。',
'EASY', 'LinkedList',
'第 1 行为链表长度 n，第 2 行为 n 个节点值。',
'返回 ListNode，判题输出格式为 [v1,v2,...]。',
'solution',
'class Solution {
    public ListNode reverseList(ListNode head) {
        // 请返回反转后的头节点
        return null;
    }
}',
'方法一：迭代反转

解题思路：
用 prev、cur 两个指针向后推进。每次先保存 cur.next，再把 cur.next 指向 prev，最后同步移动 prev 和 cur。

易错点：
改指针前必须保存 next，否则会丢失原链表后半段。

复杂度：
时间复杂度 O(n)，空间复杂度 O(1)。

Java 参考实现：
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
```',
'反转链表的核心是逐个改变 next 指向。',
'使用 prev、cur、next 三个变量避免断链。',
'循环中先保存 next = cur.next，再 cur.next = prev，最后移动 prev 和 cur。', 1, NOW(), NOW()),
(21, '合并两个有序链表',
'任务说明：
给定两个按升序排列的单链表 list1 和 list2，请把它们合并成一个新的升序链表。合并过程可以复用原链表节点，不需要创建所有新节点。

返回要求：
返回合并后链表的头节点；如果两个链表都为空，返回 null。

约束与边界：
任意一个链表都可能为空，两个链表的长度也可能不同。比较当前两个节点时把较小节点接到结果链表尾部，循环结束后不要忘记接上剩余链表。',
'EASY', 'LinkedList',
'依次输入 list1 长度和值，再输入 list2 长度和值。',
'返回 ListNode，判题输出格式为 [v1,v2,...]。',
'solution',
'class Solution {
    public ListNode mergeTwoLists(ListNode list1, ListNode list2) {
        // 请返回合并后的链表
        return null;
    }
}',
'方法一：哨兵节点

解题思路：
创建 dummy 和 tail。比较两个链表当前节点，把较小节点接到 tail 后，再移动对应链表和 tail。

易错点：
循环结束后要把剩余非空链表整体接到 tail.next。

复杂度：
时间复杂度 O(n + m)，空间复杂度 O(1)。

Java 参考实现：
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
```',
'两个链表已经有序，可以像归并排序一样合并。',
'dummy 节点能简化头节点处理。',
'while 两个链表都不为空时接较小节点，最后 tail.next 接剩余链表。', 1, NOW(), NOW()),
(141, '环形链表',
'任务说明：
给定一个单链表的头节点 head，请判断链表中是否存在环。环表示某个节点的 next 指针指向链表中已经出现过的节点，使得继续沿 next 前进会重复访问节点。

返回要求：
如果链表中存在环，返回 true；否则返回 false。

约束与边界：
链表可能为空，也可能只有一个节点。不要尝试无限遍历链表；可以使用快慢指针在 O(1) 额外空间内检测是否相遇。',
'EASY', 'LinkedList',
'第 1 行为链表长度 n，第 2 行为节点值，第 3 行为 pos，-1 表示无环。',
'返回 boolean。',
'solution',
'class Solution {
    public boolean hasCycle(ListNode head) {
        // 请判断链表是否有环
        return false;
    }
}',
'方法一：快慢指针

解题思路：
slow 每次走一步，fast 每次走两步。如果链表有环，fast 最终会追上 slow；如果无环，fast 会先到 null。

易错点：
循环条件要同时保证 fast 和 fast.next 不为空。

复杂度：
时间复杂度 O(n)，空间复杂度 O(1)。

Java 参考实现：
```java
class Solution {
    public boolean hasCycle(ListNode head) {
        ListNode slow = head;
        ListNode fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) {
                return true;
            }
        }
        return false;
    }
}
```',
'环可以用两个速度不同的指针检测。',
'fast 每次两步，slow 每次一步。',
'当 fast == slow 返回 true；循环结束说明无环。', 1, NOW(), NOW()),
(102, '二叉树的层序遍历',
'任务说明：
给定一棵二叉树的根节点 root，请按照从上到下、每层从左到右的顺序访问节点。你需要把同一层的节点值放在同一个列表中。

返回要求：
返回 List<List<Integer>>，外层列表表示层，内层列表表示该层从左到右的节点值。空树返回空列表。

约束与边界：
树可能为空，也可能只有一条链。使用队列做 BFS 时，每一层开始前要固定当前队列长度，避免把下一层新加入的节点混入当前层。',
'MEDIUM', 'Tree',
'第 1 行为层序 token 数量 n，第 2 行为 n 个 token，null 表示空节点。',
'返回 List<List<Integer>>，判题输出格式为 [[...],[...]]。',
'solution',
'class Solution {
    public List<List<Integer>> levelOrder(TreeNode root) {
        // 请返回层序遍历结果
        return new ArrayList<>();
    }
}',
'方法一：队列 BFS

解题思路：
使用队列保存当前待访问节点。每一轮先记录当前队列长度 size，只处理这一层的 size 个节点。

易错点：
不要在同一层循环中无限读取新加入的下一层节点。

复杂度：
时间复杂度 O(n)，空间复杂度 O(n)。

Java 参考实现：
```java
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

class Solution {
    public List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        if (root == null) {
            return result;
        }

        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        while (!queue.isEmpty()) {
            int size = queue.size();
            List<Integer> level = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                TreeNode node = queue.poll();
                level.add(node.val);
                if (node.left != null) {
                    queue.offer(node.left);
                }
                if (node.right != null) {
                    queue.offer(node.right);
                }
            }
            result.add(level);
        }
        return result;
    }
}
```',
'层序遍历天然适合队列。',
'每轮用当前 queue.size() 固定本层节点数。',
'弹出本层节点、记录值，并把非空左右孩子加入队列。', 1, NOW(), NOW()),
(104, '二叉树的最大深度',
'任务说明：
给定一棵二叉树的根节点 root，请计算从根节点到最远叶子节点路径上的节点数量。这个数量就是二叉树的最大深度。

返回要求：
返回最大深度对应的整数；空树的最大深度为 0。

约束与边界：
树可能为空，左右子树高度可能明显不平衡。递归解法需要写清楚空节点终止条件，并同时比较左右子树深度。',
'EASY', 'Tree',
'第 1 行为层序 token 数量 n，第 2 行为 n 个 token，null 表示空节点。',
'返回整数。',
'solution',
'class Solution {
    public int maxDepth(TreeNode root) {
        // 请返回最大深度
        return 0;
    }
}',
'方法一：递归

解题思路：
空节点深度为 0，非空节点深度等于左右子树最大深度加 1。

易错点：
不要只沿一侧递归；需要同时比较左右子树。

复杂度：
时间复杂度 O(n)，递归栈空间 O(h)。

Java 参考实现：
```java
class Solution {
    public int maxDepth(TreeNode root) {
        if (root == null) {
            return 0;
        }
        return Math.max(maxDepth(root.left), maxDepth(root.right)) + 1;
    }
}
```',
'深度问题可以拆成左右子树深度。',
'root == null 时返回 0。',
'返回 Math.max(maxDepth(root.left), maxDepth(root.right)) + 1。', 1, NOW(), NOW()),
(226, '翻转二叉树',
'任务说明：
给定一棵二叉树的根节点 root，请将整棵树左右翻转。对每个节点来说，它的左子树和右子树都需要互换，子树内部也要继续执行同样的翻转。

返回要求：
返回翻转后的根节点；如果输入为空树，返回 null。

约束与边界：
树可能为空，也可能高度不平衡。递归或迭代都可以，关键是处理每个节点时不要丢失原来的左右子树引用。',
'EASY', 'Tree',
'第 1 行为层序 token 数量 n，第 2 行为 n 个 token，null 表示空节点。',
'返回 TreeNode，判题输出翻转后的层序数组。',
'solution',
'class Solution {
    public TreeNode invertTree(TreeNode root) {
        // 请返回翻转后的根节点
        return root;
    }
}',
'方法一：递归交换

解题思路：
对每个节点交换 left 和 right，然后递归处理交换后的左右子树。

易错点：
交换后再递归或先递归再交换都可以，但不要丢失某一侧子树引用。

复杂度：
时间复杂度 O(n)，递归栈空间 O(h)。

Java 参考实现：
```java
class Solution {
    public TreeNode invertTree(TreeNode root) {
        if (root == null) {
            return null;
        }
        TreeNode left = root.left;
        root.left = invertTree(root.right);
        root.right = invertTree(left);
        return root;
    }
}
```',
'翻转操作发生在每一个节点上。',
'当前节点只需要交换左右孩子。',
'如果 root 为空返回 null；否则交换 root.left/root.right，再递归处理。', 1, NOW(), NOW()),
(70, '爬楼梯',
'任务说明：
你正在爬一段共有 n 阶的楼梯。每次可以向上走 1 阶或 2 阶，请计算一共有多少种不同的走法可以恰好到达第 n 阶。

返回要求：
返回不同走法的数量，结果为整数。

约束与边界：
n 至少为 1。最后一步只可能来自第 n - 1 阶或第 n - 2 阶，因此可以把问题拆成相邻状态的转移，并注意 n = 1、n = 2 的初始化。',
'EASY', 'DynamicProgramming',
'第 1 行输入 n。',
'返回整数。',
'solution',
'class Solution {
    public int climbStairs(int n) {
        // 请返回爬到第 n 阶的方法数
        return 0;
    }
}',
'方法一：动态规划

解题思路：
到第 i 阶的方法来自第 i - 1 阶再走 1 步，或第 i - 2 阶再走 2 步，所以 f(i)=f(i-1)+f(i-2)。

易错点：
n=1 和 n=2 的初始化要清楚。

复杂度：
时间复杂度 O(n)，空间可优化到 O(1)。

Java 参考实现：
```java
class Solution {
    public int climbStairs(int n) {
        if (n <= 2) {
            return n;
        }
        int prev2 = 1;
        int prev1 = 2;
        for (int i = 3; i <= n; i++) {
            int current = prev1 + prev2;
            prev2 = prev1;
            prev1 = current;
        }
        return prev1;
    }
}
```',
'最后一步只能来自 n-1 或 n-2。',
'定义 f(i) 表示到第 i 阶的方法数。',
'初始化一阶和二阶，再滚动计算到 n。', 1, NOW(), NOW()),
(198, '打家劫舍',
'任务说明：
给定一个非负整数数组 nums，其中 nums[i] 表示第 i 间房屋中的金额。你不能在同一晚偷相邻的两间房，否则会触发警报。请计算在不触发警报的前提下能获得的最大金额。

返回要求：
返回可以偷到的最高金额；如果没有房屋，返回 0。

约束与边界：
房屋金额可能为 0，数组长度可能很小。每间房只有偷或不偷两种选择，偷当前房时不能选择前一间房，需要用动态规划维护前缀最优值。',
'MEDIUM', 'DynamicProgramming',
'第 1 行为数组长度 n，第 2 行为 n 个非负整数。',
'返回整数。',
'solution',
'class Solution {
    public int rob(int[] nums) {
        // 请返回最大金额
        return 0;
    }
}',
'方法一：动态规划

解题思路：
到第 i 间房时，要么不偷它，收益为前一间的最优值；要么偷它，收益为前两间最优值加当前金额。

易错点：
状态转移不能同时选择相邻房屋。

复杂度：
时间复杂度 O(n)，空间可优化到 O(1)。

Java 参考实现：
```java
class Solution {
    public int rob(int[] nums) {
        int prev2 = 0;
        int prev1 = 0;
        for (int num : nums) {
            int current = Math.max(prev1, prev2 + num);
            prev2 = prev1;
            prev1 = current;
        }
        return prev1;
    }
}
```',
'每间房只有偷或不偷两种选择。',
'偷当前房时，前一间不能偷。',
'维护 prev2 和 prev1，当前值为 max(prev1, prev2 + nums[i])。', 1, NOW(), NOW()),
(121, '买卖股票的最佳时机',
'任务说明：
给定一个数组 prices，其中 prices[i] 表示第 i 天的股票价格。你最多只能完成一次交易，也就是先选择某一天买入，再选择之后的某一天卖出。

返回要求：
返回这次交易能够获得的最大利润；如果无论如何都无法获利，返回 0。

约束与边界：
卖出日期必须晚于买入日期，不能用未来的低价去计算过去的利润。遍历价格时可以维护当前见过的最低买入价，并用当天价格尝试更新最大利润。',
'EASY', 'Greedy',
'第 1 行为数组长度 n，第 2 行为 n 个价格。',
'返回整数。',
'solution',
'class Solution {
    public int maxProfit(int[] prices) {
        // 请返回最大利润
        return 0;
    }
}',
'方法一：维护最低买入价

解题思路：
从左到右遍历价格，维护目前见过的最低价格 minPrice。每天尝试用 prices[i] - minPrice 更新最大利润，再更新最低价。

易错点：
卖出必须发生在买入之后，不能用未来最低价计算过去利润。

复杂度：
时间复杂度 O(n)，空间复杂度 O(1)。

Java 参考实现：
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
```',
'只允许一次买卖，买入必须在卖出前。',
'遍历时维护到当前天为止的最低买入价。',
'profit = max(profit, price - minPrice)，然后更新 minPrice。', 1, NOW(), NOW());

INSERT INTO problem_knowledge_point (problem_id, knowledge_point_id) VALUES
(1, 1), (1, 2),
(49, 2), (49, 3),
(128, 2),
(206, 4),
(21, 4),
(141, 5),
(102, 7),
(104, 6),
(226, 6),
(70, 8),
(198, 8),
(121, 9);

INSERT INTO test_case (problem_id, input_data, expected_output, is_sample, weight) VALUES
(1, '4
2 7 11 15
9
', '[0,1]', 1, 1),
(1, '3
3 2 4
6
', '[1,2]', 0, 1),
(1, '2
3 3
6
', '[0,1]', 0, 1),
(49, '6
eat tea tan ate nat bat
', '[["ate","eat","tea"],["bat"],["nat","tan"]]', 1, 1),
(49, '1
a
', '[["a"]]', 0, 1),
(49, '3
abc bca cab
', '[["abc","bca","cab"]]', 0, 1),
(128, '6
100 4 200 1 3 2
', '4', 1, 1),
(128, '10
0 3 7 2 5 8 4 6 0 1
', '9', 0, 1),
(128, '0
', '0', 0, 1),
(206, '5
1 2 3 4 5
', '[5,4,3,2,1]', 1, 1),
(206, '2
1 2
', '[2,1]', 0, 1),
(206, '0
', '[]', 0, 1),
(21, '3
1 2 4
3
1 3 4
', '[1,1,2,3,4,4]', 1, 1),
(21, '0
0
', '[]', 0, 1),
(21, '2
2 5
3
1 3 4
', '[1,2,3,4,5]', 0, 1),
(141, '4
3 2 0 -4
1
', 'true', 1, 1),
(141, '2
1 2
0
', 'true', 0, 1),
(141, '1
1
-1
', 'false', 0, 1),
(102, '7
3 9 20 null null 15 7
', '[[3],[9,20],[15,7]]', 1, 1),
(102, '1
1
', '[[1]]', 0, 1),
(102, '0
', '[]', 0, 1),
(104, '7
3 9 20 null null 15 7
', '3', 1, 1),
(104, '2
1 null
', '1', 0, 1),
(104, '0
', '0', 0, 1),
(226, '7
4 2 7 1 3 6 9
', '[4,7,2,9,6,3,1]', 1, 1),
(226, '3
2 1 3
', '[2,3,1]', 0, 1),
(226, '0
', '[]', 0, 1),
(70, '2
', '2', 1, 1),
(70, '3
', '3', 0, 1),
(70, '5
', '8', 0, 1),
(198, '4
1 2 3 1
', '4', 1, 1),
(198, '5
2 7 9 3 1
', '12', 0, 1),
(198, '1
5
', '5', 0, 1),
(121, '6
7 1 5 3 6 4
', '5', 1, 1),
(121, '5
7 6 4 3 1
', '0', 0, 1),
(121, '2
1 2
', '1', 0, 1);
