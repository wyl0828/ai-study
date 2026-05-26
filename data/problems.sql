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
(9, '贪心维护最优值', 'Greedy', '遍历时维护当前最优候选。'),
(10, '滑动窗口', 'SlidingWindow', '用左右指针维护满足条件的连续区间。'),
(11, '双指针去重', 'TwoPointers', '排序后用双指针收缩搜索空间并处理重复元素。'),
(12, '栈匹配', 'Stack', '使用栈维护最近未匹配的符号或状态。'),
(13, '区间合并', 'Array', '排序区间并维护当前合并边界。'),
(14, '二分边界', 'BinarySearch', '在有序数组中通过左右边界收缩定位目标。'),
(15, '网格搜索', 'Graph', '在二维网格中使用 DFS 或 BFS 访问连通区域。');

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
}','方法一：哈希表

解题思路：
先从暴力法理解问题：
最直接的想法是枚举两个下标 i 和 j，检查 nums[i] + nums[j] 是否等于 target。这样一定能做出来，但每个数都要和后面的很多数配对，时间复杂度是 O(n^2)。当数组变长时，重复检查会很多。

为什么想到 HashMap：
题目真正要问的是：当我站在 nums[i] 这个位置时，能不能立刻知道前面有没有一个数等于 target - nums[i]。如果能 O(1) 查到这个"需要的另一个数"，就不用再写内层循环。HashMap 正好适合保存"已经看过的数 -> 它的下标"。

关键变量怎么理解：
- indexByValue：只保存已经遍历过的元素和值对应的下标。
- need：当前 nums[i] 还差多少才能凑成 target。
- i：当前正在尝试作为第二个数的位置。

用样例走一遍：
nums = [2,7,11,15], target = 9。
1. i=0，nums[i]=2，need=7。map 为空，找不到 7，于是记录 2 -> 0。
2. i=1，nums[i]=7，need=2。map 里有 2 -> 0，说明之前的 2 和当前的 7 可以组成 9，返回 [0,1]。

为什么必须先查再放：
如果先把当前数放进 map，再查 need，遇到 nums=[3,3], target=6 时，第一个 3 会把自己放进去，然后立刻查到自己，可能返回 [0,0]。但题目要求两个不同位置，所以当前元素不能和自己配对。正确顺序是：先查前面有没有 need，再把当前数放进去给后面使用。

伪代码：
创建 map
遍历每个下标 i：
    need = target - nums[i]
    如果 map 里有 need：返回 [map.get(need), i]
    否则把 nums[i] 和 i 放入 map

新手常见错误：
1. 先 put 再 containsKey，导致同一个元素被使用两次。
2. HashMap 只存值不存下标，最后不知道该返回哪个位置。
3. 忘记重复数字场景，例如 [3,3]。

复杂度：
每个元素最多查询和写入一次，时间复杂度 O(n)，HashMap 最多保存 n 个元素，空间复杂度 O(n)。

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
}','方法一：排序签名分组

解题思路：
先从朴素思路理解问题：
判断两个字符串是不是异位词，可以逐个字符计数再比较。问题是有很多字符串，如果每两个都比较一次，会出现大量重复判断，分组逻辑也会很乱。

为什么想到分组 key：
互为异位词的字符串，本质上拥有相同的字符组成。把每个字符串转换成一个稳定的标识，所有标识相同的字符串放到同一组。排序后的字符串就是最直观的 key，例如 eat、tea、ate 排序后都是 aet。

关键变量怎么理解：
- key：当前字符串排序后的结果，代表它的字符组成。
- groups：key 到字符串列表的映射。
- result：groups 中所有列表组成的最终答案。

用样例走一遍：
strs = [eat, tea, tan, ate, nat, bat]。eat 排序为 aet，tea 也是 aet，所以进入同一组。tan 和 nat 排序都是 ant，进入另一组。bat 排序为 abt，单独一组。

伪代码：
创建 map
遍历每个字符串 str：
    chars = str.toCharArray 并排序
    key = new String(chars)
    把 str 加入 map[key]
返回 map 的所有 value

新手常见错误：
1. 只比较长度，长度相同不代表字符组成相同。
2. 用原字符串当 key，无法把 tea 和 eat 分到一起。
3. 忘记初始化分组列表，直接 add 导致空指针。

复杂度：
设字符串个数为 n，最长长度为 k。每个字符串排序 O(k log k)，总时间 O(n * k log k)。Map 和结果列表需要 O(n * k) 空间。

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
}','方法一：HashSet 起点扩展

解题思路：
先从朴素思路理解问题：
可以把每个数都当作连续序列的起点，向后查 x+1、x+2。但如果数组里有 1,2,3,4，每个数都重复扩展同一段，会浪费大量时间。

为什么想到只从起点扩展：
一段连续序列真正的起点有一个特征：x-1 不存在。只要 x-1 存在，说明 x 不是起点，它已经会被更前面的数覆盖。这样每段连续序列只会被完整扫描一次。

关键变量怎么理解：
- values：保存所有数字，用于 O(1) 查询某个数是否存在。
- num：当前尝试判断的数字。
- current：正在向后扩展的连续值。
- best：目前最长连续长度。

用样例走一遍：
nums = [100,4,200,1,3,2]。1 没有前驱 0，所以从 1 开始扩展，找到 2、3、4，长度为 4。2、3、4 都有前驱，所以跳过，不重复扩展。

伪代码：
把所有数放入 set
遍历 set 中每个 num：
    如果 set 包含 num - 1：跳过
    从 num 开始向后查 num+1、num+2...
    更新 best
返回 best

新手常见错误：
1. 从每个数字都扩展，时间复杂度退化。
2. 忘记去重，重复数字干扰长度计算。
3. 只排序后找连续，虽然能做，但不是题目常问的 O(n) 思路。

