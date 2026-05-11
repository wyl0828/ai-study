USE ai_interview_coach;

INSERT INTO `user` (id, username, password_hash, email, created_at, updated_at) VALUES
(1, 'demo', 'demo-only-not-for-production', 'demo@example.com', NOW(), NOW());

INSERT INTO knowledge_point (id, name, category, description) VALUES
(1, '数组遍历', 'Array', '遍历数组并处理下标边界。'),
(2, 'HashMap 基础查找', 'HashMap', '使用 HashMap 完成互补值查找和成员判断。'),
(3, '字符计数', 'HashMap', '使用数组或 HashMap 统计字符出现次数。'),
(4, '链表指针', 'LinkedList', '安全移动和重连链表指针。'),
(5, '二叉树递归', 'Tree', '使用递归处理二叉树，并写清楚终止条件。'),
(6, '二叉树层序遍历', 'Tree', '使用队列完成二叉树按层遍历。'),
(7, '动态规划状态', 'DynamicProgramming', '定义状态、初始化和状态转移。'),
(8, '二分优化', 'DynamicProgramming', '用二分维护递增序列的候选结尾。');

INSERT INTO problem
(id, title, description, difficulty, category, input_format, output_format, code_mode, template_code, solution_outline, hint_level1, hint_level2, hint_level3, enabled, created_at, updated_at)
VALUES
(101, '两数之和',
'给定一个整数数组和一个目标值，请找出数组中和为目标值的两个元素下标。若不存在满足条件的组合，输出 -1 -1。',
'EASY', 'HashMap',
'第 1 行输入数组长度 n，第 2 行输入 n 个整数，第 3 行输入目标值 target。',
'输出两个下标，用空格分隔；若不存在答案，输出 -1 -1。',
'acm',
'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] nums = new int[n];
        for (int i = 0; i < n; i++) {
            nums[i] = sc.nextInt();
        }
        int target = sc.nextInt();

        // TODO: 输出两个下标，例如：System.out.println(i + " " + j);
        System.out.println("-1 -1");
    }
}',
'使用 HashMap 记录数值到下标的映射。每遍历一个元素，先检查 target - nums[i] 是否已经出现，再插入当前元素。',
'思考每个数需要寻找的另一个数是什么，不要只靠双重循环枚举。',
'可以用 HashMap 记录已经遍历过的数值和下标，重点注意查询和写入的顺序。',
'遍历 nums 时，先检查 target - nums[i] 是否已经出现；如果出现就输出两个下标，否则再记录当前 nums[i]。',
1, NOW(), NOW()),
(102, '有效字母异位词',
'给定两个只包含小写字母的字符串 s 和 t，判断 t 是否是 s 的字母异位词。',
'EASY', 'HashMap',
'给定参数 s 和 t，无需处理标准输入。',
'返回 true 或 false，无需自行打印结果。',
'solution',
'class Solution {
    public boolean isAnagram(String s, String t) {
        // 请在这里实现判断逻辑
        return false;
    }
}',
'统计两个字符串的字符出现次数，也可以遍历 s 时加一、遍历 t 时减一。',
'异位词的关键是两个字符串中每个字符出现次数完全一致。',
'字符串只包含小写字母时，可以用长度为 26 的数组替代 HashMap 做字符计数。',
'先判断长度是否相等；遍历 s 时计数加一，遍历 t 时计数减一，最后检查所有计数是否都为 0。',
1, NOW(), NOW()),
(103, '反转链表',
'给定一个单链表的头节点 head，请反转链表，并返回反转后的头节点。',
'EASY', 'LinkedList',
'给定参数 head，无需处理标准输入。',
'返回反转后的链表头节点，无需自行打印结果。',
'solution',
'class Solution {
    public ListNode reverseList(ListNode head) {
        // 请在这里实现反转链表逻辑
        return null;
    }
}',
'使用 prev、cur、next 三个指针。移动 cur 的同时反转 cur.next 指向。',
'反转链表时要保存下一个节点，否则断开指针后会丢失后续链表。',
'使用 prev、cur、next 三个指针，循环中逐步改变 cur.next 的方向。',
'当 cur 不为空时，先保存 next = cur.next，再令 cur.next 指向 prev，然后同步移动 prev 和 cur。',
1, NOW(), NOW()),
(104, '合并两个有序链表',
'给定两个升序链表 list1 和 list2，请合并为一个升序链表并返回合并后的头节点。',
'EASY', 'LinkedList',
'给定参数 list1 和 list2，无需处理标准输入。',
'返回合并后的链表头节点，无需自行打印结果。',
'solution',
'class Solution {
    public ListNode mergeTwoLists(ListNode list1, ListNode list2) {
        // 请在这里实现合并逻辑
        return null;
    }
}',
'使用哨兵节点，每次连接当前值更小的节点，并继续移动指针。',
'考虑从头实现合并逻辑，而不是直接返回 null 或只处理其中一个链表。',
'使用 dummy 头节点可以简化链表连接过程，注意循环中比较节点值并移动指针。',
'循环比较 list1 和 list2 当前节点，将较小节点接到结果链表后面；循环结束后连接剩余链表。',
1, NOW(), NOW()),
(105, '二叉树的最大深度',
'给定一棵按层序表示的二叉树，请计算它的最大深度。其中 null 表示空节点。',
'EASY', 'Tree',
'第 1 行输入节点数量 n，第 2 行输入 n 个层序节点，例如 3 9 20 null null 15 7。',
'输出最大深度，结果为一个整数。',
'acm',
'import java.util.*;

public class Main {
    static class TreeNode {
        String val;
        TreeNode left;
        TreeNode right;
        TreeNode(String val) { this.val = val; }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        String[] values = new String[n];
        for (int i = 0; i < n; i++) values[i] = sc.next();

        // TODO: 构建二叉树并输出最大深度。
    }
}',
'先按层构建二叉树，再用左右子树深度较大值加一，空节点深度为 0。',
'最大深度可以拆成左右子树的最大深度问题。',
'递归时空节点深度为 0，非空节点深度等于左右子树较大值加 1。',
'定义 depth(node)：如果 node 为空返回 0，否则返回 max(depth(node.left), depth(node.right)) + 1。',
1, NOW(), NOW()),
(106, '二叉树层序遍历',
'给定一棵按层序表示的二叉树，请从上到下按层输出每一层的节点值。',
'MEDIUM', 'Tree',
'第 1 行输入节点数量 n，第 2 行输入 n 个层序节点，其中 null 表示空节点。',
'按层输出节点值，层与层之间用分号分隔，同一层内用空格分隔。',
'acm',
'import java.util.*;

public class Main {
    static class TreeNode {
        String val;
        TreeNode left;
        TreeNode right;
        TreeNode(String val) { this.val = val; }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        String[] values = new String[n];
        for (int i = 0; i < n; i++) values[i] = sc.next();

        // TODO: 构建二叉树，按层遍历并输出每一层。
    }
}',
'使用队列。每一层只处理当前队列长度对应的节点，再进入下一层。',
'层序遍历需要按层处理，而不是简单地把所有节点放在一起输出。',
'使用队列保存待访问节点，每一轮只处理当前队列长度对应的一层。',
'当队列不为空时，记录当前 size，循环 size 次弹出节点并加入本层结果，同时把非空左右孩子入队。',
1, NOW(), NOW()),
(107, '爬楼梯',
'每次可以爬 1 个或 2 个台阶，计算到达第 n 阶共有多少种不同走法。',
'EASY', 'DynamicProgramming',
'第 1 行输入台阶数 n。',
'输出不同走法数量。',
'acm',
'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();

        // TODO: 使用动态规划计算走法数量。
        System.out.println(0);
    }
}',
'令 dp[i] 表示到达第 i 阶的走法数，初始化 dp[0] = 1、dp[1] = 1。',
'到达第 n 阶的方式来自第 n-1 阶和第 n-2 阶。',
'定义 dp[i] 表示到达第 i 阶的走法数，注意初始化小 n 的情况。',
'初始化 dp[0] = 1、dp[1] = 1，从 2 到 n 计算 dp[i] = dp[i - 1] + dp[i - 2]。',
1, NOW(), NOW()),
(108, '最长递增子序列',
'给定一个整数数组，返回其中最长严格递增子序列的长度。',
'MEDIUM', 'DynamicProgramming',
'第 1 行输入数组长度 n，第 2 行输入 n 个整数。',
'输出最长递增子序列的长度。',
'acm',
'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] nums = new int[n];
        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();

        // TODO: 对当前用例，使用 O(n^2) 动态规划即可。
        System.out.println(0);
    }
}',
'令 dp[i] 表示以第 i 个元素结尾的最长递增子序列长度，也可以用二分维护候选结尾。',
'最长递增子序列不要求连续，只要求选择出的元素保持相对顺序且严格递增。',
'MVP 中可以先用 O(n^2) 动态规划，定义 dp[i] 为以 nums[i] 结尾的 LIS 长度。',
'对每个 i，枚举 j < i；如果 nums[j] < nums[i]，用 dp[j] + 1 更新 dp[i]，答案是所有 dp[i] 的最大值。',
1, NOW(), NOW());

