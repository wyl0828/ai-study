-- 修复 problem 表 hint 字段中文数据
-- 解决 ERROR 1366 (HY000): Incorrect string value

SET NAMES utf8mb4;

ALTER DATABASE ai_interview_coach
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

ALTER TABLE problem
CONVERT TO CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

UPDATE problem SET
hint_level1 = '思考每个数需要寻找的另一个数是什么，不要只靠双重循环枚举。',
hint_level2 = '可以用 HashMap 记录已经遍历过的数值和下标，重点注意查询和写入的顺序。',
hint_level3 = '遍历 nums 时，先检查 target - nums[i] 是否已经出现；如果出现就输出两个下标，否则再记录当前 nums[i]。'
WHERE id = 101;

UPDATE problem SET
hint_level1 = '异位词的关键是两个字符串中每个字符出现次数完全一致。',
hint_level2 = '字符串只包含小写字母时，可以用长度为 26 的数组替代 HashMap 做字符计数。',
hint_level3 = '先判断长度是否相等；遍历 s 时计数加一，遍历 t 时计数减一，最后检查所有计数是否都为 0。'
WHERE id = 102;

UPDATE problem SET
hint_level1 = '反转链表时要保存下一个节点，否则断开指针后会丢失后续链表。',
hint_level2 = '使用 prev、cur、next 三个指针，循环中逐步改变 cur.next 的方向。',
hint_level3 = '当 cur 不为空时，先保存 next = cur.next，再令 cur.next 指向 prev，然后同步移动 prev 和 cur。'
WHERE id = 103;

UPDATE problem SET
hint_level1 = '考虑从头实现合并逻辑，而不是直接返回 null 或只处理其中一个链表。',
hint_level2 = '使用 dummy 头节点可以简化链表连接过程，注意循环中比较节点值并移动指针。',
hint_level3 = '循环比较 list1 和 list2 当前节点，将较小节点接到结果链表后面；循环结束后连接剩余链表。'
WHERE id = 104;

UPDATE problem SET
hint_level1 = '最大深度可以拆成左右子树的最大深度问题。',
hint_level2 = '递归时空节点深度为 0，非空节点深度等于左右子树较大值加 1。',
hint_level3 = '定义 depth(node)：如果 node 为空返回 0，否则返回 max(depth(node.left), depth(node.right)) + 1。'
WHERE id = 105;

UPDATE problem SET
hint_level1 = '层序遍历需要按层处理，而不是简单地把所有节点放在一起输出。',
hint_level2 = '使用队列保存待访问节点，每一轮只处理当前队列长度对应的一层。',
hint_level3 = '当队列不为空时，记录当前 size，循环 size 次弹出节点并加入本层结果，同时把非空左右孩子入队。'
WHERE id = 106;

UPDATE problem SET
hint_level1 = '到达第 n 阶的方式来自第 n-1 阶和第 n-2 阶。',
hint_level2 = '定义 dp[i] 表示到达第 i 阶的走法数，注意初始化小 n 的情况。',
hint_level3 = '初始化 dp[0] = 1、dp[1] = 1，从 2 到 n 计算 dp[i] = dp[i - 1] + dp[i - 2]。'
WHERE id = 107;

UPDATE problem SET
hint_level1 = '最长递增子序列不要求连续，只要求选择出的元素保持相对顺序且严格递增。',
hint_level2 = 'MVP 中可以先用 O(n^2) 动态规划，定义 dp[i] 为以 nums[i] 结尾的 LIS 长度。',
hint_level3 = '对每个 i，枚举 j < i；如果 nums[j] < nums[i]，用 dp[j] + 1 更新 dp[i]，答案是所有 dp[i] 的最大值。'
WHERE id = 108;

-- 验证
SELECT id, hint_level1 IS NOT NULL AS has_hints FROM problem WHERE id BETWEEN 101 AND 108;