复杂度：
每个数字最多作为某段的一部分被扩展一次，平均时间复杂度 O(n)。HashSet 占 O(n) 空间。

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
}','方法一：迭代反转

解题思路：
先把问题画成指针变化：
链表不是数组，不能靠交换下标完成反转。真正要做的是把每个节点的 next 指针改成指向前一个节点。例如 1 -> 2 -> 3 要变成 3 -> 2 -> 1。

为什么需要三个变量：
- cur：当前正在处理的节点。
- prev：已经反转好的前半段头节点，也是 cur 反转后应该指向的节点。
- next：临时保存 cur 原来的下一个节点，防止改 cur.next 后找不到后半段。

用样例走一遍：
原链表是 1 -> 2 -> 3。
开始时 prev=null，cur=1。
1. 保存 next=2；把 1.next 指向 null；prev 移到 1；cur 移到 2。现在反转好的部分是 1 -> null，未处理部分是 2 -> 3。
2. 保存 next=3；把 2.next 指向 1；prev 移到 2；cur 移到 3。现在反转好的部分是 2 -> 1 -> null。
3. 保存 next=null；把 3.next 指向 2；prev 移到 3；cur 变成 null。循环结束。

为什么最后返回 prev：
循环结束时，cur 已经走到 null，说明原链表处理完了。prev 指向最后处理的节点，也就是反转后新链表的头节点。很多新手会错误返回 head，但原来的 head 已经变成尾节点。

伪代码：
prev = null
cur = head
while cur != null:
    next = cur.next
    cur.next = prev
    prev = cur
    cur = next
return prev

新手常见错误：
1. 没有先保存 next，导致 cur.next 改掉后丢失剩余链表。
2. 循环结束返回 head，而不是 prev。
3. 移动顺序写反，例如先 cur = cur.next 再改指针。

复杂度：
每个节点只处理一次，时间复杂度 O(n)。只用了几个指针变量，空间复杂度 O(1)。

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
}','方法一：哨兵节点合并

解题思路：
先从朴素思路理解问题：
两个链表本来已经各自有序，如果把所有值拿出来放进数组再排序，也能得到答案。但这样没有利用"两个链表已经有序"这个条件，还会额外占用空间。

为什么想到双指针合并：
每次只需要比较 list1 和 list2 当前头节点，较小的节点一定是合并后链表的下一个节点。接上它之后，对应链表向后移动，继续比较即可。这和归并排序中的 merge 步骤一样。

关键变量怎么理解：
- dummy：虚拟头节点，用来统一处理结果链表的头部。
- tail：结果链表当前的尾节点，新节点都接到 tail.next。
- list1/list2：两个链表尚未合并部分的头节点。

用样例走一遍：
list1 = [1,2,4]，list2 = [1,3,4]。先比较 1 和 1，接 list1 的 1；再比较 2 和 list2 的 1，接 list2 的 1；然后依次接 2、3、4、4。

伪代码：
dummy = new ListNode(0)
tail = dummy
while list1 和 list2 都不为空：
    接较小节点到 tail.next
    移动被接走的链表
    tail 后移
把剩余非空链表接到 tail.next
返回 dummy.next

新手常见错误：
1. 不用 dummy，导致头节点初始化分支很多。
2. 循环结束忘记接剩余链表。
3. 接上节点后忘记移动 tail，结果链表被覆盖。

复杂度：
两个链表的每个节点最多访问一次，时间复杂度 O(n + m)。只使用少量指针变量，空间复杂度 O(1)。

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
}','方法一：快慢指针

解题思路：
先从朴素思路理解问题：
可以用 HashSet 记录访问过的节点，如果再次遇到同一个节点，就说明有环。这很直观，但需要 O(n) 额外空间。

为什么想到快慢指针：
如果链表有环，fast 每次走两步，slow 每次走一步。进入环后，fast 会不断追近 slow，最终相遇。如果没有环，fast 会先走到 null。

关键变量怎么理解：
- slow：慢指针，每次走一步。
- fast：快指针，每次走两步。
- fast == slow：两个指针在环内相遇，说明有环。

用样例走一遍：
链表 3 -> 2 -> 0 -> -4，并且 -4 指回 2。slow 和 fast 从 head 出发，fast 先进入环并绕圈，因为它速度更快，最终会在环内追上 slow。

伪代码：
slow = head, fast = head
while fast != null 且 fast.next != null：
    slow = slow.next
    fast = fast.next.next
    如果 slow == fast：返回 true
返回 false

新手常见错误：
1. 循环条件只写 fast != null，访问 fast.next.next 时空指针。
2. 用节点值判断相遇，而不是节点引用。
3. fast 和 slow 移动前后判断顺序混乱，导致单节点无环场景出错。

复杂度：
每个指针最多走 O(n) 步，时间复杂度 O(n)。只使用两个指针，空间复杂度 O(1)。

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
}','方法一：队列 BFS

解题思路：
先从朴素思路理解问题：
层序遍历要求一层一层输出。如果用普通 DFS，也能记录深度再分组，但对新手来说更绕。题目本身的"从上到下、从左到右"更像队列的先进先出过程。

为什么想到队列：
访问某一层节点时，把它们的孩子按顺序加入队列。当前层处理完后，队列里自然就是下一层节点。队列能保证先加入的左侧节点先被访问。

关键变量怎么理解：
- queue：保存待访问节点。
- size：当前层的节点数量，必须在处理本层前固定。
- level：当前层收集到的节点值。
- result：所有层的结果。

用样例走一遍：
树 [3,9,20,null,null,15,7]。队列先放 3，size=1，得到第一层 [3]，加入 9 和 20。下一轮 size=2，只处理 9 和 20，得到 [9,20]，再把 15 和 7 加入队列。