INSERT INTO problem_knowledge_point (problem_id, knowledge_point_id) VALUES
(101, 1), (101, 2),
(102, 3),
(103, 4),
(104, 4),
(105, 5),
(106, 6),
(107, 7),
(108, 7), (108, 8);

INSERT INTO test_case (problem_id, input_data, expected_output, is_sample, weight) VALUES
(101, '4
2 7 11 15
9
', '0 1', 1, 1),
(101, '3
3 2 4
6
', '1 2', 0, 1),
(101, '2
3 3
6
', '0 1', 0, 1),
(102, 'anagram
nagaram
', 'true', 1, 1),
(102, 'rat
car
', 'false', 0, 1),
(102, 'aacc
ccac
', 'false', 0, 1),
(103, '5
1 2 3 4 5
', '5 4 3 2 1', 1, 1),
(103, '2
1 2
', '2 1', 1, 1),
(103, '0
', '', 1, 1),
(104, '3
1 2 4
3
1 3 4
', '1 1 2 3 4 4', 1, 1),
(104, '0
0
', '', 0, 1),
(104, '2
2 5
3
1 3 4
', '1 2 3 4 5', 0, 1),
(105, '7
3 9 20 null null 15 7
', '3', 1, 1),
(105, '2
1 null
', '1', 0, 1),
(105, '0
', '0', 0, 1),
(106, '7
3 9 20 null null 15 7
', '3;9 20;15 7', 1, 1),
(106, '1
1
', '1', 0, 1),
(106, '0
', '', 0, 1),
(107, '2
', '2', 1, 1),
(107, '3
', '3', 0, 1),
(107, '5
', '8', 0, 1),
(108, '8
10 9 2 5 3 7 101 18
', '4', 1, 1),
(108, '6
0 1 0 3 2 3
', '4', 0, 1),
(108, '7
7 7 7 7 7 7 7
', '1', 0, 1);
