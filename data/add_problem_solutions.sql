USE ai_interview_coach;

UPDATE problem
SET solution_outline = '方法一：哈希表

解题思路：
两数之和的关键不是枚举所有组合，而是在遍历当前数字时，快速判断它需要的另一个数字是否已经出现。用 HashMap 保存 "已经遍历过的数值 -> 下标"，每到 nums[i]，先查 target - nums[i]，找到就输出两个下标，找不到再把当前数字放入表中。

关键步骤：
1. 创建 HashMap<Integer, Integer> 保存数值和下标。
2. 从左到右遍历数组。
3. 对当前 nums[i] 计算 complement = target - nums[i]。
4. 如果 complement 已经在 map 中，输出 map.get(complement) 和 i。
5. 如果没有找到，再记录 nums[i] 和 i。

易错点：
不要先把当前数字放入 map 再查找，否则 target = 2 * nums[i] 时可能把同一个元素用两次。

复杂度：
时间复杂度：O(n)
空间复杂度：O(n)

Java 参考实现：
```java
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] nums = new int[n];
        for (int i = 0; i < n; i++) {
            nums[i] = sc.nextInt();
        }
        int target = sc.nextInt();

        Map<Integer, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int complement = target - nums[i];
            if (indexMap.containsKey(complement)) {
                System.out.println(indexMap.get(complement) + " " + i);
                return;
            }
            indexMap.put(nums[i], i);
        }

        System.out.println("-1 -1");
    }
}
```'
WHERE id = 101;

UPDATE problem
SET solution_outline = '方法一：计数数组

解题思路：
字母异位词要求两个字符串长度相同，并且每个字符出现次数完全一致。由于题目只包含小写字母，可以用长度为 26 的数组统计频次：遍历 s 时加一，遍历 t 时减一，最后所有位置都应该回到 0。

关键步骤：
1. 如果 s 和 t 长度不同，直接返回 false。
2. 创建 int[26] 作为字符频次数组。
3. 同一轮循环中对 s.charAt(i) 计数加一，对 t.charAt(i) 计数减一。
4. 遍历计数数组，存在非 0 值说明字符频次不同。

易错点：
只比较字符串长度不够；只用 contains 或排序外的局部判断，也无法保证每个字符次数一致。

复杂度：
时间复杂度：O(n)
空间复杂度：O(1)

Java 参考实现：
```java
class Solution {
    public boolean isAnagram(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        int[] counts = new int[26];
        for (int i = 0; i < s.length(); i++) {
            counts[s.charAt(i) - ''a'']++;
            counts[t.charAt(i) - ''a'']--;
        }

        for (int count : counts) {
            if (count != 0) {
                return false;
            }
        }
        return true;
    }
}
```'
WHERE id = 102;

UPDATE problem
SET solution_outline = '方法一：迭代反转

解题思路：
反转链表时，核心动作是把当前节点的 next 指针改为指向前一个节点。为了不丢失后续链表，每次改指针前都要先保存 cur.next。

关键步骤：
1. prev 初始化为 null，cur 初始化为 head。
2. 循环中先保存 next = cur.next。
3. 令 cur.next = prev，完成当前节点反向连接。
4. prev 和 cur 同步向后移动。
5. 循环结束时 prev 就是新的头节点。

易错点：
如果先改 cur.next 再保存 next，原链表后半段会丢失；返回值也应该是 prev，不是原来的 head。

复杂度：
时间复杂度：O(n)
空间复杂度：O(1)

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
```'
WHERE id = 103;

UPDATE problem
SET solution_outline = '方法一：迭代合并

解题思路：
合并两个有序链表可以使用哨兵节点简化边界处理。每次比较 list1 和 list2 当前节点，把较小的节点接到结果链表后面，然后移动对应链表指针。

关键步骤：
1. 创建 dummy 节点和 tail 指针。
2. 当两个链表都不为空时，比较当前节点值。
3. 把较小节点接到 tail.next，并移动该链表指针。
4. tail 向后移动一位。
5. 循环结束后，把非空的剩余链表整体接到 tail.next。

易错点：
不要每次新建节点复制值，直接复用原节点即可；循环结束后别忘了连接剩余链表。

复杂度：
时间复杂度：O(n + m)
空间复杂度：O(1)

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
```'
WHERE id = 104;