伪代码：
如果 root 为空，返回空列表
root 入队
while queue 不空：
    size = queue.size()
    创建 level
    循环 size 次：弹出节点，记录值，左右孩子入队
    result.add(level)

新手常见错误：
1. 不固定 size，导致下一层节点混进当前层。
2. 空树没有提前返回，出现空指针。
3. 只加入左孩子或右孩子，漏掉另一侧。

复杂度：
每个节点入队出队一次，时间复杂度 O(n)。队列最多保存一层节点，最坏空间复杂度 O(n)。

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
}','方法一：递归求高度

解题思路：
先从朴素思路理解问题：
最大深度就是从根节点到最远叶子节点经过的节点数。可以想象每往下一层，深度就加 1，左右子树里更深的那一边决定整棵树的深度。

为什么想到递归：
一棵树的最大深度可以拆成子问题：左子树最大深度、右子树最大深度。当前节点的最大深度就是两者较大值再加上当前节点这一层。

关键变量怎么理解：
- root：当前子树根节点。
- maxDepth(root.left)：左子树深度。
- maxDepth(root.right)：右子树深度。
- +1：把当前 root 这一层算进去。

用样例走一遍：
树 [3,9,20,null,null,15,7]。节点 9 的左右为空，深度是 1。节点 20 的左右子节点 15 和 7 深度都是 1，所以 20 深度是 2。根节点 3 的深度是 max(1,2)+1=3。

伪代码：
如果 root == null：返回 0
left = maxDepth(root.left)
right = maxDepth(root.right)
返回 max(left, right) + 1

新手常见错误：
1. 空节点返回 1，导致整体深度多算一层。
2. 只沿左子树或右子树递归，没有取最大值。
3. 忘记 +1，漏掉当前节点。

复杂度：
每个节点访问一次，时间复杂度 O(n)。递归栈深度等于树高，空间复杂度 O(h)。

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
}','方法一：递归交换左右子树

解题思路：
先从朴素思路理解问题：
翻转二叉树不是只交换根节点的左右孩子，还要让每个子节点内部也完成同样的左右交换。也就是说，这是一个会重复发生在每个节点上的操作。

为什么想到递归：
对当前节点来说，翻转后的左子树应该来自原来的右子树，翻转后的右子树应该来自原来的左子树。左右子树本身也需要继续翻转，这正好是递归定义。

关键变量怎么理解：
- root：当前要翻转的子树根节点。
- left：临时保存原左子树，避免交换时丢失引用。
- invertTree(root.right)：翻转原右子树后放到左边。
- invertTree(left)：翻转原左子树后放到右边。

用样例走一遍：
树 [4,2,7,1,3,6,9]。根节点 4 的左右子树交换，左边变成 7，右边变成 2。然后递归处理 7，把 6 和 9 交换；递归处理 2，把 1 和 3 交换。

伪代码：
如果 root == null：返回 null
保存 left = root.left
root.left = invertTree(root.right)
root.right = invertTree(left)
返回 root

新手常见错误：
1. 只交换根节点，忘记递归处理子树。
2. 没有临时保存 left，导致原左子树引用丢失。
3. 空树没有处理，出现空指针。

复杂度：
每个节点访问一次，时间复杂度 O(n)。递归栈深度等于树高，空间复杂度 O(h)。

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
}','方法一：动态规划

解题思路：
先从朴素思路理解问题：
到第 n 阶，可以把所有走法列出来。但 n 增大后，分支会指数级增长：每一步都可以走 1 或 2 阶，很多中间状态会被重复计算。

为什么想到动态规划：
到达第 i 阶的最后一步只有两种来源：从第 i-1 阶走 1 步，或者从第 i-2 阶走 2 步。所以到第 i 阶的方法数等于前两个状态之和。

关键变量怎么理解：
- f(i)：到达第 i 阶的方法数。
- prev2：f(i-2)。
- prev1：f(i-1)。
- current：当前计算出的 f(i)。

用样例走一遍：
n = 5。f(1)=1，f(2)=2。f(3)=3，表示 1+1+1、1+2、2+1。f(4)=5，f(5)=8。

伪代码：
如果 n <= 2，返回 n
prev2 = 1, prev1 = 2
从 3 到 n：
    current = prev1 + prev2
    prev2 = prev1
    prev1 = current
返回 prev1

新手常见错误：
1. 把 f(0)、f(1)、f(2) 的定义混乱，导致初始化错。
2. 写递归但不加记忆化，重复计算严重。
3. 滚动变量更新顺序写反。

复杂度：
从 3 到 n 遍历一次，时间复杂度 O(n)。滚动变量只占 O(1) 空间。

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
}','方法一：动态规划

解题思路：
先从朴素思路理解问题：
每间房都可以选择偷或不偷，但相邻房屋不能同时偷。如果用递归枚举所有选择，分支会很多，并且同一个前缀问题会被重复计算。

为什么想到动态规划：
走到当前房屋时，最优选择只有两种：不偷当前房，收益就是前一间为止的最优值；偷当前房，就不能偷前一间，收益是前两间为止的最优值加当前金额。

关键变量怎么理解：
- prev1：处理到前一间房时的最大收益。
- prev2：处理到前两间房时的最大收益。
- current：处理到当前房时的最大收益。
- num：当前房屋金额。

用样例走一遍：
nums = [2,7,9,3,1]。到 2，最好是 2。到 7，最好是 7。到 9，可以偷 2+9=11，比 7 大。到 3，可以选择不偷保持 11。到 1，最好是 12。

伪代码：
prev2 = 0, prev1 = 0
遍历每个金额 num：
    current = max(prev1, prev2 + num)
    prev2 = prev1
    prev1 = current
返回 prev1

新手常见错误：
1. 只看当前房和前一房大小，忽略前缀最优。
2. 偷当前房时错误地加上 prev1，违反不能偷相邻房。
3. 数组长度为 0 或 1 时初始化处理混乱。

复杂度：
遍历一次数组，时间复杂度 O(n)。滚动变量代替 dp 数组，空间复杂度 O(1)。

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
}','方法一：维护最低买入价

解题思路：
先从朴素思路理解问题：
可以枚举买入日和卖出日，计算所有 prices[j] - prices[i]，其中 j 必须大于 i。这样能找出答案，但需要 O(n^2) 时间。

为什么想到一次遍历：
当我们走到某一天准备卖出时，最好的买入日一定是它之前价格最低的那一天。所以不需要枚举所有买入日，只要维护"到今天之前见过的最低价格"，再用今天价格尝试更新利润。

关键变量怎么理解：
- minPrice：遍历到当前天为止见过的最低买入价。
- best：目前能获得的最大利润。
- price：当前天价格，可以尝试作为卖出价。

用样例走一遍：
prices = [7,1,5,3,6,4]。第一天最低价是 7。第二天价格 1，更适合买入，minPrice 更新为 1。第三天价格 5，如果今天卖出利润是 4。第五天价格 6，利润是 5，更新 best。

伪代码：
minPrice = 很大值
best = 0
遍历 price：
    best = max(best, price - minPrice)
    minPrice = min(minPrice, price)
返回 best

新手常见错误：
1. 用未来最低价计算过去卖出日，违反先买后卖。
2. 价格一直下降时返回负数，正确答案应为 0。
3. 先更新 minPrice 再算利润虽然多数情况下也可行，但讲解时容易混淆"买入必须在卖出前"的约束。

复杂度：
只遍历一次数组，时间复杂度 O(n)。只使用两个变量，空间复杂度 O(1)。

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
'profit = max(profit, price - minPrice)，然后更新 minPrice。', 1, NOW(), NOW()),
(3, '无重复字符的最长子串',
'任务说明：
给定一个字符串 s，请找出其中不含重复字符的最长连续子串长度。子串必须是原字符串中连续的一段。

返回要求：
返回最长无重复字符子串的长度；空字符串返回 0。

约束与边界：
字符串中可能出现重复字符，窗口左边界只能向右移动，不能因为遇到旧字符而回退。维护字符最近出现位置时要特别注意 left 的更新范围。',
'MEDIUM', 'SlidingWindow',
'第 1 行输入字符串 s；空输入按空字符串处理。',
'返回整数。',
'solution',
'class Solution {
    public int lengthOfLongestSubstring(String s) {
        // 请返回最长无重复子串长度
        return 0;
    }
}','方法一：滑动窗口

解题思路：
先从朴素思路理解问题：
如果从每个位置开始枚举子串，再检查这个子串里有没有重复字符，也能做出来。但这样会反复检查很多重叠区间，比如 abcabcbb 里从 a 开始、从 b 开始的子串会重复扫描大量字符，整体容易退化到 O(n^2) 甚至更慢。

为什么想到滑动窗口：
题目要求的是连续子串，而且条件是窗口内不能有重复字符。连续区间最适合用 left 和 right 两个边界维护。right 负责向右扩张，left 负责在出现重复时收缩，让窗口重新变成无重复状态。

关键变量怎么理解：
- left：当前无重复窗口的左边界。
- right：当前正在加入窗口的字符位置。
- lastIndex：每个字符最近一次出现的位置。
- best：目前见过的最长合法窗口长度。

用样例走一遍：
s = abcabcbb。right 走到第二个 a 时，发现 a 上次在 0 出现过，而 0 还在窗口内，所以 left 移到 1。后面遇到重复 b 时同理，left 只会向右移动，不会回退。

伪代码：
left = 0
遍历 right：
    如果字符上次出现位置 >= left：left 移到上次位置 + 1
    记录当前字符位置
    用 right - left + 1 更新答案

新手常见错误：
1. 遇到重复字符时直接 left = lastIndex + 1，没有取 max，导致 left 被窗口外的旧位置拉回去。
2. 把子序列和子串混淆，子串必须连续。
3. 更新 best 的时机放错，漏掉当前窗口长度。

复杂度：
每个字符最多被 right 扫一次，left 只向右移动，时间复杂度 O(n)。HashMap 保存字符位置，空间复杂度 O(k)。

Java 参考实现：
```java
import java.util.HashMap;
import java.util.Map;

class Solution {
    public int lengthOfLongestSubstring(String s) {
        Map<Character, Integer> lastIndex = new HashMap<>();
        int left = 0;
        int best = 0;
        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            if (lastIndex.containsKey(c)) {
                left = Math.max(left, lastIndex.get(c) + 1);
            }
            lastIndex.put(c, right);
            best = Math.max(best, right - left + 1);
        }
        return best;
    }
}
```',
'连续子串可以用窗口维护。',
'遇到重复字符时，左边界移动到上次出现位置之后。',
'left = Math.max(left, lastIndex.get(c) + 1)，再更新 best。', 1, NOW(), NOW()),
(15, '三数之和',
'任务说明：
给定整数数组 nums，请找出所有不重复的三元组，使三元组中的三个数之和等于 0。

返回要求：
返回 List<List<Integer>>。每个三元组内部可以按升序返回，结果集合不能包含重复三元组。

约束与边界：
数组可能包含大量重复数字。排序后固定第一个数，再用左右指针寻找另外两个数，同时跳过重复值，是面试中最常见的写法。',
'MEDIUM', 'TwoPointers',
'第 1 行为数组长度 n，第 2 行为 n 个整数。',
'返回 List<List<Integer>>，判题会规范化三元组顺序。',
'solution',
'class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        // 请返回所有不重复三元组
        return new ArrayList<>();
    }
}','方法一：排序 + 双指针

解题思路：
先从暴力法理解问题：
最直接的办法是枚举三个下标 i、j、k，检查 nums[i] + nums[j] + nums[k] 是否等于 0。这个思路容易懂，但复杂度是 O(n^3)，而且还要额外处理重复三元组，写起来又慢又乱。

为什么先排序：
排序后，数组从小到大排列。这样我们固定一个数 nums[i] 后，剩下的问题就变成：在 i 右侧找两个数，让它们的和等于 -nums[i]。因为右侧有序，就可以用双指针根据当前和的大小移动，而不是盲目枚举。

双指针为什么能动：
固定 i 后，left 指向 i+1，right 指向数组末尾。
- 如果 sum = nums[i] + nums[left] + nums[right] 太小，说明需要更大的数，所以 left 右移。
- 如果 sum 太大，说明需要更小的数，所以 right 左移。
- 如果 sum 等于 0，记录答案，然后 left 和 right 同时移动继续找。

用样例走一遍：
nums = [-1,0,1,2,-1,-4]，排序后是 [-4,-1,-1,0,1,2]。
1. i=0，固定 -4，left=-1，right=2，sum=-3 太小，left 右移，最终找不到。
2. i=1，固定 -1，left=-1，right=2，sum=0，得到 [-1,-1,2]。
3. 移动后 left=0，right=1，sum=0，得到 [-1,0,1]。
4. i=2 也是 -1，和前一个固定数重复，跳过，否则会生成重复答案。

为什么要去重：
结果不能包含重复三元组。去重分三处：
1. 固定数 nums[i] 和前一个一样时跳过。
2. 找到答案后，left 跳过和刚才一样的值。
3. 找到答案后，right 跳过和刚才一样的值。

伪代码：
排序 nums
遍历 i 从 0 到 n-3：
    如果 i > 0 且 nums[i] == nums[i-1]，跳过
    left = i + 1, right = n - 1
    while left < right:
        sum = nums[i] + nums[left] + nums[right]
        如果 sum == 0：记录三元组，并跳过重复 left/right
        如果 sum < 0：left++
        如果 sum > 0：right--

新手常见错误：
1. 忘记排序，导致双指针移动没有依据。
2. 只对 i 去重，忘记对 left/right 去重。
3. 找到答案后只移动一个指针，容易死循环或漏解。
4. 把 two sum 的 HashMap 写法直接套过来，重复结果处理会变复杂。

复杂度：
排序需要 O(n log n)，外层固定 i，内层双指针总共 O(n)，整体 O(n^2)。结果列表不计入额外空间时，额外空间主要取决于排序实现。

Java 参考实现：
```java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> result = new ArrayList<>();
        for (int i = 0; i < nums.length - 2; i++) {
            if (i > 0 && nums[i] == nums[i - 1]) {
                continue;
            }
            int left = i + 1;
            int right = nums.length - 1;
            while (left < right) {
                int sum = nums[i] + nums[left] + nums[right];
                if (sum == 0) {
                    result.add(List.of(nums[i], nums[left], nums[right]));
                    left++;
                    right--;
                    while (left < right && nums[left] == nums[left - 1]) {
                        left++;
                    }
                    while (left < right && nums[right] == nums[right + 1]) {
                        right--;
                    }
                } else if (sum < 0) {
                    left++;
                } else {
                    right--;
                }
            }
        }
        return result;
    }
}
```',
'先排序，让双指针可以根据和的大小移动。',
'固定 i 后，在右侧找 two sum。',
'跳过重复 i、重复 left 和重复 right，避免重复三元组。', 1, NOW(), NOW()),
(20, '有效的括号',
'任务说明：
给定一个只包含括号字符的字符串 s，请判断括号是否有效。有效表示每个左括号都能被同类型右括号按正确顺序闭合。

返回要求：
如果字符串有效返回 true，否则返回 false。

约束与边界：
右括号不能先出现，括号类型必须匹配，最后栈中不能剩下未闭合的左括号。',
'EASY', 'Stack',
'第 1 行输入括号字符串 s。',
'返回 boolean。',
'solution',
'class Solution {
    public boolean isValid(String s) {
        // 请判断括号是否合法
        return false;
    }
}','方法一：栈匹配

解题思路：
先从朴素思路理解问题：
括号是否合法，不是简单统计左右括号数量相等。例如 ([)] 数量是对的，但顺序错了。我们真正要检查的是：最后打开的左括号，必须最先被对应的右括号关闭。

为什么想到栈：
这种"最后出现、最先处理"的关系就是栈。遇到左括号时先放进栈；遇到右括号时，必须和栈顶的左括号匹配。如果栈顶不匹配，说明最近打开的括号没有被正确关闭。

关键变量怎么理解：
- stack：保存尚未被匹配的左括号。
- 当前字符 c：决定是入栈，还是拿它和栈顶做匹配。
- 匹配规则：右括号只能匹配同类型左括号。

用样例走一遍：
s = ()[]{}。遇到 ( 入栈，遇到 ) 时弹出 (，匹配成功。后面的 [] 和 {} 同理。s = ([)] 时，遇到 ) 时栈顶是 [，类型不匹配，所以直接 false。

伪代码：
遍历每个字符：
    如果是左括号：入栈
    如果是右括号：
        栈空则 false
        弹出栈顶，检查类型是否匹配
遍历结束后，栈必须为空

新手常见错误：
1. 只统计括号数量，不检查顺序和类型。
2. 遇到右括号时忘记先判断栈是否为空。
3. 遍历结束忘记检查栈为空，导致 (( 被误判为 true。

复杂度：
每个字符最多入栈或出栈一次，时间复杂度 O(n)，栈最多保存 n 个左括号，空间复杂度 O(n)。

Java 参考实现：
```java
import java.util.ArrayDeque;
import java.util.Deque;

class Solution {
    public boolean isValid(String s) {
        Deque<Character> stack = new ArrayDeque<>();
        for (int i = 0; i < s.length(); i++) {
            String token = String.valueOf(s.charAt(i));
            if ("(".equals(token) || "[".equals(token) || "{".equals(token)) {
                stack.push(s.charAt(i));
            } else {
                if (stack.isEmpty()) {
                    return false;
                }
                char left = stack.pop();
                if (")".equals(token) && left != "(".charAt(0)) {
                    return false;
                }
                if ("]".equals(token) && left != "[".charAt(0)) {
                    return false;
                }
                if ("}".equals(token) && left != "{".charAt(0)) {
                    return false;
                }
            }
        }
        return stack.isEmpty();
    }
}
```',
'最近打开的左括号必须最先被关闭。',
'用栈保存尚未匹配的左括号。',
'遇到右括号时弹栈并检查类型，最后 stack.isEmpty()。', 1, NOW(), NOW()),
(53, '最大子数组和',
'任务说明：
给定整数数组 nums，请找出一个和最大的连续子数组，并返回该子数组的和。

返回要求：
返回最大连续子数组和。子数组至少包含一个元素。

约束与边界：
数组可能全是负数，此时答案是最大的那个负数。不要把初始答案写成 0，否则会误判全负数组。',
'MEDIUM', 'DynamicProgramming',
'第 1 行为数组长度 n，第 2 行为 n 个整数。',
'返回整数。',
'solution',
'class Solution {
    public int maxSubArray(int[] nums) {
        // 请返回最大连续子数组和
        return 0;
    }
}','方法一：动态规划 / Kadane 算法

解题思路：
先从朴素思路理解问题：
可以枚举所有连续子数组，再计算每段和，取最大值。这样虽然直观，但子数组数量是 O(n^2)，如果每段再重新求和会更慢。

为什么想到动态规划：
连续子数组有一个关键限制：如果子数组必须以当前位置结尾，那么它只有两种选择：要么接在前一个位置的最佳子数组后面，要么从当前元素重新开始。这个选择可以用一个变量 current 表示。

关键变量怎么理解：
- current：以当前元素结尾的最大子数组和。
- best：所有位置里见过的最大子数组和。
- nums[i]：当前元素，决定是接上旧段还是另起一段。

用样例走一遍：
nums = [-2,1,-3,4,-1,2,1,-5,4]。走到 4 时，前面的 current 已经是负数，接上它只会拖累，所以从 4 重新开始。后面连续加上 -1、2、1 得到 6，更新 best。

伪代码：
current = nums[0]
best = nums[0]
从 i=1 开始遍历：
    current = max(nums[i], current + nums[i])
    best = max(best, current)
返回 best

新手常见错误：
1. 把 best 初始化为 0，导致全负数组错误返回 0。
2. 只维护正数和，忘记子数组至少包含一个元素。
3. 不理解 current 是"必须以当前位置结尾"，和全局 best 混在一起。

复杂度：
只遍历数组一次，时间复杂度 O(n)。只用 current 和 best 两个变量，空间复杂度 O(1)。

Java 参考实现：
```java
class Solution {
    public int maxSubArray(int[] nums) {
        int current = nums[0];
        int best = nums[0];
        for (int i = 1; i < nums.length; i++) {
            current = Math.max(nums[i], current + nums[i]);
            best = Math.max(best, current);
        }
        return best;
    }
}
```',
'关注以当前位置结尾的最优子数组。',
'当前元素可以另起一段，也可以接到前一段后面。',
'current = max(nums[i], current + nums[i])，best 持续取最大。', 1, NOW(), NOW()),
(56, '合并区间',
'任务说明：
给定若干区间 intervals，其中每个区间为 [start, end]，请合并所有重叠区间。

返回要求：
返回合并后的区间数组，区间按起点升序排列。

约束与边界：
区间可能乱序输入，可能存在完全包含、端点相接或多个连续重叠的情况。先按起点排序，再维护当前合并区间最清楚。',
'MEDIUM', 'Array',
'第 1 行为区间数量 n，之后 n 行每行两个整数 start end。',
'返回 int[][]，判题输出格式为 [[s,e],...]。',
'solution',
'class Solution {
    public int[][] merge(int[][] intervals) {
        // 请返回合并后的区间
        return new int[0][0];
    }
}','方法一：排序后扫描

解题思路：
先从朴素思路理解问题：
如果不排序，就很难判断一个新区间到底应该和前面哪几个区间合并。可能需要反复扫描已有结果，逻辑复杂且容易漏掉传递重叠。

为什么先排序：
按起点排序后，区间只需要和"当前合并结果的最后一个区间"比较。因为后面的区间起点只会越来越大，如果它和最后一个区间都不重叠，就不可能和更早的区间重叠。

关键变量怎么理解：
- intervals：按起点排序后的区间数组。
- merged：已经合并好的区间列表。
- last：merged 中最后一个区间，代表当前正在扩展的合并区间。

用样例走一遍：
[[1,3],[2,6],[8,10],[15,18]]。先看 [1,3] 放入结果。下一个 [2,6] 的起点 2 <= 3，说明重叠，合并成 [1,6]。之后 [8,10] 起点大于 6，不重叠，另起新区间。

伪代码：
按 start 排序
遍历每个 interval：
    如果 merged 为空或 interval.start > last.end：加入新区间
    否则 last.end = max(last.end, interval.end)
返回 merged

新手常见错误：
1. 忘记排序，导致只比较相邻输入区间会漏合并。
2. 重叠条件写成 start < lastEnd，漏掉 [1,4] 和 [4,5] 这种端点相接。
3. 合并时直接覆盖 end，而不是取 max。

复杂度：
排序 O(n log n)，扫描 O(n)，总时间 O(n log n)。结果列表最多保存 n 个区间，空间复杂度 O(n)。

Java 参考实现：
```java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Solution {
    public int[][] merge(int[][] intervals) {
        Arrays.sort(intervals, (a, b) -> Integer.compare(a[0], b[0]));
        List<int[]> merged = new ArrayList<>();
        for (int[] interval : intervals) {
            if (merged.isEmpty() || interval[0] > merged.get(merged.size() - 1)[1]) {
                merged.add(new int[] {interval[0], interval[1]});
            } else {
                int[] last = merged.get(merged.size() - 1);
                last[1] = Math.max(last[1], interval[1]);
            }
        }
        return merged.toArray(new int[merged.size()][]);
    }
}
```',
'先排序，重叠关系才容易判断。',
'维护已经合并结果的最后一个区间。',
'如果当前 start <= lastEnd，更新 lastEnd = max(lastEnd, end)。', 1, NOW(), NOW()),
(94, '二叉树的中序遍历',
'任务说明：
给定一棵二叉树的根节点 root，请返回它的中序遍历结果。中序遍历顺序为左子树、当前节点、右子树。

返回要求：
返回 List<Integer>，空树返回空列表。

约束与边界：
递归写法要处理 root 为空；迭代写法要正确维护栈和当前指针。面试中要能说清遍历顺序。',
'EASY', 'Tree',
'第 1 行为层序 token 数量 n，第 2 行为 n 个 token，null 表示空节点。',
'返回 List<Integer>，判题输出格式为 [v1,v2,...]。',
'solution',
'class Solution {
    public List<Integer> inorderTraversal(TreeNode root) {
        // 请返回中序遍历结果
        return new ArrayList<>();
    }
}','方法一：递归中序遍历

解题思路：
先从朴素思路理解问题：
二叉树不是线性结构，不能像数组一样从左到右直接遍历。题目要求的是中序，也就是对每个节点都遵守"左子树 -> 当前节点 -> 右子树"的访问顺序。

为什么想到递归：
二叉树本身就是递归结构：一棵树由根节点、左子树、右子树组成。中序遍历可以自然定义为：先中序遍历左子树，再访问根节点，再中序遍历右子树。

关键变量怎么理解：
- root：当前正在处理的子树根节点。
- result：按中序顺序收集节点值的列表。
- dfs：负责把当前子树的中序结果追加到 result。

用样例走一遍：
树 [1,null,2,3]。从 1 开始，左子树为空，先加入 1；再进入右子树 2。对 2 来说，先访问左子树 3，所以加入 3，再加入 2，结果是 [1,3,2]。

伪代码：
dfs(root)：
    如果 root == null：返回
    dfs(root.left)
    result.add(root.val)
    dfs(root.right)

新手常见错误：
1. 把中序写成前序，先 add root 再遍历左子树。
2. 忘记 root == null 的终止条件。
3. 每次递归都 new 一个 result，导致结果丢失。

复杂度：
每个节点访问一次，时间复杂度 O(n)。递归栈深度等于树高，空间复杂度 O(h)。

Java 参考实现：
```java
import java.util.ArrayList;
import java.util.List;

class Solution {
    public List<Integer> inorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        dfs(root, result);
        return result;
    }

    private void dfs(TreeNode root, List<Integer> result) {
        if (root == null) {
            return;
        }
        dfs(root.left, result);
        result.add(root.val);
        dfs(root.right, result);
    }
}
```',
'中序遍历顺序是左、根、右。',
'递归函数接收当前节点和结果列表。',
'root 为 null 时返回，否则 dfs(left)、add(root.val)、dfs(right)。', 1, NOW(), NOW()),
(200, '岛屿数量',
'任务说明：
给定一个由 1 和 0 组成的二维网格 grid，其中 1 表示陆地，0 表示水域。请计算网格中岛屿的数量。

返回要求：
返回岛屿数量。上下左右相邻的陆地属于同一个岛屿，斜向不连通。

约束与边界：
网格可能为空或只有水域。访问过的陆地需要标记，否则 DFS 或 BFS 会重复访问甚至死循环。',
'MEDIUM', 'Graph',
'第 1 行为行数 m 和列数 n，之后 m 行每行为 0/1 字符串。',
'返回整数。',
'solution',
'class Solution {
    public int numIslands(char[][] grid) {
        // 请返回岛屿数量
        return 0;
    }
}','方法一：DFS 淹没岛屿

解题思路：
先从朴素思路理解问题：
岛屿是由上下左右相邻的陆地组成的连通块。只数 1 的数量不对，因为一整片相连陆地只能算一个岛屿。

为什么想到 DFS：
当我们遇到一个还没访问过的 1，就说明发现了一个新岛屿。接下来要把和它相连的所有 1 都标记掉，避免后面重复计数。DFS 正好适合从一个格子出发，把连通区域全部走完。

关键变量怎么理解：
- grid：原始网格，也可以直接用来标记访问状态。
- count：已经发现的岛屿数量。
- r/c：当前 DFS 访问的行列位置。
- 四个方向：上、下、左、右，斜角不算相邻。

用样例走一遍：
遇到左上角第一个 1，count 加 1，然后 DFS 把和它连在一起的所有 1 都改成 0。之后扫描到这些位置时已经不是 1，不会重复计数。再遇到新的独立 1，才说明是新岛屿。

伪代码：
count = 0
遍历每个格子：
    如果 grid[r][c] == 1：
        count++
        dfs(r, c)
dfs(r,c)：
    越界或不是 1：返回
    标记为 0
    递归四个方向

新手常见错误：
1. 忘记标记访问过的陆地，导致重复计数或递归死循环。
2. 把斜向也算连通，和题意不符。
3. 行列边界判断写反，访问 grid[r][c] 前没有检查越界。

复杂度：
每个格子最多访问一次，时间复杂度 O(mn)。递归栈最坏可能达到 O(mn)。

Java 参考实现：
```java
class Solution {
    public int numIslands(char[][] grid) {
        int count = 0;
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                if (grid[r][c] == "1".charAt(0)) {
                    count++;
                    dfs(grid, r, c);
                }
            }
        }
        return count;
    }

    private void dfs(char[][] grid, int r, int c) {
        if (r < 0 || r >= grid.length || c < 0 || c >= grid[r].length || grid[r][c] != "1".charAt(0)) {
            return;
        }
        grid[r][c] = "0".charAt(0);
        dfs(grid, r + 1, c);
        dfs(grid, r - 1, c);
        dfs(grid, r, c + 1);
        dfs(grid, r, c - 1);
    }
}
```',
'遇到一块未访问陆地，就发现一个新岛屿。',
'从该陆地出发 DFS，把相邻陆地全部标记为已访问。',
'四方向递归，越界或不是陆地就返回。', 1, NOW(), NOW()),
(704, '二分查找',
'任务说明：
给定一个升序整数数组 nums 和目标值 target，请在数组中查找 target。

返回要求：
如果 target 存在，返回它的下标；否则返回 -1。

约束与边界：
数组已经升序且不含重复元素。二分查找的重点是循环条件、mid 计算和左右边界更新，避免死循环或越界。',
'EASY', 'BinarySearch',
'第 1 行为数组长度 n，第 2 行为 n 个整数，第 3 行为 target。',
'返回整数下标或 -1。',
'solution',
'class Solution {
    public int search(int[] nums, int target) {
        // 请返回 target 下标
        return -1;
    }
}','方法一：闭区间二分查找

解题思路：
先从朴素思路理解问题：
可以从左到右逐个比较 nums[i] 和 target，这样一定能找到，但没有利用数组已经升序这个条件，时间复杂度是 O(n)。

为什么想到二分：
数组有序时，查看中间元素就能排除一半区间。如果 nums[mid] 小于 target，target 只可能在右半边；如果 nums[mid] 大于 target，只可能在左半边。

关键变量怎么理解：
- left/right：当前搜索区间的左右边界，这里使用闭区间 [left, right]。
- mid：当前区间中点。
- nums[mid]：决定往左找、往右找，还是直接返回。

用样例走一遍：
nums = [-1,0,3,5,9,12], target = 9。初始 left=0,right=5，mid=2，nums[2]=3，小于 9，所以 left=3。再算 mid=4，nums[4]=9，找到返回 4。

伪代码：
left = 0, right = n - 1
while left <= right：
    mid = left + (right - left) / 2
    如果 nums[mid] == target：返回 mid
    如果 nums[mid] < target：left = mid + 1
    否则 right = mid - 1
返回 -1

新手常见错误：
1. 闭区间写法下循环条件写成 left < right，漏掉最后一个元素。
2. 更新边界写成 left = mid 或 right = mid，可能死循环。
3. mid = (left + right) / 2 在极大数组下可能溢出，推荐 left + (right-left)/2。

复杂度：
每次排除一半区间，时间复杂度 O(log n)。只使用常数变量，空间复杂度 O(1)。

Java 参考实现：
```java
class Solution {
    public int search(int[] nums, int target) {
        int left = 0;
        int right = nums.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] == target) {
                return mid;
            }
            if (nums[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return -1;
    }
}
```',
'数组有序，优先想到二分。',
'每次比较 nums[mid] 和 target，排除一半区间。',
'闭区间写法使用 while (left <= right)，边界更新为 mid + 1 / mid - 1。', 1, NOW(), NOW());

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
(121, 9),
(3, 10),
(15, 1), (15, 11),
(20, 12),
(53, 8),
(56, 13),
(94, 6),
(200, 15),
(704, 14);

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
', '1', 0, 1),
(3, 'abcabcbb
', '3', 1, 1),
(3, 'bbbbb
', '1', 0, 1),
(3, 'pwwkew
', '3', 0, 1),
(15, '6
-1 0 1 2 -1 -4
', '[[-1,-1,2],[-1,0,1]]', 1, 1),
(15, '3
0 1 1
', '[]', 0, 1),
(15, '3
0 0 0
', '[[0,0,0]]', 0, 1),
(20, '()[]{}
', 'true', 1, 1),
(20, '(]
', 'false', 0, 1),
(20, '([)]
', 'false', 0, 1),
(53, '9
-2 1 -3 4 -1 2 1 -5 4
', '6', 1, 1),
(53, '1
1
', '1', 0, 1),
(53, '5
-2 -1 -3 -4 -5
', '-1', 0, 1),
(56, '4
1 3
2 6
8 10
15 18
', '[[1,6],[8,10],[15,18]]', 1, 1),
(56, '2
1 4
4 5
', '[[1,5]]', 0, 1),
(56, '3
1 4
0 2
3 5
', '[[0,5]]', 0, 1),
(94, '5
1 null 2 3 null
', '[1,3,2]', 1, 1),
(94, '0
', '[]', 0, 1),
(94, '3
2 1 3
', '[1,2,3]', 0, 1),
(200, '4 5
11110
11010
11000
00000
', '1', 1, 1),
(200, '4 5
11000
11000
00100
00011
', '3', 0, 1),
(200, '1 1
0
', '0', 0, 1),
(704, '6
-1 0 3 5 9 12
9
', '4', 1, 1),
(704, '6
-1 0 3 5 9 12
2
', '-1', 0, 1),
(704, '1
5
5
', '0', 0, 1);
